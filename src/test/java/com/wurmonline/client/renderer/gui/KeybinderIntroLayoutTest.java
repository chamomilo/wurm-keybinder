package com.wurmonline.client.renderer.gui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class KeybinderIntroLayoutTest {
    @Test
    public void bannerUsesItsNaturalSizeWithoutStretching() {
        assertEquals(800, KeybinderIntroLayout.bannerWidth(800));
        assertEquals(200, KeybinderIntroLayout.bannerHeight(800));
    }
}
