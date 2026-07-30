package org.keybinder.wurm.model;

import java.util.Objects;

public final class SmartImproveStep implements KeybindStep {
    private final TargetSpec target;

    public SmartImproveStep(TargetSpec target) {
        this.target = Objects.requireNonNull(target, "target");
    }

    @Override public StepKind getKind() { return StepKind.SMART_IMPROVE; }
    public TargetSpec getTarget() { return target; }
}
