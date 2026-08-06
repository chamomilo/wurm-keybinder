package org.keybinder.wurm.recording;

import org.junit.Test;
import org.keybinder.wurm.command.ExactObjectTarget;
import org.keybinder.wurm.command.InventoryFilterTarget;
import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.model.BulkStorageItem;
import org.keybinder.wurm.model.InventoryReference;

import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SelectionControllerTest {
    @Test
    public void exactObjectSelectionStoresClickedItemIdentity() {
        SelectionController controller =
                new SelectionController(new EventLogger(Logger.getAnonymousLogger()));

        controller.requestExactObject();
        assertTrue(controller.acceptExactObject(123456L, "steel pickaxe"));

        String selected = controller.consumeSelectedTarget();
        assertTrue(ExactObjectTarget.isExact(selected));
        assertEquals(123456L, ExactObjectTarget.id(selected));
        assertEquals("steel pickaxe", ExactObjectTarget.name(selected));
        assertFalse(controller.acceptExactObject(7L, "second item"));
    }

    @Test public void bulkSourceStoresBothStorageAndItemIdentity() {
        SelectionController controller =
                new SelectionController(new EventLogger(Logger.getAnonymousLogger()));

        controller.requestBulkSource();
        assertTrue(controller.acceptBulkSource(
                101L, "bulk storage bin", 202L, "barley"));
        // The editor polls the generic target result first. It must not steal
        // the completion flag from the dedicated bulk-source result.
        assertNull(controller.consumeSelectedTarget());
        BulkStorageItem selected = controller.consumeSelectedBulkSource();

        assertEquals(101L, selected.getStorage().getId());
        assertEquals(202L, selected.getItem().getId());
        assertEquals("barley", selected.getItem().getName());
    }

    @Test public void inventoryFilterStoresTypeButNeverClickedRuntimeId() {
        SelectionController controller =
                new SelectionController(new EventLogger(Logger.getAnonymousLogger()));

        controller.requestInventoryFilter();
        assertTrue(controller.acceptInventoryFilter("salty water"));
        String selected = controller.consumeSelectedTarget();

        assertTrue(InventoryFilterTarget.isInventoryFilter(selected));
        assertEquals("water", InventoryFilterTarget.type(selected));
        assertFalse(selected.contains("123456"));
    }

    @Test public void allFilterSelectionModesCaptureTheSameCanonicalType() {
        SelectionController controller =
                new SelectionController(new EventLogger(Logger.getAnonymousLogger()));

        controller.requestNearbyType();
        assertTrue(controller.acceptNearbyType("rare oakenwood log (glowing)"));
        assertEquals("nearby log", controller.consumeSelectedTarget());

        controller.requestHoverType();
        assertTrue(controller.acceptHoverType("supreme log (searing hot), cedarwood"));
        assertEquals("hover-type log", controller.consumeSelectedTarget());

        controller.requestInventoryFilter();
        assertTrue(controller.acceptInventoryFilter("fantastic steel log (boiling)"));
        assertEquals("inventory+filter log", controller.consumeSelectedTarget());
    }

    @Test public void bulkDestinationStoresExactInventoryIdentity() {
        SelectionController controller =
                new SelectionController(new EventLogger(Logger.getAnonymousLogger()));

        controller.requestBulkDestination();
        assertTrue(controller.acceptBulkDestination(303L, "small barrel"));
        assertNull(controller.consumeSelectedTarget());
        assertNull(controller.consumeSelectedBulkSource());
        InventoryReference selected = controller.consumeSelectedBulkDestination();

        assertEquals(303L, selected.getId());
        assertEquals("small barrel", selected.getName());
    }

    @Test public void bulkDestinationAcceptsNativePlayerInventorySentinel() {
        SelectionController controller =
                new SelectionController(new EventLogger(Logger.getAnonymousLogger()));

        controller.requestBulkDestination();
        assertTrue(controller.acceptBulkDestination(-1L, "inventory"));

        assertEquals(-1L, controller.consumeSelectedBulkDestination().getId());
    }
}
