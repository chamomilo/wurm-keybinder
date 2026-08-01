package org.keybinder.wurm.integration;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Test;
import org.keybinder.wurm.command.StepUnavailableException;
import org.keybinder.wurm.model.ItemSelector;

public class ActionSourceResolverTest {
    @After public void reset() { ActionSourceOverride.resetForTests(); }

    @Test public void currentActiveNeverRequiresTheOptionalHook() throws Exception {
        ActionSourceResolver.ResolvedSource source =
                new ActionSourceResolver(null).resolve(ItemSelector.currentActive(), null);
        assertFalse(source.hasOverride());
    }

    @Test public void explicitSourceFailsBeforeHudAccessWhenCapabilityIsMissing() throws Exception {
        try {
            new ActionSourceResolver(null).resolve(ItemSelector.emptyHand(), null);
            fail("Expected unavailable source capability");
        } catch (StepUnavailableException expected) {
            assertFalse(expected.getMessage().trim().isEmpty());
        }
    }

    @Test public void emptyHandUsesMinusOneWithoutChangingHudState() throws Exception {
        ActionSourceOverride.markHookAvailable();
        ActionSourceResolver.ResolvedSource source =
                new ActionSourceResolver(null).resolve(ItemSelector.emptyHand(), null);
        assertTrue(source.hasOverride());
        assertEquals(-1L, source.getSourceId());
    }
}
