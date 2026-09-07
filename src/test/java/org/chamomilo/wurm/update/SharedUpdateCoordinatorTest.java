package org.chamomilo.wurm.update;

import org.gotti.wurmunlimited.modloader.interfaces.ModEntry;
import org.gotti.wurmunlimited.modloader.interfaces.Versioned;
import org.junit.Test;

import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SharedUpdateCoordinatorTest {
    @Test public void firstHostOwnsTheProcessWideRegistry() {
        SharedUpdateCoordinator.Registry registry = new SharedUpdateCoordinator.Registry();
        RecordingHost first = new RecordingHost();
        RecordingHost second = new RecordingHost();

        assertTrue(registry.registerHost("keybinder", first));
        assertFalse(registry.registerHost("waypointer", second));
        assertEquals("keybinder", registry.ownerId());
        assertEquals(first, registry.owner());
    }

    @Test public void modListenerMetadataCollectsAllProviderModsAndDeduplicatesCallbacks() {
        SharedUpdateCoordinator.Registry registry = new SharedUpdateCoordinator.Registry();
        FakeEntry keybinder = entry("keybinder", "Keybinder", "0.9.0",
                "chamomilo/wurm-keybinder", "keybinder-{version}.zip");
        FakeEntry waypointer = entry("waypointer", "Waypointer", "1.4.2",
                "chamomilo/wurm-waypointer", "waypointer-{version}.zip");

        registry.observe(keybinder);
        registry.observe(waypointer);
        registry.observe(keybinder);
        registry.observe(new FakeEntry("foreign", new Properties(), new VersionedMod("8.0.0")));

        List<UpdateTarget> targets = registry.snapshot();
        assertEquals(2, targets.size());
        assertEquals("keybinder", targets.get(0).getId());
        assertEquals("waypointer", targets.get(1).getId());
        assertEquals("1.4.2", targets.get(1).getInstalledVersion());
    }

    @Test public void newerReleaseUsesExactConfiguredAsset() throws Exception {
        GitHubReleaseClient client = new GitHubReleaseClient(repository ->
                "{\"tag_name\":\"v0.10.0\",\"assets\":[{"
                        + "\"browser_download_url\":\"https://github.com/chamomilo/"
                        + "wurm-keybinder/releases/download/v0.10.0/"
                        + "keybinder-0.10.0.zip\"}]}");
        UpdateTarget target = SharedUpdateCoordinator.targetFrom(entry(
                "keybinder", "Keybinder", "0.9.9", "chamomilo/wurm-keybinder",
                "keybinder-{version}.zip"));

        ModUpdate update = GitHubReleaseClient.findUpdate(
                target, client.readLatest(target.getRepository()));

        assertNotNull(update);
        assertEquals("0.10.0", update.getLatestVersion());
        assertEquals("https://github.com/chamomilo/wurm-keybinder/releases/"
                        + "download/v0.10.0/keybinder-0.10.0.zip",
                update.getDownloadUrl());
    }

    @Test public void missingAssetFallsBackToRepositoryLatestReleasePage() throws Exception {
        GitHubReleaseClient client = new GitHubReleaseClient(repository ->
                "{\"tag_name\":\"v1.5.0\",\"assets\":[]}");
        UpdateTarget target = SharedUpdateCoordinator.targetFrom(entry(
                "waypointer", "Waypointer", "1.4.2", "chamomilo/wurm-waypointer",
                "waypointer-{version}.zip"));

        ModUpdate update = GitHubReleaseClient.findUpdate(
                target, client.readLatest(target.getRepository()));

        assertNotNull(update);
        assertEquals("https://github.com/chamomilo/wurm-waypointer/releases/latest",
                update.getDownloadUrl());
    }

    @Test public void currentOlderAndMalformedReleaseTagsDoNotNotify() {
        UpdateTarget target = SharedUpdateCoordinator.targetFrom(entry(
                "keybinder", "Keybinder", "0.9.0", "chamomilo/wurm-keybinder",
                "keybinder-{version}.zip"));

        assertNull(update(target, "v0.9.0"));
        assertNull(update(target, "v0.8.9"));
        assertNull(update(target, "nightly"));
    }

    @Test public void notificationUsesTheRequiredUserFacingTemplate() {
        ModUpdate update = new ModUpdate("keybinder", "Keybinder", "1.0.5",
                "1.0.6", "https://example.invalid/keybinder-1.0.6.zip");

        assertEquals("Wurm Keybinder Mod. Installed version: 1.0.5. "
                        + "Available version: 1.0.6. You can download here: "
                        + "https://example.invalid/keybinder-1.0.6.zip",
                update.getNotificationText());
    }

    @Test public void incompleteOrDifferentProtocolMetadataIsIgnored() {
        FakeEntry complete = entry("keybinder", "Keybinder", "0.9.0",
                "chamomilo/wurm-keybinder", "keybinder-{version}.zip");
        complete.getProperties().setProperty("updateCoordinatorProtocol", "2");
        assertNull(SharedUpdateCoordinator.targetFrom(complete));

        Properties incomplete = new Properties();
        incomplete.setProperty("updateProvider", "chamomilo");
        incomplete.setProperty("updateCoordinatorProtocol", "1");
        assertNull(SharedUpdateCoordinator.targetFrom(
                new FakeEntry("broken", incomplete, new VersionedMod("1.0.0"))));
    }

    private static ModUpdate update(UpdateTarget target, String tag) {
        return GitHubReleaseClient.findUpdate(target,
                GitHubReleaseClient.ReleaseSnapshot.fromPayload(
                        "{\"tag_name\":\"" + tag + "\"}"));
    }

    private static FakeEntry entry(String id, String name, String version,
                                   String repository, String asset) {
        Properties properties = new Properties();
        properties.setProperty("updateProvider", "chamomilo");
        properties.setProperty("updateCoordinatorProtocol", "1");
        properties.setProperty("updateId", id);
        properties.setProperty("updateName", name);
        properties.setProperty("updateRepo", repository);
        properties.setProperty("updateAsset", asset);
        properties.setProperty("version", "ignored-by-Versioned");
        return new FakeEntry(id, properties, new VersionedMod(version));
    }

    private static final class RecordingHost implements SharedUpdateCoordinator.Host {
        @Override public void updatesReady(List<ModUpdate> updates) { }
        @Override public void checkFailed(String repository, Throwable failure) { }
    }

    private static final class VersionedMod implements Versioned {
        private final String version;
        private VersionedMod(String version) { this.version = version; }
        @Override public String getVersion() { return version; }
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
