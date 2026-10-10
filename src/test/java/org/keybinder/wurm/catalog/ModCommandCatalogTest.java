package org.keybinder.wurm.catalog;

import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ModCommandCatalogTest {
    @Test
    public void staysHiddenWhenProviderIsNotInstalled() {
        ModCommandCatalog catalog = ModCommandCatalog.load(
                "not.installed.ClientMod", "Missing", getClass().getClassLoader());

        assertFalse(catalog.isVisible());
        assertTrue(catalog.getEntries().isEmpty());
    }

    @Test
    public void discoversCompleteCommandsFromAnyProviderNamespace() {
        ModCommandCatalog catalog = ModCommandCatalog.load(
                ValidProvider.class.getName(), "Holster Weapons", getClass().getClassLoader());

        assertTrue(catalog.isVisible());
        assertEquals("Holster Weapons", catalog.getDisplayName());
        assertEquals(3, catalog.getEntries().size());
        assertEquals("Toggle holster", catalog.find(" HOLSTER TOGGLE ").getDisplayName());
        assertEquals("tp orbit-hold", catalog.getEntries().get(2).getCommand());
        for (ModCommandCatalog.Entry entry : catalog.getEntries()) {
            String command = entry.getCommand().toLowerCase(Locale.ENGLISH);
            assertFalse(command.contains("<"));
            assertFalse(command.contains("{"));
        }
    }

    @Test
    public void filtersPlaceholdersMalformedRowsAndDuplicates() {
        ModCommandCatalog catalog = ModCommandCatalog.load(
                PartlyInvalidProvider.class.getName(), "Mixed", getClass().getClassLoader());

        assertTrue(catalog.isVisible());
        assertEquals(2, catalog.getEntries().size());
        assertNotNull(catalog.find("holster toggle"));
        assertNotNull(catalog.find("say hello"));
        assertNull(catalog.find("holster offset <x>"));
    }

    @Test
    public void hidesProviderWithWrongContract() {
        ModCommandCatalog catalog = ModCommandCatalog.load(
                WrongProvider.class.getName(), "Wrong", getClass().getClassLoader());

        assertFalse(catalog.isVisible());
    }

    public static final class ValidProvider {
        public static String[][] getKeybinderCommandCatalog() {
            return new String[][]{
                    {"Toggle holster", "holster toggle"},
                    {"Open studio", "holster studio"},
                    {"Hold orbit", "tp orbit-hold"}
            };
        }
    }

    public static final class PartlyInvalidProvider {
        public static String[][] getKeybinderCommandCatalog() {
            return new String[][]{
                    {"Toggle holster", "holster toggle"},
                    {"Duplicate", "HOLSTER TOGGLE"},
                    {"Incomplete offset", "holster offset <x>"},
                    {"Other namespace", "say hello"},
                    {"Missing command"},
                    {"Leading slash", "/holster toggle"},
                    {"Multiline", "holster toggle\nquit"}
            };
        }
    }

    public static final class WrongProvider {
        public static String getKeybinderCommandCatalog() {
            return "holster toggle";
        }
    }
}
