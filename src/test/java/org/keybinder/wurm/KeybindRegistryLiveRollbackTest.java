package org.keybinder.wurm;

import com.wurmonline.client.console.WurmConsole;
import org.junit.Test;
import org.keybinder.wurm.bind.BindSnapshot;
import org.keybinder.wurm.bind.ManagedBindAccess;
import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.migration.CustomActionsImporter;
import org.keybinder.wurm.model.ConsoleCommandStep;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.queue.ActionQueueCostCalculator;
import org.keybinder.wurm.storage.KeybindStore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class KeybindRegistryLiveRollbackTest {
    @Test
    public void failedLiveInstallRestoresRegistryStoreAndBothChords() throws Exception {
        Path file = Files.createTempDirectory("registry-live-rollback")
                .resolve("records.properties");
        KeybindStore store = new KeybindStore(file);
        FailingBinds binds = new FailingBinds();
        KeybindRegistry registry = new KeybindRegistry(store, binds,
                new CustomActionsImporter(), new ActionQueueCostCalculator(),
                new EventLogger(Logger.getAnonymousLogger()));
        KeybindRecord original = record("record", "Original", "R", "say original");
        registry.add(original, null, 10);
        assertEquals("keybinder_run record", binds.command("R"));

        binds.failNextInstall = true;
        try {
            registry.updateKeybind(original.getId(), "Changed", "T",
                    Collections.<KeybindStep>singletonList(
                            new ConsoleCommandStep("say changed")),
                    "User", "Server", null, 10);
            fail("Expected live install failure");
        } catch (IllegalStateException expected) { }

        assertEquals("R", registry.find(original.getId()).getKey());
        assertEquals("keybinder_run record", binds.command("R"));
        assertNull(binds.command("T"));
        assertEquals("R", store.load().get(0).getKey());
    }

    private static KeybindRecord record(String id, String name, String key, String command) {
        return new KeybindRecord(id, name, key,
                Collections.<KeybindStep>singletonList(new ConsoleCommandStep(command)));
    }

    private static final class FailingBinds implements ManagedBindAccess {
        private final Map<String, String> values = new LinkedHashMap<String, String>();
        private boolean failNextInstall;

        private String command(String key) { return values.get(normalize(key)); }

        @Override public List<BindSnapshot> snapshot(WurmConsole console) {
            List<BindSnapshot> result = new ArrayList<BindSnapshot>();
            for (Map.Entry<String, String> entry : values.entrySet())
                result.add(new BindSnapshot(0, entry.getKey(), entry.getValue()));
            return result;
        }

        @Override public BindSnapshot findByKey(WurmConsole console, String key) {
            String command = command(key);
            return command == null ? null : new BindSnapshot(0, key, command);
        }

        @Override public void install(WurmConsole console, String key, String command) {
            values.put(normalize(key), command);
            if (failNextInstall) {
                failNextInstall = false;
                throw new IllegalStateException("simulated live install failure");
            }
        }

        @Override public boolean removeIfOwned(WurmConsole console, String key,
                                               String expectedCommand) {
            String current = command(key);
            if (current == null || !current.equalsIgnoreCase(expectedCommand)) return false;
            values.remove(normalize(key));
            return true;
        }

        private static String normalize(String key) {
            return key.toUpperCase(Locale.ENGLISH);
        }
    }
}
