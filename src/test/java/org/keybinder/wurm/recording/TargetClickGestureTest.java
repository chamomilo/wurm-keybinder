package org.keybinder.wurm.recording;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TargetClickGestureTest {
    @Test public void shortStationaryClickMatches() {
        TargetClickGesture gesture = new TargetClickGesture();
        gesture.press(100, 200, 1000L);

        assertTrue(gesture.isCandidate(103, 197, 1999L));
        assertTrue(gesture.matches(103, 197, 1999L, false, false));
    }

    @Test public void dragMovementTimeoutAndMouseLookAreRejected() {
        TargetClickGesture gesture = new TargetClickGesture();
        gesture.press(10, 10, 100L);
        assertFalse(gesture.matches(14, 10, 101L, false, false));
        assertFalse(gesture.matches(10, 10, 1101L, false, false));
        assertFalse(gesture.matches(10, 10, 101L, true, false));

        gesture.press(10, 10, 100L);
        gesture.dragged();
        assertFalse(gesture.matches(10, 10, 101L, false, false));
    }

    @Test public void resetDisarmsGesture() {
        TargetClickGesture gesture = new TargetClickGesture();
        gesture.press(1, 1, 10L);
        gesture.reset();

        assertFalse(gesture.isCandidate(1, 1, 11L));
    }
}
