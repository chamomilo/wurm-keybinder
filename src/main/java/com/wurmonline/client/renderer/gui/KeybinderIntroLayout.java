package com.wurmonline.client.renderer.gui;

/** Pure layout calculations kept separate so they can be tested without loading Wurm/JavaFX. */
final class KeybinderIntroLayout {
    private static final int BANNER_WIDTH = 800;
    private static final int BANNER_HEIGHT = 200;

    private KeybinderIntroLayout() { }

    static int bannerWidth(int contentWidth) {
        return BANNER_WIDTH;
    }

    static int bannerHeight(int contentWidth) {
        return BANNER_HEIGHT;
    }
}
