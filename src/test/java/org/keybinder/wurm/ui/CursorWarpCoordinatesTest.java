package org.keybinder.wurm.ui;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class CursorWarpCoordinatesTest {
    @Test public void convertsCenterAndInvertsY() {
        CursorWarpCoordinates.Point point = CursorWarpCoordinates.fromGuiCenter(
                10, 20, 40, 10, 100, 80);
        assertEquals(30, point.getX());
        assertEquals(55, point.getY());
    }

    @Test public void clampsToClientBounds() {
        CursorWarpCoordinates.Point point = CursorWarpCoordinates.fromGuiCenter(
                500, -100, 20, 10, 100, 80);
        assertEquals(99, point.getX());
        assertEquals(79, point.getY());
    }

    @Test public void restoresAnExactGuiPointAndInvertsY() {
        CursorWarpCoordinates.Point point = CursorWarpCoordinates.fromGuiPoint(
                73, 19, 100, 80);
        assertEquals(73, point.getX());
        assertEquals(61, point.getY());
    }
}
