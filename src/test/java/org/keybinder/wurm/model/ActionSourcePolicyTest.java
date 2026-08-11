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
    public void openNeverCarriesAnItemTool() {
        assertFalse(ActionSourcePolicy.acceptsSelectableTool(PlayerAction.OPEN.getId()));
        ActionStep open = new ActionStep(PlayerAction.OPEN.getId(),
                ItemSelector.exactObject(123L, "hatchet"),
                TargetSpec.simple(TargetKind.HOVER), "Open");
        assertEquals(ItemSelectorKind.EMPTY_HAND, open.getSource().getKind());
    }

    @Test
    public void openInventoryContainerUsesOnlyItsTarget() {
        short openInventoryContainer = 568;
        assertFalse(ActionSourcePolicy.acceptsSelectableTool(openInventoryContainer));
        ActionStep open = new ActionStep(openInventoryContainer,
                ItemSelector.currentActive(), TargetSpec.exactObject(456L, "journal"),
                "Open");
        assertEquals(ItemSelectorKind.EMPTY_HAND, open.getSource().getKind());
    }

    @Test
    public void drinkUsesOnlyItsTargetAndNeverCarriesATool() {
        short drink = 183;
        assertFalse(ActionSourcePolicy.acceptsSelectableTool(drink));
        ActionStep step = new ActionStep(drink,
                ItemSelector.currentActive(), TargetSpec.inventoryFilter("water"), "Drink");
        assertEquals(ItemSelectorKind.EMPTY_HAND, step.getSource().getKind());
    }

    @Test
    public void ordinaryActionsKeepTheirSelectedTool() {
        assertTrue(ActionSourcePolicy.acceptsSelectableTool(PlayerAction.IMPROVE.getId()));
        ActionStep improve = new ActionStep(PlayerAction.IMPROVE.getId(),
                ItemSelector.toolbeltSlot(4), TargetSpec.simple(TargetKind.HOVER), "Improve");
        assertEquals(ItemSelectorKind.TOOLBELT_SLOT, improve.getSource().getKind());
        assertEquals(4, improve.getSource().getSlot());
    }

    @Test
    public void auditedCatalogKeepsOnlyCommandsThatUseAnItemSource() {
        assertFalse(ActionSourcePolicy.acceptsSelectableTool(PlayerAction.PUSH.getId()));
        assertFalse(ActionSourcePolicy.acceptsSelectableTool(PlayerAction.PULL.getId()));
        assertFalse(ActionSourcePolicy.acceptsSelectableTool(
                PlayerAction.TURN_CLOCKWISE.getId()));
        assertFalse(ActionSourcePolicy.acceptsSelectableTool(PlayerAction.EXAMINE.getId()));
        assertFalse(ActionSourcePolicy.acceptsSelectableTool(PlayerAction.REPAIR.getId()));
        assertFalse(ActionSourcePolicy.acceptsSelectableTool(PlayerAction.PRAY.getId()));
        assertTrue(ActionSourcePolicy.acceptsSelectableTool(PlayerAction.FISH.getId()));
        assertTrue(ActionSourcePolicy.acceptsSelectableTool(PlayerAction.FILET.getId()));
        assertTrue(ActionSourcePolicy.acceptsSelectableTool(PlayerAction.FIRSTAID.getId()));
        assertTrue(ActionSourcePolicy.acceptsSelectableTool(PlayerAction.DIG.getId()));
        assertTrue(ActionSourcePolicy.acceptsSelectableTool(PlayerAction.BLESS.getId()));
    }

    @Test
    public void ordinaryActionsKeepPortableInventoryFilterTool() {
        ActionStep improve = new ActionStep(PlayerAction.IMPROVE.getId(),
                ItemSelector.inventoryFilter("rare steel hammer (glowing)"),
                TargetSpec.simple(TargetKind.HOVER), "Improve");

        assertEquals(ItemSelectorKind.INVENTORY_FILTER,
                improve.getSource().getKind());
        assertEquals("hammer", improve.getSource().getText());
    }
}
