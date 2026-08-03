package org.keybinder.wurm.command;

/**
 * One selected world item's Examine-derived state. This is metadata capture,
 * not an execution lock: Improve requests never wait for or reserve this state.
 */
public final class WorldImproveTracker {
    private static final long NONE = Long.MIN_VALUE;
    private long targetId = NONE;
    private RequirementFamily requirement;
    private boolean damaged;
    private boolean descriptionConfirmed;

    public synchronized void examineSent(long id) {
        if (id <= 0) {
            clear();
            return;
        }
        // Wurm may send the double-click Examine just before SelectBar finishes
        // switching to the clicked item. The Event and execution paths perform
        // the authoritative selected-ID check after that transition.
        targetId = id;
        requirement = null;
        damaged = false;
        descriptionConfirmed = false;
    }

    public synchronized void selectionChanged(long selectedId) {
        // During a double click the DEFAULT_ACTION send and SelectBar update can
        // arrive in either order. Before the Examine description confirms the
        // capture, the Event hook is the authoritative selected-ID check.
        if (descriptionConfirmed && targetId != NONE && selectedId != targetId) clear();
    }

    public synchronized void event(long selectedId, String context, String message) {
        if (context == null || !":event".equalsIgnoreCase(context.trim())) return;
        if (targetId == NONE) return;
        if (selectedId != targetId) {
            clear();
            return;
        }
        if (!descriptionConfirmed) {
            // DEFAULT_ACTION is only a possible Examine. Do not let another
            // default-action response seed Smart Improve state.
            if (!WorldImproveEventParser.isExamineDescription(message)) return;
            descriptionConfirmed = true;
        }
        WorldImproveEventParser.Parsed parsed = WorldImproveEventParser.parse(message);
        if (parsed.getRequirement() != null) requirement = parsed.getRequirement();
        if (parsed.getDamaged() != null) damaged = parsed.getDamaged();
    }

    public synchronized Snapshot snapshot(long selectedId) {
        if (!descriptionConfirmed || targetId == NONE || selectedId != targetId
                || requirement == null) return null;
        return new Snapshot(targetId, requirement, damaged);
    }

    /** Repair is deterministic in WU; update local state when it is sent. */
    public synchronized void repaired(long id) {
        if (targetId == id) damaged = false;
    }

    public synchronized void clear() {
        targetId = NONE;
        requirement = null;
        damaged = false;
        descriptionConfirmed = false;
    }

    public static final class Snapshot {
        private final long targetId;
        private final RequirementFamily requirement;
        private final boolean damaged;

        Snapshot(long targetId, RequirementFamily requirement, boolean damaged) {
            this.targetId = targetId;
            this.requirement = requirement;
            this.damaged = damaged;
        }

        public long getTargetId() { return targetId; }
        public RequirementFamily getRequirement() { return requirement; }
        public boolean isDamaged() { return damaged; }
    }
}
