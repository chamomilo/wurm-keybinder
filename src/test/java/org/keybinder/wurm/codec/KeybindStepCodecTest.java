package org.keybinder.wurm.codec;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;
import org.junit.Test;
import org.keybinder.wurm.model.SmartImproveSourceMode;
import org.keybinder.wurm.model.SmartImproveStep;

import static org.junit.Assert.assertEquals;

public class KeybindStepCodecTest {
    @Test public void missingImproveSourceModeKeepsLegacyInventoryFallback() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("step.kind", "SMART_IMPROVE");
        properties.setProperty("step.target", Base64.getEncoder().encodeToString(
                "hover".getBytes(StandardCharsets.UTF_8)));

        SmartImproveStep stored = (SmartImproveStep) KeybindStepCodec.readStore(
                properties, "step.", "legacy");
        SmartImproveStep transferred = (SmartImproveStep) KeybindStepCodec.readTransfer(
                properties, "step.");

        assertEquals(SmartImproveSourceMode.TOOLBELT_THEN_INVENTORY,
                stored.getSourceMode());
        assertEquals(SmartImproveSourceMode.TOOLBELT_THEN_INVENTORY,
                transferred.getSourceMode());
    }
}
