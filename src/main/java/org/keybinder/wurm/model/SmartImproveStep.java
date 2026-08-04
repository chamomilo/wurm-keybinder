package org.keybinder.wurm.model;

import java.util.Objects;

public final class SmartImproveStep implements KeybindStep {
    private final TargetSpec target;
    private final SmartImproveSourceMode sourceMode;

    public SmartImproveStep(TargetSpec target) {
        this(target, SmartImproveSourceMode.TOOLBELT_THEN_INVENTORY);
    }

    public SmartImproveStep(TargetSpec target, SmartImproveSourceMode sourceMode) {
        this.target = Objects.requireNonNull(target, "target");
        this.sourceMode = sourceMode == null
                ? SmartImproveSourceMode.TOOLBELT_THEN_INVENTORY : sourceMode;
    }

    @Override public StepKind getKind() { return StepKind.SMART_IMPROVE; }
    public TargetSpec getTarget() { return target; }
    public SmartImproveSourceMode getSourceMode() { return sourceMode; }
}
