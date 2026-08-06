package org.keybinder.wurm.storage;

import org.junit.Test;
import org.keybinder.wurm.model.ArcheologyIdentifySourceMode;
import org.keybinder.wurm.model.ArcheologyIdentifyStep;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.TargetKind;
import org.keybinder.wurm.model.TargetSpec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class ArcheologyIdentifyStoreTest {
    @Test public void versionedStoreRoundTripsIdentifyStep() throws Exception {
        Path file = Files.createTempDirectory("keybinder-archeology")
                .resolve("keybinds.properties");
        KeybindStore store = new KeybindStore(file);
        KeybindRecord record = new KeybindRecord("archeology", "Identify", "I",
                Collections.<KeybindStep>singletonList(new ArcheologyIdentifyStep(
                        TargetSpec.simple(TargetKind.HOVER),
                        ArcheologyIdentifySourceMode.TOOLBELT_ONLY)));

        store.save(Collections.singletonList(record));
        ArcheologyIdentifyStep loaded = (ArcheologyIdentifyStep) store.load().get(0)
                .getKeybindSteps().get(0);

        assertEquals(TargetKind.HOVER, loaded.getTarget().getKind());
        assertEquals(ArcheologyIdentifySourceMode.TOOLBELT_ONLY,
                loaded.getSourceMode());
    }
}
