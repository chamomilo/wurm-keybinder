package org.keybinder.wurm.integration;

/** Business rules for rows that may become portable inventory targets. */
public final class InventorySelectionPolicy {
    private InventorySelectionPolicy() {
    }

    public static boolean isSelectable(boolean inventoryGroup,
                                       String baseName, String displayName) {
        return !inventoryGroup
                && !isServiceInventoryRow(baseName)
                && !isServiceInventoryRow(displayName);
    }

    private static boolean isServiceInventoryRow(String value) {
        return value != null && "inventory".equalsIgnoreCase(value.trim());
    }
}
