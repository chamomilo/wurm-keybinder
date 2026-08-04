package org.keybinder.wurm.transfer;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.ActivateToolStep;
import org.keybinder.wurm.model.ConsoleCommandStep;
import org.keybinder.wurm.model.ItemSelector;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.KeybindVariant;
import org.keybinder.wurm.model.SmartImproveStep;
import org.keybinder.wurm.model.TargetKind;
import org.keybinder.wurm.model.TargetSpec;
import org.keybinder.wurm.model.VanillaActionStep;
import org.keybinder.wurm.model.BulkDestinationKind;
import org.keybinder.wurm.model.BulkStorageItem;
import org.keybinder.wurm.model.BulkTransferStep;
import org.keybinder.wurm.model.InventoryReference;

public class KeybindTransferStoreTest {
    @Rule public final TemporaryFolder temporary = new TemporaryFolder();

    @Test public void roundTripsUnicodeMixedStepsSelectorsAndActiveIndex() throws Exception {
        List<KeybindStep> first = Arrays.<KeybindStep>asList(
                action(30000, ItemSelector.currentActive(), TargetSpec.simple(TargetKind.HOVER)),
                action(30001, ItemSelector.emptyHand(), TargetSpec.simple(TargetKind.BODY)),
                action(30002, ItemSelector.hoveredItem(), TargetSpec.hoverType("iron pickaxe")),
                action(30003, ItemSelector.toolbeltSlot(10), TargetSpec.nearbyType("oak chest")),
                action(30004, ItemSelector.equipmentSlot(7), TargetSpec.nearbyRadius(4.5f)),
                action(30005, ItemSelector.exactObject(123L, "picareta rara"),
                        TargetSpec.exactObject(456L, "árvore")),
                new ActivateToolStep(TargetSpec.toolbeltSlot(2)),
                new SmartImproveStep(TargetSpec.simple(TargetKind.SELECTED)),
                new VanillaActionStep("CLIMB"),
                new ConsoleCommandStep("say olá", true));
        KeybindVariant a = new KeybindVariant("variant-a", "Padrão", first);
        KeybindVariant b = new KeybindVariant("variant-b", "Alternativa",
                Collections.<KeybindStep>emptyList());
        KeybindRecord record = new KeybindRecord("runtime-id", "Ação portátil", "CTRL+P",
                Arrays.asList(a, b), b.getId());
        record.setHudMulti(true);
        record.setEnabled(false);
        record.setOriginalCommand("must not transfer");

        Path file = temporary.newFile("all.keybinder").toPath();
        new KeybindTransferStore().write(file, Collections.singletonList(record),
                "user", "server", "0.7.0");
        List<PortableKeybindDefinition> loaded = new KeybindTransferStore().read(file);

        assertEquals(1, loaded.size());
        PortableKeybindDefinition definition = loaded.get(0);
        assertEquals("Ação portátil", definition.getName());
        assertEquals(1, definition.getActiveVariantIndex());
        assertTrue(definition.isHudMulti());
        assertTrue(definition.hasExactObject());
        assertEquals(SemanticFingerprint.of(PortableKeybindDefinition.fromRecord(record)),
                SemanticFingerprint.of(definition));
        KeybindRecord imported = definition.toRecord("new", "new server");
        assertNotEquals(record.getId(), imported.getId());
        assertNotEquals(record.getVariants().get(0).getId(), imported.getVariants().get(0).getId());
        assertEquals("", imported.getOriginalCommand());
    }

    @Test public void fingerprintIsDeterministicAndSensitiveToSource() {
        PortableKeybindDefinition first = definition(ItemSelector.emptyHand());
        PortableKeybindDefinition same = definition(ItemSelector.emptyHand());
        PortableKeybindDefinition changed = definition(ItemSelector.currentActive());
        assertEquals(SemanticFingerprint.of(first), SemanticFingerprint.of(same));
        assertNotEquals(SemanticFingerprint.of(first), SemanticFingerprint.of(changed));
    }

    @Test public void rejectsMalformedBase64AndUnknownVersion() throws Exception {
        Path malformed = temporary.newFile("bad.keybinder").toPath();
        Files.write(malformed, Arrays.asList("format=keybinder-transfer", "version=1",
                "definitionSchema=8", "count=1", "record.0.name=%%%"),
                StandardCharsets.ISO_8859_1);
        assertReadFails(malformed);

        Path version = temporary.newFile("version.keybinder").toPath();
        Files.write(version, Arrays.asList("format=keybinder-transfer", "version=2",
                "definitionSchema=8", "count=0"), StandardCharsets.ISO_8859_1);
        assertReadFails(version);
    }

    @Test public void roundTripsServerBoundBulkTransferAsExactObjectDefinition()
            throws Exception {
        BulkTransferStep bulk = new BulkTransferStep(new BulkStorageItem(
                new InventoryReference(101L, "bulk storage bin"),
                new InventoryReference(202L, "barley (100x)")), 43,
                BulkDestinationKind.CAPTURED_INVENTORY,
                new InventoryReference(303L, "small barrel"));
        KeybindRecord record = new KeybindRecord("bulk", "Bulk", "B",
                Collections.<KeybindStep>singletonList(bulk));
        Path file = temporary.newFile("bulk.keybinder").toPath();

        new KeybindTransferStore().write(file, Collections.singletonList(record),
                "user", "server", "0.7.0");
        PortableKeybindDefinition definition =
                new KeybindTransferStore().read(file).get(0);
        BulkTransferStep loaded = (BulkTransferStep)
                definition.getVariants().get(0).getSteps().get(0);

        assertTrue(definition.hasExactObject());
        assertEquals(202L, loaded.getSource().getItem().getId());
        assertEquals(43, loaded.getQuantity());
        assertEquals(303L, loaded.getCapturedDestination().getId());
    }

    private static ActionStep action(int id, ItemSelector source, TargetSpec target) {
        return new ActionStep((short) id, source, target, "Ação " + id);
    }

    private static PortableKeybindDefinition definition(ItemSelector source) {
        KeybindRecord record = new KeybindRecord(null, "name", "R",
                Collections.<KeybindStep>singletonList(action(30000, source,
                        TargetSpec.simple(TargetKind.HOVER))));
        return PortableKeybindDefinition.fromRecord(record);
    }

    private static void assertReadFails(Path file) {
        try {
            new KeybindTransferStore().read(file);
            fail("Expected malformed transfer to fail");
        } catch (IOException expected) { }
    }
}
