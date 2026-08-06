package org.keybinder.wurm.command;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.renderer.PickableUnit;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.gui.PaperDollSlot;
import com.wurmonline.shared.constants.PlayerAction;
import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.integration.ClientAccess;
import org.keybinder.wurm.integration.ExecutionOriginGuard;
import org.keybinder.wurm.model.ArcheologyIdentifySourceMode;
import org.keybinder.wurm.model.ArcheologyIdentifyStep;
import org.keybinder.wurm.model.TargetKind;
import org.keybinder.wurm.model.TargetSpec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** Resolves and sends an icon-directed batch of archaeology Identify actions. */
public final class ArcheologyIdentifyExecutor {
    interface ActionSender {
        void send(HeadsUpDisplay hud, long sourceId, long targetId);
    }

    private final ClientAccess access;
    private final EventLogger log;
    private final ActionSender sender;
    private final ThreadLocal<Map<ArcheologyIdentifyStep, PreparedBatch>> prepared =
            new ThreadLocal<Map<ArcheologyIdentifyStep, PreparedBatch>>() {
                @Override protected Map<ArcheologyIdentifyStep, PreparedBatch> initialValue() {
                    return new IdentityHashMap<ArcheologyIdentifyStep, PreparedBatch>();
                }
            };

    public ArcheologyIdentifyExecutor(ClientAccess access, EventLogger log) {
        this(access, log, new ActionSender() {
            @Override public void send(HeadsUpDisplay hud, long sourceId, long targetId) {
                hud.getWorld().getServerConnection().sendAction(sourceId,
                        new long[]{targetId}, PlayerAction.IDENTIFY);
            }
        });
    }

    ArcheologyIdentifyExecutor(ClientAccess access, EventLogger log,
                               ActionSender sender) {
        this.access = access;
        this.log = log;
        this.sender = sender;
    }

    /** Resolves the complete batch before queue preflight and caches it for execution. */
    public int runtimeCost(ArcheologyIdentifyStep step, HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        PreparedBatch batch = prepare(step, hud, Integer.MAX_VALUE);
        prepared.get().put(step, batch);
        return batch.getQueueCost();
    }

    int prepareWithinBudget(ArcheologyIdentifyStep step, HeadsUpDisplay hud, int budget)
            throws ReflectiveOperationException {
        PreparedBatch batch = prepare(step, hud, budget);
        prepared.get().put(step, batch);
        return batch.getQueueCost();
    }

    public void execute(ArcheologyIdentifyStep step, HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        PreparedBatch batch = prepared.get().remove(step);
        if (batch == null) batch = prepare(step, hud, Integer.MAX_VALUE);
        executePreparedBatch(hud, batch);
    }

    void executePreparedBatch(HeadsUpDisplay hud, PreparedBatch batch) {
        if (batch == null) return;
        for (PreparedIdentify action : batch.getItems()) {
            logSelection(action.tool, action.targetName);
            executePrepared(hud, action.tool.getCandidate().getId(), action.targetId);
        }
    }

    void executePrepared(HeadsUpDisplay hud, long sourceId, long targetId) {
        ExecutionOriginGuard.enterInternal();
        try {
            sender.send(hud, sourceId, targetId);
        } finally {
            ExecutionOriginGuard.exitInternal();
        }
    }

    void clearPrepared() { prepared.remove(); }

    private PreparedBatch prepare(ArcheologyIdentifyStep step, HeadsUpDisplay hud,
                                  int budget)
            throws ReflectiveOperationException {
        List<InventoryMetaItem> targets = resolveTargets(step.getTarget(), hud);
        Set<Long> excluded = new HashSet<Long>();
        for (InventoryMetaItem target : targets)
            if (ArcheologyIdentifyPolicy.isUnidentifiedFragment(target))
                excluded.add(target.getId());

        List<ImproveResourceCandidate> toolbelt = toolbeltCandidates(hud, excluded);
        ImproveResourceCandidate inventory = step.getSourceMode()
                == ArcheologyIdentifySourceMode.TOOLBELT_THEN_INVENTORY
                ? SmartImproveExecutor.inventoryCandidate(
                        access.playerInventoryRoot(hud), excluded) : null;
        Consumer<String> debug = log == null ? null : new Consumer<String>() {
            @Override public void accept(String message) { log.debug(message); }
        };
        return prepareCandidates(targets, toolbelt, inventory, step.getSourceMode(),
                budget, step.getTarget().getKind() == TargetKind.HOVER, debug);
    }

