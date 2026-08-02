package org.keybinder.wurm.integration;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Installs independent client hooks so one unavailable capability cannot block the rest. */
public final class FailOpenHookInstaller {
    public interface Installation {
        void install() throws Exception;
    }

    private final Logger logger;

    public FailOpenHookInstaller(Logger logger) {
        this.logger = logger;
    }

    public boolean install(String name, Installation installation) {
        try {
            installation.install();
            logger.fine("Installed Keybinder " + name + " integration");
            return true;
        } catch (Throwable failure) {
            logger.log(Level.WARNING, "Keybinder " + name
                    + " integration is unavailable; other features will continue ("
                    + failure.getClass().getName() + ": "
                    + String.valueOf(failure.getMessage()) + ")", failure);
            return false;
        }
    }
}
