package org.keybinder.wurm.command;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ImproveSuccessChanceEstimatorTest {
    private final ImproveSuccessChanceEstimator estimator =
            new ImproveSuccessChanceEstimator();

    @Test
    public void equalEffectiveSkillAndDifficultyIsApproximatelyEven() {
        int chance = estimator.estimate(50.0, 0.0, 50.0,
                50.0, 0.0, false);

        assertTrue(chance >= 48 && chance <= 52);
    }

    @Test
    public void targetQualityLowersAndToolQualityRaisesSuccessChance() {
        int baseline = estimator.estimate(50.0, 0.0, 50.0,
                50.0, 0.0, false);
        int harderTarget = estimator.estimate(50.0, 0.0, 70.0,
                50.0, 0.0, false);
        int betterTool = estimator.estimate(50.0, 0.0, 50.0,
                90.0, 0.0, false);

        assertTrue(harderTarget < baseline);
        assertTrue(betterTool > baseline);
    }

    @Test
    public void toolDamageAndParentSkillAreApplied() {
        int undamaged = estimator.estimate(50.0, 0.0, 50.0,
                90.0, 0.0, false);
        int damaged = estimator.estimate(50.0, 0.0, 50.0,
                90.0, 50.0, false);
        int withParent = estimator.estimate(50.0, 70.0, 50.0,
                50.0, 0.0, false);

        assertTrue(damaged < undamaged);
        assertTrue(withParent > estimator.estimate(50.0, 0.0, 50.0,
                50.0, 0.0, false));
    }

    @Test
    public void builtInBodyToolDoesNotReduceSkillByItsQuality() {
        int bodyTool = estimator.estimateWithoutToolQuality(
                50.0, 0.0, 50.0, false, 0.0);
        int ordinaryLowQualityTool = estimator.estimate(
                50.0, 0.0, 50.0, 1.0, 0.0, false);

        assertTrue(bodyTool > ordinaryLowQualityTool);
    }

    @Test
    public void priestAndRepairerBonusesChangeCurrentSuccessChance() {
        int ordinary = estimator.estimate(50.0, 0.0, 50.0,
                50.0, 0.0, false, 0.0);
        int priest = estimator.estimate(50.0, 0.0, 50.0,
                50.0, 0.0, false, -20.0);
        int repairerBonus = estimator.estimate(50.0, 0.0, 50.0,
                50.0, 0.0, false, 10.0);

        assertTrue(priest < ordinary);
        assertTrue(repairerBonus > ordinary);
    }

    @Test
    public void epicCurveAndAboveTableFallbackMatchServerRules() {
        int normal = estimator.estimate(50.0, 0.0, 50.0,
                50.0, 0.0, false);
        int epic = estimator.estimate(50.0, 0.0, 50.0,
                50.0, 0.0, true);

        assertTrue(epic > normal);
        assertEquals(50, estimator.estimate(100.0, 0.0, 100.0,
                100.0, 0.0, false));
    }
}
