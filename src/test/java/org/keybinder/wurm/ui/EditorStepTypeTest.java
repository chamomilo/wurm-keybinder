package org.keybinder.wurm.ui;

import org.junit.Test;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.ConsoleCommandStep;
import org.keybinder.wurm.model.ItemSelector;
import org.keybinder.wurm.model.TargetKind;
import org.keybinder.wurm.model.TargetSpec;
import org.keybinder.wurm.model.VanillaActionStep;
import org.keybinder.wurm.model.StepKind;
import org.keybinder.wurm.model.BulkDestinationKind;
import org.keybinder.wurm.model.BulkStorageItem;
import org.keybinder.wurm.model.BulkTransferStep;
import org.keybinder.wurm.model.InventoryReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EditorStepTypeTest {
    @Test public void ordinaryActionNeverReopensAsCatalogProcedure() {
        ActionStep cutDown = new ActionStep((short) 96, ItemSelector.toolbeltSlot(3),
                TargetSpec.simple(TargetKind.HOVER), "Cut down");

        assertEquals(3, EditorStepType.initialIndex(cutDown));
        assertEquals(3, cutDown.getSource().getSlot());
    }

    @Test public void onlyNativeCompatibilityStepNeedsCatalogClassification() {
        assertEquals(-1, EditorStepType.initialIndex(new VanillaActionStep("MAIN_MENU")));
    }

    @Test public void consoleAndVanillaCommandsDoNotOfferActionCapture() {
        assertEquals(2, EditorStepType.initialIndex(
                new ConsoleCommandStep("toggle livemap")));
        assertFalse(EditorStepType.supportsCapture(StepKind.CONSOLE_COMMAND));
        assertFalse(EditorStepType.supportsCapture(StepKind.VANILLA_ACTION));
        assertTrue(EditorStepType.supportsCapture(StepKind.CUSTOM_ACTION));
        assertTrue(EditorStepType.supportsCapture(StepKind.ACTIVATE_TOOL));
        assertTrue(EditorStepType.supportsCapture(StepKind.SMART_IMPROVE));
        assertFalse(EditorStepType.supportsCapture(StepKind.BULK_TRANSFER));
    }

    @Test public void bulkTransferHasItsOwnEditorType() {
        BulkTransferStep bulk = new BulkTransferStep(new BulkStorageItem(
                new InventoryReference(1L, "bulk storage bin"),
                new InventoryReference(2L, "barley")), 1,
                BulkDestinationKind.PLAYER_INVENTORY, null);
        assertEquals(4, EditorStepType.initialIndex(bulk));
    }
}
