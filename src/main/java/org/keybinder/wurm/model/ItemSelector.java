package org.keybinder.wurm.model;

import java.util.Objects;
import org.keybinder.wurm.i18n.Messages;

/** Immutable source selector. It is intentionally separate from {@link TargetSpec}. */
public final class ItemSelector {
    private final ItemSelectorKind kind;
    private final int slot;
    private final long objectId;
    private final String text;

    private ItemSelector(ItemSelectorKind kind, int slot, long objectId, String text) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.slot = slot;
        this.objectId = objectId;
        this.text = text == null ? "" : text.trim();
    }

    public static ItemSelector currentActive() {
        return new ItemSelector(ItemSelectorKind.CURRENT_ACTIVE, 0, 0L, "");
    }

    public static ItemSelector emptyHand() {
        return new ItemSelector(ItemSelectorKind.EMPTY_HAND, 0, -1L, "");
    }

    public static ItemSelector hoveredItem() {
        return new ItemSelector(ItemSelectorKind.HOVERED_ITEM, 0, 0L, "");
    }

    public static ItemSelector toolbeltSlot(int oneBasedSlot) {
        if (oneBasedSlot < 1 || oneBasedSlot > 10)
            throw new IllegalArgumentException(Messages.text("validation.toolbelt_slot"));
        return new ItemSelector(ItemSelectorKind.TOOLBELT_SLOT, oneBasedSlot, 0L, "");
    }

    public static ItemSelector equipmentSlot(int slot) {
        if (slot < 0 || slot > Byte.MAX_VALUE)
            throw new IllegalArgumentException(Messages.text("validation.equipment_slot"));
        return new ItemSelector(ItemSelectorKind.EQUIPMENT_SLOT, slot, 0L, "");
    }

    public static ItemSelector inventoryFilter(String type) {
        String value = ObjectTypeNormalizer.normalizeType(type);
        return new ItemSelector(ItemSelectorKind.INVENTORY_FILTER, 0, 0L, value);
    }

    public static ItemSelector exactObject(long objectId, String displayLabel) {
        String label = displayLabel == null ? "" : displayLabel.trim();
        if (label.isEmpty())
            throw new IllegalArgumentException(Messages.text("validation.object_name_missing"));
        return new ItemSelector(ItemSelectorKind.EXACT_OBJECT, 0, objectId, label);
    }

    public static ItemSelector copyOf(ItemSelector source) {
        if (source == null) return currentActive();
        return new ItemSelector(source.kind, source.slot, source.objectId, source.text);
    }

    public ItemSelectorKind getKind() { return kind; }
    public int getSlot() { return slot; }
    public long getObjectId() { return objectId; }
    public String getText() { return text; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ItemSelector)) return false;
        ItemSelector that = (ItemSelector) other;
        return kind == that.kind && slot == that.slot && objectId == that.objectId
                && text.equals(that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, slot, objectId, text);
    }
}
