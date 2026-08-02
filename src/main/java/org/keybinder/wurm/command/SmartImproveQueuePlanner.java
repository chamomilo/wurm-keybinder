package org.keybinder.wurm.command;

/** Pure queue-cost arithmetic for deterministic Smart Improve inventory batches. */
final class SmartImproveQueuePlanner {
    private SmartImproveQueuePlanner() {}

    static int itemCost(boolean damaged) {
        return damaged ? 2 : 1;
    }

    static int itemCost(boolean damaged, boolean temperatureReady) {
        return temperatureReady ? itemCost(damaged) : damaged ? 1 : 0;
    }

    static int batchCost(boolean... damaged) {
        int total = 0;
        if (damaged != null)
            for (boolean value : damaged) total += itemCost(value);
        return total;
    }

    static int fittedPrefixCost(int budget, int... itemCosts) {
        int total = 0;
        if (itemCosts == null) return total;
        for (int cost : itemCosts) {
            validateCost(cost);
            if (total + cost > Math.max(0, budget)) break;
            total += cost;
        }
        return total;
    }

    static int fittedPrefixLength(int budget, int... itemCosts) {
        int total = 0;
        int count = 0;
        if (itemCosts == null) return count;
        for (int cost : itemCosts) {
            validateCost(cost);
            if (total + cost > Math.max(0, budget)) break;
            total += cost;
            count++;
        }
        return count;
    }

    private static void validateCost(int cost) {
        if (cost < 0 || cost > 2)
            throw new IllegalArgumentException("Inventory improve cost must be 0, 1, or 2");
    }
}
