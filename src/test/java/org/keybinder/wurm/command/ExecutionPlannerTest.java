package org.keybinder.wurm.command;

import org.junit.Test;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.ItemSelector;
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

    @Test
    public void unavailableExplicitToolSkipsOnlyItsOwnStep() {
        KeybindStep missingTool = new ActionStep((short) 97,
                ItemSelector.exactObject(42L, "fruit press"),
                TargetSpec.hoverType("strawberries"), "Create strawberry juice");
        KeybindStep matchingAlternative = new ActionStep((short) 98,
                ItemSelector.exactObject(42L, "fruit press"),
                TargetSpec.hoverType("green grapes"), "Create green grape juice");

        ExecutionPlan plan = new ExecutionPlanner().plan(
                Arrays.asList(missingTool, matchingAlternative), step -> {
                    if (step == missingTool)
                        throw new StepUnavailableException("Tool fruit press was not found");
                    return 1;
                });

        assertTrue(plan.getEntries().get(0).isSkipped());
        assertFalse(plan.getEntries().get(1).isSkipped());
        assertEquals(1, plan.getQueueCost());
    }

    @Test
    public void automaticNearbyFanOutUsesOnlyTheRemainingQueueCapacity() {
        KeybindStep fixed = new ActionStep((short) 97,
                TargetSpec.simple(TargetKind.HOVER));
        KeybindStep automaticNearby = new ActionStep((short) 98,
                TargetSpec.simple(TargetKind.NEARBY));
        ExecutionPlan resolved = new ExecutionPlanner().plan(
                Arrays.asList(fixed, automaticNearby),
                step -> step == automaticNearby ? 20 : 1);

        ExecutionPlan capped = KeybindExecutionService
                .capDynamicFanOutWithinQueue(resolved, 8);

        assertEquals(8, capped.getQueueCost());
        assertEquals(1, capped.getEntries().get(0).getQueueCost());
        assertEquals(7, capped.getEntries().get(1).getQueueCost());
    }

    @Test
    public void filteredFanOutDoesNotHideAKeybindThatCannotFitOnce() {
        KeybindStep first = new ActionStep((short) 97,
                TargetSpec.simple(TargetKind.HOVER));
        KeybindStep second = new ActionStep((short) 98,
                TargetSpec.simple(TargetKind.HOVER));
        KeybindStep filtered = new ActionStep((short) 99,
                TargetSpec.hoverType("strawberries"));
        ExecutionPlan resolved = new ExecutionPlanner().plan(
                Arrays.asList(first, second, filtered), step -> step == filtered ? 20 : 1);

        ExecutionPlan minimum = KeybindExecutionService
                .capDynamicFanOutWithinQueue(resolved, 2);

        assertEquals(3, minimum.getQueueCost());
    }

    @Test
    public void subsequentFilteredFanOutIsRejectedWhenQueueIsFull() {
        KeybindStep filtered = new ActionStep((short) 98,
                TargetSpec.hoverType("strawberries"));
        ExecutionPlan resolved = new ExecutionPlanner().plan(
                Arrays.asList(filtered), step -> 20);

        ExecutionPlan minimum = KeybindExecutionService
                .capDynamicFanOutWithinQueue(resolved, 0);

        assertEquals(1, minimum.getQueueCost());
    }
}
