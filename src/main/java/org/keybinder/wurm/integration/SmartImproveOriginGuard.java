package org.keybinder.wurm.integration;

/** Identifies queue entries sent by Smart Improve without changing Wurm actions. */
public final class SmartImproveOriginGuard {
    private static final ThreadLocal<Integer> DEPTH = new ThreadLocal<Integer>() {
        @Override protected Integer initialValue() { return 0; }
    };

    private SmartImproveOriginGuard() { }

    public static void enter() { DEPTH.set(DEPTH.get() + 1); }

    public static void exit() {
        int depth = DEPTH.get() - 1;
        if (depth <= 0) DEPTH.remove();
        else DEPTH.set(depth);
    }

    public static boolean isActive() { return DEPTH.get() > 0; }
}
