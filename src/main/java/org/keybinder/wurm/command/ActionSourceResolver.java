package org.keybinder.wurm.command;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.gui.PaperDollSlot;
import org.keybinder.wurm.integration.ActionSourceOverride;
import org.keybinder.wurm.integration.ClientAccess;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.model.ItemSelector;
import org.keybinder.wurm.model.ItemSelectorKind;

/** Resolves one portable action source during per-step preflight. */
public final class ActionSourceResolver {
    private final ClientAccess access;

    public ActionSourceResolver(ClientAccess access) { this.access = access; }

    public ResolvedSource resolve(ItemSelector selector, HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        if (selector == null || selector.getKind() == ItemSelectorKind.CURRENT_ACTIVE)
            return ResolvedSource.ordinary();
        if (!ActionSourceOverride.isHookAvailable())
            throw unavailable(Messages.text("source.capability_unavailable"));
        switch (selector.getKind()) {
            case EMPTY_HAND:
                return ResolvedSource.override(-1L);
            case HOVERED_ITEM:
                return hoveredInventoryItem(hud);
            case TOOLBELT_SLOT: {
                InventoryMetaItem item = hud.getToolBelt().getItemInSlot(selector.getSlot() - 1);
                if (item == null)
                    throw unavailable(Messages.text("source.toolbelt_empty", selector.getSlot()));
                return ResolvedSource.override(item.getId());
            }
            case EQUIPMENT_SLOT: {
                PaperDollSlot frame = access.equipmentSlot(
                        hud.getPaperDollInventory(), (byte) selector.getSlot());
                InventoryMetaItem item = frame == null || frame.getEquippedItem() == null
                        ? null : frame.getEquippedItem().getItem();
                if (item == null)
                    throw unavailable(Messages.text("source.equipment_empty", selector.getSlot()));
                return ResolvedSource.override(item.getId());
            }
            case EXACT_OBJECT: {
                InventoryMetaItem item = access.inventoryItem(hud, selector.getObjectId());
                if (item == null)
                    throw unavailable(Messages.text("source.exact_unavailable", selector.getText()));
                return ResolvedSource.override(item.getId());
            }
            default:
                throw unavailable(Messages.text("source.unsupported",
                        ItemSelectorCodec.display(selector)));
        }
    }

    private ResolvedSource hoveredInventoryItem(HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        com.wurmonline.client.WurmClientBase client = hud.getWorld().getClient();
        long[] ids = hud.getCommandTargetsFrom(client.getXMouse(), client.getYMouse());
        if (ids == null || ids.length != 1)
            throw unavailable(Messages.text("source.hovered_one_required"));
        InventoryMetaItem item = access.inventoryItem(hud, ids[0]);
        if (item == null)
            throw unavailable(Messages.text("source.hovered_inventory_required"));
        return ResolvedSource.override(item.getId());
    }

    private static StepUnavailableException unavailable(String reason) {
        return new StepUnavailableException(reason);
    }

    public static final class ResolvedSource {
        private final boolean override;
        private final long sourceId;

        private ResolvedSource(boolean override, long sourceId) {
            this.override = override;
            this.sourceId = sourceId;
        }

        public static ResolvedSource ordinary() { return new ResolvedSource(false, 0L); }
        public static ResolvedSource override(long id) { return new ResolvedSource(true, id); }
        public boolean hasOverride() { return override; }
        public long getSourceId() { return sourceId; }
    }
}
