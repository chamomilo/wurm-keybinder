package org.keybinder.wurm.validation;

import org.junit.Test;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.BulkDestinationKind;
import org.keybinder.wurm.model.BulkStorageItem;
import org.keybinder.wurm.model.BulkTransferStep;
import org.keybinder.wurm.model.InventoryReference;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.TargetKind;
import org.keybinder.wurm.model.TargetSpec;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.fail;

public class BulkTransferValidationTest {
    @Test public void acceptsPositiveQuantityUpToCapturedMaximum() {
        KeybindValidator.validatePendingSteps("Take barley", "B",
                Collections.<KeybindStep>singletonList(step(1)));
        KeybindValidator.validatePendingSteps("Take barley", "B",
                Collections.<KeybindStep>singletonList(step(5)));
    }

    @Test public void rejectsZeroOrQuantityAboveCapturedMaximum() {
        assertInvalid(Collections.<KeybindStep>singletonList(step(0)));
        assertInvalid(Collections.<KeybindStep>singletonList(step(6)));
    }

    @Test public void acceptsMultipleBulkTransfersInOneVariant() {
        KeybindValidator.validatePendingSteps("Bulk", "B",
                Arrays.<KeybindStep>asList(step(1), step(1), step(1)));
    }

    @Test public void acceptsBulkTransferMixedWithAnotherStep() {
        KeybindValidator.validatePendingSteps("Bulk then light", "B",
                Arrays.<KeybindStep>asList(step(1),
                        new ActionStep((short) 1,
                                TargetSpec.simple(TargetKind.HOVER))));
    }

    private static void assertInvalid(java.util.List<KeybindStep> steps) {
        try {
            KeybindValidator.validatePendingSteps("Bulk", "B", steps);
            fail("invalid bulk transfer accepted");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    private static BulkTransferStep step(int quantity) {
        return new BulkTransferStep(new BulkStorageItem(
                new InventoryReference(101L, "bulk storage bin"),
                new InventoryReference(202L, "barley (5x)")), quantity,
                BulkDestinationKind.PLAYER_INVENTORY, null);
    }
}
