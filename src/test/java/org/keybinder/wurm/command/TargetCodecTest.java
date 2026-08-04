package org.keybinder.wurm.command;

import org.junit.Test;
import org.keybinder.wurm.model.TargetKind;
import org.keybinder.wurm.model.TargetSpec;

import static org.junit.Assert.assertEquals;

public class TargetCodecTest {
    @Test
    public void decodesParameterizedTargets() {
        assertEquals(10, TargetCodec.decode("@tb10").getSlot());
        assertEquals(3, TargetCodec.decode("@eq3").getSlot());
        assertEquals(4.5f, TargetCodec.decode("@nearby4.5").getRadius(), 0.001f);
        assertEquals("tree stump", TargetCodec.decode("nearby tree stump").getText());
    }

    @Test
    public void roundTripsLegacyNearbyRadius() {
        TargetSpec target = TargetCodec.decode("@nearby4");
        assertEquals(TargetKind.NEARBY_RADIUS, target.getKind());
        assertEquals("@nearby4", TargetCodec.encode(target));
        assertEquals("Nearby objects within 4", TargetCodec.display(target));
    }

    @Test
    public void automaticNearbyUsesTheSelectedActionsRangeWithoutStoredRadius() {
        TargetSpec target = TargetCodec.decode("nearby");
        assertEquals(TargetKind.NEARBY, target.getKind());
        assertEquals("nearby", TargetCodec.encode(target));
        assertEquals("Nearby objects", TargetCodec.display(target));
    }

    @Test
    public void roundTripsTileDirection() {
        TargetSpec target = TargetCodec.decode("tile_nw");
        assertEquals(TargetKind.TILE, target.getKind());
        assertEquals("tile_nw", TargetCodec.encode(target));
    }

    @Test
    public void roundTripsCurrentRide() {
        TargetSpec target = TargetCodec.decode("current ride");
        assertEquals(TargetKind.CURRENT_RIDE, target.getKind());
        assertEquals("current ride", TargetCodec.encode(target));
        assertEquals("Current ride", TargetCodec.display(target));
    }

    @Test
    public void roundTripsPendingUnresolvedTarget() {
        TargetSpec target = TargetCodec.decode("unresolved");
        assertEquals(TargetKind.UNRESOLVED, target.getKind());
        assertEquals("unresolved", TargetCodec.encode(target));
    }

    @Test
    public void roundTripsPortableInventoryFilterWithoutRuntimeItemId() {
        TargetSpec target = TargetCodec.decode("inventory+filter water, cedarwood");

        assertEquals(TargetKind.INVENTORY_FILTER, target.getKind());
        assertEquals("water", target.getText());
        assertEquals("inventory+filter water", TargetCodec.encode(target));
        assertEquals("Inventory + filter: water", TargetCodec.display(target));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsToolbeltSlotOutsideRange() {
        TargetCodec.decode("@tb11");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonPositiveNearbyRadius() {
        TargetCodec.decode("@nearby0");
    }
}
