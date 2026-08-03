package org.keybinder.wurm.integration;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExecutionOriginGuardTest {
    @Test public void nestedInternalExecutionUnwindsWithoutGoingNegative() {
        assertFalse(ExecutionOriginGuard.isInternal());
        ExecutionOriginGuard.enterInternal();
        ExecutionOriginGuard.enterInternal();
        assertTrue(ExecutionOriginGuard.isInternal());
        ExecutionOriginGuard.exitInternal();
        assertTrue(ExecutionOriginGuard.isInternal());
        ExecutionOriginGuard.exitInternal();
        ExecutionOriginGuard.exitInternal();
        assertFalse(ExecutionOriginGuard.isInternal());
    }
}
