package org.keybinder.wurm.integration;

/** Strict ID choice for an inventory-window bulk destination. */
public final class BulkInventoryDestinationPolicy {
    private BulkInventoryDestinationPolicy() { }

    /** Wurm uses -1 as the native pseudo-target for the player's inventory. */
    public static boolean isValid(long id) {
        return id > 0L || id == -1L;
    }

    public static long resolve(long rowId, boolean rowIsContainer, long windowRootId) {
        if (!isValid(windowRootId)) return 0L;
        return rowIsContainer && rowId > 0L ? rowId : windowRootId;
    }
}
