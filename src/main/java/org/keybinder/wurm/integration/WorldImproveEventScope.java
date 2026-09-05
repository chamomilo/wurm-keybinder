package org.keybinder.wurm.integration;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Carries the silent-Examine decision from the inbound server-message hook to
 * the downstream chat renderer. The scope starts before HUD mods can consume
 * the message, so Smart Improve still receives metadata when another SelectBar
 * intentionally hides its own Examine output.
 */
public final class WorldImproveEventScope {
    private static final ThreadLocal<Deque<Boolean>> SCOPES =
            new ThreadLocal<Deque<Boolean>>() {
                @Override protected Deque<Boolean> initialValue() {
                    return new ArrayDeque<Boolean>();
                }
            };

    private WorldImproveEventScope() { }

    public static void enter(boolean suppress) {
        SCOPES.get().push(Boolean.valueOf(suppress));
    }

    public static boolean shouldSuppress() {
        Deque<Boolean> scopes = SCOPES.get();
        return !scopes.isEmpty() && scopes.peek().booleanValue();
    }

    public static void exit() {
        Deque<Boolean> scopes = SCOPES.get();
        if (!scopes.isEmpty()) scopes.pop();
        if (scopes.isEmpty()) SCOPES.remove();
    }
}
