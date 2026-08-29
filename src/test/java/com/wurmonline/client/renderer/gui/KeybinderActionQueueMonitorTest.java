package com.wurmonline.client.renderer.gui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
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

    @Test
    public void leftSideMirrorsAnchorLampColumnAndArrowDirection() {
        assertEquals(0, KeybinderActionQueueMonitorHitBox.anchoredX(true, 1920, 220));
        assertEquals(1700, KeybinderActionQueueMonitorHitBox.anchoredX(false, 1920, 220));

        assertEquals(1198, KeybinderActionQueueMonitorHitBox.lampColumnLeft(
                true, 1000, 220, 2, 20));
        assertEquals(1002, KeybinderActionQueueMonitorHitBox.lampColumnLeft(
                false, 1000, 220, 2, 20));

        assertTrue(KeybinderActionQueueMonitorHitBox.arrowPointsRight(true, false));
        assertFalse(KeybinderActionQueueMonitorHitBox.arrowPointsRight(true, true));
        assertFalse(KeybinderActionQueueMonitorHitBox.arrowPointsRight(false, false));
        assertTrue(KeybinderActionQueueMonitorHitBox.arrowPointsRight(false, true));

        assertEquals(1040, KeybinderActionQueueMonitorHitBox.alignedTextLeft(
                true, 1010, 1100, 60));
        assertEquals(1010, KeybinderActionQueueMonitorHitBox.alignedTextLeft(
                false, 1010, 1100, 60));
    }
}
