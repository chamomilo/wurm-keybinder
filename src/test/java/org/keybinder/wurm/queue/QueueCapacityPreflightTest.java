package org.keybinder.wurm.queue;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class QueueCapacityPreflightTest {
    @Test
    public void calculatesRemainingSlotsWithoutNegativeValues() {
        assertEquals(4, QueueCapacityPreflight.remaining(4, 0));
        assertEquals(1, QueueCapacityPreflight.remaining(4, 3));
        assertEquals(0, QueueCapacityPreflight.remaining(4, 9));
        assertEquals(Integer.MAX_VALUE, QueueCapacityPreflight.remaining(-1, 9));
    }

    @Test
    public void remainingCapacityCanBeRecheckedForEveryStep() {
        assertEquals(1, QueueCapacityPreflight.remaining(4, 3));
        assertEquals(0, QueueCapacityPreflight.remaining(4, 4));
    }
}
