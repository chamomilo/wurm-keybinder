package org.keybinder.wurm.command;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.renderer.PickableUnit;
import com.wurmonline.client.renderer.cell.CellRenderable;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.gui.PaperDollSlot;
import com.wurmonline.shared.constants.PlayerAction;
import org.keybinder.wurm.integration.ClientAccess;
import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.model.SmartImproveStep;
import org.keybinder.wurm.model.TargetKind;
import org.keybinder.wurm.model.TargetSpec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.IdentityHashMap;

import static org.keybinder.wurm.command.SmartImproveInventoryPolicy.findDescendant;
import static org.keybinder.wurm.command.SmartImproveInventoryPolicy.isImprovable;
import static org.keybinder.wurm.command.SmartImproveInventoryPolicy.isTargetTemperatureReady;
import static org.keybinder.wurm.command.SmartImproveInventoryPolicy.isToolTemperatureReady;
import static org.keybinder.wurm.command.SmartImproveInventoryPolicy.needsRepair;
import static org.keybinder.wurm.command.SmartImproveInventoryPolicy.orderedTargets;
import static org.keybinder.wurm.command.SmartImproveQueuePlanner.fittedPrefixCost;
import static org.keybinder.wurm.command.SmartImproveQueuePlanner.fittedPrefixLength;

public final class SmartImproveExecutor {
    private final ClientAccess access;
    private final ImproveRequirementTracker tracker;
    private final EventLogger log;
    private final java.util.function.Consumer<Runnable> scheduler;
    private static final double ACTION_REACH_SQUARED = 16.0d;
    private final ThreadLocal<Map<SmartImproveStep, PreparedInventoryPlan>> prepared =
            new ThreadLocal<Map<SmartImproveStep, PreparedInventoryPlan>>() {
                @Override protected Map<SmartImproveStep, PreparedInventoryPlan> initialValue() {
                    return new IdentityHashMap<SmartImproveStep, PreparedInventoryPlan>();
                }
            };

    public SmartImproveExecutor(ClientAccess access, ImproveRequirementTracker tracker, EventLogger log,
                                java.util.function.Consumer<Runnable> scheduler) {
        this.access = access;
        this.tracker = tracker;
        this.log = log;
        this.scheduler = scheduler;
    }

    public int runtimeCost(SmartImproveStep step, HeadsUpDisplay hud) throws ReflectiveOperationException {
        List<InventoryMetaItem> items = inventoryTargets(step.getTarget(), hud);
        if (!items.isEmpty()) {
            int total = 0;
            for (InventoryMetaItem item : items) {
                PreparedInventoryItem decision = assessInventoryItem(item, hud);
                requireImprovable(item);
                if (decision.improve && decision.tool == null) {
                    String required = tracker.toolName(item.getId());
                    throw missingTool(required, displayName(item));
                }
                total += decision.queueCost();
            }
            return total;
        }
        long targetId = worldTargetId(step.getTarget(), hud);
        ToolbeltTool tool = worldImproveTool(targetId, hud);
        if (tool == null)
            throw new StepUnavailableException(Messages.text("improve.world_tool_missing"));
        return (tracker.damaged(targetId) ? 1 : 0)
                + (isToolTemperatureReady(tool.item) ? 1 : 0);
    }

    /**
     * Prepares the deterministic inventory prefix that fits the currently free
     * queue budget. Temperature-blocked metal work keeps only its optional Repair
     * cost; ordinary items cost Repair + Improve or Improve. World targets use
     * the damage state learned from Wurm's Event messages.
     */
    int prepareWithinBudget(SmartImproveStep step, HeadsUpDisplay hud, int budget)
            throws ReflectiveOperationException {
        List<InventoryMetaItem> items = inventoryTargets(step.getTarget(), hud);
        if (items.isEmpty()) return runtimeCost(step, hud);
        int available = Math.max(0, budget);
        List<PreparedInventoryItem> decisions =
                new ArrayList<PreparedInventoryItem>(items.size());
        int[] costs = new int[items.size()];
        for (int i = 0; i < items.size(); i++) {
            PreparedInventoryItem decision = assessInventoryItem(items.get(i), hud);
            decisions.add(decision);
            costs[i] = decision.queueCost();
        }
        int plannedCost = fittedPrefixCost(available, costs);
        int cost = 0;
        List<PreparedInventoryItem> selectedItems = new ArrayList<PreparedInventoryItem>();
        for (PreparedInventoryItem decision : decisions) {
            InventoryMetaItem item = decision.target;
            int itemCost = decision.queueCost();
            if (cost + itemCost > plannedCost) break;
            requireImprovable(item);
            if (decision.improve && decision.tool == null) {
                String required = tracker.toolName(item.getId());
                throw missingTool(required, displayName(item));
            }
            selectedItems.add(decision);
            cost += itemCost;
        }
        prepared.get().put(step, new PreparedInventoryPlan(selectedItems, plannedCost));
        return plannedCost;
    }

