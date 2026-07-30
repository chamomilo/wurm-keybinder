package org.keybinder.wurm.queue;

import org.keybinder.wurm.i18n.Messages;

/** Pure remaining-capacity calculation shared by runtime preflight tests. */
public final class QueueCapacityPreflight {
    private QueueCapacityPreflight() {}

    public static int remaining(int limit, int occupied) {
        if (limit <= 0) return Integer.MAX_VALUE;
        return Math.max(0, limit - Math.max(0, occupied));
    }

    public static void requireFits(int required, int limit, int occupied) {
        int free = remaining(limit, occupied);
        if (required <= free) return;
        throw new QueueCapacityException(Messages.text("execution.queue_remaining",
                required, free, limit, Math.max(0, occupied)));
    }
}
