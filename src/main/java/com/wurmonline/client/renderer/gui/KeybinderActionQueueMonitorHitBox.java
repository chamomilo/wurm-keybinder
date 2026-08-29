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
}
