package org.keybinder.wurm.queue;

/** Immutable row shown by the compact HUD action-queue monitor. */
public final class ActionQueueEntry {
    private final long sequence;
    private final String action;
    private final String source;
    private final String target;
    private final long targetId;
    private final boolean smartImprove;
    private final boolean active;
    private final boolean cancellationRequested;

    ActionQueueEntry(long sequence, String action, String source, String target,
                     long targetId, boolean smartImprove, boolean active,
                     boolean cancellationRequested) {
        this.sequence = sequence;
        this.action = safe(action);
        this.source = safe(source);
        this.target = safe(target);
        this.targetId = targetId;
        this.smartImprove = smartImprove;
        this.active = active;
        this.cancellationRequested = cancellationRequested;
    }

    public long getSequence() { return sequence; }
    public String getAction() { return action; }
    public String getSource() { return source; }
    public String getTarget() { return target; }
    public long getTargetId() { return targetId; }
    public boolean isSmartImprove() { return smartImprove; }
    public boolean isActive() { return active; }
    public boolean isCancellationRequested() { return cancellationRequested; }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
