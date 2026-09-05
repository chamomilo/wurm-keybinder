package org.keybinder.wurm.integration;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WorldImproveEventScopeTest {
    @Test public void silentDecisionSurvivesOnlyInsideInboundMessageScope() {
        assertFalse(WorldImproveEventScope.shouldSuppress());

        WorldImproveEventScope.enter(true);
        try {
            assertTrue(WorldImproveEventScope.shouldSuppress());
            WorldImproveEventScope.enter(false);
            try {
                assertFalse(WorldImproveEventScope.shouldSuppress());
            } finally {
                WorldImproveEventScope.exit();
            }
            assertTrue(WorldImproveEventScope.shouldSuppress());
        } finally {
            WorldImproveEventScope.exit();
        }

        assertFalse(WorldImproveEventScope.shouldSuppress());
    }
}
