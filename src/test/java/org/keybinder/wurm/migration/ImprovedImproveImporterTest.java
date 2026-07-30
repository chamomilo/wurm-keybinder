package org.keybinder.wurm.migration;

import org.junit.Test;
import org.keybinder.wurm.model.SmartImproveStep;
import org.keybinder.wurm.model.TargetKind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ImprovedImproveImporterTest {
    private final ImprovedImproveImporter importer = new ImprovedImproveImporter();

    @Test
    public void recognizesKnownHistoricalCommands() {
        assertTrue(importer.supports("improve"));
        assertTrue(importer.supports("i2improve"));
        assertTrue(importer.supports("improveitems"));
        assertFalse(importer.supports("keybinder_run id"));
    }

    @Test
    public void convertsToNativeSmartImproveStep() {
        SmartImproveStep step = (SmartImproveStep) importer.importCommand("i2improve").get(0);
        assertEquals(TargetKind.HOVER, step.getTarget().getKind());
    }
}
