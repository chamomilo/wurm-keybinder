package org.keybinder.wurm.transfer;

import org.junit.Test;
import org.keybinder.wurm.model.ArcheologyIdentifySourceMode;
import org.keybinder.wurm.model.ArcheologyIdentifyStep;
import org.keybinder.wurm.model.KeybindDefinitionCopier;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.TargetSpec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class ArcheologyIdentifyTransferTest {
    @Test public void portableTransferRoundTripsAndFlagsExactRuntimeTarget()
            throws Exception {
        KeybindRecord record = record(ArcheologyIdentifySourceMode.TOOLBELT_ONLY);
        Path file = Files.createTempFile("archeology-identify", ".keybinder");

        new KeybindTransferStore().write(file, Collections.singletonList(record),
                "user", "server", "0.7.1");
        PortableKeybindDefinition portable =
                new KeybindTransferStore().read(file).get(0);
        ArcheologyIdentifyStep imported = (ArcheologyIdentifyStep) portable
                .toRecord("user", "server").getKeybindSteps().get(0);

        assertTrue(portable.hasExactObject());
        assertEquals(123L, imported.getTarget().getObjectId());
        assertEquals(ArcheologyIdentifySourceMode.TOOLBELT_ONLY,
                imported.getSourceMode());
    }

    @Test public void copierPreservesSemanticsAndFingerprintIncludesSourceMode() {
        ArcheologyIdentifyStep copied = (ArcheologyIdentifyStep)
                new KeybindDefinitionCopier().copyStep(
                        record(ArcheologyIdentifySourceMode.TOOLBELT_ONLY)
                                .getKeybindSteps().get(0));

        assertEquals(123L, copied.getTarget().getObjectId());
        assertEquals(ArcheologyIdentifySourceMode.TOOLBELT_ONLY,
                copied.getSourceMode());
        assertNotEquals(SemanticFingerprint.of(PortableKeybindDefinition.fromRecord(
                        record(ArcheologyIdentifySourceMode.TOOLBELT_ONLY))),
                SemanticFingerprint.of(PortableKeybindDefinition.fromRecord(
                        record(ArcheologyIdentifySourceMode.TOOLBELT_THEN_INVENTORY))));
    }

    private static KeybindRecord record(ArcheologyIdentifySourceMode mode) {
        return new KeybindRecord(null, "Identify", "I",
                Collections.<KeybindStep>singletonList(new ArcheologyIdentifyStep(
                        TargetSpec.exactObject(123L, "unidentified statue fragment"),
                        mode)));
    }
}
