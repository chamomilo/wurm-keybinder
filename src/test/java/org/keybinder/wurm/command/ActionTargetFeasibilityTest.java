package org.keybinder.wurm.command;

import com.wurmonline.shared.constants.PlayerAction;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ActionTargetFeasibilityTest {
    @Test public void inventoryTargetRequiresTheNativeInventoryMask() {
        assertTrue(ActionExecutor.acceptsInventoryTarget(PlayerAction.ANY_ITEM));
        assertFalse(ActionExecutor.acceptsInventoryTarget(PlayerAction.CREATURE));
    }

    @Test public void tileTargetUsesThePlayersCurrentLayer() {
        assertTrue(ActionExecutor.acceptsTileTarget(PlayerAction.SURFACE_TILE, 0));
        assertTrue(ActionExecutor.acceptsTileTarget(PlayerAction.SURFACE_TILE_BORDER, 0));
        assertFalse(ActionExecutor.acceptsTileTarget(PlayerAction.CAVE_TILE, 0));
        assertTrue(ActionExecutor.acceptsTileTarget(PlayerAction.CAVE_TILE, -1));
        assertFalse(ActionExecutor.acceptsTileTarget(PlayerAction.SURFACE_TILE, -1));
    }
}
