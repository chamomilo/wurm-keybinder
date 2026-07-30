package org.keybinder.wurm.migration;

import org.junit.Test;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.ActivateToolStep;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.TargetKind;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CustomActionsImporterTest {
    private final CustomActionsImporter importer = new CustomActionsImporter();

    @Test
    public void importsActionChainIntoNativeSteps() {
        List<KeybindStep> steps = importer.importCommand("act 154 tile | 183 @tb1");
        assertEquals(2, steps.size());
        assertEquals(TargetKind.TILE, ((ActionStep) steps.get(0)).getTarget().getKind());
        assertEquals(TargetKind.TOOLBELT_SLOT,
                ((ActionStep) steps.get(1)).getTarget().getKind());
    }

    @Test
    public void normalizesLegacyToolbeltOverload() {
        List<KeybindStep> steps = importer.importCommand("3 toolbelt | 154 tile");
        assertTrue(steps.get(0) instanceof ActivateToolStep);
        assertEquals(3, ((ActivateToolStep) steps.get(0)).getTarget().getSlot());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMalformedChain() {
        importer.importCommand("154 tile |");
    }
}
