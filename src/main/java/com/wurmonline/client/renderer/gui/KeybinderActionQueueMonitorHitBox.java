package com.wurmonline.client.renderer.gui;

/** Coordinate helper kept independent from Wurm GUI static initialization. */
final class KeybinderActionQueueMonitorHitBox {
    private KeybinderActionQueueMonitorHitBox() {
    }

    static boolean contains(int mouseX, int mouseY, int left, int top,
                            int width, int height) {
        return mouseX >= left && mouseX < left + width
                && mouseY >= top && mouseY < top + height;
    }

    static int anchoredX(boolean leftSide, int screenWidth, int componentWidth) {
        return leftSide ? 0 : Math.max(0, screenWidth - componentWidth);
    }

    static int lampColumnLeft(boolean leftSide, int componentX,
                              int componentWidth, int margin, int lampSize) {
        return leftSide ? componentX + componentWidth - margin - lampSize
                : componentX + margin;
    }

    static boolean arrowPointsRight(boolean leftSide, boolean expanded) {
        return leftSide != expanded;
    }

    static int alignedTextLeft(boolean leftSide, int defaultLeft,
                               int textRight, int textWidth) {
        return leftSide ? Math.max(defaultLeft, textRight - textWidth) : defaultLeft;
    }
}
