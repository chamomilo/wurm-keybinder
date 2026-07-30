package org.keybinder.wurm.queue;

import java.util.function.LongSupplier;

/**
 * Short-lived estimate of action slots sent to the server but not yet
 * completed. The client has no authoritative server-queue snapshot, so an
 * idle HUD reconciles stale estimates quickly instead of remaining falsely
 * full forever.
 */
public final class ActionQueueOccupancyTracker {
    private static final long DEFAULT_IDLE_RECONCILE_MILLIS = 1_000L;
    private static final int MAX_TRACKED_ACTIONS = 1_000;

    private final LongSupplier clock;
    private final long idleReconcileMillis;
    private int outstanding;
    private boolean actionActive;
    private long idleSince = -1L;

    public ActionQueueOccupancyTracker() {
        this(System::currentTimeMillis, DEFAULT_IDLE_RECONCILE_MILLIS);
    }

    ActionQueueOccupancyTracker(LongSupplier clock, long idleReconcileMillis) {
        this.clock = clock;
        this.idleReconcileMillis = idleReconcileMillis;
    }

    public synchronized void actionsSent(int count) {
        if (count <= 0) return;
        reconcileIdle(clock.getAsLong());
        outstanding = Math.min(MAX_TRACKED_ACTIONS, outstanding + count);
        if (!actionActive && idleSince < 0L) idleSince = clock.getAsLong();
    }

    public synchronized void actionState(String actionText, float durationSeconds) {
        long now = clock.getAsLong();
        boolean active = actionText != null && !actionText.trim().isEmpty()
                && durationSeconds > 0.0f;
        if (active) {
            actionActive = true;
            idleSince = -1L;
            if (outstanding == 0) outstanding = 1;
            return;
        }
        if (outstanding > 0) outstanding--;
        actionActive = false;
        idleSince = now;
    }

    public synchronized int occupied(boolean hudShowsAction) {
        long now = clock.getAsLong();
        if (hudShowsAction) {
            actionActive = true;
            idleSince = -1L;
            if (outstanding == 0) outstanding = 1;
        } else if (actionActive) {
            if (outstanding > 0) outstanding--;
            actionActive = false;
            idleSince = now;
        }
        reconcileIdle(now);
        return outstanding;
    }

    public synchronized void clear() {
        outstanding = 0;
        actionActive = false;
        idleSince = -1L;
    }

    private void reconcileIdle(long now) {
        if (!actionActive && outstanding > 0 && idleSince >= 0L
                && now - idleSince >= idleReconcileMillis)
            clear();
    }
}
