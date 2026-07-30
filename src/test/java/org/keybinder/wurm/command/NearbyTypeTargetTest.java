package org.keybinder.wurm.command;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NearbyTypeTargetTest {
    @Test
    public void removesTreeMaterialAndMatchesAnyTreeStump() {
        String target = NearbyTypeTarget.encode("walnut tree stump");
        assertEquals("nearby tree stump", target);
        assertTrue(NearbyTypeTarget.matches(target, "birch tree stump"));
        assertTrue(NearbyTypeTarget.matches(target, "apple tree stump"));
        assertFalse(NearbyTypeTarget.matches(target, "walnut felled tree"));
    }

    @Test
    public void removesWoodSpeciesFromFelledTrees() {
        String target = NearbyTypeTarget.encode("cedarwood felled tree");
        assertEquals("nearby felled tree", target);
        assertTrue(NearbyTypeTarget.matches(target, "birchwood felled tree"));
        assertTrue(NearbyTypeTarget.matches(target, "oakenwood felled tree"));
    }

    @Test
    public void removesKnownMaterialFromOtherObjectTypes() {
        assertEquals("nearby large anvil", NearbyTypeTarget.encode("iron large anvil"));
        assertTrue(NearbyTypeTarget.matches("nearby large anvil", "steel large anvil"));
    }

    @Test
    public void preservesFoodStorageBinType() {
        String target = NearbyTypeTarget.encode("food storage bin");
        assertEquals("nearby food storage bin", target);
        assertTrue(NearbyTypeTarget.matches(target, "food storage bin"));
        assertFalse(NearbyTypeTarget.matches(target, "bulk storage bin"));
    }

    @Test
    public void removesCreatureAgeAndConditionModifiers() {
        String target = NearbyTypeTarget.encode("aged fat horse");
        assertEquals("nearby horse", target);
        assertTrue(NearbyTypeTarget.matches(target, "young horse"));
        assertTrue(NearbyTypeTarget.matches(target, "venerable fat horse"));
        assertFalse(NearbyTypeTarget.matches(target, "aged fat cow"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptyType() {
        NearbyTypeTarget.type("nearby ");
    }
}
