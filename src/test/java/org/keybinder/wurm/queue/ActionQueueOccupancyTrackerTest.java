package org.keybinder.wurm.queue;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;

public class ActionQueueOccupancyTrackerTest {
    @Test
    public void tracksSentActionsAndServerCompletions() {
        AtomicLong now = new AtomicLong(100L);
        ActionQueueOccupancyTracker tracker =
                new ActionQueueOccupancyTracker(now::get, 1_000L);

        tracker.actionsSent(3);
        assertEquals(3, tracker.occupied(false));
        tracker.actionState("Pushing", 5.0f);
        assertEquals(3, tracker.occupied(true));
        tracker.actionState("", 0.0f);
        assertEquals(2, tracker.occupied(false));
        tracker.actionState("Pushing", 5.0f);
        tracker.actionState("", 0.0f);
        assertEquals(1, tracker.occupied(false));
        tracker.actionState("Pushing", 5.0f);
        tracker.actionState("", 0.0f);
        assertEquals(0, tracker.occupied(false));
    }

    @Test
    public void staleFullEstimateIsDiscardedWhenHudRemainsIdle() {
        AtomicLong now = new AtomicLong(100L);
        ActionQueueOccupancyTracker tracker =
                new ActionQueueOccupancyTracker(now::get, 1_000L);
        tracker.actionsSent(4);

        now.set(1_099L);
        assertEquals(4, tracker.occupied(false));
        now.set(1_100L);
        assertEquals(0, tracker.occupied(false));
    }

    @Test
    public void activeActionPreventsIdleReconciliation() {
        AtomicLong now = new AtomicLong(100L);
        ActionQueueOccupancyTracker tracker =
                new ActionQueueOccupancyTracker(now::get, 1_000L);
        tracker.actionsSent(2);
        tracker.actionState("Improving", 10.0f);

        now.set(5_000L);

        assertEquals(2, tracker.occupied(true));
    }

    @Test
    public void serverActionStartedBeforeObservationStillOccupiesOneSlot() {
        ActionQueueOccupancyTracker tracker = new ActionQueueOccupancyTracker();

        tracker.actionState("Mining", 5.0f);

        assertEquals(1, tracker.occupied(true));
    }
}
