package org.keybinder.wurm.command;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ActionRangeResolverTest {
    private final ActionRangeResolver ranges = new ActionRangeResolver();

    @Test
    public void usesWurmActionEntryRanges() {
        assertEquals(6.0f, ranges.actionRange((short) 6), 0.001f);
        assertEquals(4.0f, ranges.actionRange((short) 97), 0.001f);
        assertEquals(200.0f, ranges.actionRange((short) 124), 0.001f);
    }

    @Test
    public void scansAtLeastTwiceTheActionRange() {
        assertEquals(12.0f, ranges.scanRange((short) 6, 4.0f), 0.001f);
        assertEquals(20.0f, ranges.scanRange((short) 97, 20.0f), 0.001f);
    }

    @Test
    public void separatesExecutableAndDiagnosticDistance() {
        assertTrue(ranges.isWithinActionRange((short) 6, 36.0d));
        assertFalse(ranges.isWithinActionRange((short) 6, 36.1d));
    }
}
