package org.keybinder.wurm.model;

import java.util.Objects;

public final class ActionStep implements KeybindStep {
    private final short actionId;
    private final TargetSpec target;

    public ActionStep(short actionId, TargetSpec target) {
        this.actionId = actionId;
        this.target = Objects.requireNonNull(target, "target");
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
}
