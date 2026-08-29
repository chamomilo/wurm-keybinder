package org.keybinder.wurm.queue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
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
    private final Deque<TrackedAction> actions = new ArrayDeque<TrackedAction>();
    private long nextSequence = 1L;
    private boolean actionActive;
    private long idleSince = -1L;

    public enum CancellationRequest {
        CURRENT,
        QUEUED_SCHEDULED,
        ALREADY_REQUESTED,
        NOT_FOUND
    }

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
        for (int i = 0; i < count && actions.size() < MAX_TRACKED_ACTIONS; i++)
            actions.addLast(new TrackedAction(nextSequence++, "", "", "", -1L));
        if (!actionActive && idleSince < 0L) idleSince = clock.getAsLong();
    }

    public synchronized void actionSent(String action, String source, String target,
                                        long targetId) {
        reconcileIdle(clock.getAsLong());
        if (actions.size() < MAX_TRACKED_ACTIONS)
            actions.addLast(new TrackedAction(nextSequence++, action, source, target, targetId));
        if (!actionActive && idleSince < 0L) idleSince = clock.getAsLong();
    }

    public synchronized void actionState(String actionText, float durationSeconds) {
        long now = clock.getAsLong();
        boolean active = actionText != null && !actionText.trim().isEmpty()
                && durationSeconds > 0.0f;
        if (active) {
            actionActive = true;
            idleSince = -1L;
            if (actions.isEmpty())
                actions.addLast(new TrackedAction(nextSequence++, actionText, "", "", -1L));
            TrackedAction current = actions.peekFirst();
            if (current != null) {
                current.active = true;
                if (current.action.isEmpty()) current.action = actionText.trim();
            }
            return;
        }
        if (actionActive && !actions.isEmpty()) actions.removeFirst();
        actionActive = false;
        idleSince = now;
    }

    public synchronized int occupied(boolean hudShowsAction) {
        long now = clock.getAsLong();
        if (hudShowsAction) {
            actionActive = true;
            idleSince = -1L;
            if (actions.isEmpty())
                actions.addLast(new TrackedAction(nextSequence++, "", "", "", -1L));
            TrackedAction current = actions.peekFirst();
            if (current != null) current.active = true;
        } else if (actionActive) {
            if (!actions.isEmpty()) actions.removeFirst();
            actionActive = false;
            idleSince = now;
        }
        reconcileIdle(now);
        return actions.size();
    }

    public synchronized List<ActionQueueEntry> snapshot(int limit) {
        reconcileIdle(clock.getAsLong());
        int remaining = Math.max(0, limit);
        List<ActionQueueEntry> result = new ArrayList<ActionQueueEntry>(
                Math.min(remaining, actions.size()));
        for (TrackedAction action : actions) {
            if (remaining-- <= 0) break;
            result.add(action.snapshot());
        }
        return result;
    }

    /**
     * The Wurm protocol can stop only the current action. A click on a later
     * entry is therefore remembered and claimed as soon as that exact entry
     * reaches the head and becomes active.
     */
    public synchronized CancellationRequest requestCancellation(long sequence) {
        TrackedAction first = actions.peekFirst();
        if (first == null) return CancellationRequest.NOT_FOUND;
        if (first.sequence != sequence) {
            for (TrackedAction action : actions) {
                if (action.sequence != sequence) continue;
                if (action.cancellationRequested)
                    return CancellationRequest.ALREADY_REQUESTED;
                action.cancellationRequested = true;
                return CancellationRequest.QUEUED_SCHEDULED;
            }
            return CancellationRequest.NOT_FOUND;
        }
        if (first.cancellationRequested) return CancellationRequest.ALREADY_REQUESTED;
        first.cancellationRequested = true;
        if (!actionActive || !first.active)
            return CancellationRequest.QUEUED_SCHEDULED;
        first.stopDispatched = true;
        return CancellationRequest.CURRENT;
    }

    /** Claims a remembered queued cancellation exactly once when it reaches the head. */
    public synchronized ActionQueueEntry claimReadyCancellation() {
        TrackedAction first = actions.peekFirst();
        if (first == null || !actionActive || !first.active
                || !first.cancellationRequested || first.stopDispatched)
            return null;
        first.stopDispatched = true;
        return first.snapshot();
    }

    public synchronized void cancellationFailed(long sequence) {
        TrackedAction first = actions.peekFirst();
        if (first != null && first.sequence == sequence) {
            first.cancellationRequested = false;
            first.stopDispatched = false;
        }
    }

    public synchronized void clear() {
        actions.clear();
        actionActive = false;
        idleSince = -1L;
    }

    private void reconcileIdle(long now) {
        if (!actionActive && !actions.isEmpty() && idleSince >= 0L
                && now - idleSince >= idleReconcileMillis)
            clear();
    }

    private static final class TrackedAction {
        private final long sequence;
        private String action;
        private final String source;
        private final String target;
        private final long targetId;
        private boolean active;
        private boolean cancellationRequested;
        private boolean stopDispatched;

        private TrackedAction(long sequence, String action, String source,
                              String target, long targetId) {
            this.sequence = sequence;
            this.action = safe(action);
            this.source = safe(source);
            this.target = safe(target);
            this.targetId = targetId;
        }

        private ActionQueueEntry snapshot() {
            return new ActionQueueEntry(sequence, action, source, target, targetId,
                    active, cancellationRequested);
        }

        private static String safe(String value) {
            return value == null ? "" : value.trim();
        }
    }
}
