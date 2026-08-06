package org.keybinder.wurm.ui;

import org.junit.Test;
import org.keybinder.wurm.model.ArcheologyIdentifySourceMode;
import org.keybinder.wurm.model.ArcheologyIdentifyStep;
import org.keybinder.wurm.model.BulkDestinationKind;
import org.keybinder.wurm.model.ItemSelector;
import org.keybinder.wurm.model.SmartImproveSourceMode;
import org.keybinder.wurm.model.StepKind;
import org.keybinder.wurm.model.TargetKind;

import static org.junit.Assert.assertEquals;

public class ArcheologyIdentifyEditorTest {
    @Test public void editorDraftPreservesTargetAndToolbeltOnlyMode() {
        ArcheologyIdentifyStep step = (ArcheologyIdentifyStep) new EditorStepDraft(
                StepKind.ARCHEOLOGY_IDENTIFY, "", "",
                ItemSelector.currentActive(), "selected", null, null,
                null, "1", BulkDestinationKind.PLAYER_INVENTORY, null,
                SmartImproveSourceMode.TOOLBELT_THEN_INVENTORY,
                ArcheologyIdentifySourceMode.TOOLBELT_ONLY).toStep(null);

        assertEquals(TargetKind.SELECTED, step.getTarget().getKind());
        assertEquals(ArcheologyIdentifySourceMode.TOOLBELT_ONLY,
                step.getSourceMode());
        assertEquals(2, EditorStepType.initialIndex(step));
    }
}
