package org.keybinder.wurm.queue;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void fourSessionsKeepActionQueueOccupancyIndependent() {
        ActionQueueOccupancyTracker[] sessions = {
                new ActionQueueOccupancyTracker(),
                new ActionQueueOccupancyTracker(),
                new ActionQueueOccupancyTracker(),
                new ActionQueueOccupancyTracker()
        };
        for (int i = 0; i < sessions.length; i++) sessions[i].actionsSent(i + 1);

        sessions[2].clear();

        assertEquals(1, sessions[0].occupied(false));
        assertEquals(2, sessions[1].occupied(false));
        assertEquals(0, sessions[2].occupied(false));
        assertEquals(4, sessions[3].occupied(false));
    }

    @Test
    public void snapshotPreservesActionToolTargetAndTenRowLimit() {
        ActionQueueOccupancyTracker tracker = new ActionQueueOccupancyTracker();
        for (int i = 0; i < 12; i++)
            tracker.actionSent("Improve " + i, "hammer", "shield " + i, 100L + i);

        java.util.List<ActionQueueEntry> snapshot = tracker.snapshot(10);

        assertEquals(10, snapshot.size());
        assertEquals("Improve 0", snapshot.get(0).getAction());
        assertEquals("hammer", snapshot.get(0).getSource());
        assertEquals("shield 0", snapshot.get(0).getTarget());
        assertEquals(100L, snapshot.get(0).getTargetId());
    }

    @Test
    public void onlyCurrentActiveActionCanRequestCancellation() {
        ActionQueueOccupancyTracker tracker = new ActionQueueOccupancyTracker();
        tracker.actionSent("Mine", "pickaxe", "tile", 3L);
        tracker.actionSent("Mine", "pickaxe", "tile", 259L);
        java.util.List<ActionQueueEntry> queued = tracker.snapshot(10);

        tracker.actionState("Mining", 5.0f);
        assertEquals(ActionQueueOccupancyTracker.CancellationRequest.QUEUED_SCHEDULED,
                tracker.requestCancellation(queued.get(1).getSequence()));
        assertTrue(tracker.snapshot(10).get(1).isCancellationRequested());

        assertEquals(ActionQueueOccupancyTracker.CancellationRequest.CURRENT,
                tracker.requestCancellation(queued.get(0).getSequence()));
        assertEquals(ActionQueueOccupancyTracker.CancellationRequest.ALREADY_REQUESTED,
                tracker.requestCancellation(queued.get(0).getSequence()));
        assertTrue(tracker.snapshot(10).get(0).isCancellationRequested());

        tracker.cancellationFailed(queued.get(0).getSequence());
        assertFalse(tracker.snapshot(10).get(0).isCancellationRequested());
    }

    @Test
    public void rememberedQueuedCancellationIsClaimedWhenThatActionStarts() {
        ActionQueueOccupancyTracker tracker = new ActionQueueOccupancyTracker();
        tracker.actionSent("Mine", "pickaxe", "first tile", 3L);
        tracker.actionSent("Improve", "rock shards", "catseye", 12L);
        java.util.List<ActionQueueEntry> queued = tracker.snapshot(10);
        long second = queued.get(1).getSequence();

        assertEquals(ActionQueueOccupancyTracker.CancellationRequest.QUEUED_SCHEDULED,
                tracker.requestCancellation(second));
        tracker.actionState("Mining", 5.0f);
        assertNull(tracker.claimReadyCancellation());
        tracker.actionState("", 0.0f);
        tracker.actionState("Improving", 5.0f);

        ActionQueueEntry ready = tracker.claimReadyCancellation();
        assertEquals(second, ready.getSequence());
        assertEquals("Improve", ready.getAction());
        assertNull(tracker.claimReadyCancellation());
    }

    @Test
    public void completionRemovesCurrentDescriptionAndPromotesNextEntry() {
        ActionQueueOccupancyTracker tracker = new ActionQueueOccupancyTracker();
        tracker.actionSent("Chop", "hatchet", "felled tree", 11L);
        tracker.actionSent("Chop", "hatchet", "felled tree", 12L);
        tracker.actionState("Chopping", 4.0f);

        tracker.actionState("", 0.0f);

        java.util.List<ActionQueueEntry> remaining = tracker.snapshot(10);
        assertEquals(1, remaining.size());
        assertEquals(12L, remaining.get(0).getTargetId());
        assertFalse(remaining.get(0).isActive());
    }
}
