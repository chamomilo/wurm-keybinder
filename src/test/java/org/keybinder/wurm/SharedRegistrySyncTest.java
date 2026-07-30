package org.keybinder.wurm;

import org.junit.Test;
import org.keybinder.wurm.bind.VanillaBindService;
import org.keybinder.wurm.migration.CustomActionsImporter;
import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.queue.ActionQueueCostCalculator;
import org.keybinder.wurm.storage.KeybindStore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SharedRegistrySyncTest {
    @Test public void externalRowsArriveDisabledForThisAccount() throws Exception {
        Path file = Files.createTempDirectory("keybinder-shared").resolve("keybinds.properties");
        KeybindRegistry first = registry(file);
        KeybindRegistry second = registry(file);
        first.load();
        second.load();

        first.setCreationContext("First", "Cluster - Novus");
        first.createDraft();

        assertFalse(first.syncExternal(null));
        assertTrue(second.syncExternal(null));
        assertEquals(1, second.snapshot().size());
        assertFalse(second.snapshot().get(0).isEnabled());
        assertEquals("First", second.snapshot().get(0).getCreatedByUser());
    }

    private KeybindRegistry registry(Path file) {
        return new KeybindRegistry(new KeybindStore(file), new VanillaBindService(),
                new CustomActionsImporter(), new ActionQueueCostCalculator(),
                new EventLogger(Logger.getLogger("SharedRegistrySyncTest")));
    }
}
