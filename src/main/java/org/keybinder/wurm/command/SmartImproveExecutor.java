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
import java.util.Comparator;
import java.util.List;

public final class SmartImproveExecutor {
    private final ClientAccess access;
    private final ImproveRequirementTracker tracker;
    private final EventLogger log;
    private final java.util.function.Consumer<Runnable> scheduler;
    private static final double ACTION_REACH_SQUARED = 16.0d;

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
                ToolbeltTool selected = improveTool(item, hud);
                if (selected == null) {
                    String required = tracker.toolName(item.getId());
                    throw missingTool(required, displayName(item));
                }
                InventoryMetaItem tool = selected.item;
                int cost = item.getDamage() > 0 ? 1 : 0;
                if (tool.getDamage() > 1.0f) cost++;
                total += cost + 1;
            }
            return total;
        }
        long targetId = worldTargetId(step.getTarget(), hud);
        if (worldImproveTool(targetId, hud) == null)
            throw new StepUnavailableException(Messages.text("improve.world_tool_missing"));
        return 2;
    }

    public void execute(SmartImproveStep step, HeadsUpDisplay hud) throws ReflectiveOperationException {
        TargetSpec target = step.getTarget();
        List<InventoryMetaItem> items = inventoryTargets(target, hud);
        if (!items.isEmpty()) {
            items.sort(Comparator.comparing(InventoryMetaItem::getQuality));
            for (InventoryMetaItem item : items) {
                ToolbeltTool selected = improveTool(item, hud);
                String itemName = displayName(item);
                if (selected == null) {
                    String required = tracker.toolName(item.getId());
                    throw missingTool(required, itemName);
                }
                InventoryMetaItem tool = selected.item;
                log.info(Messages.text("improve.using", tool.getBaseName(),
                        selected.slot, itemName));
                tracker.expect(item.getId());
                if (item.getDamage() > 0) hud.sendAction(PlayerAction.REPAIR, item.getId());
                if (tool.getDamage() > 1.0f) hud.sendAction(PlayerAction.REPAIR, tool.getId());
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
        log.info(Messages.text("improve.using",
                tool.getBaseName(), toolSelection.slot, itemName));
        tracker.expect(id);
        hud.sendAction(PlayerAction.REPAIR, id);
        tracker.repaired(id);
        hud.getWorld().getServerConnection().sendAction(tool.getId(), new long[]{id}, PlayerAction.IMPROVE);
    }

    private List<InventoryMetaItem> inventoryTargets(TargetSpec target, HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        List<InventoryMetaItem> result = new ArrayList<InventoryMetaItem>();
        if (target.getKind() == TargetKind.ACTIVE_TOOL) {
            InventoryMetaItem item = access.activeTool(hud);
            if (item != null) result.add(item);
            return result;
        }
        if (target.getKind() == TargetKind.TOOLBELT_SLOT) {
            InventoryMetaItem item = hud.getToolBelt().getItemInSlot(target.getSlot() - 1);
            if (item != null) result.add(item);
            return result;
        }
        if (target.getKind() == TargetKind.EQUIPMENT_SLOT) {
            PaperDollSlot frame = access.equipmentSlot(
                    hud.getPaperDollInventory(), (byte) target.getSlot());
            if (frame != null && frame.getEquippedItem() != null) result.add(frame.getEquippedItem().getItem());
            return result;
        }
        if (target.getKind() == TargetKind.EXACT_OBJECT) {
            InventoryMetaItem item = access.inventoryItem(hud, target.getObjectId());
            if (item != null) result.add(item);
            return result;
        }
        if (target.getKind() == TargetKind.HOVER) {
            com.wurmonline.client.WurmClientBase client = hud.getWorld().getClient();
            long[] ids = hud.getCommandTargetsFrom(client.getXMouse(), client.getYMouse());
            if (ids != null) for (long id : ids) {
                InventoryMetaItem item = access.inventoryItem(hud, id);
                if (item != null) result.add(item);
            }
        }
        return result;
    }

    private ToolbeltTool improveTool(InventoryMetaItem target, HeadsUpDisplay hud) {
        for (int i = 0; i < hud.getToolBelt().getSlotCount(); i++) {
            InventoryMetaItem candidate = hud.getToolBelt().getItemInSlot(i);
            if (candidate != null && candidate.getType() == target.getImproveIconId())
                return new ToolbeltTool(candidate, i + 1);
        }
        return null;
    }

    private static String displayName(InventoryMetaItem item) {
        String name = item.getDisplayName();
        return name == null || name.trim().isEmpty()
                ? Messages.text("improve.generic_item") : name;
    }

    private static final class ToolbeltTool {
        private final InventoryMetaItem item;
        private final int slot;

        private ToolbeltTool(InventoryMetaItem item, int slot) {
            this.item = item;
            this.slot = slot;
        }
    }

    private ToolbeltTool worldImproveTool(long targetId, HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        String required = tracker.toolName(targetId);
        if (required != null) {
            for (int i = 0; i < hud.getToolBelt().getSlotCount(); i++) {
                InventoryMetaItem candidate = hud.getToolBelt().getItemInSlot(i);
                if (candidate != null && candidate.getBaseName().toLowerCase(
                        java.util.Locale.ENGLISH).contains(required))
                    return new ToolbeltTool(candidate, i + 1);
            }
            return null;
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
