package org.keybinder.wurm.integration;

/** Scoped source-ID override consumed only by the patched HUD send path. */
public final class ActionSourceOverride {
    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<Long>();
    private static volatile boolean hookAvailable;

    private ActionSourceOverride() { }

    public static long overrideOr(long ordinarySourceId) {
        Long override = CURRENT.get();
        return override == null ? ordinarySourceId : override.longValue();
    }

    public static Scope push(long sourceId) {
        Long previous = CURRENT.get();
        CURRENT.set(sourceId);
        return new Scope(previous);
    }

    public static boolean isHookAvailable() { return hookAvailable; }
    public static void markHookAvailable() { hookAvailable = true; }
    static void resetForTests() { hookAvailable = false; CURRENT.remove(); }

    public static final class Scope implements AutoCloseable {
        private final Long previous;
        private boolean closed;

        private Scope(Long previous) { this.previous = previous; }

        @Override public void close() {
            if (closed) return;
            closed = true;
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }
}
