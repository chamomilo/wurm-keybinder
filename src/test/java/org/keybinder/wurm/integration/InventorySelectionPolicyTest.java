package org.keybinder.wurm.integration;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InventorySelectionPolicyTest {
    @Test public void rejectsGroupsAndTheExplicitInventoryServiceRow() {
        assertFalse(InventorySelectionPolicy.isSelectable(true, "backpack", "Backpack"));
        assertFalse(InventorySelectionPolicy.isSelectable(false, "inventory", "Inventory"));
        assertFalse(InventorySelectionPolicy.isSelectable(false, " Inventory ", "anything"));
        assertFalse(InventorySelectionPolicy.isSelectable(false, "anything", "INVENTORY"));
    }

    @Test public void acceptsRealItemsIncludingContainers() {
        assertTrue(InventorySelectionPolicy.isSelectable(false, "backpack", "Backpack"));
        assertTrue(InventorySelectionPolicy.isSelectable(false, "hatchet", "Hatchet, iron"));
        assertTrue(InventorySelectionPolicy.isSelectable(false, null, null));
    }
}
