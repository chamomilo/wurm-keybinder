package org.keybinder.wurm.command;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WorldImproveTrackerTest {
    @Test public void requestedWorldObjectsUseTheirStandardExaminePhrases() {
        assertObject(101L, "forge", "The forge has some irregularities that must "
                + "be removed with a stone chisel. Ql: 88.48, Dam: 0.0.",
                RequirementFamily.STONE_CHISEL);
        assertObject(102L, "golden altar", "The golden altar has some dents that "
                + "must be flattened by a hammer. Ql: 70.0, Dam: 0.0.",
                RequirementFamily.HAMMER);
        assertObject(103L, "food storage bin", "You notice some notches you must "
                + "carve away in order to improve the food storage bin. Ql: 70.0, Dam: 0.0.",
                RequirementFamily.CARVING_KNIFE);
        assertObject(104L, "wagon hitched to horses", "You must use a mallet on "
                + "the wagon in order to improve it. Ql: 70.0, Dam: 0.0.",
                RequirementFamily.MALLET);
    }

    @Test public void parsesEveryVanillaRequirementFamily() {
        assertRequirement("improve it with a log", RequirementFamily.LOG);
        assertRequirement("improve it with a lump", RequirementFamily.LUMP);
        assertRequirement("improve it with leather", RequirementFamily.LEATHER);
        assertRequirement("need to polish it with a pelt", RequirementFamily.PELT);
        assertRequirement("improve it with a string", RequirementFamily.STRING);
        assertRequirement("improve it with clay", RequirementFamily.CLAY);
        assertRequirement("improve it with marble shards", RequirementFamily.SHARD);
        assertRequirement("carve away the notches", RequirementFamily.CARVING_KNIFE);
        assertRequirement("use a stone chisel", RequirementFamily.STONE_CHISEL);
        assertRequirement("use a mallet", RequirementFamily.MALLET);
        assertRequirement("use a file", RequirementFamily.FILE);
        assertRequirement("to be sharpened with a whetstone", RequirementFamily.WHETSTONE);
        assertRequirement("must be flattened by a hammer", RequirementFamily.HAMMER);
        assertRequirement("need to temper it in water", RequirementFamily.WATER);
        assertRequirement("must be backstitched with a needle", RequirementFamily.NEEDLE);
        assertRequirement("punch holes with an awl", RequirementFamily.AWL);
        assertRequirement("use a leather knife", RequirementFamily.LEATHER_KNIFE);
        assertRequirement("must be cut away with scissors", RequirementFamily.SCISSORS);
        assertRequirement("remove flaws by hand", RequirementFamily.BODY_HAND);
        assertRequirement("fix flaws with a clay shaper", RequirementFamily.CLAY_SHAPER);
        assertRequirement("fix flaws with a spatula", RequirementFamily.SPATULA);
    }

    @Test public void exactExamineIdAndSelectionOwnAllFollowingMetadata() {
        WorldImproveTracker tracker = new WorldImproveTracker();
        tracker.examineSent(44L);
        tracker.event(44L, ":Event",
                "A forge. Ql: 80.0, Dam: 12.50. It has some dents that must "
                        + "be flattened by a hammer.");

        WorldImproveTracker.Snapshot state = tracker.snapshot(44L);
        assertEquals(44L, state.getTargetId());
        assertEquals(RequirementFamily.HAMMER, state.getRequirement());
        assertTrue(state.isDamaged());

        tracker.repaired(44L);
        assertFalse(tracker.snapshot(44L).isDamaged());
        tracker.event(44L, ":Event", "You damage the forge a little.");
        assertTrue(tracker.snapshot(44L).isDamaged());
    }

    @Test public void selectionChangeImmediatelyDropsStateWithoutTimeout() {
        WorldImproveTracker tracker = new WorldImproveTracker();
        tracker.examineSent(10L);
        tracker.event(10L, ":Event", "You must use a file to improve it. "
                + "Ql: 50.0, Dam: 0.0.");
        assertEquals(RequirementFamily.FILE,
                tracker.snapshot(10L).getRequirement());

        tracker.selectionChanged(11L);
        assertNull(tracker.snapshot(10L));
        tracker.event(11L, ":Event", "You must use a mallet to improve it.");
        assertNull(tracker.snapshot(11L));
    }

    @Test public void unrelatedTabsAndMismatchedExamineTargetsAreIgnored() {
        WorldImproveTracker tracker = new WorldImproveTracker();
        tracker.examineSent(20L);
        tracker.event(21L, ":Event", "You must use a hammer.");
        assertNull(tracker.snapshot(21L));

        tracker.examineSent(21L);
        tracker.event(21L, ":Combat", "You must use a hammer.");
        assertNull(tracker.snapshot(21L));
    }

    @Test public void examineMayPrecedeSelectBarTransition() {
        WorldImproveTracker tracker = new WorldImproveTracker();
        tracker.examineSent(30L);
        tracker.selectionChanged(30L);
        tracker.event(30L, ":Event", "The forge has some irregularities that "
                + "must be removed with a stone chisel. Ql: 88.48, Dam: 0.0.");

        assertEquals(RequirementFamily.STONE_CHISEL,
                tracker.snapshot(30L).getRequirement());
    }

    @Test public void exactDoubleClickForgeDescriptionConfirmsWorldState() {
        WorldImproveTracker tracker = new WorldImproveTracker();
        tracker.examineSent(31L); // candidate came from DEFAULT_ACTION
        tracker.event(31L, ":Event", "A forge made from stone bricks and clay, "
                + "intended for smelting and smithing. The forge has some irregularities "
                + "that must be removed with a stone chisel. Ql: 88.48199, Dam: 0.0. "
                + "The forge has been firmly secured to the ground by Chamomilo. "
                + "You can barely make out the signature of its maker, '.ha.omilo'. "
                + "The fire is not lit.");

        assertEquals(RequirementFamily.STONE_CHISEL,
                tracker.snapshot(31L).getRequirement());
    }

    @Test public void defaultActionCandidateIgnoresNonExamineEventText() {
        WorldImproveTracker tracker = new WorldImproveTracker();
        tracker.examineSent(32L);
        tracker.event(32L, ":Event", "You hear a raven in the distance.");
        assertNull(tracker.snapshot(32L));

        tracker.event(32L, ":Event", "A forge has some irregularities that must "
                + "be removed with a stone chisel. Ql: 88.48, Dam: 0.0.");
        assertEquals(RequirementFamily.STONE_CHISEL,
                tracker.snapshot(32L).getRequirement());
    }

    @Test public void portableWorldItemMayOmitQualityAndDamageFields() {
        WorldImproveTracker tracker = new WorldImproveTracker();
        tracker.examineSent(33L);
        tracker.event(33L, ":Event", "A heavy knife with a bent blade perfect "
                + "for butchering. This is a very rare and interesting version "
                + "of the item. You need to temper the butchering knife by "
                + "dipping it in water while it's hot.");

        WorldImproveTracker.Snapshot state = tracker.snapshot(33L);
        assertEquals(RequirementFamily.WATER, state.getRequirement());
        assertFalse(state.isDamaged());
    }

    @Test public void targetToolNameInSuccessTextIsNotARequirement() {
        assertNull(WorldImproveEventParser.parse(
                "You improve the hammer a little.").getRequirement());
        assertNull(WorldImproveEventParser.parse(
                "You improve the leather knife a little.").getRequirement());
        assertNull(WorldImproveEventParser.parse(
                "You improve the clay shaper a little.").getRequirement());
    }

    private static void assertObject(long id, String ignoredName, String phrase,
                                     RequirementFamily expected) {
        WorldImproveTracker tracker = new WorldImproveTracker();
        tracker.examineSent(id);
        tracker.event(id, ":Event", phrase);
        assertEquals(ignoredName, expected, tracker.snapshot(id).getRequirement());
    }

    private static void assertRequirement(String phrase, RequirementFamily expected) {
        assertEquals(phrase, expected,
                WorldImproveEventParser.parse(phrase).getRequirement());
    }
}
