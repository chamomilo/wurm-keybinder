package org.keybinder.wurm.command;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.game.SkillLogic;
import com.wurmonline.client.game.SkillLogicSet;
import com.wurmonline.client.renderer.PickableUnit;
import com.wurmonline.client.renderer.SubPickableUnit;
import com.wurmonline.client.renderer.cell.GroundItemCellRenderable;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.gui.PaperDollSlot;
import com.wurmonline.shared.constants.PlayerAction;
import com.wurmonline.shared.util.MaterialUtilities;
import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.integration.ClientAccess;
import org.keybinder.wurm.integration.CreationSkillRegistry;
import org.keybinder.wurm.integration.ExecutionOriginGuard;
import org.keybinder.wurm.integration.ExecutionHoverOverride;
import org.keybinder.wurm.model.SmartImproveStep;
import org.keybinder.wurm.model.SmartImproveSourceMode;
import org.keybinder.wurm.model.TargetKind;
import org.keybinder.wurm.model.TargetSpec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.keybinder.wurm.command.SmartImproveInventoryPolicy.isImprovable;
import static org.keybinder.wurm.command.SmartImproveInventoryPolicy.isTargetTemperatureReady;
import static org.keybinder.wurm.command.SmartImproveInventoryPolicy.needsRepair;
import static org.keybinder.wurm.command.SmartImproveInventoryPolicy.orderedTargets;

/**
 * Smart Improve executor. Inventory targets use their live metadata directly;
 * one selected world item may use metadata captured from the player's ordinary
 * Examine. Both paths resolve resources through the same strict table and never
 * lock or wait for an Improve response.
 */
public final class SmartImproveExecutor {
    private final ClientAccess access;
    private final EventLogger log;
    private final WorldImproveTracker world;
    private final ImproveSourceResolver resources = new ImproveSourceResolver();
    private final ImproveMaterialCompatibilityTable table =
            new ImproveMaterialCompatibilityTable();
    private final ImproveSuccessChanceEstimator chanceEstimator =
            new ImproveSuccessChanceEstimator();
    private final ImproveRarityChanceEstimator rarityChanceEstimator =
            new ImproveRarityChanceEstimator();
    private HeadsUpDisplay cachedSkillCatalogHud;
    private long cachedSkillCatalogRevision = Long.MIN_VALUE;
    private ImproveSkillCatalog cachedSkillCatalog;
    private final ThreadLocal<Map<SmartImproveStep, PreparedBatch>> prepared =
            new ThreadLocal<Map<SmartImproveStep, PreparedBatch>>() {
                @Override protected Map<SmartImproveStep, PreparedBatch> initialValue() {
                    return new IdentityHashMap<SmartImproveStep, PreparedBatch>();
                }
            };

    public SmartImproveExecutor(ClientAccess access, EventLogger log) {
        this(access, log, new WorldImproveTracker());
    }

    public SmartImproveExecutor(ClientAccess access, EventLogger log,
                                WorldImproveTracker world) {
        this.access = access;
        this.log = log;
        this.world = world;
    }

    public int runtimeCost(SmartImproveStep step, HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        return prepare(step, hud, Integer.MAX_VALUE).queueCost;
    }

    int prepareWithinBudget(SmartImproveStep step, HeadsUpDisplay hud, int budget)
            throws ReflectiveOperationException {
        PreparedBatch batch = prepare(step, hud, budget);
        prepared.get().put(step, batch);
        return batch.queueCost;
    }

    int prepareWithinBudget(SmartImproveStep step, HeadsUpDisplay hud, int budget,
                            int queueLimit, java.util.function.IntSupplier occupied)
            throws ReflectiveOperationException {
        return prepareWithinBudget(step, hud, budget);
    }

