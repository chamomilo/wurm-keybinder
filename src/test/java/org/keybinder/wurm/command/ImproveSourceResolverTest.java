package org.keybinder.wurm.command;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ImproveSourceResolverTest {
    private final ImproveSourceResolver resolver = new ImproveSourceResolver();
    private final ResourceRequirement hammer = new ResourceRequirement(
            RequirementFamily.HAMMER, null, null, false, "hammer");

    @Test public void firstCompatibleToolbeltSlotWinsOverInventory() {
        ImproveResourceCandidate wrong = item(1L, "file");
        ImproveResourceCandidate beltHammer = item(2L, "hammer");
        ImproveResourceCandidate inventoryHammer = item(3L, "hammer");

        ResolvedImproveResource selected = resolver.resolve(
                Arrays.asList(wrong, beltHammer), root(inventoryHammer),
                null, hammer, null);

        assertEquals(2L, selected.getCandidate().getId());
        assertTrue(selected.isToolbelt());
    }

    @Test public void inventoryIsUsedOnlyWhenToolbeltHasNoMatch() {
        ImproveResourceCandidate inventoryHammer = item(3L, "hammer");

        ResolvedImproveResource selected = resolver.resolve(
                Collections.singletonList(item(1L, "file")), root(inventoryHammer),
                null, hammer, null);

        assertEquals(3L, selected.getCandidate().getId());
        assertFalse(selected.isToolbelt());
    }

    private static ImproveResourceCandidate root(ImproveResourceCandidate... children) {
        return new ImproveResourceCandidate(100L, "inventory", "inventory",
                (byte) 0, (short) 0, 0f, 0f, 0f, (byte) 0,
                Arrays.asList(children));
    }

    private static ImproveResourceCandidate item(long id, String name) {
        return new ImproveResourceCandidate(id, name, name,
                (byte) 0, (short) 0, 0f, 0f, 0f, (byte) 0,
                Collections.<ImproveResourceCandidate>emptyList());
    }
}