    private List<InventoryMetaItem> resolveTargets(TargetSpec requested, HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        if (requested == null || hud == null)
            throw new StepUnavailableException(Messages.text("archeology.target_required"));
        Map<Long, InventoryMetaItem> targets =
                new LinkedHashMap<Long, InventoryMetaItem>();
        TargetKind kind = requested.getKind();
        if (kind == TargetKind.ACTIVE_TOOL) {
            addTarget(targets, access.activeTool(hud));
        } else if (kind == TargetKind.TOOLBELT_SLOT) {
            addTarget(targets,
                    hud.getToolBelt().getItemInSlot(requested.getSlot() - 1));
        } else if (kind == TargetKind.EQUIPMENT_SLOT) {
            PaperDollSlot frame = access.equipmentSlot(
                    hud.getPaperDollInventory(), (byte) requested.getSlot());
            if (frame != null && frame.getEquippedItem() != null)
                addTarget(targets, frame.getEquippedItem().getItem());
        } else if (kind == TargetKind.EXACT_OBJECT) {
            addTarget(targets, access.inventoryItem(hud, requested.getObjectId()));
        } else if (kind == TargetKind.SELECTED) {
            PickableUnit selected = access.selected(hud.getSelectBar());
            if (selected != null)
                addTarget(targets, access.inventoryItem(hud, selected.getId()));
        } else if (kind == TargetKind.HOVER) {
            addHoveredInventoryTargets(targets, hud);
        } else {
            throw new StepUnavailableException(Messages.text(
                    "archeology.unsupported_target", TargetCodec.display(requested)));
        }
        if (targets.isEmpty())
            throw new StepUnavailableException(Messages.text("archeology.target_required"));
        return SmartImproveInventoryPolicy.orderedTargets(targets.values());
    }

    private void addHoveredInventoryTargets(Map<Long, InventoryMetaItem> candidates,
                                            HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        com.wurmonline.client.WurmClientBase client = hud.getWorld().getClient();
        long[] ids = hud.getCommandTargetsFrom(client.getXMouse(), client.getYMouse());
        if (ids != null) for (long id : ids) {
            InventoryMetaItem item = access.inventoryItem(hud, id);
            if (item != null) candidates.put(item.getId(), item);
        }
    }

    private static void addTarget(Map<Long, InventoryMetaItem> targets,
                                  InventoryMetaItem target) {
        if (target != null) targets.put(target.getId(), target);
    }

    private static List<ImproveResourceCandidate> toolbeltCandidates(
            HeadsUpDisplay hud, Set<Long> excluded) {
        List<ImproveResourceCandidate> result = new ArrayList<ImproveResourceCandidate>();
        if (hud == null || hud.getToolBelt() == null) return result;
        for (int slot = 0; slot < 10; slot++) {
            ImproveResourceCandidate candidate = SmartImproveExecutor.inventoryCandidate(
                    hud.getToolBelt().getItemInSlot(slot), excluded);
            if (candidate != null) result.add(candidate);
        }
        return result;
    }

    static String sourceModeLabel(ArcheologyIdentifySourceMode sourceMode) {
        return Messages.text(sourceMode == ArcheologyIdentifySourceMode.TOOLBELT_ONLY
                ? "archeology.source_mode.toolbelt_only"
                : "archeology.source_mode.toolbelt_inventory");
    }

    static ResolvedImproveResource resolveTool(
            List<ImproveResourceCandidate> toolbelt,
            ImproveResourceCandidate inventory,
            ResourceRequirement requirement,
            ArcheologyIdentifySourceMode sourceMode,
            Consumer<String> debug) {
        return new ImproveSourceResolver().resolve(toolbelt, inventory, null,
                requirement,
                sourceMode == ArcheologyIdentifySourceMode.TOOLBELT_THEN_INVENTORY,
                debug);
    }

