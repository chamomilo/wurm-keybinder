package org.keybinder.wurm.integration;

import com.wurmonline.client.renderer.PickableUnit;
import com.wurmonline.client.renderer.cell.GroundItemCellRenderable;

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
        private final PickableUnit hoveredTarget;

        public Snapshot(long worldObjectId, boolean groundItem) {
            this.worldObjectId = worldObjectId;
            this.groundItem = groundItem;
            this.hoveredTarget = null;
        }

        public Snapshot(PickableUnit hoveredTarget) {
            if (hoveredTarget == null)
                throw new IllegalArgumentException("hoveredTarget is required");
            this.worldObjectId = hoveredTarget.getId();
            this.groundItem = hoveredTarget instanceof GroundItemCellRenderable;
            this.hoveredTarget = hoveredTarget;
        }

        public long getWorldObjectId() { return worldObjectId; }
        public boolean isGroundItem() { return groundItem; }
        public PickableUnit getHoveredTarget() { return hoveredTarget; }
        public boolean targetMatches(int targetMask) {
            // The ID-only constructor remains for compatibility with existing
            // callers. Production hover capture retains the native PickableUnit
            // so tile, wall, creature, and item targets keep Wurm's own mask check.
            return hoveredTarget == null || hoveredTarget.targetMatches(targetMask);
        }
        public String getHoverName() {
            return hoveredTarget == null ? "" : hoveredTarget.getHoverName();
        }
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