    public void execute(SmartImproveStep step, HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        PreparedBatch batch = prepared.get().remove(step);
        if (batch == null) batch = prepare(step, hud, Integer.MAX_VALUE);
        ExecutionOriginGuard.enterInternal();
        try {
            for (PreparedItem item : batch.items) {
                if (item.repair) {
                    hud.sendAction(PlayerAction.REPAIR, item.targetId);
                    if (item.world) world.repaired(item.targetId);
                }
                if (item.unavailableReason != null) {
                    log.info(item.unavailableReason);
                    continue;
                }
                if (item.resource == null) continue;
                logSelection(item.resource, item.targetName, item.targetQuality,
                        item.successChance, item.rarityChance, item.rarityLabel);
                hud.getWorld().getServerConnection().sendAction(
                        item.resource.getCandidate().getId(),
                        new long[]{item.targetId}, PlayerAction.IMPROVE);
            }
        } finally {
            ExecutionOriginGuard.exitInternal();
        }
    }

    void clearPrepared() { prepared.remove(); }

    private PreparedBatch prepare(SmartImproveStep step, HeadsUpDisplay hud, int budget)
            throws ReflectiveOperationException {
        List<InventoryMetaItem> targets = inventoryTargets(step.getTarget(), hud);
        boolean inventoryMetadata = !targets.isEmpty();
        for (InventoryMetaItem target : targets)
            inventoryMetadata &= hasLocalImproveMetadata(target);
        if (!inventoryMetadata) return prepareWorld(step, hud, budget);

        int[] costs = new int[targets.size()];
        boolean[] repairs = new boolean[targets.size()];
        boolean[] improvements = new boolean[targets.size()];
        for (int i = 0; i < targets.size(); i++) {
            InventoryMetaItem target = targets.get(i);
            requireImprovable(target);
            repairs[i] = needsRepair(target);
            improvements[i] = isTargetTemperatureReady(target);
            costs[i] = SmartImproveQueuePlanner.itemCost(
                    repairs[i], improvements[i]);
        }

        int count = SmartImproveQueuePlanner.fittedPrefixLength(budget, costs);
        Set<Long> excludedTargets = new HashSet<Long>();
        boolean needsSource = false;
        for (int i = 0; i < count; i++) {
            excludedTargets.add(targets.get(i).getId());
            needsSource |= improvements[i];
        }

        ImproveResourceCandidate inventory = needsSource
                && step.getSourceMode() == SmartImproveSourceMode.TOOLBELT_THEN_INVENTORY
                ? InventoryResourceScanner.inventoryCandidate(
                        access.playerInventoryRoot(hud), excludedTargets)
                : null;
        List<PreparedItem> result = new ArrayList<PreparedItem>(count);
        ImproveSkillCatalog skillCatalog = improveSkillCatalog(hud);
        int queueCost = 0;
        for (int i = 0; i < count; i++) {
            InventoryMetaItem target = targets.get(i);
            ResolvedImproveResource source = null;
            if (improvements[i]) {
                ResourceRequirement requirement;
                try {
                    requirement = table.resolve(target.getImproveIconId(),
                            target.getMaterialId(), access.objectType(target));
                } catch (UnsupportedImproveMaterialException unsupported) {
                    throw new StepUnavailableException(unsupported.getMessage());
                }
                source = resolveSource(hud, inventory, requirement, excludedTargets,
                        step.getSourceMode());
                if (source == null)
                    throw new StepUnavailableException(Messages.text(
                            "improve.exact_resource_missing",
                            requirement.getMissingResourceLabel(),
                            displayName(target), sourceModeLabel(step.getSourceMode())));
            }
            Integer successChance = source == null ? null
                    : estimateSuccessChance(hud, skillCatalog, target, source);
            ImproveRarityChanceEstimator.Range rarityChance =
                    estimateRarityChance(successChance, target.getRarity(),
                            examinedRarityRuneModifier(target.getId()), source);
            String unavailableReason = improvements[i] ? null : Messages.text(
                    "improve.target_too_cold", displayName(target));
            result.add(new PreparedItem(target.getId(), displayName(target),
                    (double) target.getQuality(), repairs[i], source, successChance,
                    rarityChance, rarityLabel(target.getRarity()),
                    unavailableReason, false));
            queueCost += costs[i];
        }
        return new PreparedBatch(result, queueCost);
    }

