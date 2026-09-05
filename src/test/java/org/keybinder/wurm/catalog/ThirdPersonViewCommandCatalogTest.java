package org.keybinder.wurm.catalog;

import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ThirdPersonViewCommandCatalogTest {
    @Test
    public void staysHiddenWhenCameraModIsNotInstalled() {
        ThirdPersonViewCommandCatalog catalog = ThirdPersonViewCommandCatalog.load(
                "not.installed.CameraMod", getClass().getClassLoader());

        assertFalse(catalog.isVisible());
        assertTrue(catalog.getEntries().isEmpty());
    }

    @Test
    public void discoversCompleteCommandsFromInstalledProvider() {
        ThirdPersonViewCommandCatalog catalog = ThirdPersonViewCommandCatalog.load(
                ValidProvider.class.getName(), getClass().getClassLoader());

        assertTrue(catalog.isVisible());
        assertEquals("3rd Person View", ThirdPersonViewCommandCatalog.DISPLAY_NAME);
        assertEquals(3, catalog.getEntries().size());
        assertEquals("Toggle camera", catalog.find(" TP TOGGLE ").getDisplayName());
        assertEquals("tp orbit-hold", catalog.getEntries().get(2).getCommand());
        for (ThirdPersonViewCommandCatalog.Entry entry : catalog.getEntries()) {
            String command = entry.getCommand().toLowerCase(Locale.ENGLISH);
            assertFalse(command.contains("tool"));
            assertFalse(command.contains("target"));
        }
    }

    @Test
    public void filtersPlaceholdersMalformedRowsAndDuplicates() {
        ThirdPersonViewCommandCatalog catalog = ThirdPersonViewCommandCatalog.load(
                PartlyInvalidProvider.class.getName(), getClass().getClassLoader());

        assertTrue(catalog.isVisible());
        assertEquals(1, catalog.getEntries().size());
        assertNotNull(catalog.find("tp toggle"));
        assertNull(catalog.find("tp zoom <0..1>"));
    }

    @Test
    public void hidesProviderWithWrongContract() {
        ThirdPersonViewCommandCatalog catalog = ThirdPersonViewCommandCatalog.load(
                WrongProvider.class.getName(), getClass().getClassLoader());

        assertFalse(catalog.isVisible());
    }

    public static final class ValidProvider {
        public static String[][] getKeybinderCommandCatalog() {
            return new String[][]{
                    {"Toggle camera", "tp toggle"},
                    {"Zoom in", "tp zoom-in"},
                    {"Hold orbit", "tp orbit-hold"}
            };
        }
    }

    public static final class PartlyInvalidProvider {
        public static String[][] getKeybinderCommandCatalog() {
            return new String[][]{
                    {"Toggle camera", "tp toggle"},
                    {"Duplicate", "TP TOGGLE"},
                    {"Incomplete zoom", "tp zoom <0..1>"},
                    {"Wrong namespace", "say hello"},
                    {"Missing command"}
            };
        }
    }

    public static final class WrongProvider {
        public static String getKeybinderCommandCatalog() {
            return "tp toggle";
        }
    }
}
