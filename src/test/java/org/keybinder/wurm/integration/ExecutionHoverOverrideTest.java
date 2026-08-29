package org.keybinder.wurm.integration;

import com.wurmonline.client.renderer.PickData;
import com.wurmonline.client.renderer.PickableUnit;
import com.wurmonline.client.renderer.Color;
import com.wurmonline.client.renderer.SubPickableUnit;
import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.backend.RenderState;
import com.wurmonline.client.resources.textures.Texture;
import com.wurmonline.shared.constants.PlayerAction;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ExecutionHoverOverrideTest {
    @Test public void nestedScopeRestoresPreviousSnapshot() {
        ExecutionHoverOverride.Snapshot outer =
                new ExecutionHoverOverride.Snapshot(44L, true);
        ExecutionHoverOverride.Snapshot inner =
                new ExecutionHoverOverride.Snapshot(55L, false);

        assertNull(ExecutionHoverOverride.current());
        try (ExecutionHoverOverride.Scope ignored = ExecutionHoverOverride.push(outer)) {
            assertEquals(44L, ExecutionHoverOverride.current().getWorldObjectId());
            assertTrue(ExecutionHoverOverride.current().isGroundItem());
            try (ExecutionHoverOverride.Scope nested = ExecutionHoverOverride.push(inner)) {
                assertEquals(55L, ExecutionHoverOverride.current().getWorldObjectId());
                assertFalse(ExecutionHoverOverride.current().isGroundItem());
            }
            assertEquals(44L, ExecutionHoverOverride.current().getWorldObjectId());
        }
        assertNull(ExecutionHoverOverride.current());
    }

    @Test public void retainedTileUsesNativeTargetMaskInsteadOfObjectLookup() {
        PickableUnit tile = new PickableUnit() {
            @Override public void getHoverDescription(PickData pickData) { }
            @Override public String getHoverName() { return "Packed dirt"; }
            @Override public void renderPicked(Queue queue, RenderState state, Color color) { }
            @Override public long getId() { return 0x0BEF03950003L; }
            @Override public Color getOutlineColor() { return null; }
            @Override public void pick(Queue queue, boolean xray) { }
            @Override public boolean targetMatches(int targetMask) {
                return (targetMask & PlayerAction.SURFACE_TILE) != 0;
            }
            @Override public List<SubPickableUnit> getSubPickableUnitList() {
                return Collections.emptyList();
            }
            @Override public Texture getIconTexture() { return null; }
            @Override public short getIconId() { return 0; }
            @Override public void preparePick() { }
        };

        ExecutionHoverOverride.Snapshot snapshot =
                new ExecutionHoverOverride.Snapshot(tile);

        assertEquals(0x0BEF03950003L, snapshot.getWorldObjectId());
        assertEquals("Packed dirt", snapshot.getHoverName());
        assertTrue(snapshot.targetMatches(PlayerAction.PACK.getTargetMask()));
        assertFalse(snapshot.targetMatches(PlayerAction.CREATURE));
    }
}
