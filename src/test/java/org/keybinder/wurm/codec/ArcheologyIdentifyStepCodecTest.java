package org.keybinder.wurm.codec;

import org.junit.Test;
import org.keybinder.wurm.model.ArcheologyIdentifySourceMode;
import org.keybinder.wurm.model.ArcheologyIdentifyStep;
import org.keybinder.wurm.model.TargetKind;
import org.keybinder.wurm.model.TargetSpec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ArcheologyIdentifyStepCodecTest {
    @Test public void storeAndTransferRoundTripTargetAndSourceMode() throws Exception {
        ArcheologyIdentifyStep original = new ArcheologyIdentifyStep(
                TargetSpec.simple(TargetKind.SELECTED),
                ArcheologyIdentifySourceMode.TOOLBELT_ONLY);
        Properties storedProperties = new Properties();
        Properties transferProperties = new Properties();

        KeybindStepCodec.writeStore(storedProperties, "step.", original);
        KeybindStepCodec.writeTransfer(transferProperties, "step.", original);
        ArcheologyIdentifyStep stored = (ArcheologyIdentifyStep)
                KeybindStepCodec.readStore(storedProperties, "step.", "record");
        ArcheologyIdentifyStep transferred = (ArcheologyIdentifyStep)
                KeybindStepCodec.readTransfer(transferProperties, "step.");

        assertEquals(TargetKind.SELECTED, stored.getTarget().getKind());
        assertEquals(ArcheologyIdentifySourceMode.TOOLBELT_ONLY,
                stored.getSourceMode());
        assertEquals(TargetKind.SELECTED, transferred.getTarget().getKind());
        assertEquals(ArcheologyIdentifySourceMode.TOOLBELT_ONLY,
                transferred.getSourceMode());
    }

    @Test public void missingModeDefaultsToToolbeltThenInventory() throws Exception {
        Properties properties = properties("ARCHEOLOGY_IDENTIFY", "hover");

        ArcheologyIdentifyStep stored = (ArcheologyIdentifyStep)
                KeybindStepCodec.readStore(properties, "step.", "legacy");
        ArcheologyIdentifyStep transferred = (ArcheologyIdentifyStep)
                KeybindStepCodec.readTransfer(properties, "step.");

        assertEquals(ArcheologyIdentifySourceMode.TOOLBELT_THEN_INVENTORY,
                stored.getSourceMode());
        assertEquals(ArcheologyIdentifySourceMode.TOOLBELT_THEN_INVENTORY,
                transferred.getSourceMode());
    }

    @Test public void malformedModeIsRejectedWithControlledIOException() throws Exception {
        Properties properties = properties("ARCHEOLOGY_IDENTIFY", "hover");
        properties.setProperty("step.archeologySourceMode", "EVERYWHERE");
        try {
            KeybindStepCodec.readStore(properties, "step.", "bad");
            fail("Malformed source mode was accepted");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("Archeology Identify"));
        }
    }

    private static Properties properties(String kind, String target) {
        Properties properties = new Properties();
        properties.setProperty("step.kind", kind);
        properties.setProperty("step.target", Base64.getEncoder().encodeToString(
                target.getBytes(StandardCharsets.UTF_8)));
        return properties;
    }
}
