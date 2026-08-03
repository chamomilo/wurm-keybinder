package org.keybinder.wurm.recording;

import com.wurmonline.shared.constants.PlayerAction;
import org.junit.Test;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.TargetKind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ActionCaptureTest {
    @Test
    public void capturesOnlyTheFirstActionAfterBeingArmed() {
        ActionCapture capture = new ActionCapture();
        capture.arm();
        capture.setTargetContext("tile");
        capture.observe(PlayerAction.EXAMINE);
        capture.observe(PlayerAction.REPAIR);

        ActionStep step = capture.poll();
        assertEquals(PlayerAction.EXAMINE.getId(), step.getActionId());
        assertEquals(TargetKind.TILE, step.getTarget().getKind());
        assertFalse(capture.isArmed());
        assertNull(capture.poll());
    }

    @Test
    public void ignoresActionsUntilExplicitlyArmed() {
        ActionCapture capture = new ActionCapture();
        capture.observe(PlayerAction.EXAMINE);
        assertNull(capture.poll());

        capture.arm();
        assertTrue(capture.isArmed());
        capture.cancel();
        capture.observe(PlayerAction.EXAMINE);
        assertNull(capture.poll());
    }
}
