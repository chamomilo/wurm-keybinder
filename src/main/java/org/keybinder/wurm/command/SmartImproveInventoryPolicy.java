package org.keybinder.wurm.command;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.shared.util.MaterialUtilities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

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

    static boolean needsRepair(InventoryMetaItem item) {
        return item != null && item.getDamage() > 0.0f;
    }

    static boolean isTargetTemperatureReady(InventoryMetaItem item) {
        return item != null && (!MaterialUtilities.isMetal(item.getMaterialId())
                || item.getTemperature() == GLOWING_TEMPERATURE);
    }

    static boolean isImprovable(InventoryMetaItem item) {
        return item != null && item.getImproveIconId() >= 0;
    }
}
