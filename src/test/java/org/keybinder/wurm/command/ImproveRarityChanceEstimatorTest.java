package org.keybinder.wurm.command;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ImproveRarityChanceEstimatorTest {
    private final ImproveRarityChanceEstimator estimator =
            new ImproveRarityChanceEstimator();

    @Test
    public void normalUnrunedForgeAtSixtyThreePercentMatchesServerRange() {
        ImproveRarityChanceEstimator.Range range = estimator.estimate(
                (byte) 0, 0.0, 0.0, (byte) 0, true);

        assertEquals(10.10, range.getMinimum(), 0.005);
        assertEquals(10.40, range.getMaximum(), 0.005);
    }

    @Test
    public void targetRarityRuneMultipliesServerTransferChance() {
        ImproveRarityChanceEstimator.Range baseline = estimator.estimate(
                (byte) 0, 0.0, 0.0, (byte) 0, false);
        ImproveRarityChanceEstimator.Range runed = estimator.estimate(
                (byte) 0, 0.10, 0.10, (byte) 0, false);

        assertEquals(baseline.getMinimum() * 1.10,
                runed.getMinimum(), 0.0001);
        assertEquals(baseline.getMaximum() * 1.10,
                runed.getMaximum(), 0.0001);
    }

    @Test
    public void existingRarityRequiresAStillHigherActionRarity() {
        ImproveRarityChanceEstimator.Range normal = estimator.estimate(
                (byte) 0, 0.0, 0.0, (byte) 0, false);
        ImproveRarityChanceEstimator.Range rare = estimator.estimate(
                (byte) 1, 0.0, 0.0, (byte) 0, false);
        ImproveRarityChanceEstimator.Range supreme = estimator.estimate(
                (byte) 2, 0.0, 0.0, (byte) 0, false);

        assertTrue(rare.getMaximum() < normal.getMinimum());
        assertTrue(supreme.getMaximum() < rare.getMinimum());
    }

    @Test
    public void rarerConsumableAddsOnlyAConservativeDeletionUpperBound() {
        ImproveRarityChanceEstimator.Range ordinary = estimator.estimate(
                (byte) 0, 0.0, 0.0, (byte) 0, true);
        ImproveRarityChanceEstimator.Range rareSource = estimator.estimate(
                (byte) 0, 0.0, 0.0, (byte) 1, true);

        assertEquals(ordinary.getMinimum(), rareSource.getMinimum(), 0.0);
        assertTrue(rareSource.getMaximum() > ordinary.getMaximum());
    }
}
