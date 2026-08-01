package org.keybinder.wurm.model;

import com.wurmonline.shared.constants.PlayerAction;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ActionSourcePolicyTest {
    @Test
    public void takeNeverCarriesAnItemTool() {
        assertFalse(ActionSourcePolicy.acceptsSelectableTool(PlayerAction.TAKE.getId()));
        ActionStep take = new ActionStep(PlayerAction.TAKE.getId(),
                ItemSelector.toolbeltSlot(4), TargetSpec.simple(TargetKind.NEARBY), "Take");
        assertEquals(ItemSelectorKind.EMPTY_HAND, take.getSource().getKind());
    }

    @Test
    public void ordinaryActionsKeepTheirSelectedTool() {
        assertTrue(ActionSourcePolicy.acceptsSelectableTool(PlayerAction.EXAMINE.getId()));
        ActionStep examine = new ActionStep(PlayerAction.EXAMINE.getId(),
                ItemSelector.toolbeltSlot(4), TargetSpec.simple(TargetKind.HOVER), "Examine");
        assertEquals(ItemSelectorKind.TOOLBELT_SLOT, examine.getSource().getKind());
        assertEquals(4, examine.getSource().getSlot());
    }
}
