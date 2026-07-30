package org.keybinder.wurm.command;

import java.util.function.LongSupplier;

/**
 * Tracks every pending Push recreation for the selected object. SelectBar's
 * native keep flag is one-shot, so a queued series must re-arm it after each
 * successful recreation until the whole series has been observed.
 */
public final class PushSelectionRetention {
    private static final long DEFAULT_TIMEOUT_MILLIS = 30_000L;
    private static final int MAX_PENDING = 100;

    private final LongSupplier clock;
    private final long timeoutMillis;
    private long targetId = -1L;
    private int pending;
    private long expiresAt;

    public PushSelectionRetention() {
        this(System::currentTimeMillis, DEFAULT_TIMEOUT_MILLIS);
    }

    PushSelectionRetention(LongSupplier clock, long timeoutMillis) {
        this.clock = clock;
        this.timeoutMillis = timeoutMillis;
    }

    public synchronized void arm(long newTargetId) {
        long now = clock.getAsLong();
        if (expired(now) || targetId != newTargetId) {
            targetId = newTargetId;
            pending = 0;
        }
        if (pending < MAX_PENDING) pending++;
        expiresAt = now + timeoutMillis;
    }

    /**
     * Returns true when SelectBar must keep the same item for another server
     * recreation. If the native selection did not succeed, the user has moved
     * on and stale retention is discarded without changing their selection.
     */
    public synchronized boolean afterRecreated(long recreatedId, boolean isStillSelected) {
        long now = clock.getAsLong();
        if (expired(now)) {
            clearState();
            return false;
        }
        if (pending == 0 || recreatedId != targetId) return false;
        if (!isStillSelected) {
            clearState();
            return false;
        }
        pending--;
        if (pending == 0) {
            clearState();
            return false;
        }
        expiresAt = now + timeoutMillis;
        return true;
    }

    public synchronized void clear() {
        clearState();
    }

    synchronized int pendingCount() {
        return pending;
    }

    private boolean expired(long now) {
        return pending > 0 && now >= expiresAt;
    }

    private void clearState() {
        targetId = -1L;
        pending = 0;
        expiresAt = 0L;
    }
}
