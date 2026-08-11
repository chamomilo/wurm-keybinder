package org.keybinder.wurm.command;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import org.keybinder.wurm.model.ObjectTypeNormalizer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Deterministic toolbelt-first lookup over the complete player inventory tree. */
public final class InventoryFilterResolver {
    public InventoryMetaItem resolve(String itemType, HeadsUpDisplay hud,
                                     InventoryMetaItem inventoryRoot) {
        List<InventoryMetaItem> toolbelt = new ArrayList<InventoryMetaItem>(10);
        if (hud != null && hud.getToolBelt() != null)
            for (int slot = 0; slot < 10; slot++)
                toolbelt.add(hud.getToolBelt().getItemInSlot(slot));
        return resolve(itemType, toolbelt, inventoryRoot);
    }

    public InventoryMetaItem resolve(String itemType,
                                     List<InventoryMetaItem> toolbeltItems,
                                     InventoryMetaItem inventoryRoot) {
        String wanted = ObjectTypeNormalizer.normalizeType(itemType);
        if (toolbeltItems != null) {
            Set<Long> visitedToolbelt = new HashSet<Long>();
            for (InventoryMetaItem item : toolbeltItems) {
                if (item == null || !visitedToolbelt.add(item.getId())) continue;
                if (matches(wanted, item)) return item;
            }
        }
        if (inventoryRoot == null) return null;
        List<InventoryMetaItem> direct = inventoryRoot.getChildren();
        if (direct == null) return null;

        // Prefer items lying directly in the main inventory. Only if none match
        // descend through backpacks and every other nested container.
        for (InventoryMetaItem item : direct)
            if (item != null && matches(wanted, item)) return item;

        Set<Long> visited = new HashSet<Long>();
        visited.add(inventoryRoot.getId());
        for (InventoryMetaItem item : direct) {
            InventoryMetaItem match = descendants(wanted, item, visited, true);
            if (match != null) return match;
        }
        return null;
    }

    private static InventoryMetaItem descendants(String wanted,
                                                   InventoryMetaItem item,
                                                   Set<Long> visited,
                                                   boolean skipSelfMatch) {
        if (item == null || !visited.add(item.getId())) return null;
        if (!skipSelfMatch && matches(wanted, item)) return item;
        List<InventoryMetaItem> children = item.getChildren();
        if (children == null) return null;
        for (InventoryMetaItem child : children) {
            InventoryMetaItem match = descendants(wanted, child, visited, false);
            if (match != null) return match;
        }
        return null;
    }

    private static boolean matches(String wanted, InventoryMetaItem item) {
        String base = item.getBaseName();
        if (base == null || base.trim().isEmpty()) base = item.getDisplayName();
        return ObjectTypeNormalizer.matchesType(wanted, base);
    }
}
