package org.keybinder.wurm.storage;

import org.junit.Test;
import org.keybinder.wurm.model.BulkDestinationKind;
import org.keybinder.wurm.model.BulkStorageItem;
import org.keybinder.wurm.model.BulkTransferStep;
import org.keybinder.wurm.model.InventoryReference;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindStep;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class BulkTransferStoreTest {
    @Test public void roundTripsExactSourceQuantityAndCapturedDestination() throws Exception {
        Path file = Files.createTempDirectory("keybinder-bulk")
                .resolve("keybinds.properties");
        KeybindStore store = new KeybindStore(file);
        BulkTransferStep original = new BulkTransferStep(new BulkStorageItem(
                new InventoryReference(101L, "bulk storage bin, cedar"),
                new InventoryReference(202L, "barley (100x)")), 43,
                BulkDestinationKind.CAPTURED_INVENTORY,
                new InventoryReference(303L, "large barrel"));
        store.save(Collections.singletonList(new KeybindRecord(
                "bulk", "Take barley", "B",
                Collections.<KeybindStep>singletonList(original))));

        BulkTransferStep loaded = (BulkTransferStep) store.load().get(0)
                .getKeybindSteps().get(0);

        assertEquals(101L, loaded.getSource().getStorage().getId());
        assertEquals("bulk storage bin, cedar", loaded.getSource().getStorage().getName());
        assertEquals(202L, loaded.getSource().getItem().getId());
        assertEquals("barley (100x)", loaded.getSource().getItem().getName());
        assertEquals(43, loaded.getQuantity());
        assertEquals(BulkDestinationKind.CAPTURED_INVENTORY,
                loaded.getDestinationKind());
        assertEquals(303L, loaded.getCapturedDestination().getId());
    }
}
