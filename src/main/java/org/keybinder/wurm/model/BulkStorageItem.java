package org.keybinder.wurm.model;

/** Exact bulk row plus the open bulk-storage window from which it was captured. */
public final class BulkStorageItem {
    private final InventoryReference storage;
    private final InventoryReference item;

    public BulkStorageItem(InventoryReference storage, InventoryReference item) {
        this.storage = storage;
        this.item = item;
    }

    public InventoryReference getStorage() { return storage; }
    public InventoryReference getItem() { return item; }

    public static BulkStorageItem copyOf(BulkStorageItem value) {
        return value == null ? null : new BulkStorageItem(
                InventoryReference.copyOf(value.storage),
                InventoryReference.copyOf(value.item));
    }

    @Override public boolean equals(Object other) {
        if (!(other instanceof BulkStorageItem)) return false;
        BulkStorageItem source = (BulkStorageItem) other;
        return equal(storage, source.storage) && equal(item, source.item);
    }

    @Override public int hashCode() {
        return 31 * (storage == null ? 0 : storage.hashCode())
                + (item == null ? 0 : item.hashCode());
    }

    private static boolean equal(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }
}
