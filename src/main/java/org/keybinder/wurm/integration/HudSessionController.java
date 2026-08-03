package org.keybinder.wurm.integration;

/** Tracks the single HUD identity currently owned by Keybinder. */
public final class HudSessionController<H> {
    private H current;

    /** Attaches {@code next} and returns the superseded HUD, if any. */
    public synchronized H replace(H next) {
        if (current == next) return null;
        H previous = current;
        current = next;
        return previous;
    }

    public synchronized H current() { return current; }

    public synchronized H clear() {
        H previous = current;
        current = null;
        return previous;
    }
}
