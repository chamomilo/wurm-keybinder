package org.keybinder.wurm.storage;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AccountKeybindStateStoreTest {
    @Test
    public void keepsIndependentActivationSetsPerAccount() throws Exception {
        Path file = Files.createTempDirectory("keybinder-account-state")
                .resolve("keybinds.accounts");
        AccountKeybindStateStore store = new AccountKeybindStateStore(file);

        assertFalse(store.load("Chamomilo").isPresent());
        store.save("Chamomilo", new HashSet<String>(Arrays.asList("one", "two")));
        store.save("Another", new HashSet<String>(Arrays.asList("three")));

        assertTrue(store.load("chamomilo").isPresent());
        assertEquals(new HashSet<String>(Arrays.asList("one", "two")),
                store.load("CHAMOMILO").getEnabledIds());
        assertEquals(new HashSet<String>(Arrays.asList("three")),
                store.load("another").getEnabledIds());
    }

    @Test
    public void emptyActivationSetIsStillAStoredAccount() throws Exception {
        Path file = Files.createTempDirectory("keybinder-empty-account-state")
                .resolve("keybinds.accounts");
        AccountKeybindStateStore store = new AccountKeybindStateStore(file);

        store.save("Chamomilo", java.util.Collections.<String>emptySet());

        assertTrue(store.load("Chamomilo").isPresent());
        assertTrue(store.load("Chamomilo").getEnabledIds().isEmpty());
    }
}
