package org.keybinder.wurm.model;

import java.util.Objects;

public final class ActionStep implements KeybindStep {
    private final short actionId;
    private final ItemSelector source;
    private final TargetSpec target;
    private String lastKnownName;

    public ActionStep(short actionId, TargetSpec target) {
        this(actionId, ItemSelector.currentActive(), target, "");
    }

    public ActionStep(short actionId, TargetSpec target, String lastKnownName) {
        this(actionId, ItemSelector.currentActive(), target, lastKnownName);
    }

    public ActionStep(short actionId, ItemSelector source, TargetSpec target,
                      String lastKnownName) {
        this.actionId = actionId;
        this.source = ActionSourcePolicy.normalize(actionId,
                Objects.requireNonNull(source, "source"));
        this.target = Objects.requireNonNull(target, "target");
        setLastKnownName(lastKnownName);
    }

    public short getActionId() {
        return actionId;
    }

    @Override
    public StepKind getKind() {
        return StepKind.CUSTOM_ACTION;
    }

    public TargetSpec getTarget() {
        return target;
    }

    public ItemSelector getSource() { return source; }

    /** Presentation metadata only; the numeric ID remains authoritative. */
    public String getLastKnownName() {
        return lastKnownName;
    }

    public void setLastKnownName(String value) {
        lastKnownName = value == null ? "" : value.trim();
    }
}
