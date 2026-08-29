package org.keybinder.wurm.i18n;

import org.junit.After;
import org.junit.Test;
import org.keybinder.wurm.KeybindRegistry;
import org.keybinder.wurm.bind.VanillaBindService;
import org.keybinder.wurm.command.TargetCodec;
import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.migration.CustomActionsImporter;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.queue.ActionQueueCostCalculator;
import org.keybinder.wurm.storage.KeybindStore;

import java.nio.file.Files;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

public class LocalizationInvariantTest {
    @After public void restoreEnglish() { Messages.select("en"); }

    @Test
    public void commandsTargetsAndActionIdsDoNotChangeWithLanguage() {
        CustomActionsImporter importer = new CustomActionsImporter();
        String command = "act 154 tile_nw | 163 @tb10 | -7 selected";

        Messages.select("en");
        String english = machineSnapshot(importer.importCommand(command));
        assertEquals("154:tile_nw|163:@tb10|-7:selected", english);
        for (Language language : Language.values()) {
            Messages.select(language.getCode());
            assertEquals(language.getCode(), english,
                    machineSnapshot(importer.importCommand(command)));
        }
    }

    @Test
    public void dispatcherCommandAndBindOwnershipMetadataRemainStable() throws Exception {
        KeybindRecord record = KeybindRecord.actionChain("Teste", "CTRL+R",
                java.util.Collections.singletonList(
                        new ActionStep((short) 163, TargetCodec.decode("tool"))));
        record.setOriginalKey("CTRL+R");
        record.setOriginalCommand("act 163 tool");
        KeybindRegistry registry = new KeybindRegistry(
                new KeybindStore(Files.createTempDirectory("locale-invariant")
                        .resolve("records.properties")),
                new VanillaBindService(), new CustomActionsImporter(),
                new ActionQueueCostCalculator(),
                new EventLogger(Logger.getAnonymousLogger()));

        Messages.select("en");
        String dispatcher = registry.dispatcherCommand(record);
        assertEquals("keybinder_run " + record.getId(), dispatcher);
        for (Language language : Language.values()) {
            Messages.select(language.getCode());
            assertEquals(language.getCode(), dispatcher,
                    registry.dispatcherCommand(record));
        }
        assertEquals("CTRL+R", record.getOriginalKey());
        assertEquals("act 163 tool", record.getOriginalCommand());
    }

    private static String machineSnapshot(List<KeybindStep> steps) {
        StringBuilder result = new StringBuilder();
        for (KeybindStep step : steps) {
            ActionStep action = (ActionStep) step;
            if (result.length() > 0) result.append('|');
            result.append(action.getActionId()).append(':')
                    .append(TargetCodec.encode(action.getTarget()));
        }
        return result.toString();
    }
}
