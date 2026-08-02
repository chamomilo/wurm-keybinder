package org.keybinder.wurm.ui;

import org.junit.Test;
import org.keybinder.wurm.catalog.VanillaKeybindCatalog;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.ConsoleCommandStep;
import org.keybinder.wurm.model.ItemSelector;
import org.keybinder.wurm.model.ItemSelectorKind;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.StepKind;
import org.keybinder.wurm.model.TargetKind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class EditorStepDraftTest {
    @Test public void customActionSnapshotBuildsTypedStep() {
        EditorStepDraft draft = new EditorStepDraft(StepKind.CUSTOM_ACTION,
                "1234", "", ItemSelector.toolbeltSlot(3), "selected", null, null);

        ActionStep step = (ActionStep) draft.toStep(id -> "Captured " + id);

        assertEquals(1234, step.getActionId());
        assertEquals("Captured 1234", step.getLastKnownName());
        assertEquals(ItemSelectorKind.TOOLBELT_SLOT, step.getSource().getKind());
        assertEquals(3, step.getSource().getSlot());
        assertEquals(TargetKind.SELECTED, step.getTarget().getKind());
    }

    @Test public void consoleSnapshotRejectsBlankAndPreservesEnteredText() {
        try {
            draft(StepKind.CONSOLE_COMMAND, "", "   ", "hover").toStep(null);
            fail("blank command accepted");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().length() > 0);
        }

        ConsoleCommandStep step = (ConsoleCommandStep)
                draft(StepKind.CONSOLE_COMMAND, "", " say hello ", "hover").toStep(null);
        assertEquals(" say hello ", step.getCommand());
    }

    @Test public void malformedAndOutOfRangeActionIdsHaveEditorValidationErrors() {
        assertInvalidAction("not-a-number");
        assertInvalidAction("32768");
    }

    @Test public void vanillaSelectionUsesTheSharedCatalogConversion() {
        VanillaKeybindCatalog catalog = new VanillaKeybindCatalog();
        VanillaKeybindCatalog.Category category = catalog.categoryFor("EXAMINE");
        VanillaKeybindCatalog.Entry entry = catalog.find("EXAMINE");

        KeybindStep step = new EditorStepDraft(StepKind.VANILLA_ACTION, "", "",
                ItemSelector.emptyHand(), "hover", category, entry).toStep(null);

        assertTrue(step instanceof ActionStep);
        assertEquals(TargetKind.HOVER, ((ActionStep) step).getTarget().getKind());
    }

    private static EditorStepDraft draft(
            StepKind kind, String id, String command, String target) {
        return new EditorStepDraft(kind, id, command, ItemSelector.currentActive(),
                target, null, null);
    }

    private static void assertInvalidAction(String id) {
        try {
            draft(StepKind.CUSTOM_ACTION, id, "", "hover").toStep(null);
            fail("invalid action accepted: " + id);
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().length() > 0);
        }
    }
}
