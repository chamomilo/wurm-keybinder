package org.keybinder.wurm.ui;

import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.ActivateToolStep;
import org.keybinder.wurm.model.ConsoleCommandStep;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.SmartImproveStep;
import org.keybinder.wurm.model.StepKind;

/** Stable editor classification based only on the persisted step model. */
public final class EditorStepType {
    private EditorStepType() {}

    /** Returns the base dropdown index, or -1 for a native compatibility step. */
    public static int initialIndex(KeybindStep step) {
        if (step instanceof ActivateToolStep) return 0;
        if (step instanceof SmartImproveStep) return 1;
        if (step instanceof ConsoleCommandStep) return 2;
        if (step == null || step instanceof ActionStep) return 3;
        return -1;
    }

    public static boolean supportsCapture(StepKind kind) {
        return kind != null && kind != StepKind.CONSOLE_COMMAND
                && kind != StepKind.VANILLA_ACTION;
    }
}
