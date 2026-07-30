package org.keybinder.wurm.model;

import java.util.Objects;

public final class ActivateToolStep implements KeybindStep {
    private final TargetSpec target;

    public ActivateToolStep(TargetSpec target) {
        this.target = Objects.requireNonNull(target, "target");
    }

    @Override public StepKind getKind() { return StepKind.ACTIVATE_TOOL; }
    public TargetSpec getTarget() { return target; }
}
