package org.keybinder.wurm.command;

import com.wurmonline.shared.constants.PlayerAction;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ResolvedActionPlanTest {
    @Test public void snapshotOwnsItsTargetIds() {
        long[] targets = {11L, 12L};
        ResolvedActionPlan plan = new ResolvedActionPlan(
                ActionSourceResolver.ResolvedSource.override(7L),
                PlayerAction.EXAMINE, targets, 2,
                false, true, false, null);

        targets[0] = 99L;

        assertArrayEquals(new long[]{11L, 12L},
                plan.targetsForExecution(2));
        assertTrue(plan.getSource().hasOverride());
        assertEquals(7L, plan.getSource().getSourceId());
        assertEquals(2, plan.getQueueCost());
        assertTrue(plan.hasObjectTargets());
        assertFalse(plan.isBatchDispatch());
    }

    @Test public void budgetedFanOutUsesThePreflightAllowance() {
        ResolvedActionPlan plan = new ResolvedActionPlan(
                ActionSourceResolver.ResolvedSource.ordinary(),
                PlayerAction.EXAMINE, new long[]{1L, 2L, 3L, 4L}, 4,
                true, false, true, null);

        assertArrayEquals(new long[]{1L, 2L},
                plan.targetsForExecution(2));
        assertTrue(plan.isBatchDispatch());
    }

    @Test public void fixedPlanDoesNotLoseTargetsWhenItsQueueCostIsOne() {
        ResolvedActionPlan plan = new ResolvedActionPlan(
                ActionSourceResolver.ResolvedSource.ordinary(),
                PlayerAction.EXAMINE, new long[]{1L, 2L, 3L}, 1,
                true, false, false, null);

        assertArrayEquals(new long[]{1L, 2L, 3L},
                plan.targetsForExecution(1));
    }
}
