package org.keybinder.wurm.integration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ExecutionHoverOverrideTest {
    @Test public void nestedScopeRestoresPreviousSnapshot() {
        ExecutionHoverOverride.Snapshot outer =
                new ExecutionHoverOverride.Snapshot(44L, true);
        ExecutionHoverOverride.Snapshot inner =
                new ExecutionHoverOverride.Snapshot(55L, false);

        assertNull(ExecutionHoverOverride.current());
        try (ExecutionHoverOverride.Scope ignored = ExecutionHoverOverride.push(outer)) {
            assertEquals(44L, ExecutionHoverOverride.current().getWorldObjectId());
            assertTrue(ExecutionHoverOverride.current().isGroundItem());
            try (ExecutionHoverOverride.Scope nested = ExecutionHoverOverride.push(inner)) {
                assertEquals(55L, ExecutionHoverOverride.current().getWorldObjectId());
                assertFalse(ExecutionHoverOverride.current().isGroundItem());
            }
            assertEquals(44L, ExecutionHoverOverride.current().getWorldObjectId());
        }
        assertNull(ExecutionHoverOverride.current());
    }
}
