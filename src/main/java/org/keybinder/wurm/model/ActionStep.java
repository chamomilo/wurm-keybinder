package org.keybinder.wurm.model;

import java.util.Objects;

public final class ActionStep implements KeybindStep {
    private final short actionId;
    private final TargetSpec target;
    private String lastKnownName;

    public ActionStep(short actionId, TargetSpec target) {
        this(actionId, target, "");
    }

    public ActionStep(short actionId, TargetSpec target, String lastKnownName) {
        this.actionId = actionId;
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

    /** Presentation metadata only; the numeric ID remains authoritative. */
    public String getLastKnownName() {
        return lastKnownName;
    }

    public void setLastKnownName(String value) {
        lastKnownName = value == null ? "" : value.trim();
    }
}
