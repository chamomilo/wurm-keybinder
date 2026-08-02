package org.keybinder.wurm.transfer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.Test;
import org.keybinder.wurm.KeybindRegistry;
import org.keybinder.wurm.bind.VanillaBindService;
import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.migration.CustomActionsImporter;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.queue.ActionQueueCostCalculator;
import org.keybinder.wurm.storage.KeybindStore;
import org.keybinder.wurm.storage.PackagedResourceLoader;

public class ValuePackProviderTest {
    @Test
    public void bundledPackIsProvidedOnceAndEveryRecordKeepsItsProvenance() throws Exception {
        Path file = Files.createTempDirectory("keybinder-value-pack")
                .resolve("keybinds.properties");
        KeybindRegistry registry = registry(file);
        Properties settings = new Properties();
        final int[] saves = {0};

        ValuePackProvider.ProvisionResult first;
        try (InputStream pack = openPack()) {
            first = new ValuePackProvider().provideIfNeeded(settings, pack, registry,
                    value -> saves[0]++);
        }

        assertTrue(first.wasProvidedNow());
        assertEquals(8, first.getImportResult().getImported());
        assertEquals("true", settings.getProperty(ValuePackProvider.PROVIDED_SETTING));
        assertEquals(1, saves[0]);
        assertEquals(8, registry.snapshot().size());
        for (KeybindRecord record : registry.snapshot()) {
            assertTrue(record.isValuePack());
            assertFalse(record.isEnabled());
        }

        ValuePackProvider.ProvisionResult second = new ValuePackProvider()
                .provideIfNeeded(settings, null, registry, value -> saves[0]++);
        assertFalse(second.wasProvidedNow());
        assertEquals(1, saves[0]);
        assertEquals(8, registry.snapshot().size());

        // Simulate an upgrade replacing the distributable mod properties with
        // its false default. The durable data marker repairs it without reimport.
        settings.setProperty(ValuePackProvider.PROVIDED_SETTING, "false");
        ValuePackProvider.ProvisionResult afterUpgrade = new ValuePackProvider()
                .provideIfNeeded(settings, null, registry, value -> saves[0]++);
        assertFalse(afterUpgrade.wasProvidedNow());
        assertEquals("true", settings.getProperty(ValuePackProvider.PROVIDED_SETTING));
        assertEquals(2, saves[0]);
        assertEquals(8, registry.snapshot().size());

        KeybindStore reloadedStore = new KeybindStore(file);
        List<KeybindRecord> reloaded = reloadedStore.load();
        assertEquals(8, reloaded.size());
        assertTrue(reloadedStore.wasValuePackProvided());
        for (KeybindRecord record : reloaded) assertTrue(record.isValuePack());
    }

    @Test
    public void failedSettingSaveLeavesFlagClearAndRetryDoesNotDuplicateRecords()
            throws Exception {
        Path file = Files.createTempDirectory("keybinder-value-pack-retry")
                .resolve("keybinds.properties");
        KeybindRegistry registry = registry(file);
        Properties settings = new Properties();
        ValuePackProvider provider = new ValuePackProvider();

        try (InputStream pack = openPack()) {
            provider.provideIfNeeded(settings, pack, registry, value -> {
                throw new IOException("settings unavailable");
            });
            fail("Expected settings save to fail");
        } catch (IOException expected) {
            assertEquals("settings unavailable", expected.getMessage());
        }
        assertFalse(Boolean.parseBoolean(settings.getProperty(
                ValuePackProvider.PROVIDED_SETTING, "false")));
        assertEquals(8, registry.snapshot().size());
        assertTrue(registry.wasValuePackProvided());

        ValuePackProvider.ProvisionResult retry;
        try (InputStream pack = openPack()) {
            retry = provider.provideIfNeeded(settings, pack, registry, value -> { });
        }
        assertFalse(retry.wasProvidedNow());
        assertEquals(0, retry.getImportResult().getImported());
        assertEquals(0, retry.getImportResult().getSkippedDuplicates());
        assertEquals(8, registry.snapshot().size());
        assertEquals("true", settings.getProperty(ValuePackProvider.PROVIDED_SETTING));
    }

    @Test
    public void staleTrueModSettingDoesNotBlockAGenuinelyCleanDataStore() throws Exception {
        Path file = Files.createTempDirectory("keybinder-value-pack-stale-setting")
                .resolve("keybinds.properties");
        KeybindRegistry registry = registry(file);
        Properties settings = new Properties();
        settings.setProperty(ValuePackProvider.PROVIDED_SETTING, "true");

        ValuePackProvider.ProvisionResult result;
        try (InputStream pack = openPack()) {
            result = new ValuePackProvider().provideIfNeeded(settings, pack, registry,
                    value -> { });
        }

        assertTrue(result.wasProvidedNow());
        assertEquals(8, result.getImportResult().getImported());
        assertEquals(8, registry.snapshot().size());
        assertTrue(registry.wasValuePackProvided());
    }

    @Test
    public void installedJarFallbackReadsPackWhenSharedLoaderHidesResources()
            throws Exception {
        Path archive = Files.createTempFile("keybinder-value-pack-runtime", ".jar");
        try {
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive));
                 InputStream source = openPack()) {
                zip.putNextEntry(new ZipEntry("keybinder/value-pack.keybinder"));
                byte[] buffer = new byte[4096];
                int read;
                while ((read = source.read(buffer)) >= 0) zip.write(buffer, 0, read);
                zip.closeEntry();
            }

            try (InputStream fallback = PackagedResourceLoader.openArchive(
                    archive, "keybinder/value-pack.keybinder")) {
                assertNotNull(fallback);
                assertEquals(8, new KeybindTransferStore().read(fallback).size());
            }
        } finally {
            Files.deleteIfExists(archive);
        }
    }

    @Test
    public void failedRegistryLoadPreventsAutomaticPackFromReplacingUserData()
            throws Exception {
        Path file = Files.createTempDirectory("keybinder-value-pack-corrupt")
                .resolve("keybinds.properties");
        Files.write(file, Arrays.asList("schema=not-a-number"),
                StandardCharsets.ISO_8859_1);
        byte[] original = Files.readAllBytes(file);
        KeybindRegistry registry = registry(file);
        assertFalse(registry.isLoadedSuccessfully());

        try (InputStream pack = openPack()) {
            new ValuePackProvider().provideIfNeeded(new Properties(), pack, registry,
                    value -> { });
            fail("Expected provisioning to stop after a failed registry load");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("not loaded successfully"));
        }

        assertArrayEquals(original, Files.readAllBytes(file));
        assertTrue(registry.snapshot().isEmpty());
    }

    private static InputStream openPack() {
        InputStream input = ValuePackProvider.class.getResourceAsStream(
                ValuePackProvider.RESOURCE);
        assertNotNull(input);
        return input;
    }

    private static KeybindRegistry registry(Path file) {
        KeybindRegistry registry = new KeybindRegistry(new KeybindStore(file),
                new VanillaBindService(), new CustomActionsImporter(),
                new ActionQueueCostCalculator(),
                new EventLogger(Logger.getLogger("ValuePackProviderTest")));
        registry.load();
        registry.setCreationContext("User", "Server");
        return registry;
    }
}
