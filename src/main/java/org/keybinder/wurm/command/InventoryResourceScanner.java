package org.keybinder.wurm.command;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Creates immutable resource snapshots from inventory and toolbelt trees. */
public final class InventoryResourceScanner {
    private InventoryResourceScanner() {}

    public static ImproveResourceCandidate inventoryCandidate(
            InventoryMetaItem root, Set<Long> excluded) {
        return candidate(root, safeExcluded(excluded), new HashSet<Long>(), true);
    }

    public static ImproveResourceCandidate singleCandidate(
            InventoryMetaItem item, Set<Long> excluded) {
        return candidate(item, safeExcluded(excluded), new HashSet<Long>(), false);
    }

    public static List<ImproveResourceCandidate> toolbeltCandidates(
            HeadsUpDisplay hud, Set<Long> excluded) {
        List<ImproveResourceCandidate> result =
                new ArrayList<ImproveResourceCandidate>();
        if (hud == null || hud.getToolBelt() == null) return result;
        Set<Long> visited = new HashSet<Long>();
        Set<Long> ignored = safeExcluded(excluded);
        for (int slot = 0; slot < 10; slot++) {
            ImproveResourceCandidate value = candidate(
                    hud.getToolBelt().getItemInSlot(slot), ignored, visited, true);
            if (value != null) result.add(value);
        }
        return result;
    }

    private static ImproveResourceCandidate candidate(
            InventoryMetaItem item, Set<Long> excluded, Set<Long> visited,
            boolean descendChildren) {
        if (item == null || excluded.contains(item.getId())
                || !visited.add(item.getId())) return null;
        List<ImproveResourceCandidate> children =
                new ArrayList<ImproveResourceCandidate>();
        if (descendChildren && item.getChildren() != null)
            for (InventoryMetaItem child : item.getChildren()) {
                ImproveResourceCandidate value = candidate(
                        child, excluded, visited, true);
                if (value != null) children.add(value);
            }
        return new ImproveResourceCandidate(item.getId(), item.getBaseName(),
                item.getDisplayName(), item.getMaterialId(), item.getType(),
                item.getR(), item.getG(), item.getB(), item.getTemperature(),
                item.getQuality(), item.getDamage(), item.getRarity(), children);
    }

    private static Set<Long> safeExcluded(Set<Long> excluded) {
        return excluded == null ? Collections.<Long>emptySet() : excluded;
    }
}
