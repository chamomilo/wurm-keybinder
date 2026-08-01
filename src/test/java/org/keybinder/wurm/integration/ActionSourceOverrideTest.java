package org.keybinder.wurm.integration;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Test;

public class ActionSourceOverrideTest {
    @After public void reset() { ActionSourceOverride.resetForTests(); }

    @Test public void nestedScopesRestorePreviousValue() {
        assertEquals(10L, ActionSourceOverride.overrideOr(10L));
        try (ActionSourceOverride.Scope outer = ActionSourceOverride.push(20L)) {
            assertEquals(20L, ActionSourceOverride.overrideOr(10L));
            try (ActionSourceOverride.Scope inner = ActionSourceOverride.push(-1L)) {
                assertEquals(-1L, ActionSourceOverride.overrideOr(10L));
            }
            assertEquals(20L, ActionSourceOverride.overrideOr(10L));
        }
        assertEquals(10L, ActionSourceOverride.overrideOr(10L));
    }

    @Test public void tryWithResourcesCleansAfterException() {
        try {
            try (ActionSourceOverride.Scope ignored = ActionSourceOverride.push(99L)) {
                throw new IllegalStateException("boom");
            }
        } catch (IllegalStateException expected) { }
        assertEquals(3L, ActionSourceOverride.overrideOr(3L));
    }
}
