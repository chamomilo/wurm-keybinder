package org.keybinder.wurm.bind;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VanillaImportPolicyTest {
    private final VanillaImportPolicy policy = new VanillaImportPolicy();

    @Test public void excludesMovementBindings() {
        assertFalse(policy.mayImport("MOVE_FORWARD"));
        assertFalse(policy.mayImport("TURN_LEFT"));
        assertFalse(policy.mayImport("STRAFE"));
    }

    @Test public void excludesHudBindingsIncludingQuotedCommands() {
        assertFalse(policy.mayImport("MAIN_MENU"));
        assertFalse(policy.mayImport("\"toggle inventory\""));
    }

    @Test public void keepsGameplayAndUnknownCustomCommands() {
        assertTrue(policy.mayImport("EXAMINE"));
        assertTrue(policy.mayImport("exec mine.txt"));
        assertTrue(policy.mayImport("act 163 tool"));
    }
}
