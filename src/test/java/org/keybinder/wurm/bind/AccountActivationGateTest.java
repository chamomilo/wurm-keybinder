package org.keybinder.wurm.bind;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AccountActivationGateTest {
    @Test public void waitsForNameAndAppliesEachAltOnce() {
        AccountActivationGate gate = new AccountActivationGate();

        assertFalse(gate.shouldApply("", 100L));
        assertTrue(gate.shouldApply("First Alt", 100L));
        gate.applied("First Alt");
        assertTrue(gate.isApplied("first alt"));
        assertFalse(gate.shouldApply("FIRST ALT", 200L));
        assertTrue(gate.shouldApply("Second Alt", 200L));
    }

    @Test public void failedLoadIsRetriedAfterDelay() {
        AccountActivationGate gate = new AccountActivationGate();
        gate.failed("Alt", 100L, 5000L);

        assertFalse(gate.shouldApply("alt", 5099L));
        assertTrue(gate.shouldApply("alt", 5100L));
        gate.clear();
        assertTrue(gate.shouldApply("alt", 5101L));
    }

    @Test public void executionRemainsBlockedUntilProfileIsAppliedAgain() {
        AccountActivationGate gate = new AccountActivationGate();

        assertFalse(gate.isApplied("Alt"));
        gate.applied("Alt");
        assertTrue(gate.isApplied("alt"));

        gate.clear();
        assertFalse(gate.isApplied("ALT"));
        assertTrue(gate.shouldApply("Alt", 100L));
    }
}
