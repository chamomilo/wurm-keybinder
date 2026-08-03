package org.keybinder.wurm.command;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.renderer.PickableUnit;
import com.wurmonline.client.renderer.cell.GroundItemCellRenderable;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.gui.PaperDollSlot;
import com.wurmonline.shared.constants.PlayerAction;
import com.wurmonline.shared.util.MaterialUtilities;
import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.integration.ClientAccess;
import org.keybinder.wurm.integration.ExecutionOriginGuard;
import org.keybinder.wurm.model.SmartImproveStep;
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
    private final ImproveResourceResolver resources = new ImproveResourceResolver();
    private final ImproveMaterialCompatibilityTable table =
            new ImproveMaterialCompatibilityTable();
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
                if (item.resource == null) continue;
                logSelection(item.resource, item.targetName);
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
        if (!inventoryMetadata) return prepareWorld(step.getTarget(), hud, budget);

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
                ? inventoryCandidate(access.playerInventoryRoot(hud), excludedTargets)
                : null;
        List<PreparedItem> result = new ArrayList<PreparedItem>(count);
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
                source = resolveSource(hud, inventory, requirement, excludedTargets);
                if (source == null)
                    throw new StepUnavailableException(Messages.text(
                            "improve.exact_resource_missing",
                            requirement.getMissingResourceLabel(),
                            displayName(target)));
            }
            result.add(new PreparedItem(target.getId(), displayName(target),
                    repairs[i], source, false));
            queueCost += costs[i];
        }
        return new PreparedBatch(result, queueCost);
    }

    private PreparedBatch prepareWorld(TargetSpec requested, HeadsUpDisplay hud,
                                       int budget)
            throws ReflectiveOperationException {
        GroundItemCellRenderable target = selectedWorldTarget(requested, hud);
        if (target == null)
            throw new StepUnavailableException(
                    Messages.text("improve.world_target_required"));

        WorldImproveTracker.Snapshot state = world.snapshot(target.getId());
        if (state == null)
            throw new StepUnavailableException(
                    Messages.text("improve.world_examine_required"));

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
            ImproveResourceCandidate inventory = inventoryCandidate(
                    access.playerInventoryRoot(hud), excluded);
            source = resolveSource(hud, inventory, requirement, excluded);
            if (source == null)
                throw new StepUnavailableException(Messages.text(
                        "improve.exact_resource_missing",
                        requirement.getMissingResourceLabel(), targetName));
        }
        List<PreparedItem> result = new ArrayList<PreparedItem>(1);
        result.add(new PreparedItem(target.getId(), targetName,
                state.isDamaged(), source, true));
        return new PreparedBatch(result, cost);
    }

    private GroundItemCellRenderable selectedWorldTarget(TargetSpec requested,
                                                         HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        PickableUnit selected = access.selected(hud.getSelectBar());
        if (!(selected instanceof GroundItemCellRenderable)) return null;
        long selectedId = selected.getId();
        if (requested.getKind() == TargetKind.SELECTED) {
            return (GroundItemCellRenderable) selected;
        }
        if (requested.getKind() == TargetKind.EXACT_OBJECT) {
            return requested.getObjectId() == selectedId
                    ? (GroundItemCellRenderable) selected : null;
        }
        if (requested.getKind() != TargetKind.HOVER) return null;
        PickableUnit hovered = hud.getWorld().getCurrentHoveredObject();
        return hovered != null && hovered.getId() == selectedId
                ? (GroundItemCellRenderable) selected : null;
    }

    static boolean worldTemperatureReady(byte material, String displayName) {
        return !MaterialUtilities.isMetal(material)
                || (displayName != null
                && displayName.toLowerCase(java.util.Locale.ENGLISH)
                .contains("(glowing)"));
    }

    private ResolvedImproveResource resolveSource(HeadsUpDisplay hud,
                                                   ImproveResourceCandidate inventory,
                                                   ResourceRequirement requirement,
                                                   Set<Long> excludedTargets) {
        if (requirement.getFamily() == RequirementFamily.BODY_HAND) {
            InventoryMetaItem hand = hud.getPaperDollInventory().getHandItem();
            ImproveResourceCandidate builtIn = candidate(hand, excludedTargets,
                    new HashSet<Long>(), false);
            if (builtIn == null || !requirement.match(builtIn).isAccepted()) return null;
            return new ResolvedImproveResource(builtIn, null, requirement);
        }
        // Candidate rejection details remain silent normally, but become visible
        // when the existing Debug logging setting is enabled. This makes server-
        // specific inventory names diagnosable without adding normal Event spam.
        return resources.resolve(inventory, requirement, log::debug);
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
        return candidate(root, excluded, new HashSet<Long>(), true);
    }

    private static ImproveResourceCandidate candidate(InventoryMetaItem item,
                                                       Set<Long> excluded,
                                                       Set<Long> visited,
                                                       boolean descendChildren) {
        if (item == null || excluded.contains(item.getId())
                || !visited.add(item.getId())) return null;
        List<ImproveResourceCandidate> children = new ArrayList<ImproveResourceCandidate>();
        if (descendChildren && item.getChildren() != null)
            for (InventoryMetaItem child : item.getChildren()) {
                ImproveResourceCandidate value = candidate(child, excluded, visited,
                        true);
                if (value != null) children.add(value);
            }
        return new ImproveResourceCandidate(item.getId(), item.getBaseName(),
                item.getDisplayName(), item.getMaterialId(), item.getType(),
                item.getR(), item.getG(), item.getB(), item.getTemperature(), children);
    }

    private void logSelection(ResolvedImproveResource selection, String itemName) {
        ImproveResourceCandidate candidate = selection.getCandidate();
        String name = candidate.getDisplayName().isEmpty()
                ? candidate.getBaseName() : candidate.getDisplayName();
        if (selection.getRequirement().getFamily() == RequirementFamily.BODY_HAND)
            log.info(Messages.text("improve.using_built_in", name, itemName));
        else if (selection.isNested())
            log.info(Messages.text("improve.using_from_inventory_container", name,
                    selection.getContainerName(), itemName));
        else
            log.info(Messages.text("improve.using_inventory", name, itemName));
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
        private final boolean repair;
        private final ResolvedImproveResource resource;
        private final boolean world;

        private PreparedItem(long targetId, String targetName, boolean repair,
                             ResolvedImproveResource resource, boolean world) {
            this.targetId = targetId;
            this.targetName = targetName;
            this.repair = repair;
            this.resource = resource;
            this.world = world;
        }
    }
}