    private PreparedBatch prepareWorld(SmartImproveStep step, HeadsUpDisplay hud,
                                       int budget)
            throws ReflectiveOperationException {
        TargetSpec requested = step.getTarget();
        WorldImproveTracker.Snapshot state = world.currentSnapshot();
        if (state == null)
            throw new StepUnavailableException(
                    Messages.text("improve.world_examine_required"));
        PickableUnit target = examinedWorldTarget(
                requested, hud, state.getTargetId());
        if (target == null) {
            logWorldResolutionFailure(requested, hud, state.getTargetId());
            throw new StepUnavailableException(
                    Messages.text("improve.world_target_required"));
        }
        logWorldResolutionSuccess(requested, hud, state.getTargetId(), target);

        byte material = access.materialId(target);
        String targetType = access.objectType(target);
        String targetName = target.getHoverName();
        if (targetName == null || targetName.trim().isEmpty()) targetName = targetType;
        boolean improve = worldTemperatureReady(material, targetName);
        int cost = SmartImproveQueuePlanner.itemCost(state.isDamaged(), improve);
        if (SmartImproveQueuePlanner.fittedPrefixLength(budget, cost) == 0)
            return new PreparedBatch(new ArrayList<PreparedItem>(), 0);

        ResolvedImproveResource source = null;
        if (improve) {
            ResourceRequirement requirement;
            try {
                requirement = table.resolve(state.getRequirement(), material, targetType);
            } catch (UnsupportedImproveMaterialException unsupported) {
                throw new StepUnavailableException(unsupported.getMessage());
            }
            Set<Long> excluded = new HashSet<Long>();
            excluded.add(target.getId());
            ImproveResourceCandidate inventory = step.getSourceMode()
                    == SmartImproveSourceMode.TOOLBELT_THEN_INVENTORY
                    ? InventoryResourceScanner.inventoryCandidate(
                            access.playerInventoryRoot(hud), excluded) : null;
            source = resolveSource(hud, inventory, requirement, excluded,
                    step.getSourceMode());
            if (source == null)
                throw new StepUnavailableException(Messages.text(
                        "improve.exact_resource_missing",
                        requirement.getMissingResourceLabel(), targetName,
                        sourceModeLabel(step.getSourceMode())));
        }
        List<PreparedItem> result = new ArrayList<PreparedItem>(1);
        Integer successChance = source == null || state.getQuality() == null ? null
                : estimateSuccessChance(hud, improveSkillCatalog(hud), targetType,
                state.getExamineText(), state.getQuality(), source);
        ImproveRarityChanceEstimator.Range rarityChance = estimateRarityChance(
                successChance, state.getRarity(), state.getRarityRuneModifier(),
                source);
        String unavailableReason = improve ? null : Messages.text(
                "improve.target_too_cold", targetName);
        result.add(new PreparedItem(state.getTargetId(), targetName,
                state.getQuality() == null ? null
                        : state.getQuality().doubleValue(),
                state.isDamaged(), source, successChance,
                rarityChance, rarityLabel(state.getRarity()),
                unavailableReason, true));
        return new PreparedBatch(result, cost);
    }

    private PickableUnit examinedWorldTarget(TargetSpec requested,
                                             HeadsUpDisplay hud,
                                             long examinedId)
            throws ReflectiveOperationException {
        PickableUnit selected = access.selected(hud.getSelectBar());
        PickableUnit hovered = hud.getWorld().getCurrentHoveredObject();
        PickableUnit resolved = worldObject(examinedId, selected, hovered, hud);
        if (resolved == null) return null;
        if (requested.getKind() == TargetKind.SELECTED) {
            return selected != null && selected.getId() == examinedId ? resolved : null;
        }
        if (requested.getKind() == TargetKind.EXACT_OBJECT) {
            return requested.getObjectId() == examinedId ? resolved : null;
        }
        if (requested.getKind() != TargetKind.HOVER) return null;
        ExecutionHoverOverride.Snapshot override = ExecutionHoverOverride.current();
        if (override != null && (override.getWorldObjectId() == examinedId
                || override.getWorldObjectId() == resolved.getId())) return resolved;
        com.wurmonline.client.WurmClientBase client = hud.getWorld().getClient();
        long[] commandTargets = hud.getCommandTargetsFrom(
                client.getXMouse(), client.getYMouse());
        if (containsId(commandTargets, examinedId)) return resolved;
        GroundItemCellRenderable hoveredGround = unwrapGround(hovered);
        if (hovered != null && (hovered.getId() == examinedId
                || sameGround(hoveredGround, unwrapGround(resolved)))) return resolved;
        // Hitched vehicles can remain the exact SelectBar object while their
        // composite 3D pick target no longer reports the vehicle ID.
        GroundItemCellRenderable selectedGround = unwrapGround(selected);
        return selected != null && (selected.getId() == examinedId
                || sameGround(selectedGround, unwrapGround(resolved))) ? resolved : null;
    }

