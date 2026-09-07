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
    private final InventoryFilterResolver inventoryFilters =
            new InventoryFilterResolver();

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
                return ResolvedSource.concreteTool(item);
            }
            case EQUIPMENT_SLOT: {
                PaperDollSlot frame = access.equipmentSlot(
                        hud.getPaperDollInventory(), (byte) selector.getSlot());
                InventoryMetaItem item = frame == null || frame.getEquippedItem() == null
                        ? null : frame.getEquippedItem().getItem();
                if (item == null)
                    throw unavailable(Messages.text("source.equipment_empty", selector.getSlot()));
                return ResolvedSource.concreteTool(item);
            }
            case INVENTORY_FILTER: {
                InventoryMetaItem item = inventoryFilters.resolve(selector.getText(), hud,
                        access.playerInventoryRoot(hud));
                if (item == null)
                    throw unavailable(Messages.text(
                            "unavailable.inventory_filter", selector.getText()));
                return ResolvedSource.concreteTool(item);
            }
            case EXACT_OBJECT: {
                InventoryMetaItem item = access.inventoryItem(hud, selector.getObjectId());
                if (item == null)
                    throw unavailable(Messages.text("source.exact_unavailable", selector.getText()));
                return ResolvedSource.concreteTool(item);
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
        return ResolvedSource.concreteTool(item);
    }

    private static StepUnavailableException unavailable(String reason) {
        return new StepUnavailableException(reason);
    }

    public static final class ResolvedSource {
        private final boolean override;
        private final long sourceId;
        private final InventoryMetaItem concreteTool;

        private ResolvedSource(boolean override, long sourceId,
                               InventoryMetaItem concreteTool) {
            this.override = override;
            this.sourceId = sourceId;
            this.concreteTool = concreteTool;
        }

        public static ResolvedSource ordinary() {
            return new ResolvedSource(false, 0L, null);
        }

        public static ResolvedSource override(long id) {
            return new ResolvedSource(true, id, null);
        }

        public static ResolvedSource concreteTool(InventoryMetaItem item) {
            if (item == null) throw new IllegalArgumentException("Concrete tool is missing");
            return new ResolvedSource(true, item.getId(), item);
        }

        public boolean hasOverride() { return override; }
        public long getSourceId() { return sourceId; }
        public InventoryMetaItem getConcreteTool() { return concreteTool; }
    }
}
