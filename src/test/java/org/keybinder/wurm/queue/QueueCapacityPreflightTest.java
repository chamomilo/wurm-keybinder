package org.keybinder.wurm.queue;

import org.junit.Test;
import org.keybinder.wurm.command.ActionExecutor;
import org.keybinder.wurm.command.KeybindExecutionService;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.TargetKind;
import org.keybinder.wurm.model.TargetSpec;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class QueueCapacityPreflightTest {
    @Test
    public void calculatesRemainingSlotsWithoutNegativeValues() {
        assertEquals(4, QueueCapacityPreflight.remaining(4, 0));
        assertEquals(1, QueueCapacityPreflight.remaining(4, 3));
        assertEquals(0, QueueCapacityPreflight.remaining(4, 9));
        assertEquals(Integer.MAX_VALUE, QueueCapacityPreflight.remaining(-1, 9));
    }

    @Test
    public void fullOrPartialQueueRejectsWholeKeybindBeforeFirstStep() throws Exception {
        KeybindStep first = new ActionStep((short) 30000,
                TargetSpec.simple(TargetKind.HOVER));
        KeybindStep second = new ActionStep((short) 30001,
                TargetSpec.simple(TargetKind.HOVER));
        KeybindRecord record = new KeybindRecord(
                "remaining-capacity", "Two actions", "R", Arrays.asList(first, second));
        KeybindExecutionService execution =
                new KeybindExecutionService(new ActionExecutor(null), null, null);

        try {
            execution.execute(record, null, 4, () -> 3);
            fail("Expected remaining-capacity preflight to reject the keybind");
        } catch (QueueCapacityException expected) {
            assertEquals(1, QueueCapacityPreflight.remaining(4, 3));
        }
    }

    @Test
    public void zeroCostWorkFitsEvenWhenQueueIsFull() {
        QueueCapacityPreflight.requireFits(0, 4, 4);
    }
}
