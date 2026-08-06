package org.keybinder.wurm.integration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CurrentServerTrackerTest {
    @Test public void replacesStaleWorldNameAfterShardTransfer() {
        CurrentServerTracker tracker = new CurrentServerTracker();
        tracker.serverInformation("Novus");
        assertEquals("Novus", tracker.currentOrWorld("Novus"));

        tracker.connectionChanging();
        assertEquals("", tracker.currentOrWorld("Novus"));

        tracker.serverInformation(" Liberty ");
        assertEquals("Liberty", tracker.currentOrWorld("Novus"));
    }

    @Test public void fallsBackToWorldBeforeConnectionLifecycleIsObserved() {
        CurrentServerTracker tracker = new CurrentServerTracker();
        assertEquals("Novus", tracker.currentOrWorld(" Novus "));
    }

    @Test public void doesNotReusePreviousNameDuringReconnect() {
        CurrentServerTracker tracker = new CurrentServerTracker();
        tracker.serverInformation("Novus");
        tracker.connectionChanging();
        assertEquals("", tracker.currentOrWorld("Novus"));
    }
}
