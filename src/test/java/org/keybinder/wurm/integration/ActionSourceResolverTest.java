package org.keybinder.wurm.integration;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Test;
import org.keybinder.wurm.command.ActionSourceResolver;
import org.keybinder.wurm.command.StepUnavailableException;
import org.keybinder.wurm.model.ItemSelector;

import java.lang.reflect.Field;

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
        assertNull(source.getConcreteTool());
    }

    @Test public void concreteSourceRetainsTheToolThatMustBecomeActive() throws Exception {
        InventoryMetaItem item = item(42L);

        ActionSourceResolver.ResolvedSource source =
                ActionSourceResolver.ResolvedSource.concreteTool(item);

        assertTrue(source.hasOverride());
        assertEquals(42L, source.getSourceId());
        assertSame(item, source.getConcreteTool());
    }

    private static InventoryMetaItem item(long id) throws Exception {
        Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
        InventoryMetaItem item = (InventoryMetaItem) unsafe.allocateInstance(
                InventoryMetaItem.class);
        Field idField = InventoryMetaItem.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(item, id);
        return item;
    }
}
