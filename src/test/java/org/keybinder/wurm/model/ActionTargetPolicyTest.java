package org.keybinder.wurm.model;

import com.wurmonline.shared.constants.PlayerAction;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class ActionTargetPolicyTest {
    @Test
    public void stopAndNoTargetUseAnInternalCurrentTile() {
        assertFalse(ActionTargetPolicy.acceptsSelectableTarget(PlayerAction.STOP.getId()));
        assertFalse(ActionTargetPolicy.acceptsSelectableTarget(PlayerAction.NO_TARGET.getId()));

        ActionStep stop = new ActionStep(PlayerAction.STOP.getId(),
                ItemSelector.currentActive(), TargetSpec.simple(TargetKind.HOVER), "Stop");
        assertEquals(TargetKind.TILE, stop.getTarget().getKind());
        assertEquals(0, stop.getTarget().getDx());
        assertEquals(0, stop.getTarget().getDy());
    }

    @Test
    public void normalAndUnknownActionsKeepTheirSelectedTarget() {
        assertTrue(ActionTargetPolicy.acceptsSelectableTarget(PlayerAction.OPEN.getId()));
        assertTrue(ActionTargetPolicy.acceptsSelectableTarget(PlayerAction.PRAY.getId()));
        assertTrue(ActionTargetPolicy.acceptsSelectableTarget((short) 30000));

        TargetSpec selected = TargetSpec.simple(TargetKind.SELECTED);
        ActionStep unknown = new ActionStep((short) 30000,
                ItemSelector.currentActive(), selected, "Server action");
        assertEquals(selected, unknown.getTarget());
    }
}