    void clearPrepared() {
        prepared.remove();
    }

    public void execute(SmartImproveStep step, HeadsUpDisplay hud) throws ReflectiveOperationException {
        TargetSpec target = step.getTarget();
        List<InventoryMetaItem> items = inventoryTargets(target, hud);
        if (!items.isEmpty()) {
            PreparedInventoryPlan preparedPlan = prepared.get().remove(step);
            List<PreparedInventoryItem> plan;
            if (preparedPlan == null) {
                prepareWithinBudget(step, hud, Integer.MAX_VALUE);
                preparedPlan = prepared.get().remove(step);
            }
            plan = preparedPlan.items;
            int[] currentCosts = new int[plan.size()];
            for (int i = 0; i < plan.size(); i++) {
                PreparedInventoryItem candidate = plan.get(i);
                currentCosts[i] = (needsRepair(candidate.target) ? 1 : 0)
                        + (candidate.improve ? 1 : 0);
            }
            int executableItems = fittedPrefixLength(
                    preparedPlan.queueBudget, currentCosts);
            if (executableItems < plan.size())
                log.warning(Messages.text("improve.queue_state_changed",
                        executableItems, plan.size(), preparedPlan.queueBudget));
            int repairs = 0;
            int improvements = 0;
            int actions = 0;
            for (int i = 0; i < executableItems; i++) {
                if (needsRepair(plan.get(i).target)) repairs++;
                if (plan.get(i).improve) improvements++;
                actions += currentCosts[i];
            }
            log.info(Messages.text("improve.plan_summary", executableItems, repairs,
                    improvements, actions, preparedPlan.queueBudget));
            for (int i = 0; i < executableItems; i++) {
                PreparedInventoryItem preparedItem = plan.get(i);
                InventoryMetaItem item = preparedItem.target;
                ToolbeltTool selected = preparedItem.tool;
                String itemName = displayName(item);
                boolean repair = needsRepair(item);
                if (!preparedItem.improve) {
                    if (selected == null) {
                        log.info(Messages.text(repair
                                        ? "improve.repairing_not_glowing"
                                        : "improve.skipping_not_glowing",
                                itemName));
                    } else {
                        logColdTool(selected, itemName, repair);
                    }
                    if (repair) {
                        hud.sendAction(PlayerAction.REPAIR, item.getId());
                        tracker.repaired(item.getId());
                    }
                    continue;
                }
                InventoryMetaItem tool = selected.item;
                logSelection(selected, itemName, repair);
                tracker.expect(item.getId());
                if (repair) {
                    hud.sendAction(PlayerAction.REPAIR, item.getId());
                    tracker.repaired(item.getId());
                }
                hud.getWorld().getServerConnection().sendAction(
                        tool.getId(), new long[]{item.getId()}, PlayerAction.IMPROVE);
            }
            return;
        }

        if (target.getKind() == TargetKind.HOVER) {
            PickableUnit hovered = worldTarget(target, hud);
            access.select(hud.getSelectBar(), hovered);
            scheduler.accept(() -> executeDeferredWorld(hovered, hud));
            return;
        }
        PickableUnit selected = target.getKind() == TargetKind.EXACT_OBJECT
                ? null : worldTarget(target, hud);
        executeWorldTarget(target, selected, hud);
    }

