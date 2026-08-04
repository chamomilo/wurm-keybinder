package org.keybinder.wurm.ui;

/** Pure GUI-to-LWJGL coordinate conversion for selector pointer warp. */
public final class CursorWarpCoordinates {
    private CursorWarpCoordinates() { }

    public static Point fromGuiCenter(int componentX, int componentY,
                                      int componentWidth, int componentHeight,
                                      int gameWidth, int gameHeight) {
        int uiX = componentX + Math.max(0, componentWidth) / 2;
        int uiY = componentY + Math.max(0, componentHeight) / 2;
        return fromGuiPoint(uiX, uiY, gameWidth, gameHeight);
    }

    public static Point fromGuiPoint(int uiX, int uiY,
                                     int gameWidth, int gameHeight) {
        int x = clamp(uiX, 0, Math.max(0, gameWidth - 1));
        int y = clamp(gameHeight - uiY, 0, Math.max(0, gameHeight - 1));
        return new Point(x, y);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public static final class Point {
        private final int x;
        private final int y;
        private Point(int x, int y) { this.x = x; this.y = y; }
        public int getX() { return x; }
        public int getY() { return y; }
    }
}
