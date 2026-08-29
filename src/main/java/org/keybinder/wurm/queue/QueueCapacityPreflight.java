package org.keybinder.wurm.queue;

/** Pure remaining-capacity calculation shared by per-step runtime checks. */
public final class QueueCapacityPreflight {
    private QueueCapacityPreflight() {}

    public static int remaining(int limit, int occupied) {
        if (limit <= 0) return Integer.MAX_VALUE;
        return Math.max(0, limit - Math.max(0, occupied));
    }
}
