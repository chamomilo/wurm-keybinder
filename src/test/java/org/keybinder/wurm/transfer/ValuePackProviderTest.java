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
import org.keybinder.wurm.resource.PackagedResourceLoader;

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
        assertEquals(11, first.getImportResult().getImported());
        assertEquals("true", settings.getProperty(ValuePackProvider.PROVIDED_SETTING));
        assertEquals(1, saves[0]);
        assertEquals(11, registry.snapshot().size());
        assertEquals(ValuePackProvider.CURRENT_REVISION, registry.getValuePackRevision());
        for (KeybindRecord record : registry.snapshot()) {
            assertTrue(record.isValuePack());
            assertFalse(record.isEnabled());
        }

        ValuePackProvider.ProvisionResult second = new ValuePackProvider()
                .provideIfNeeded(settings, null, registry, value -> saves[0]++);
        assertFalse(second.wasProvidedNow());
        assertEquals(1, saves[0]);
        assertEquals(11, registry.snapshot().size());

        // Simulate an upgrade replacing the distributable mod properties with
        // its false default. The durable data marker repairs it without reimport.
        settings.setProperty(ValuePackProvider.PROVIDED_SETTING, "false");
        ValuePackProvider.ProvisionResult afterUpgrade = new ValuePackProvider()
                .provideIfNeeded(settings, null, registry, value -> saves[0]++);
        assertFalse(afterUpgrade.wasProvidedNow());
        assertEquals("true", settings.getProperty(ValuePackProvider.PROVIDED_SETTING));
        assertEquals(2, saves[0]);
        assertEquals(11, registry.snapshot().size());

        KeybindStore reloadedStore = new KeybindStore(file);
        List<KeybindRecord> reloaded = reloadedStore.load();
        assertEquals(11, reloaded.size());
        assertTrue(reloadedStore.wasValuePackProvided());
        assertEquals(ValuePackProvider.CURRENT_REVISION, reloadedStore.getValuePackRevision());
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
        assertEquals(11, registry.snapshot().size());
        assertTrue(registry.wasValuePackProvided());

        ValuePackProvider.ProvisionResult retry;
        try (InputStream pack = openPack()) {
            retry = provider.provideIfNeeded(settings, pack, registry, value -> { });
        }
        assertFalse(retry.wasProvidedNow());
        assertEquals(0, retry.getImportResult().getImported());
        assertEquals(0, retry.getImportResult().getSkippedDuplicates());
        assertEquals(11, registry.snapshot().size());
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
        assertEquals(11, result.getImportResult().getImported());
        assertEquals(11, registry.snapshot().size());
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
                assertEquals(11, new KeybindTransferStore().read(fallback).size());
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

    @Test
    public void bundledDefaultMatchesTheElevenCurrentKeybinds() throws Exception {
        List<PortableKeybindDefinition> definitions;
        try (InputStream pack = openPack()) { definitions = new KeybindTransferStore().read(pack); }
        String[] names = {"embark/disembark", "Telekinetic Push", "Telekinetic Pull", "clockwise",
                "counterclockwise", "Zoom in", "Zoom out", "orbit", "(Quick) Menu for HUD",
                "(Multi) Multi-Key", "pick or plant sprout (from iinventory)"};
        String[] keys = {"TAB", "CTRL+MOUSE_WHEEL_UP", "CTRL+MOUSE_WHEEL_DOWN",
                "CTRL+SHIFT+MOUSE_WHEEL_UP", "CTRL+SHIFT+MOUSE_WHEEL_DOWN", "MOUSE_WHEEL_UP",
                "MOUSE_WHEEL_DOWN", "MOUSE2", "Q", "E", "F"};
        assertEquals(names.length, definitions.size());
        for (int i = 0; i < names.length; i++) {
            assertEquals(names[i], definitions.get(i).getName());
            assertEquals(keys[i], definitions.get(i).getIntendedKey());
        }
        assertTrue(definitions.get(8).isHudMulti());
        assertEquals(12, definitions.get(8).getVariants().size());
        assertFalse(definitions.get(9).isHudMulti());
        assertEquals(10, definitions.get(9).getVariants().size());
        assertEquals(2, definitions.get(10).getVariants().get(0).getSteps().size());
    }

    @Test
    public void oldPackRecipientsReceiveTheRevisionWithoutChangingTheirEnabledBind() throws Exception {
        Path file = Files.createTempDirectory("keybinder-pack-upgrade").resolve("keybinds.properties");
        List<PortableKeybindDefinition> definitions;
        try (InputStream pack = openPack()) { definitions = new KeybindTransferStore().read(pack); }
        KeybindRecord existing = definitions.get(5).toRecord("User", "Server");
        existing.setEnabled(true);
        existing.setPreviousManagedCommand("my-owned-command");
        KeybindStore oldStore = new KeybindStore(file);
        oldStore.setValuePackProvided(true);
        oldStore.save(java.util.Collections.singletonList(existing));
        // Version 1 installations had only the boolean marker.
        Properties oldData = new Properties();
        try (InputStream input = Files.newInputStream(file)) { oldData.load(input); }
        oldData.remove("valuePackRevision");
        try (java.io.OutputStream output = Files.newOutputStream(file)) { oldData.store(output, "legacy pack"); }

        KeybindRegistry registry = registry(file);
        assertEquals(1, registry.getValuePackRevision());
        try (InputStream pack = openPack()) {
            ValuePackProvider.ProvisionResult result = new ValuePackProvider().provideIfNeeded(
                    new Properties(), pack, registry, value -> {});
            assertEquals(10, result.getImportResult().getImported());
            assertEquals(1, result.getImportResult().getSkippedDuplicates());
        }
        KeybindRecord retained = registry.find(existing.getId());
        assertTrue(retained.isEnabled());
        assertTrue(retained.isValuePack());
        assertEquals("my-owned-command", retained.getPreviousManagedCommand());
        for (KeybindRecord record : registry.snapshot()) {
            assertTrue(record.isValuePack());
            if (!record.getId().equals(existing.getId())) assertFalse(record.isEnabled());
        }
        KeybindRegistry reloaded = registry(file);
        assertEquals(ValuePackProvider.CURRENT_REVISION, reloaded.getValuePackRevision());
        assertFalse(new ValuePackProvider().provideIfNeeded(new Properties(), null, reloaded,
                value -> {}).wasProvidedNow());
        assertEquals(11, reloaded.snapshot().size());
    }

    @Test
    public void failedPackUpgradeRollsBackItsRevisionAndRecords() throws Exception {
        Path file = Files.createTempDirectory("keybinder-pack-upgrade-failure").resolve("keybinds.properties");
        KeybindStore legacy = new KeybindStore(file);
        legacy.setValuePackProvided(true);
        legacy.save(java.util.Collections.emptyList());
        KeybindRegistry registry = registry(file);
        Path blockedBackup = file.resolveSibling(file.getFileName() + ".bak");
        Files.createDirectory(blockedBackup);
        Path blocker = blockedBackup.resolve("blocker");
        Files.write(blocker, new byte[]{1});
        try (InputStream pack = openPack()) {
            new ValuePackProvider().provideIfNeeded(new Properties(), pack, registry, value -> {});
            fail("Expected registry save to fail");
        } catch (IOException expected) { assertNotNull(expected.getMessage()); }
        assertEquals(1, registry.getValuePackRevision());
        assertTrue(registry.snapshot().isEmpty());
        assertEquals(1, registry(file).getValuePackRevision());
        Files.delete(blocker);
        Files.delete(blockedBackup);
        try (InputStream pack = openPack()) {
            assertEquals(11, new ValuePackProvider().provideIfNeeded(new Properties(), pack, registry,
                    value -> {}).getImportResult().getImported());
        }
    }

    @Test public void oldHudRecordLoadsWithQuickTagAndReimportKeepsItsIdentity() throws Exception {
        Path file = Files.createTempDirectory("keybinder-quick-upgrade").resolve("keybinds.properties");
        PortableKeybindDefinition definition;
        try (InputStream pack = openPack()) {
            definition = new KeybindTransferStore().read(pack).get(8);
        }
        KeybindRecord old = definition.toRecord("User", "Server");
        old.setName("(HUD) (Multi) Menu for HUD");
        old.setEnabled(false);
        old.setValuePack(true);
        new KeybindStore(file).save(java.util.Collections.singletonList(old));
        KeybindRegistry loaded = registry(file);
        KeybindRecord migrated = loaded.find(old.getId());
        assertEquals("(Quick) Menu for HUD", migrated.getName());
        assertTrue(migrated.isHudMulti());
        assertFalse(migrated.isEnabled());
        assertEquals(old.getActiveVariantId(), migrated.getActiveVariantId());
        assertEquals(old.getVariants().get(0).getId(), migrated.getVariants().get(0).getId());
        assertEquals(12, migrated.getVariants().size());
        PortableKeybindDefinition legacy = new PortableKeybindDefinition(old.getName(),
                definition.getIntendedKey(), true, definition.getActiveVariantIndex(),
                definition.getVariants());
        TransferImportResult result = loaded.importValuePack(java.util.Collections.singletonList(legacy));
        assertEquals(0, result.getImported());
        assertEquals(1, result.getSkippedDuplicates());
        assertEquals(1, loaded.snapshot().size());
        assertEquals(old.getId(), registry(file).snapshot().get(0).getId());
    }

    private static InputStream openPack() {
        InputStream input = ValuePackProvider.class.getResourceAsStream(
                ValuePackProvider.RESOURCE);
        assertNotNull(input);
        return input;
    }

    private static KeybindRegistry registry(Path file) {
        return registry(new KeybindStore(file));
    }

    private static KeybindRegistry registry(KeybindStore store) {
        KeybindRegistry registry = new KeybindRegistry(store,
                new VanillaBindService(), new CustomActionsImporter(),
                new ActionQueueCostCalculator(),
                new EventLogger(Logger.getLogger("ValuePackProviderTest")));
        registry.load();
        registry.setCreationContext("User", "Server");
        return registry;
    }
}
