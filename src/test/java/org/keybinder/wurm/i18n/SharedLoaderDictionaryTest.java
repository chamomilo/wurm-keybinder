package org.keybinder.wurm.i18n;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.keybinder.wurm.storage.PackagedResourceLoader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SharedLoaderDictionaryTest {
    @Test
    public void packagedDictionaryLoadsWhenSharedLoaderCannotExposeResources() throws Exception {
        Path archive = Files.createTempFile("keybinder-shared-loader", ".jar");
        try {
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
                zip.putNextEntry(new ZipEntry("keybinder/i18n/messages_pt-BR.properties"));
                zip.write("probe=Não foi possível\n".getBytes("UTF-8"));
                zip.closeEntry();
            }

            InputStream stream = PackagedResourceLoader.openArchive(
                    archive, "keybinder/i18n/messages_pt-BR.properties");
            assertNotNull(stream);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, "UTF-8"))) {
                assertEquals("probe=Não foi possível", reader.readLine());
            }
        } finally {
            Files.deleteIfExists(archive);
        }
    }
}
