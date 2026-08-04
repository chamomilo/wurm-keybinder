package org.keybinder.wurm.integration;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import org.keybinder.wurm.model.InventoryReference;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Resolves the nearest bulk container that owns a clicked inventory row. */
public final class BulkStorageSourceResolver {
    public interface ItemLookup {
        InventoryMetaItem find(long id) throws ReflectiveOperationException;
    }

    public InventoryReference resolve(InventoryMetaItem clicked,
                                      List<InventoryMetaItem> visibleAncestors,
                                      InventoryReference containingWindow,
                                      InventoryMetaItem windowRoot,
                                      ItemLookup lookup)
            throws ReflectiveOperationException {
        if (clicked == null) return null;
        Set<Long> visited = new HashSet<Long>();
        visited.add(clicked.getId());
        if (visibleAncestors != null)
            for (InventoryMetaItem ancestor : visibleAncestors) {
                if (ancestor == null || !visited.add(ancestor.getId())) continue;
                if (isBulkStorage(ancestor))
                    return new InventoryReference(
                            ancestor.getId(), preferredName(ancestor));
            }
        long parentId = clicked.getParentId();
        while (parentId > 0L && visited.add(parentId)) {
            InventoryMetaItem parent = lookup == null ? null : lookup.find(parentId);
            if (parent == null) break;
            if (isBulkStorage(parent))
                return new InventoryReference(parent.getId(), preferredName(parent));
            parentId = parent.getParentId();
        }

        if (containingWindow == null
                || containingWindow.getId() <= 0L
                || containingWindow.getId() == clicked.getId()) return null;
        if (!BulkStorageSelectionPolicy.isBulkStorage(
                windowRoot == null ? "" : windowRoot.getBaseName(),
                windowRoot == null ? "" : windowRoot.getDisplayName(),
                containingWindow.getName())) return null;
        return containingWindow;
    }

    private static boolean isBulkStorage(InventoryMetaItem item) {
        return BulkStorageSelectionPolicy.isBulkStorage(
                item.getBaseName(), item.getDisplayName(), "");
    }

    private static String preferredName(InventoryMetaItem item) {
        String display = item.getDisplayName();
        if (display != null && !display.trim().isEmpty()) return display.trim();
        String base = item.getBaseName();
        return base == null ? "" : base.trim();
    }
}
