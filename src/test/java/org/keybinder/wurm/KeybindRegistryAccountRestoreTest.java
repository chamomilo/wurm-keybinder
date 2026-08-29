package org.keybinder.wurm;

import com.wurmonline.client.console.WurmConsole;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.Test;
import org.keybinder.wurm.bind.BindSnapshot;
import org.keybinder.wurm.bind.ManagedBindAccess;
import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.migration.CustomActionsImporter;
import org.keybinder.wurm.model.ConsoleCommandStep;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.queue.ActionQueueCostCalculator;
import org.keybinder.wurm.storage.AccountKeybindStateStore;
import org.keybinder.wurm.storage.KeybindStore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class KeybindRegistryAccountRestoreTest {
    @Test public void fourSimultaneousAltSessionsShareDefinitionsButKeepLiveBindsIndependent()
            throws Exception {
        Path directory = Files.createTempDirectory("account-live-sessions");
        Path definitions = directory.resolve("keybinds.properties");
        AccountKeybindStateStore accounts = new AccountKeybindStateStore(
                directory.resolve("accounts.properties"));
        List<KeybindRecord> shared = Arrays.asList(
                record("hud-menu", "HUD menu", "Q"),
                record("hud-other", "Other HUD", "Q"),
                record("multi-a", "Multi A", "E"),
                record("multi-b", "Multi B", "E"),
                record("smelt", "Smelt", "R"),
                record("combine", "Combine", "T"));
        for (KeybindRecord record : shared) record.setEnabled(false);
        new KeybindStore(definitions).save(shared);
        accounts.save("Alt One", set("hud-menu", "multi-a", "smelt"));
        accounts.save("Alt Two", set("hud-menu", "multi-b", "combine"));
        accounts.save("Alt Three", set("hud-other", "multi-a", "combine"));
        accounts.save("Alt Four", set("hud-other", "multi-b", "smelt"));

        String[] names = {"Alt One", "Alt Two", "Alt Three", "Alt Four"};
        String[][] expected = {
                {"hud-menu", "multi-a", "smelt"},
                {"hud-menu", "multi-b", "combine"},
                {"hud-other", "multi-a", "combine"},
                {"hud-other", "multi-b", "smelt"}
        };
        String[][] keys = {{"Q", "E", "R"}, {"Q", "E", "T"},
                {"Q", "E", "T"}, {"Q", "E", "R"}};
        for (int i = 0; i < names.length; i++) {
            MemoryBinds live = new MemoryBinds();
            live.put("Q", "keybinder_run hud-menu");
            live.put("E", "keybinder_run multi-a");
            live.put("R", "keybinder_run smelt");
            KeybindRegistry session = registry(definitions, accounts, live);

            assertTrue(session.restoreAccountBindings(names[i], console()));

            for (int j = 0; j < expected[i].length; j++)
                assertEquals("keybinder_run " + expected[i][j], live.get(keys[i][j]));
            for (KeybindRecord record : session.snapshot())
                assertEquals(set(expected[i]).contains(record.getId()), record.isEnabled());
        }
    }

    private static KeybindRegistry registry(Path definitions,
                                            AccountKeybindStateStore accounts,
                                            MemoryBinds binds) {
        KeybindRegistry registry = new KeybindRegistry(new KeybindStore(definitions),
                accounts, binds, new CustomActionsImporter(),
                new ActionQueueCostCalculator(),
                new EventLogger(Logger.getAnonymousLogger()));
        registry.load();
        return registry;
    }

    private static KeybindRecord record(String id, String name, String key) {
        return new KeybindRecord(id, name, key,
                Collections.<KeybindStep>singletonList(
                        new ConsoleCommandStep("say " + id)));
    }

    private static java.util.Set<String> set(String... values) {
        return new java.util.HashSet<String>(Arrays.asList(values));
    }

    private static WurmConsole console() throws Exception {
        Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
        return (WurmConsole) unsafe.allocateInstance(WurmConsole.class);
    }

    private static final class MemoryBinds implements ManagedBindAccess {
        private final Map<String, String> values = new LinkedHashMap<String, String>();

        private void put(String key, String command) {
            values.put(normalize(key), command);
        }

        private String get(String key) {
            return values.get(normalize(key));
        }

        @Override public List<BindSnapshot> snapshot(WurmConsole console) {
            List<BindSnapshot> result = new ArrayList<BindSnapshot>();
            for (Map.Entry<String, String> value : values.entrySet())
                result.add(new BindSnapshot(0, value.getKey(), value.getValue()));
            return result;
        }

        @Override public BindSnapshot findByKey(WurmConsole console, String key) {
            String command = get(key);
            return command == null ? null : new BindSnapshot(0, key, command);
        }

        @Override public void install(WurmConsole console, String key, String command) {
            put(key, command);
        }

        @Override public boolean removeIfOwned(WurmConsole console, String key,
                                               String expectedCommand) {
            String command = get(key);
            if (command == null || !command.equalsIgnoreCase(expectedCommand)) return false;
            values.remove(normalize(key));
            return true;
        }

        private static String normalize(String key) {
            return key.trim().toUpperCase(Locale.ENGLISH);
        }
    }
}
