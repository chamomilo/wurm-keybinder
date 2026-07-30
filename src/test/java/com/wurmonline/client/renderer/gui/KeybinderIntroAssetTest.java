package com.wurmonline.client.renderer.gui;

import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class KeybinderIntroAssetTest {
    @Test
    public void bannerIsRgbaAndMatchesItsNaturalUiSize() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/keybinder/intro-banner.png")) {
            assertNotNull("intro banner resource", input);
            BufferedImage image = ImageIO.read(input);
            assertNotNull("decoded intro banner", image);
            assertEquals(800, image.getWidth());
            assertEquals(200, image.getHeight());
            assertEquals(image.getWidth(), image.getHeight() * 4);
            assertTrue("banner must carry an alpha channel", image.getColorModel().hasAlpha());
        }
    }
}