    private PickableUnit worldObject(long id, PickableUnit selected,
                                     PickableUnit hovered, HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        PickableUnit exact = exactWorldObject(id, selected, hovered);
        if (exact != null) return exact;
        GroundItemCellRenderable selectedGround = unwrapGround(selected);
        if (selectedGround != null && (selected.getId() == id
                || selectedGround.getId() == id)) return selectedGround;
        GroundItemCellRenderable hoveredGround = unwrapGround(hovered);
        if (hoveredGround != null && (hovered.getId() == id
                || hoveredGround.getId() == id)) return hoveredGround;
        com.wurmonline.client.comm.ServerConnectionListenerClass listener =
                hud.getWorld().getServerConnection().getServerConnectionListener();
        GroundItemCellRenderable ground = access.groundItems(listener).get(id);
        if (ground != null) return ground;
        return listener.getCreatures().get(id);
    }

    static PickableUnit exactWorldObject(long id, PickableUnit selected,
                                         PickableUnit hovered) {
        if (selected != null && selected.getId() == id) return selected;
        return hovered != null && hovered.getId() == id ? hovered : null;
    }

    static GroundItemCellRenderable unwrapGround(PickableUnit unit) {
        PickableUnit current = unit;
        int depth = 0;
        while (current instanceof SubPickableUnit && depth++ < 8)
            current = ((SubPickableUnit) current).getParent();
        return current instanceof GroundItemCellRenderable
                ? (GroundItemCellRenderable) current : null;
    }

    private static boolean sameGround(GroundItemCellRenderable left,
                                      GroundItemCellRenderable right) {
        return left != null && right != null
                && (left == right || left.getId() == right.getId());
    }

    private void logWorldResolutionFailure(TargetSpec requested,
                                           HeadsUpDisplay hud,
                                           long examinedId) {
        try {
            PickableUnit selected = access.selected(hud.getSelectBar());
            PickableUnit hovered = hud.getWorld().getCurrentHoveredObject();
            com.wurmonline.client.WurmClientBase client = hud.getWorld().getClient();
            long[] targets = hud.getCommandTargetsFrom(
                    client.getXMouse(), client.getYMouse());
            com.wurmonline.client.comm.ServerConnectionListenerClass listener =
                    hud.getWorld().getServerConnection().getServerConnectionListener();
            GroundItemCellRenderable stored = access.groundItems(listener).get(examinedId);
            PickableUnit creature = listener.getCreatures().get(examinedId);
            log.diagnostic("Smart Improve world target rejected: requested="
                    + requested.getKind() + ", examinedId=" + examinedId
                    + ", selected=" + describePickable(selected)
                    + ", hovered=" + describePickable(hovered)
                    + ", commandTargets=" + java.util.Arrays.toString(targets)
                    + ", groundItemsExact=" + (stored != null)
                    + ", creaturesExact=" + (creature != null));
        } catch (Throwable failure) {
            log.diagnostic("Smart Improve world target diagnostic failed: "
                    + failure.getClass().getName() + ": " + failure.getMessage());
        }
    }

