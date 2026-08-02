package org.keybinder.wurm.command;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.shared.util.MaterialUtilities;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/** Stable item ordering, traversal, and eligibility rules used by Smart Improve. */
final class SmartImproveInventoryPolicy {
    private static final byte GLOWING_TEMPERATURE = 5;

    private SmartImproveInventoryPolicy() {}

    static List<InventoryMetaItem> orderedTargets(Collection<InventoryMetaItem> targets) {
        List<InventoryMetaItem> ordered = new ArrayList<InventoryMetaItem>(targets);
        // ID is a stable tie breaker. Sorting this detached list changes only the
        // order in which actions are queued; Wurm's visible inventory stays intact.
        ordered.sort(Comparator.comparingDouble(InventoryMetaItem::getQuality)
                .thenComparingLong(InventoryMetaItem::getId));
        return ordered;
    }

    static InventoryMetaItem findDescendant(
            InventoryMetaItem container, Predicate<InventoryMetaItem> matches) {
        if (container == null || container.getChildren() == null) return null;
        Set<Long> visited = new HashSet<Long>();
        visited.add(container.getId());
        ArrayDeque<InventoryMetaItem> pending = new ArrayDeque<InventoryMetaItem>();
        for (InventoryMetaItem child : container.getChildren())
            if (child != null) pending.addLast(child);
        while (!pending.isEmpty()) {
            InventoryMetaItem item = pending.removeFirst();
            if (!visited.add(item.getId())) continue;
            if (matches.test(item)) return item;
            if (item.getChildren() != null)
                for (InventoryMetaItem child : item.getChildren())
                    if (child != null) pending.addLast(child);
        }
        return null;
    }

    static boolean needsRepair(InventoryMetaItem item) {
        return item != null && item.getDamage() > 0.0f;
    }

    static boolean isTargetTemperatureReady(InventoryMetaItem item) {
        return item != null && (!MaterialUtilities.isMetal(item.getMaterialId())
                || item.getTemperature() == GLOWING_TEMPERATURE);
    }

    static boolean isToolTemperatureReady(InventoryMetaItem item) {
        if (item == null) return false;
        String name = item.getBaseName();
        boolean metalLump = MaterialUtilities.isMetal(item.getMaterialId())
                && name != null && name.toLowerCase(java.util.Locale.ENGLISH).contains("lump");
        return !metalLump || item.getTemperature() == GLOWING_TEMPERATURE;
    }

    static boolean isImprovable(InventoryMetaItem item) {
        return item != null && item.getImproveIconId() >= 0;
    }
}