    static PreparedBatch prepareCandidates(
            List<InventoryMetaItem> targets,
            List<ImproveResourceCandidate> toolbelt,
            ImproveResourceCandidate inventory,
            ArcheologyIdentifySourceMode sourceMode,
            int budget,
            boolean hoverBatch,
            Consumer<String> debug) {
        if (targets == null || targets.isEmpty())
            throw new StepUnavailableException(Messages.text("archeology.target_required"));
        List<InventoryMetaItem> fragments = new ArrayList<InventoryMetaItem>();
        for (InventoryMetaItem target :
                SmartImproveInventoryPolicy.orderedTargets(targets)) {
            if (ArcheologyIdentifyPolicy.isUnidentifiedFragment(target)) {
                fragments.add(target);
            } else if (!hoverBatch) {
                ArcheologyIdentifyPolicy.requiredTool(target);
            }
        }
        if (fragments.isEmpty())
            throw new StepUnavailableException(Messages.text("archeology.no_fragments"));
        if (budget <= 0)
            return new PreparedBatch(new ArrayList<PreparedIdentify>());

        List<PreparedIdentify> actions = new ArrayList<PreparedIdentify>();
        StepUnavailableException firstFailure = null;
        for (InventoryMetaItem target : fragments) {
            try {
                ResourceRequirement requirement =
                        ArcheologyIdentifyPolicy.requiredTool(target);
                ResolvedImproveResource tool = resolveTool(toolbelt, inventory,
                        requirement, sourceMode, debug);
                if (tool == null) throw toolMissing(target, requirement, sourceMode);
                actions.add(new PreparedIdentify(target.getId(),
                        ArcheologyIdentifyPolicy.displayName(target), tool));
                if (actions.size() >= budget) break;
            } catch (StepUnavailableException unavailable) {
                if (!hoverBatch) throw unavailable;
                if (firstFailure == null) firstFailure = unavailable;
                if (debug != null) debug.accept("Archeology Identify skipped \""
                        + ArcheologyIdentifyPolicy.displayName(target) + "\": "
                        + unavailable.getMessage());
            }
        }
        if (actions.isEmpty() && firstFailure != null) throw firstFailure;
        return new PreparedBatch(actions);
    }

    private static StepUnavailableException toolMissing(
            InventoryMetaItem target,
            ResourceRequirement requirement,
            ArcheologyIdentifySourceMode sourceMode) {
        return new StepUnavailableException(Messages.text(
                "archeology.tool_missing", requirement.getMissingResourceLabel(),
                ArcheologyIdentifyPolicy.displayName(target),
                sourceModeLabel(sourceMode)));
    }

    private void logSelection(ResolvedImproveResource selection, String targetName) {
        if (log == null) return;
        ImproveResourceCandidate candidate = selection.getCandidate();
        String name = candidate.getDisplayName().isEmpty()
                ? candidate.getBaseName() : candidate.getDisplayName();
        if (selection.isToolbelt() && selection.isNested())
            log.info(Messages.text("archeology.using_from_toolbelt_container", name,
                    selection.getContainerName(), targetName));
        else if (selection.isToolbelt())
            log.info(Messages.text("archeology.using_toolbelt", name, targetName));
        else if (selection.isNested())
            log.info(Messages.text("archeology.using_from_inventory_container", name,
                    selection.getContainerName(), targetName));
        else
            log.info(Messages.text("archeology.using_inventory", name, targetName));
    }

    static final class PreparedBatch {
        private final List<PreparedIdentify> items;

        private PreparedBatch(List<PreparedIdentify> items) {
            this.items = items;
        }

        int getQueueCost() { return items.size(); }
        List<PreparedIdentify> getItems() { return items; }
    }

    static final class PreparedIdentify {
        private final long targetId;
        private final String targetName;
        private final ResolvedImproveResource tool;

        private PreparedIdentify(long targetId, String targetName,
                                 ResolvedImproveResource tool) {
            this.targetId = targetId;
            this.targetName = targetName;
            this.tool = tool;
        }

        long getTargetId() { return targetId; }
        ResolvedImproveResource getTool() { return tool; }
    }
}