    private void logWorldResolutionSuccess(TargetSpec requested,
                                           HeadsUpDisplay hud,
                                           long examinedId,
                                           PickableUnit resolved) {
        try {
            PickableUnit selected = access.selected(hud.getSelectBar());
            PickableUnit hovered = hud.getWorld().getCurrentHoveredObject();
            com.wurmonline.client.WurmClientBase client = hud.getWorld().getClient();
            long[] targets = hud.getCommandTargetsFrom(
                    client.getXMouse(), client.getYMouse());
            ExecutionHoverOverride.Snapshot override = ExecutionHoverOverride.current();
            log.debug("Smart Improve world target resolved: requested="
                    + requested.getKind() + ", examinedId=" + examinedId
                    + ", resolved=" + describePickable(resolved)
                    + ", selected=" + describePickable(selected)
                    + ", hovered=" + describePickable(hovered)
                    + ", commandTargets=" + java.util.Arrays.toString(targets)
                    + ", overrideId=" + (override == null ? "none"
                    : String.valueOf(override.getWorldObjectId())));
        } catch (Throwable failure) {
            log.diagnostic("Smart Improve world target success diagnostic failed: "
                    + failure.getClass().getName() + ": " + failure.getMessage());
        }
    }

    private static String describePickable(PickableUnit unit) {
        if (unit == null) return "null";
        GroundItemCellRenderable ground = unwrapGround(unit);
        return unit.getClass().getName() + "{id=" + unit.getId()
                + ", name=\"" + unit.getHoverName() + "\", groundId="
                + (ground == null ? "none" : String.valueOf(ground.getId())) + "}";
    }

    static boolean containsId(long[] ids, long expected) {
        if (ids == null) return false;
        for (long id : ids) if (id == expected) return true;
        return false;
    }

    static boolean worldTemperatureReady(byte material, String displayName) {
        return !MaterialUtilities.isMetal(material)
                || (displayName != null
                && displayName.toLowerCase(java.util.Locale.ENGLISH)
                .contains("(glowing)"));
    }

    static String sourceModeLabel(SmartImproveSourceMode sourceMode) {
        return Messages.text(sourceMode == SmartImproveSourceMode.TOOLBELT_ONLY
                ? "improve.source_mode.toolbelt_only"
                : "improve.source_mode.toolbelt_inventory");
    }

    private ResolvedImproveResource resolveSource(HeadsUpDisplay hud,
                                                   ImproveResourceCandidate inventory,
                                                   ResourceRequirement requirement,
                                                   Set<Long> excludedTargets,
                                                   SmartImproveSourceMode sourceMode) {
        List<ImproveResourceCandidate> toolbelt =
                InventoryResourceScanner.toolbeltCandidates(hud, excludedTargets);
        ImproveResourceCandidate builtIn = null;
        if (requirement.getFamily() == RequirementFamily.BODY_HAND) {
            InventoryMetaItem hand = hud.getPaperDollInventory().getHandItem();
            builtIn = InventoryResourceScanner.singleCandidate(hand, excludedTargets);
        }
        // Candidate rejection details remain silent normally, but become visible
        // when the existing Debug logging setting is enabled. This makes server-
        // specific inventory names diagnosable without adding normal Event spam.
        ResolvedImproveResource resolved = resources.resolve(
                toolbelt, inventory, builtIn, requirement,
                sourceMode == SmartImproveSourceMode.TOOLBELT_THEN_INVENTORY,
                log::debug);
        if (resolved == null) {
            log.diagnostic("Smart Improve resource not found: requirement="
                    + requirement + ", sourceMode=" + sourceMode);
        } else {
            ImproveResourceCandidate chosen = resolved.getCandidate();
            log.debug("Smart Improve resource selected: requirement="
                    + requirement + ", id=" + chosen.getId()
                    + ", baseName=\"" + chosen.getBaseName()
                    + "\", displayName=\"" + chosen.getDisplayName()
                    + "\", image=" + chosen.getImageId()
                    + ", material=" + (chosen.getMaterialId() & 0xff)
                    + ", temperature=" + chosen.getTemperature()
                    + ", quality=" + chosen.getQuality()
                    + ", damage=" + chosen.getDamage());
        }
        return resolved;
    }