    private void executeDeferredWorld(PickableUnit target, HeadsUpDisplay hud) {
        org.keybinder.wurm.recording.ShadowRecorder.enterInternal();
        try {
            PickableUnit selected = access.selected(hud.getSelectBar());
            if (selected == null || selected.getId() != target.getId()) {
                access.select(hud.getSelectBar(), target);
                selected = target;
            }
            executeWorldTarget(TargetSpec.simple(TargetKind.SELECTED), selected, hud);
        } catch (Throwable e) {
            log.error(Messages.text("improve.deferred_failed",
                    e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()), e);
        } finally {
            org.keybinder.wurm.recording.ShadowRecorder.exitInternal();
        }
    }

    private void executeWorldTarget(TargetSpec target, PickableUnit selected, HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        long id;
        String itemName;
        if (target.getKind() == TargetKind.EXACT_OBJECT) {
            ensureExactKnown(target, hud);
            id = target.getObjectId();
            itemName = target.getText().isEmpty()
                    ? Messages.text("target.exact_generic") : target.getText();
        } else {
            validateWorldTarget(selected);
            id = selected.getId();
            itemName = safeName(selected.getHoverName());
        }
        String required = tracker.toolName(id);
        ToolbeltTool toolSelection = worldImproveTool(id, hud);
        if (toolSelection == null) {
            throw missingTool(required, itemName);
        }
        InventoryMetaItem tool = toolSelection.item;
        boolean repair = tracker.damaged(id);
        if (!isToolTemperatureReady(tool)) {
            logColdTool(toolSelection, itemName, repair);
            if (repair) {
                tracker.expect(id);
                hud.sendAction(PlayerAction.REPAIR, id);
                tracker.repaired(id);
            }
            return;
        }
        logSelection(toolSelection, itemName, repair);
        tracker.expect(id);
        if (repair) {
            hud.sendAction(PlayerAction.REPAIR, id);
            tracker.repaired(id);
        }
        hud.getWorld().getServerConnection().sendAction(tool.getId(), new long[]{id}, PlayerAction.IMPROVE);
    }

    private List<InventoryMetaItem> inventoryTargets(TargetSpec target, HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        Map<Long, InventoryMetaItem> result = new LinkedHashMap<Long, InventoryMetaItem>();
        if (target.getKind() == TargetKind.ACTIVE_TOOL) {
            InventoryMetaItem item = access.activeTool(hud);
            addTarget(result, item);
            return orderedTargets(result.values());
        }
        if (target.getKind() == TargetKind.TOOLBELT_SLOT) {
            InventoryMetaItem item = hud.getToolBelt().getItemInSlot(target.getSlot() - 1);
            addTarget(result, item);
            return orderedTargets(result.values());
        }
        if (target.getKind() == TargetKind.EQUIPMENT_SLOT) {
            PaperDollSlot frame = access.equipmentSlot(
                    hud.getPaperDollInventory(), (byte) target.getSlot());
            if (frame != null && frame.getEquippedItem() != null)
                addTarget(result, frame.getEquippedItem().getItem());
            return orderedTargets(result.values());
        }
        if (target.getKind() == TargetKind.EXACT_OBJECT) {
            InventoryMetaItem item = access.inventoryItem(hud, target.getObjectId());
            addTarget(result, item);
            return orderedTargets(result.values());
        }
        if (target.getKind() == TargetKind.HOVER) {
            com.wurmonline.client.WurmClientBase client = hud.getWorld().getClient();
            long[] ids = hud.getCommandTargetsFrom(client.getXMouse(), client.getYMouse());
            if (ids != null) for (long id : ids) {
                InventoryMetaItem item = access.inventoryItem(hud, id);
                addTarget(result, item);
            }
        }
        return orderedTargets(result.values());
    }

    private ToolbeltTool improveTool(InventoryMetaItem target, HeadsUpDisplay hud,
                                     boolean temperatureReady) {
        final short requiredType = target.getImproveIconId();
        final long targetId = target.getId();
        // Preserve the old behaviour when a matching item is directly on the belt.
        for (int i = 0; i < hud.getToolBelt().getSlotCount(); i++) {
            InventoryMetaItem candidate = hud.getToolBelt().getItemInSlot(i);
            if (candidate != null && candidate.getId() != targetId
                    && candidate.getType() == requiredType
                    && isToolTemperatureReady(candidate) == temperatureReady)
                return new ToolbeltTool(candidate, i + 1);
        }
        // A belt slot may itself be a backpack, quiver, bucket, etc. Search its
        // contents without changing the active item or the inventory tree order.
        for (int i = 0; i < hud.getToolBelt().getSlotCount(); i++) {
            InventoryMetaItem container = hud.getToolBelt().getItemInSlot(i);
            InventoryMetaItem candidate = findDescendant(container,
                    item -> item.getId() != targetId && item.getType() == requiredType
                            && isToolTemperatureReady(item) == temperatureReady);
            if (candidate != null)
                return new ToolbeltTool(candidate, i + 1, displayName(container));
        }
        return null;
    }

