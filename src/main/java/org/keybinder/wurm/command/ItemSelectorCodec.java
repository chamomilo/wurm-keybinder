package org.keybinder.wurm.command;

import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.model.ItemSelector;
import org.keybinder.wurm.model.ItemSelectorKind;

/** Stable machine and display codec for per-action source selectors. */
public final class ItemSelectorCodec {
    private ItemSelectorCodec() { }

    public static String encode(ItemSelector selector) {
        switch (selector.getKind()) {
            case CURRENT_ACTIVE: return "current-active";
            case EMPTY_HAND: return "empty-hand";
            case HOVERED_ITEM: return "hovered-item";
            case TOOLBELT_SLOT: return "@tb" + selector.getSlot();
            case EQUIPMENT_SLOT: return "@eq" + selector.getSlot();
            case INVENTORY_FILTER:
                return InventoryFilterTarget.encode(selector.getText());
            case EXACT_OBJECT:
                return ExactObjectTarget.encode(selector.getObjectId(), selector.getText());
            default: throw new IllegalArgumentException("Unsupported source " + selector.getKind());
        }
    }

    public static ItemSelector decode(String encoded) {
        String value = encoded == null ? "" : encoded.trim();
        if (value.isEmpty() || "current-active".equals(value)) return ItemSelector.currentActive();
        if ("empty-hand".equals(value)) return ItemSelector.emptyHand();
        if ("hovered-item".equals(value)) return ItemSelector.hoveredItem();
        if (value.startsWith("@tb")) return ItemSelector.toolbeltSlot(parse(value.substring(3)));
        if (value.startsWith("@eq")) return ItemSelector.equipmentSlot(parse(value.substring(3)));
        if (InventoryFilterTarget.isInventoryFilter(value))
            return ItemSelector.inventoryFilter(InventoryFilterTarget.type(value));
        if (ExactObjectTarget.isExact(value))
            return ItemSelector.exactObject(ExactObjectTarget.id(value), ExactObjectTarget.name(value));
        throw new IllegalArgumentException(Messages.text("validation.source_unknown", value));
    }

    public static String display(ItemSelector selector) {
        switch (selector.getKind()) {
            case CURRENT_ACTIVE: return Messages.text("source.current_active");
            case EMPTY_HAND: return Messages.text("source.empty_hand");
            case HOVERED_ITEM: return Messages.text("source.hovered_item");
            case TOOLBELT_SLOT: return Messages.text("source.toolbelt_slot", selector.getSlot());
            case EQUIPMENT_SLOT: return Messages.text("source.equipment_slot", selector.getSlot());
            case INVENTORY_FILTER:
                return Messages.text("source.inventory_filter_named", selector.getText());
            case EXACT_OBJECT: return selector.getText();
            default: return selector.getKind().name();
        }
    }

    public static ItemSelector fromFields(String kind, int slot, long objectId, String text) {
        ItemSelectorKind parsed = ItemSelectorKind.valueOf(kind);
        switch (parsed) {
            case CURRENT_ACTIVE: return ItemSelector.currentActive();
            case EMPTY_HAND: return ItemSelector.emptyHand();
            case HOVERED_ITEM: return ItemSelector.hoveredItem();
            case TOOLBELT_SLOT: return ItemSelector.toolbeltSlot(slot);
            case EQUIPMENT_SLOT: return ItemSelector.equipmentSlot(slot);
            case INVENTORY_FILTER: return ItemSelector.inventoryFilter(text);
            case EXACT_OBJECT: return ItemSelector.exactObject(objectId, text);
            default: throw new IllegalArgumentException("Unsupported source " + parsed);
        }
    }

    private static int parse(String value) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException(Messages.text("validation.source_parameter"), e);
        }
    }
}