    private List<InventoryMetaItem> inventoryTargets(TargetSpec target,
                                                     HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        Map<Long, InventoryMetaItem> result = new LinkedHashMap<Long, InventoryMetaItem>();
        if (target.getKind() == TargetKind.ACTIVE_TOOL) {
            addTarget(result, access.activeTool(hud));
        } else if (target.getKind() == TargetKind.TOOLBELT_SLOT) {
            addTarget(result, hud.getToolBelt().getItemInSlot(target.getSlot() - 1));
        } else if (target.getKind() == TargetKind.EQUIPMENT_SLOT) {
            PaperDollSlot frame = access.equipmentSlot(
                    hud.getPaperDollInventory(), (byte) target.getSlot());
            if (frame != null && frame.getEquippedItem() != null)
                addTarget(result, frame.getEquippedItem().getItem());
        } else if (target.getKind() == TargetKind.BODY) {
            addTarget(result, access.bodyItem(hud.getPaperDollInventory()));
        } else if (target.getKind() == TargetKind.EXACT_OBJECT) {
            addTarget(result, access.inventoryItem(hud, target.getObjectId()));
        } else if (target.getKind() == TargetKind.SELECTED) {
            PickableUnit selected = access.selected(hud.getSelectBar());
            if (selected != null) addTarget(result,
                    access.inventoryItem(hud, selected.getId()));
        } else if (target.getKind() == TargetKind.HOVER) {
            com.wurmonline.client.WurmClientBase client = hud.getWorld().getClient();
            long[] ids = hud.getCommandTargetsFrom(client.getXMouse(), client.getYMouse());
            if (ids != null) for (long id : ids)
                addTarget(result, access.inventoryItem(hud, id));
        }
        return orderedTargets(result.values());
    }

    static ImproveResourceCandidate inventoryCandidate(InventoryMetaItem root,
                                                        Set<Long> excluded) {
        return InventoryResourceScanner.inventoryCandidate(root, excluded);
    }

    private void logSelection(ResolvedImproveResource selection, String itemName,
                              Double targetQuality, Integer successChance,
                              ImproveRarityChanceEstimator.Range rarityChance,
                              String rarityLabel) {
        ImproveResourceCandidate candidate = selection.getCandidate();
        String name = candidate.getDisplayName().isEmpty()
                ? candidate.getBaseName() : candidate.getDisplayName();
        String message;
        if (selection.isToolbelt() && selection.isNested())
            message = Messages.text("improve.using_from_toolbelt_container", name,
                    selection.getContainerName(), itemName);
        else if (selection.isToolbelt())
            message = Messages.text("improve.using_toolbelt", name, itemName);
        else if (selection.isBuiltIn())
            message = Messages.text("improve.using_built_in", name, itemName);
        else if (selection.isNested()
                && "backpack".equals(selection.getContainerName()))
            message = Messages.text("improve.using_backpack", name, itemName);
        else if (selection.isNested())
            message = Messages.text("improve.using_from_inventory_container", name,
                    selection.getContainerName(), itemName);
        else
            message = Messages.text("improve.using_inventory", name, itemName);
        log.info(withEstimatedChances(withTargetQuality(message, targetQuality),
                successChance, rarityChance, rarityLabel));
    }

    static String withTargetQuality(String message, Double quality) {
        if (quality == null || quality.isNaN() || quality.isInfinite())
            return message;
        return message + " QL " + String.format(java.util.Locale.ROOT,
                "%.2f", quality);
    }

    static String withEstimatedChances(String message, Integer improveChance,
                                       ImproveRarityChanceEstimator.Range rarityChance,
                                       String rarityLabel) {
        if (improveChance == null) return message;
        StringBuilder result = new StringBuilder(message)
                .append(" (improve chance ~").append(improveChance).append('%');
        if (rarityChance != null && rarityLabel != null) {
            result.append(", improve to ").append(rarityLabel)
                    .append(" chance after drumroll ")
                    .append(formatPercent(rarityChance.getMaximum()))
                    .append('%');
        }
        return result.append(')').toString();
    }

