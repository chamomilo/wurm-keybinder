package org.keybinder.wurm.catalog;

import com.wurmonline.shared.constants.PlayerAction;
import org.junit.Test;

import static org.junit.Assert.*;

public class VanillaPlayerActionCatalogTest {
    private final VanillaPlayerActionCatalog catalog =
            new VanillaPlayerActionCatalog();

    @Test
    public void inheritsCanonicalMasksForKnownVanillaActions() {
        assertSame(PlayerAction.PUSH, catalog.find((short) 99));
        assertEquals(PlayerAction.GROUND_ITEM,
                catalog.resolveOrGeneric((short) 99).getTargetMask());

        assertSame(PlayerAction.DESTROY_ITEM, catalog.find((short) 83));
        assertEquals(PlayerAction.GROUND_ITEM,
                catalog.resolveOrGeneric((short) 83).getTargetMask());
        assertEquals(PlayerAction.HOUSE,
                catalog.resolveOrGeneric((short) 174).getTargetMask());
        assertEquals(PlayerAction.ANY_TILE,
                catalog.resolveOrGeneric((short) 191).getTargetMask());
    }

    @Test
    public void unknownModOrServerActionKeepsAnythingFallback() {
        short unknown = 12345;
        assertNull(catalog.find(unknown));
        PlayerAction action = catalog.resolveOrGeneric(unknown);
        assertEquals(unknown, action.getId());
        assertEquals(PlayerAction.ANYTHING, action.getTargetMask());
        assertFalse(action.isInstant());
    }
}
