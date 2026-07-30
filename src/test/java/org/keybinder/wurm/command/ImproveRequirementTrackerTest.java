package org.keybinder.wurm.command;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class ImproveRequirementTrackerTest {
    @Test
    public void startsEmptyAndClearsSafely() {
        ImproveRequirementTracker tracker = new ImproveRequirementTracker();
        assertNull(tracker.toolName(42L));
        assertFalse(tracker.damaged(42L));
        tracker.clear();
        assertNull(tracker.toolName(42L));
    }
}
