package org.keybinder.wurm.integration;

/** Thread-local recursion guard shared by execution and passive observation. */
public final class ExecutionOriginGuard {
    private static final ThreadLocal<Integer> DEPTH = new ThreadLocal<Integer>() {
        @Override protected Integer initialValue() { return 0; }
    };

    private ExecutionOriginGuard() { }

    public static void enterInternal() { DEPTH.set(DEPTH.get() + 1); }
    public static void exitInternal() { DEPTH.set(Math.max(0, DEPTH.get() - 1)); }
    public static boolean isInternal() { return DEPTH.get() > 0; }
}
