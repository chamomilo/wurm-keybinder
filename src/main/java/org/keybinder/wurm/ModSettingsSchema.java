package org.keybinder.wurm;

import org.chamomilo.wurm.update.ModSetting;
import java.util.Arrays;
import java.util.List;

/** Product metadata for the shared native Settings editor. */
final class ModSettingsSchema {
    private ModSettingsSchema() { }
    static List<ModSetting> specs() {
        return Arrays.asList(
            ModSetting.bool("General", "centerViewAfterEmbark", "Center view after embark", "Turn the view to the vehicle heading when you take control.", true, true),
            ModSetting.choice("General", "queueMonitorSide", "Action queue position", "Choose the screen edge used for the live action queue.", "right", true, "left", "right"),
            ModSetting.text("Data storage", "dataFile", "Keybind file", "File storing keybind definitions. The new file is loaded on the next client start.", "mods/keybinder/keybinds.properties", false)
        );
    }
}
