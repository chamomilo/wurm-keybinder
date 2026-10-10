package org.keybinder.wurm.catalog;

import org.gotti.wurmunlimited.modloader.interfaces.ModEntry;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ModCommandCatalogRegistryTest {
    @Before
    public void resetRegistry() {
        ModCommandCatalogRegistry.clearForTests();
    }

    @Test
    public void observesEveryCompatibleModAndUsesUpdateName() {
        ModCommandCatalogRegistry.modInitialized(entry(
                "holster-weapons", "Holster Weapons", new HolsterProvider()));
        ModCommandCatalogRegistry.modInitialized(entry(
                "third-person-view", "3rd Person View", new CameraProvider()));
        ModCommandCatalogRegistry.modInitialized(entry(
                "unrelated", "Unrelated", new Object()));

        List<ModCommandCatalog> catalogs = ModCommandCatalogRegistry.snapshot();
        assertEquals(2, catalogs.size());
        assertEquals("3rd Person View", catalogs.get(0).getDisplayName());
        assertEquals("Holster Weapons", catalogs.get(1).getDisplayName());
        assertEquals("holster toggle",
                catalogs.get(1).getEntries().get(0).getCommand());
    }

    @Test
    public void replacesRepeatedCallbackFromSameProvider() {
        ModCommandCatalogRegistry.modInitialized(entry(
                "first", "First name", new HolsterProvider()));
        ModCommandCatalogRegistry.modInitialized(entry(
                "second", "Renamed", new HolsterProvider()));

        List<ModCommandCatalog> catalogs = ModCommandCatalogRegistry.snapshot();
        assertEquals(1, catalogs.size());
        assertEquals("Renamed", catalogs.get(0).getDisplayName());
    }

    @Test
    public void ignoresNullAndUnsupportedMods() {
        ModCommandCatalogRegistry.modInitialized(null);
        ModCommandCatalogRegistry.modInitialized(entry("empty", "", null));
        ModCommandCatalogRegistry.modInitialized(entry("plain", "", new Object()));

        assertTrue(ModCommandCatalogRegistry.snapshot().isEmpty());
    }

    private static ModEntry<Object> entry(String id, String updateName, Object mod) {
        Properties properties = new Properties();
        if (updateName != null) properties.setProperty("updateName", updateName);
        return new FakeEntry(id, properties, mod);
    }

    public static final class HolsterProvider {
        public static String[][] getKeybinderCommandCatalog() {
            return new String[][]{{"Toggle holster", "holster toggle"}};
        }
    }

    public static final class CameraProvider {
        public static String[][] getKeybinderCommandCatalog() {
            return new String[][]{{"Toggle camera", "tp toggle"}};
        }
    }

    private static final class FakeEntry implements ModEntry<Object> {
        private final String name;
        private final Properties properties;
        private final Object mod;

        private FakeEntry(String name, Properties properties, Object mod) {
            this.name = name;
            this.properties = properties;
            this.mod = mod;
        }

        @Override public String getName() { return name; }
        @Override public Properties getProperties() { return properties; }
        @Override public Object getWurmMod() { return mod; }
    }
}
