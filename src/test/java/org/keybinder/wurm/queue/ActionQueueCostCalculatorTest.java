package org.keybinder.wurm.queue;

import org.keybinder.wurm.migration.CustomActionsImporter;
import org.junit.Test;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.ActivateToolStep;
import org.keybinder.wurm.model.ConsoleCommandStep;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.SmartImproveStep;
import org.keybinder.wurm.model.TargetKind;
import org.keybinder.wurm.model.TargetSpec;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class ActionQueueCostCalculatorTest {
    private final CustomActionsImporter importer = new CustomActionsImporter();
    private final ActionQueueCostCalculator calculator = new ActionQueueCostCalculator();

    @Test
    public void toolbeltSelectionDoesNotConsumeQueue() {
        KeybindRecord record = new KeybindRecord("id", "Legacy", "R",
                importer.importCommand("3 toolbelt | 154 tile | 4 toolbelt | 318 tile"));
        QueueCost cost = calculator.keybindCost(record);
        assertEquals(QueueCost.Kind.FIXED, cost.getKind());
        assertEquals(2, cost.getValue());
    }

    @Test
    public void areaCostsNine() {
        ActionStep step = (ActionStep) importer.importCommand("154 area").get(0);
        assertEquals(9, calculator.stepCost(step).getValue());
    }

    @Test
    public void nearbyIsDynamic() {
        assertEquals(QueueCost.Kind.DYNAMIC,
                calculator.stepCost((ActionStep) importer.importCommand(
                        "6 @nearby4.5").get(0)).getKind());
    }

    @Test
    public void filteredTargetsHaveZeroDesignTimeCost() {
        ActionStep automaticNearby = new ActionStep((short) 6,
                TargetSpec.simple(TargetKind.NEARBY));
        ActionStep nearbyFilter = new ActionStep((short) 6,
                TargetSpec.nearbyType("oak chest"));
        ActionStep hoverFilter = new ActionStep((short) 6,
                TargetSpec.hoverType("strawberries"));

        QueueCost automaticNearbyCost = calculator.stepCost(automaticNearby);
        QueueCost nearbyCost = calculator.stepCost(nearbyFilter);
        QueueCost hoverCost = calculator.stepCost(hoverFilter);
        assertEquals(QueueCost.Kind.FIXED, automaticNearbyCost.getKind());
        assertEquals(0, automaticNearbyCost.getValue());
        assertEquals(QueueCost.Kind.FIXED, nearbyCost.getKind());
        assertEquals(0, nearbyCost.getValue());
        assertEquals(QueueCost.Kind.FIXED, hoverCost.getKind());
        assertEquals(0, hoverCost.getValue());
    }

    @Test
    public void filteredTargetsDoNotMakeMixedKeybindProblematicAtDesignTime() {
        KeybindRecord record = new KeybindRecord("id", "Filtered", "R",
                Arrays.<KeybindStep>asList(
                        new ActionStep((short) 6, TargetSpec.nearbyType("oak chest")),
                        new ActionStep((short) 6, TargetSpec.hoverType("strawberries")),
                        new ActionStep((short) 6, TargetSpec.simple(TargetKind.HOVER))));

        QueueCost cost = calculator.keybindCost(record);
        assertEquals(QueueCost.Kind.FIXED, cost.getKind());
        assertEquals(1, cost.getValue());
    }

    @Test
    public void activateToolIsZeroCostInManagedKeybind() {
        KeybindRecord record = new KeybindRecord("id", "Activate", "R",
                Arrays.<KeybindStep>asList(new ActivateToolStep(TargetSpec.toolbeltSlot(1)),
                        new ActionStep((short) 192, TargetSpec.simple(TargetKind.HOVER))));
        QueueCost cost = calculator.keybindCost(record);
        assertEquals(QueueCost.Kind.FIXED, cost.getKind());
        assertEquals(1, cost.getValue());
    }

    @Test
    public void smartImproveIsDynamic() {
        KeybindRecord record = new KeybindRecord("id", "Improve", "R",
                Arrays.<KeybindStep>asList(
                        new SmartImproveStep(TargetSpec.simple(TargetKind.HOVER))));
        assertEquals(QueueCost.Kind.DYNAMIC, calculator.keybindCost(record).getKind());
    }

    @Test
    public void genericConsoleCommandHasUnknownCost() {
        KeybindRecord record = new KeybindRecord("id", "Command", "R",
                Arrays.<KeybindStep>asList(new ConsoleCommandStep("toggle inventory")));
        assertEquals(QueueCost.Kind.UNKNOWN, calculator.keybindCost(record).getKind());
    }
}
