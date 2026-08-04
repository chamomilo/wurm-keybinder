package org.keybinder.wurm.command;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BulkTransferRejectionMessageTest {
    @Test public void recognizesStandardPermissionDenialsOnlyInEvent() {
        assertTrue(BulkTransferRejectionMessage.matches(":Event",
                "That would be illegal here. You can check the settlement token for the local laws."));
        assertTrue(BulkTransferRejectionMessage.matches(":event",
                "You do not have permission to take that item."));
        assertTrue(BulkTransferRejectionMessage.matches(":Event",
                "You are not allowed to do that."));
        assertTrue(BulkTransferRejectionMessage.matches(":Event",
                "Only ingredients that are used to make food can be put onto a roasting dish."));
        assertFalse(BulkTransferRejectionMessage.matches(":Local",
                "That would be illegal here."));
        assertFalse(BulkTransferRejectionMessage.matches(":Event",
                "You are too far away."));
    }
}