    private static String formatPercent(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private ImproveRarityChanceEstimator.Range estimateRarityChance(
            Integer improveChance, byte targetRarity, float rarityRuneModifier,
            ResolvedImproveResource source) {
        if (improveChance == null || source == null || targetRarity >= 3)
            return null;
        RequirementKind kind = source.getRequirement().getFamily().getKind();
        boolean mayBeConsumed = kind == RequirementKind.CONSUMABLE
                || kind == RequirementKind.WATER;
        return rarityChanceEstimator.estimate(targetRarity,
                rarityRuneModifier, rarityRuneModifier,
                source.getCandidate().getRarity(), mayBeConsumed);
    }

    private float examinedRarityRuneModifier(long targetId) {
        WorldImproveTracker.Snapshot examined = world.currentSnapshot();
        return examined != null && examined.getTargetId() == targetId
                ? examined.getRarityRuneModifier() : 0.0f;
    }

    private static String rarityLabel(byte targetRarity) {
        if (targetRarity <= 0) return "rare";
        if (targetRarity == 1) return "supreme";
        if (targetRarity == 2) return "fantastic";
        return null;
    }

    private ImproveSkillCatalog improveSkillCatalog(HeadsUpDisplay hud) {
        long knownRevision = cachedSkillCatalog != null
                && cachedSkillCatalogHud == hud
                ? cachedSkillCatalogRevision : Long.MIN_VALUE;
        CreationSkillRegistry.Snapshot snapshot =
                CreationSkillRegistry.snapshotAfter(knownRevision);
        if (snapshot == null) return cachedSkillCatalog;

        List<org.keybinder.wurm.catalog.CreationSkillEntry> entries =
                new ArrayList<org.keybinder.wurm.catalog.CreationSkillEntry>(
                        snapshot.getEntries());
        // The receive hook is authoritative. Retain one defensive live-window
        // fallback for clients where that optional hook could not be installed.
        if (entries.isEmpty()) {
            try {
                entries.addAll(access.creationSkills(hud));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
                log.diagnostic("Smart Improve creation-window catalog unavailable: "
                        + unavailable.getClass().getName() + ": "
                        + unavailable.getMessage());
            }
        }
        log.debug("Smart Improve creation-skill catalog contains "
                + entries.size() + " captured/window rows");
        cachedSkillCatalog = new ImproveSkillCatalog(entries);
        cachedSkillCatalogHud = hud;
        cachedSkillCatalogRevision = snapshot.getRevision();
        return cachedSkillCatalog;
    }

    private Integer estimateSuccessChance(HeadsUpDisplay hud,
                                          ImproveSkillCatalog catalog,
                                          InventoryMetaItem target,
                                          ResolvedImproveResource source) {
        return estimateSuccessChance(hud, catalog, access.objectType(target),
                null, target.getQuality(), source);
    }

    private Integer estimateSuccessChance(HeadsUpDisplay hud,
                                          ImproveSkillCatalog catalog,
                                          String targetType,
                                          String examineText,
                                          double targetQuality,
                                          ResolvedImproveResource source) {
        try {
            String skillName = catalog.skillFor(targetType, examineText);
            if (skillName == null) {
                log.diagnostic("Smart Improve chance unavailable: no creation skill for \""
                        + targetType + "\" in " + catalog.size() + " recipe rows");
                return null;
            }
            SkillLogic skill = SkillLogicSet.getSkill(skillName);
            if (skill == null) {
                log.diagnostic("Smart Improve chance unavailable: player skill \""
                        + skillName + "\" is not loaded");
                return null;
            }
            SkillLogic parent = hud.getWorld().getPlayer().getSkillSet()
                    .getParentSkill(skill.getId());
            double parentValue = parent == null || parent.getId() == skill.getId()
                    ? 0.0 : parent.getValue();
            ImproveResourceCandidate candidate = source.getCandidate();
            double actionBonus = improveActionBonus(hud);
            // Rarity and affinity affect gains after the roll, not its sign.
            // InventoryMetaItem exposes no structured source-rune effects, so
            // the displayed result remains an explicitly approximate value.
            int chance;
            if (source.isBuiltIn())
                chance = chanceEstimator.estimateWithoutToolQuality(
                        skill.getValue(), parentValue, targetQuality,
                        hud.getWorld().isServerEpic(), actionBonus);
            else
                chance = chanceEstimator.estimate(skill.getValue(), parentValue,
                        targetQuality, candidate.getQuality(),
                        candidate.getDamage(), hud.getWorld().isServerEpic(),
                        actionBonus);
            log.debug("Smart Improve chance calculated: targetType=\""
                    + targetType + "\", skill=\"" + skillName + "\"="
                    + skill.getValue() + ", parent=" + parentValue
                    + ", targetQL=" + targetQuality + ", sourceQL="
                    + candidate.getQuality() + ", sourceDamage="
                    + candidate.getDamage() + ", actionBonus=" + actionBonus
                    + ", chance=" + chance + "%");
            return chance;
        } catch (RuntimeException | LinkageError unavailable) {
            log.diagnostic("Smart Improve chance unavailable: "
                    + unavailable.getClass().getName() + ": " + unavailable.getMessage());
            return null;
        }
    }

    private static double improveActionBonus(HeadsUpDisplay hud) {
        com.wurmonline.client.game.PlayerObj player = hud.getWorld().getPlayer();
        double bonus = "PRIEST".equals(String.valueOf(
                player.getReligiousDedication())) ? -20.0 : 0.0;
        if (!"VYNORA".equals(String.valueOf(player.getReligion()))) return bonus;
        SkillLogic faith = SkillLogicSet.getSkill("Faith");
        SkillLogic favor = SkillLogicSet.getSkill("Favor");
        return faith != null && favor != null
                && faith.getValue() >= 80.0f && favor.getValue() >= 40.0f
                ? bonus + 10.0 : bonus;
    }

    private static void addTarget(Map<Long, InventoryMetaItem> result,
                                  InventoryMetaItem item) {
        if (item != null) result.put(item.getId(), item);
    }

    /**
     * An opened world container is represented by a synthetic root whose ID is
     * the world object's ID, but whose item type and Improve icon are both -1.
     * Its children have real metadata; the root itself does not.
     */
    static boolean hasLocalImproveMetadata(InventoryMetaItem item) {
        return item != null
                && !(item.getType() == (short) -1
                && item.getImproveIconId() == (short) -1);
    }

    private static void requireImprovable(InventoryMetaItem item) {
        if (!isImprovable(item))
            throw new StepUnavailableException(Messages.text(
                    "improve.cannot_improve", displayName(item)));
    }

    private static String displayName(InventoryMetaItem item) {
        if (item == null) return Messages.text("improve.generic_item");
        String value = item.getDisplayName();
        return value == null || value.trim().isEmpty()
                ? Messages.text("improve.generic_item") : value;
    }

    private static final class PreparedBatch {
        private final List<PreparedItem> items;
        private final int queueCost;

        private PreparedBatch(List<PreparedItem> items, int queueCost) {
            this.items = items;
            this.queueCost = queueCost;
        }
    }

    private static final class PreparedItem {
        private final long targetId;
        private final String targetName;
        private final Double targetQuality;
        private final boolean repair;
        private final ResolvedImproveResource resource;
        private final Integer successChance;
        private final ImproveRarityChanceEstimator.Range rarityChance;
        private final String rarityLabel;
        private final String unavailableReason;
        private final boolean world;

        private PreparedItem(long targetId, String targetName,
                             Double targetQuality, boolean repair,
                             ResolvedImproveResource resource,
                             Integer successChance,
                             ImproveRarityChanceEstimator.Range rarityChance,
                             String rarityLabel, String unavailableReason,
                             boolean world) {
            this.targetId = targetId;
            this.targetName = targetName;
            this.targetQuality = targetQuality;
            this.repair = repair;
            this.resource = resource;
            this.successChance = successChance;
            this.rarityChance = rarityChance;
            this.rarityLabel = rarityLabel;
            this.unavailableReason = unavailableReason;
            this.world = world;
        }
    }
}
