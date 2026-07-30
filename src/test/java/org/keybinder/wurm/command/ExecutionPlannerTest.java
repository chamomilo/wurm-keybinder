package org.keybinder.wurm.command;

import org.junit.Test;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.TargetKind;
import org.keybinder.wurm.model.TargetSpec;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExecutionPlannerTest {
    @Test
    public void unavailableStepIsSkippedAndRemainingCostIsPreserved() {
        KeybindStep missing = new ActionStep((short) 97,
                TargetSpec.exactObject(42L, "Stump"));
        KeybindStep available = new ActionStep((short) 2,
                TargetSpec.simple(TargetKind.HOVER));

        ExecutionPlan plan = new ExecutionPlanner().plan(Arrays.asList(missing, available),
                step -> {
                    if (step == missing)
                        throw new StepUnavailableException("Exact object \"Stump\" was not found");
                    return 1;
                });

        assertTrue(plan.getEntries().get(0).isSkipped());
        assertTrue(plan.getEntries().get(0).getSkippedBy() instanceof StepUnavailableException);
        assertFalse(plan.getEntries().get(1).isSkipped());
        assertEquals(1, plan.getQueueCost());
        ActionExecutor actions = new ActionExecutor(null,
                actionId -> actionId == 97 ? "Chop up" : "Action " + actionId);
        KeybindExecutionService execution =
                new KeybindExecutionService(actions, null, null);
        assertEquals("Step 1 (Chop up on Stump): Exact object \"Stump\" was not found, skipping.",
                execution.skipMessage(0, missing,
                        "Exact object \"Stump\" was not found"));
        assertEquals("Command Chop up skipped as player is too far from nearest stump. Come closer.",
                execution.skipMessage(0, missing,
                        "Command Chop up skipped as player is too far from nearest stump. Come closer."));
    }
}
