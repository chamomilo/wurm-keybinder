package org.keybinder.wurm.codec;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;
import org.junit.Test;
import org.keybinder.wurm.model.SmartImproveSourceMode;
import org.keybinder.wurm.model.SmartImproveStep;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.ItemSelector;
import org.keybinder.wurm.model.TargetKind;
import org.keybinder.wurm.model.TargetSpec;

import static org.junit.Assert.assertEquals;

public class KeybindStepCodecTest {
    @Test public void transferRoundTripsInventoryFilterActionSource() throws Exception {
        Properties properties = new Properties();
        ActionStep original = new ActionStep((short) 192,
                ItemSelector.inventoryFilter("rare steel hammer (glowing)"),
                TargetSpec.simple(TargetKind.HOVER), "Improve");

        KeybindStepCodec.writeTransfer(properties, "step.", original);
        ActionStep loaded = (ActionStep) KeybindStepCodec.readTransfer(
                properties, "step.");

        assertEquals(original.getSource(), loaded.getSource());
        assertEquals("inventory+filter hammer",
                org.keybinder.wurm.command.ItemSelectorCodec.encode(
                        loaded.getSource()));
    }

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
