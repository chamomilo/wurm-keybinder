package org.keybinder.wurm.ui;

import com.wurmonline.client.console.WurmConsole;
import org.junit.Test;
import org.keybinder.wurm.KeybindRegistry;
import org.keybinder.wurm.bind.BindSnapshot;
import org.keybinder.wurm.bind.ManagedBindAccess;
import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.migration.CustomActionsImporter;
import org.keybinder.wurm.model.ConflictResolution;
import org.keybinder.wurm.model.ConsoleCommandStep;
import org.keybinder.wurm.model.KeybindConflict;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindVariant;
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EditorWorkflowTest {
    @Test public void conflictWaitsForExplicitResolutionAndCancelKeepsDraftUntouched()
            throws Exception {
        Fixture fixture = new Fixture();
        KeybindRecord draft = fixture.registry.createDraft();
        fixture.binds.put("R", "EXAMINE");

        boolean saved = fixture.workflow.saveVariants(draft.getId(), "Reviewed", "R",
                variants(), draft.getActiveVariantId(), false, "User", "Server");

        assertFalse(saved);
        assertNotNull(fixture.environment.conflict);
        assertEquals("", fixture.registry.find(draft.getId()).getKey());
        fixture.workflow.resolve(ConflictResolution.CANCEL);
        assertTrue(fixture.environment.conflictClosed);
        assertEquals("", fixture.registry.find(draft.getId()).getKey());
    }

    @Test public void conflictFreeSaveCommitsAndClosesEditor() throws Exception {
        Fixture fixture = new Fixture();
        KeybindRecord draft = fixture.registry.createDraft();

        assertTrue(fixture.workflow.saveVariants(draft.getId(), "Reviewed", "R",
                variants(), draft.getActiveVariantId(), false, "User", "Server"));

        assertEquals("R", fixture.registry.find(draft.getId()).getKey());
        assertTrue(fixture.environment.editorClosed);
        assertTrue(fixture.environment.refreshed);
    }

    private static List<KeybindVariant> variants() {
        return Collections.singletonList(new KeybindVariant(null, "",
                Collections.singletonList(new ConsoleCommandStep("say hello"))));
    }

    private static final class Fixture {
        private final MemoryBinds binds = new MemoryBinds();
        private final KeybindRegistry registry;
        private final Environment environment = new Environment();
        private final EditorWorkflow workflow;

        private Fixture() throws Exception {
            registry = new KeybindRegistry(new KeybindStore(
                    Files.createTempDirectory("editor-workflow").resolve("records.properties")),
                    binds, new CustomActionsImporter(), new ActionQueueCostCalculator(),
                    new EventLogger(Logger.getAnonymousLogger()));
            workflow = new EditorWorkflow(registry,
                    new EventLogger(Logger.getAnonymousLogger()), environment);
        }
    }

    private static final class Environment implements EditorWorkflow.Environment {
        private KeybindConflict conflict;
        private boolean conflictClosed;
        private boolean editorClosed;
        private boolean refreshed;
        @Override public WurmConsole console() { return null; }
        @Override public int queueLimit() { return 10; }
        @Override public void showConflict(KeybindConflict value) { conflict = value; }
        @Override public void closeConflict() { conflictClosed = true; conflict = null; }
        @Override public void closeEditor() { editorClosed = true; }
        @Override public void refreshList() { refreshed = true; }
        @Override public void enabledStateChanged(String id, boolean enabled) { }
        @Override public void extractedFrom(KeybindRecord parent) { }
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
