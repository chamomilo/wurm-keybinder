package org.keybinder.wurm.bind;

import org.junit.Test;
import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.model.ConsoleCommandStep;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.storage.AccountKeybindStateStore;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Logger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class AccountBindingCoordinatorTest {
    @Test public void restoresOnlyTheAccountLocalEnabledSet() throws Exception {
        AccountKeybindStateStore store = new AccountKeybindStateStore(
                Files.createTempDirectory("account-coordinator").resolve("accounts.properties"));
        KeybindRecord first = record("first");
        KeybindRecord second = record("second");
        store.save("Player", Collections.singleton("second"));
        AccountBindingCoordinator coordinator = new AccountBindingCoordinator(store,
                new EventLogger(Logger.getAnonymousLogger()));

        coordinator.activate("Player", Arrays.asList(first, second));

        assertFalse(first.isEnabled());
        assertTrue(second.isEnabled());
    }

    @Test public void switchingAltsRestoresTheirIndependentPicks() throws Exception {
        AccountKeybindStateStore store = new AccountKeybindStateStore(
                Files.createTempDirectory("account-switch").resolve("accounts.properties"));
        KeybindRecord first = record("first");
        KeybindRecord second = record("second");
        store.save("First Alt", Collections.singleton("first"));
        store.save("Second Alt", Collections.singleton("second"));
        AccountBindingCoordinator coordinator = new AccountBindingCoordinator(store,
                new EventLogger(Logger.getAnonymousLogger()));

        coordinator.activate("First Alt", Arrays.asList(first, second));
        assertTrue(first.isEnabled());
        assertFalse(second.isEnabled());

        coordinator.activate("Second Alt", Arrays.asList(first, second));
        assertFalse(first.isEnabled());
        assertTrue(second.isEnabled());
    }

    @Test public void switchingAccountsPersistsTheDepartingAccountsLatestPicks()
            throws Exception {
        AccountKeybindStateStore store = new AccountKeybindStateStore(
                Files.createTempDirectory("account-departure").resolve("accounts.properties"));
        KeybindRecord first = record("first");
        KeybindRecord second = record("second");
        store.save("First Alt", Collections.singleton("first"));
        store.save("Second Alt", Collections.singleton("second"));
        AccountBindingCoordinator coordinator = new AccountBindingCoordinator(store,
                new EventLogger(Logger.getAnonymousLogger()));

        coordinator.activate("First Alt", Arrays.asList(first, second));
        first.setEnabled(false);
        second.setEnabled(true);
        coordinator.activate("Second Alt", Arrays.asList(first, second));

        assertEquals(Collections.singleton("second"),
                store.load("First Alt").getEnabledIds());
    }

    @Test public void simultaneousSessionsKeepIndependentInMemorySelections()
            throws Exception {
        AccountKeybindStateStore store = new AccountKeybindStateStore(
                Files.createTempDirectory("account-sessions").resolve("accounts.properties"));
        store.save("First Alt", Collections.singleton("first"));
        store.save("Second Alt", Collections.singleton("second"));
        AccountBindingCoordinator firstSession = new AccountBindingCoordinator(store,
                new EventLogger(Logger.getAnonymousLogger()));
        AccountBindingCoordinator secondSession = new AccountBindingCoordinator(store,
                new EventLogger(Logger.getAnonymousLogger()));
        KeybindRecord firstA = record("first"), secondA = record("second");
        KeybindRecord firstB = record("first"), secondB = record("second");

        firstSession.activate("First Alt", Arrays.asList(firstA, secondA));
        secondSession.activate("Second Alt", Arrays.asList(firstB, secondB));

        assertTrue(firstA.isEnabled());
        assertFalse(secondA.isEnabled());
        assertFalse(firstB.isEnabled());
        assertTrue(secondB.isEnabled());
    }

    private static KeybindRecord record(String id) {
        return new KeybindRecord(id, id, id.substring(0, 1).toUpperCase(),
                Collections.singletonList(new ConsoleCommandStep("say " + id)));
    }
}
