package com.wurmonline.client.renderer.gui;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class KeybinderActionQueueMonitorTest {
    @Test
    public void arrowRowHasItsOwnClickTargetAboveTheFirstLamp() {
        int x = 100;
        int y = 200;

        assertTrue(KeybinderActionQueueMonitorHitBox.contains(
                102, 202, x + 2, y + 2, 20, 18));
        assertTrue(KeybinderActionQueueMonitorHitBox.contains(
                121, 219, x + 2, y + 2, 20, 18));
        assertFalse(KeybinderActionQueueMonitorHitBox.contains(
                101, 210, x + 2, y + 2, 20, 18));
        assertFalse(KeybinderActionQueueMonitorHitBox.contains(
                122, 210, x + 2, y + 2, 20, 18));
        assertFalse(KeybinderActionQueueMonitorHitBox.contains(
                110, 220, x + 2, y + 2, 20, 18));
    }
}
