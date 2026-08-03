package org.keybinder.wurm;

import com.wurmonline.client.console.WurmConsole;
import org.junit.Test;
import org.keybinder.wurm.bind.BindSnapshot;
import org.keybinder.wurm.bind.ManagedBindAccess;
import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.migration.CustomActionsImporter;
import org.keybinder.wurm.model.RecordType;
import org.keybinder.wurm.queue.ActionQueueCostCalculator;
import org.keybinder.wurm.storage.KeybindStore;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

public class KeybindRegistryVanillaImportTest {
    @Test public void importsOnlyExplicitlyReviewedRowsAndPreservesOtherLiveBinds()
            throws Exception {
        MemoryBinds live = new MemoryBinds();
        live.put("R", "act 163 tool");
        live.put("X", "exec custom.txt");
        KeybindRegistry registry = registry(live);

        int imported = registry.importReviewed(null,
                Collections.singletonList(new BindSnapshot(0, "R", "act 163 tool")), 10);

        assertEquals(1, imported);
        assertEquals(1, registry.snapshot().size());
        assertEquals(RecordType.ACTION_CHAIN, registry.snapshot().get(0).getType());
        assertEquals("keybinder_run " + registry.snapshot().get(0).getId(), live.get("R"));
        assertEquals("exec custom.txt", live.get("X"));
    }

    @Test public void explicitlySelectedUnknownCommandIsStoredAsExactRawCommand()
            throws Exception {
        MemoryBinds live = new MemoryBinds();
        live.put("X", "exec Custom File.txt");
        KeybindRegistry registry = registry(live);

        assertEquals(1, registry.importReviewed(null,
                Collections.singletonList(
                        new BindSnapshot(0, "X", "exec Custom File.txt")), 10));

        assertEquals(RecordType.RAW_VANILLA_COMMAND,
                registry.snapshot().get(0).getType());
        assertEquals("exec Custom File.txt",
                registry.snapshot().get(0).getOriginalCommand());
    }

    private static KeybindRegistry registry(MemoryBinds binds) throws Exception {
        KeybindStore store = new KeybindStore(Files.createTempDirectory("import-reviewed")
                .resolve("records.properties"));
        return new KeybindRegistry(store, binds, new CustomActionsImporter(),
                new ActionQueueCostCalculator(),
                new EventLogger(Logger.getAnonymousLogger()));
    }

    private static final class MemoryBinds implements ManagedBindAccess {
        private final Map<String, String> values = new LinkedHashMap<String, String>();
        void put(String key, String command) { values.put(normalize(key), command); }
        String get(String key) { return values.get(normalize(key)); }

        @Override public List<BindSnapshot> snapshot(WurmConsole console) {
            List<BindSnapshot> result = new ArrayList<BindSnapshot>();
            for (Map.Entry<String, String> entry : values.entrySet())
                result.add(new BindSnapshot(0, entry.getKey(), entry.getValue()));
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
            return key.toUpperCase(Locale.ENGLISH);
        }
    }
}
