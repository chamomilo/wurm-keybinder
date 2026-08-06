package org.keybinder.wurm.ui;

import org.junit.Test;
import org.keybinder.wurm.model.StepKind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EditorRowLayoutTest {
    @Test public void targetedActionUsesStableThreeColumnPlan() {
        EditorRowLayout.Plan plan = plan(
                StepKind.CUSTOM_ACTION, false, false, true);

        assertEquals(EditorRowLayout.Shape.THREE_COLUMNS, plan.getShape());
        assertTrue(plan.isCaptureEnabled());
        assertEquals(195, plan.getActionWidth());
        assertEquals(195, plan.getSourceWidth());
        assertEquals(197, plan.getTargetWidth());
    }

    @Test public void commandUsesSingleFieldAndDisablesCapture() {
        EditorRowLayout.Plan plan = plan(
                StepKind.CONSOLE_COMMAND, false, false, false);

        assertEquals(EditorRowLayout.Shape.SINGLE_FIELD, plan.getShape());
        assertFalse(plan.isCaptureEnabled());
        assertEquals(637, plan.getActionWidth());
    }

    @Test public void vanillaTargetKeepsColumnsWithoutCapture() {
        EditorRowLayout.Plan plan = plan(
                StepKind.VANILLA_ACTION, true, true, true);

        assertEquals(EditorRowLayout.Shape.THREE_COLUMNS, plan.getShape());
        assertFalse(plan.isCaptureEnabled());
    }

    private static EditorRowLayout.Plan plan(StepKind kind, boolean vanilla,
                                               boolean vanillaTarget,
                                               boolean actionTarget) {
        return EditorRowLayout.plan(kind, vanilla, vanillaTarget, actionTarget,
                900, 48, 120, 22, 24, 9, 8, 145, 150);
    }
}
