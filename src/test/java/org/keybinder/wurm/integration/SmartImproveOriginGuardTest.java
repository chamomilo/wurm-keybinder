package org.keybinder.wurm.integration;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SmartImproveOriginGuardTest {
    @Test public void nestedScopesPreserveSmartImproveOrigin() {
        assertFalse(SmartImproveOriginGuard.isActive());
        SmartImproveOriginGuard.enter();
        SmartImproveOriginGuard.enter();
        assertTrue(SmartImproveOriginGuard.isActive());
        SmartImproveOriginGuard.exit();
        assertTrue(SmartImproveOriginGuard.isActive());
        SmartImproveOriginGuard.exit();
        assertFalse(SmartImproveOriginGuard.isActive());
    }
}
