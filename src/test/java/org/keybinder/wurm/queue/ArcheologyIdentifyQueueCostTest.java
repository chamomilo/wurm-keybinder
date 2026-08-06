package org.keybinder.wurm.queue;

import org.junit.Test;
import org.keybinder.wurm.model.ArcheologyIdentifyStep;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.TargetKind;
import org.keybinder.wurm.model.TargetSpec;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class ArcheologyIdentifyQueueCostTest {
    @Test public void hoveredIdentifyBatchHasDynamicQueuedActionCost() {
        KeybindRecord record = new KeybindRecord(null, "Identify", "I",
                Collections.<KeybindStep>singletonList(new ArcheologyIdentifyStep(
                        TargetSpec.simple(TargetKind.HOVER))));

        QueueCost cost = new ActionQueueCostCalculator().keybindCost(record);

        assertEquals(QueueCost.Kind.DYNAMIC, cost.getKind());
    }

    @Test public void singleIdentifyTargetIsOneFixedQueuedAction() {
        KeybindRecord record = new KeybindRecord(null, "Identify", "I",
                Collections.<KeybindStep>singletonList(new ArcheologyIdentifyStep(
                        TargetSpec.simple(TargetKind.SELECTED))));

        QueueCost cost = new ActionQueueCostCalculator().keybindCost(record);

        assertEquals(QueueCost.Kind.FIXED, cost.getKind());
        assertEquals(1, cost.getValue());
    }
}
