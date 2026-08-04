package org.keybinder.wurm.integration;

/** Preserves a world hover while Keybinder's variant selector owns the mouse. */
public final class ExecutionHoverOverride {
    private static final ThreadLocal<Snapshot> CURRENT = new ThreadLocal<Snapshot>();

    private ExecutionHoverOverride() { }

    public static Snapshot current() { return CURRENT.get(); }

    public static Scope push(Snapshot snapshot) {
        Snapshot previous = CURRENT.get();
        if (snapshot == null) CURRENT.remove();
        else CURRENT.set(snapshot);
        return new Scope(previous);
    }

    public static final class Snapshot {
        private final long worldObjectId;
        private final boolean groundItem;

        public Snapshot(long worldObjectId, boolean groundItem) {
            this.worldObjectId = worldObjectId;
            this.groundItem = groundItem;
        }

        public long getWorldObjectId() { return worldObjectId; }
        public boolean isGroundItem() { return groundItem; }
    }

    public static final class Scope implements AutoCloseable {
        private final Snapshot previous;
        private boolean closed;

        private Scope(Snapshot previous) { this.previous = previous; }

        @Override public void close() {
            if (closed) return;
            closed = true;
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }
}
