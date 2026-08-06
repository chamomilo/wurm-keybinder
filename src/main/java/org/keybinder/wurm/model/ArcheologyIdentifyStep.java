package org.keybinder.wurm.model;

import java.util.Objects;

/** Identifies one inventory-backed archaeology fragment with its indicated tool. */
public final class ArcheologyIdentifyStep implements KeybindStep {
    private final TargetSpec target;
    private final ArcheologyIdentifySourceMode sourceMode;

    public ArcheologyIdentifyStep(TargetSpec target) {
        this(target, ArcheologyIdentifySourceMode.TOOLBELT_THEN_INVENTORY);
    }

    public ArcheologyIdentifyStep(TargetSpec target,
                                  ArcheologyIdentifySourceMode sourceMode) {
        this.target = Objects.requireNonNull(target, "target");
        this.sourceMode = sourceMode == null
                ? ArcheologyIdentifySourceMode.TOOLBELT_THEN_INVENTORY
                : sourceMode;
    }

    @Override public StepKind getKind() { return StepKind.ARCHEOLOGY_IDENTIFY; }
    public TargetSpec getTarget() { return target; }
    public ArcheologyIdentifySourceMode getSourceMode() { return sourceMode; }
}
