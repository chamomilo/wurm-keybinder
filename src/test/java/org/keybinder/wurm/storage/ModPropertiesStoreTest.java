package org.keybinder.wurm.storage;

import org.junit.Test;
import org.keybinder.wurm.i18n.LocalizationSettings;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class ModPropertiesStoreTest {
    @Test
    public void savesAndLoadsPortugueseWithoutDroppingOldSettings() throws Exception {
        Path file = Files.createTempDirectory("keybinder-settings")
                .resolve("keybinder.properties");
        Properties source = new Properties();
        source.setProperty("skipIntroPage", "true");
        source.setProperty("valuePackProvided", "true");
        source.setProperty("existingSetting", "unchanged");
        LocalizationSettings.save(source, "pt-BR");

        new ModPropertiesStore(file).save(source);

        Properties loaded = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            loaded.load(input);
        }
        assertEquals("pt-BR", LocalizationSettings.load(loaded));
        assertEquals("true", loaded.getProperty("skipIntroPage"));
        assertEquals("true", loaded.getProperty("valuePackProvided"));
        assertEquals("unchanged", loaded.getProperty("existingSetting"));
    }
}
