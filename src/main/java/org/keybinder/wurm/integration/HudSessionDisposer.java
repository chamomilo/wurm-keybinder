package org.keybinder.wurm.integration;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Fail-open cleanup of transient components owned by one HUD instance. */
public final class HudSessionDisposer<H, C> {
    public interface ComponentHider<H, C> {
        void hide(H hud, C component) throws Exception;
    }

    private final Logger logger;
    private final ComponentHider<H, C> hider;

    public HudSessionDisposer(Logger logger, ComponentHider<H, C> hider) {
        this.logger = logger;
        this.hider = hider;
    }

    @SafeVarargs
    public final void hideAll(H hud, C... components) {
        if (hud == null || hider == null || components == null) return;
        for (C component : components) {
            if (component == null) continue;
            try {
                hider.hide(hud, component);
            } catch (Throwable failure) {
                logger.log(Level.FINE, "Unable to dispose old HUD component", failure);
            }
        }
    }
}
