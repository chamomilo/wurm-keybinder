package org.keybinder.wurm.command;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ActionExecutorHoverTypeTest {
    @Test
    public void absentOrUnresolvedHoverIsSilent() {
        assertFalse(ActionExecutor.shouldReportHoverTypeMismatch(0, 0));
        assertFalse(ActionExecutor.shouldReportHoverTypeMismatch(1, 0));
    }

    @Test
    public void aResolvedWrongHoveredTypeIsStillReported() {
        assertTrue(ActionExecutor.shouldReportHoverTypeMismatch(1, 1));
    }
}
