package org.keybinder.wurm.storage;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

    @Test
    public void fourConcurrentSessionsDoNotOverwriteOtherAccountEntries()
            throws Exception {
        Path file = Files.createTempDirectory("keybinder-concurrent-account-state")
                .resolve("keybinds.accounts");
        ExecutorService pool = Executors.newFixedThreadPool(4);
        CountDownLatch ready = new CountDownLatch(4);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> writes = new ArrayList<Future<?>>();
        try {
            for (int i = 0; i < 4; i++) {
                final int session = i;
                writes.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    new AccountKeybindStateStore(file).save("Alt " + session,
                            new HashSet<String>(Arrays.asList(
                                    "shared", "private-" + session)));
                    return null;
                }));
            }
            ready.await();
            start.countDown();
            for (Future<?> write : writes) write.get();
        } finally {
            pool.shutdownNow();
        }

        AccountKeybindStateStore stored = new AccountKeybindStateStore(file);
        for (int i = 0; i < 4; i++)
            assertEquals(new HashSet<String>(Arrays.asList(
                            "shared", "private-" + i)),
                    stored.load("Alt " + i).getEnabledIds());
    }
}