    private static void addTarget(Map<Long, InventoryMetaItem> result,
                                  InventoryMetaItem item) {
        if (item != null) result.put(item.getId(), item);
    }

    private static String displayName(InventoryMetaItem item) {
        String name = item.getDisplayName();
        return name == null || name.trim().isEmpty()
                ? Messages.text("improve.generic_item") : name;
    }

    private PreparedInventoryItem assessInventoryItem(InventoryMetaItem item,
                                                       HeadsUpDisplay hud) {
        boolean repair = needsRepair(item);
        if (!isImprovable(item))
            return new PreparedInventoryItem(item, null, repair, true);
        if (!isTargetTemperatureReady(item))
            return new PreparedInventoryItem(item, null, repair, false);
        ToolbeltTool ready = improveTool(item, hud, true);
        if (ready != null)
            return new PreparedInventoryItem(item, ready, repair, true);
        ToolbeltTool cold = improveTool(item, hud, false);
        if (cold != null)
            return new PreparedInventoryItem(item, cold, repair, false);
        return new PreparedInventoryItem(item, null, repair, true);
    }

    private static final class ToolbeltTool {
        private final InventoryMetaItem item;
        private final int slot;
        private final String container;

        private ToolbeltTool(InventoryMetaItem item, int slot) {
            this(item, slot, null);
        }

        private ToolbeltTool(InventoryMetaItem item, int slot, String container) {
            this.item = item;
            this.slot = slot;
            this.container = container;
        }
    }

    private static final class PreparedInventoryItem {
        private final InventoryMetaItem target;
        private final ToolbeltTool tool;
        private final boolean repair;
        private final boolean improve;

        private PreparedInventoryItem(InventoryMetaItem target, ToolbeltTool tool,
                                      boolean repair, boolean improve) {
            this.target = target;
            this.tool = tool;
            this.repair = repair;
            this.improve = improve;
        }

        private int queueCost() {
            return (repair ? 1 : 0) + (improve ? 1 : 0);
        }
    }

    private static final class PreparedInventoryPlan {
        private final List<PreparedInventoryItem> items;
        private final int queueBudget;

        private PreparedInventoryPlan(List<PreparedInventoryItem> items, int queueBudget) {
            this.items = items;
            this.queueBudget = queueBudget;
        }
    }

    private static void requireImprovable(InventoryMetaItem item) {
        if (!isImprovable(item))
            throw new StepUnavailableException(
                    Messages.text("improve.cannot_improve", displayName(item)));
    }

    private void logSelection(ToolbeltTool selection, String itemName, boolean repairing) {
        if (selection.container == null) {
            log.info(Messages.text(repairing ? "improve.repairing_using" : "improve.using",
                    selection.item.getBaseName(),
                    selection.slot, itemName));
        } else {
            log.info(Messages.text(repairing
                            ? "improve.repairing_using_from_container"
                            : "improve.using_from_container",
                    selection.item.getBaseName(), selection.container,
                    selection.slot, itemName));
        }
    }

    private void logColdTool(ToolbeltTool selection, String itemName, boolean repairing) {
        if (selection.container == null) {
            log.info(Messages.text(repairing
                            ? "improve.repairing_cold_tool"
                            : "improve.skipping_cold_tool",
                    itemName, selection.item.getBaseName(), selection.slot));
        } else {
            log.info(Messages.text(repairing
                            ? "improve.repairing_cold_tool_from_container"
                            : "improve.skipping_cold_tool_from_container",
                    itemName, selection.item.getBaseName(), selection.container,
                    selection.slot));
        }
    }

