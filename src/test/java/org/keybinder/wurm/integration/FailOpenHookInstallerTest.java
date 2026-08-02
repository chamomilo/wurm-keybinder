package org.keybinder.wurm.integration;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FailOpenHookInstallerTest {
    @Test public void oneFailedCapabilityDoesNotPreventTheNextInstallation() {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        FailOpenHookInstaller installer = new FailOpenHookInstaller(logger);
        AtomicInteger installed = new AtomicInteger();

        assertFalse(installer.install("broken", () -> {
            throw new ReflectiveOperationException("missing signature");
        }));
        assertTrue(installer.install("working", installed::incrementAndGet));

        assertEquals(1, installed.get());
    }
}
