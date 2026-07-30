package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import org.keybinder.wurm.integration.InventorySelectionPolicy;

/**
 * Package-access adapter for resolving the inventory row clicked in Wurm's
 * package-private tree implementation.
 */
public final class KeybinderInventorySelectionBridge {
    private KeybinderInventorySelectionBridge() {
    }

    public static InventoryMetaItem itemAt(Object panel, int x, int y) {
        if (!(panel instanceof WurmTreeList.TreeListPanel)) return null;
        WTreeListNode<?> node = ((WurmTreeList.TreeListPanel) panel).getNodeAt(x, y);
        if (node == null || node.item == null) return null;
        Object row = node.item;
        if (row instanceof InventoryListComponent.InventoryTreeListItem) {
            InventoryListComponent.InventoryTreeListItem inventoryRow =
                    (InventoryListComponent.InventoryTreeListItem) row;
            if (inventoryRow.item == null
                    || !InventorySelectionPolicy.isSelectable(
                    inventoryRow.isInventoryGroup,
                    inventoryRow.item.getBaseName(),
                    inventoryRow.item.getDisplayName()))
                return null;
            return inventoryRow.item;
        }
        if (row instanceof InventoryContainerWindow.InventoryContainerItem) {
            InventoryMetaItem item =
                    ((InventoryContainerWindow.InventoryContainerItem) row).getItem();
            if (item == null || !InventorySelectionPolicy.isSelectable(
                    false, item.getBaseName(), item.getDisplayName()))
                return null;
            return item;
        }
        return null;
    }
}