    private ToolbeltTool worldImproveTool(long targetId, HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        String required = tracker.toolName(targetId);
        if (required != null) {
            ToolbeltTool ready = worldImproveTool(required, hud, true);
            return ready != null ? ready : worldImproveTool(required, hud, false);
        }
        InventoryMetaItem active = access.activeTool(hud);
        if (active == null) return null;
        for (int i = 0; i < hud.getToolBelt().getSlotCount(); i++) {
            InventoryMetaItem candidate = hud.getToolBelt().getItemInSlot(i);
            if (candidate != null && candidate.getId() == active.getId())
                return new ToolbeltTool(candidate, i + 1);
        }
        return null;
    }

    private ToolbeltTool worldImproveTool(String required, HeadsUpDisplay hud,
                                          boolean temperatureReady) {
        final String requiredName = required.toLowerCase(java.util.Locale.ENGLISH);
        for (int i = 0; i < hud.getToolBelt().getSlotCount(); i++) {
            InventoryMetaItem candidate = hud.getToolBelt().getItemInSlot(i);
            if (candidate != null && candidate.getBaseName() != null
                    && candidate.getBaseName().toLowerCase(java.util.Locale.ENGLISH)
                    .contains(requiredName)
                    && isToolTemperatureReady(candidate) == temperatureReady)
                return new ToolbeltTool(candidate, i + 1);
        }
        for (int i = 0; i < hud.getToolBelt().getSlotCount(); i++) {
            InventoryMetaItem container = hud.getToolBelt().getItemInSlot(i);
            InventoryMetaItem candidate = findDescendant(container,
                    item -> item.getBaseName() != null
                            && item.getBaseName().toLowerCase(java.util.Locale.ENGLISH)
                            .contains(requiredName)
                            && isToolTemperatureReady(item) == temperatureReady);
            if (candidate != null)
                return new ToolbeltTool(candidate, i + 1, displayName(container));
        }
        return null;
    }

    private long worldTargetId(TargetSpec target, HeadsUpDisplay hud) throws ReflectiveOperationException {
        if (target.getKind() == TargetKind.EXACT_OBJECT) {
            ensureExactKnown(target, hud);
            return target.getObjectId();
        }
        PickableUnit selected = worldTarget(target, hud);
        validateWorldTarget(selected);
        return selected.getId();
    }

    private PickableUnit worldTarget(TargetSpec target, HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        PickableUnit unit;
        if (target.getKind() == TargetKind.SELECTED) unit = access.selected(hud.getSelectBar());
        else if (target.getKind() == TargetKind.HOVER)
            unit = hud.getWorld().getCurrentHoveredObject();
        else throw new IllegalArgumentException(Messages.text(
                    "improve.unsupported_target", target.getKind()));
        if (unit == null)
            throw new StepUnavailableException(Messages.text("improve.target_missing"));
        return unit;
    }

    private static void validateWorldTarget(PickableUnit selected) {
        String itemName = safeName(selected.getHoverName());
        if (!selected.targetMatches(PlayerAction.IMPROVE.getTargetMask())) {
            throw new StepUnavailableException(
                    Messages.text("improve.cannot_improve", itemName));
        }
        if (selected instanceof CellRenderable
                && ((CellRenderable) selected).getSquaredLengthFromPlayer()
                > ACTION_REACH_SQUARED) {
            throw new StepUnavailableException(Messages.text("improve.too_far", itemName));
        }
    }

    private void ensureExactKnown(TargetSpec target, HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        long id = target.getObjectId();
        if (access.inventoryItem(hud, id) != null) return;
        PickableUnit selected = access.selected(hud.getSelectBar());
        if (selected != null && selected.getId() == id) return;
        PickableUnit hovered = hud.getWorld().getCurrentHoveredObject();
        if (hovered != null && hovered.getId() == id) return;
        throw new StepUnavailableException(Messages.text("unavailable.exact_object",
                target.getText().isEmpty()
                        ? Long.toString(id) : "\"" + target.getText() + "\""));
    }

    private static String safeName(String value) {
        return value == null || value.trim().isEmpty()
                ? Messages.text("improve.generic_item") : value;
    }

    private static StepUnavailableException missingTool(String required, String itemName) {
        String tool = required == null
                ? Messages.text("improve.required_by_item") : "\"" + required + "\"";
        return new StepUnavailableException(
                Messages.text("improve.tool_missing", tool, itemName));
    }
}
