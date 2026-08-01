package org.keybinder.wurm;

import static org.junit.Assert.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.junit.Test;
import org.keybinder.wurm.bind.VanillaBindService;
import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.i18n.DisableReason;
import org.keybinder.wurm.migration.CustomActionsImporter;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.ConsoleCommandStep;
import org.keybinder.wurm.model.ItemSelector;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.KeybindVariant;
import org.keybinder.wurm.model.TargetKind;
import org.keybinder.wurm.model.TargetSpec;
import org.keybinder.wurm.queue.ActionQueueCostCalculator;
import org.keybinder.wurm.storage.KeybindStore;
import org.keybinder.wurm.transfer.PortableKeybindDefinition;
import org.keybinder.wurm.transfer.TransferImportResult;

public class KeybindRegistryDefinitionMutationTest {
    @Test public void duplicateUsesNewIdsPreservesDefinitionAndClearsOwnership() throws Exception {
        KeybindRegistry registry = registry();
        KeybindVariant first = new KeybindVariant("one", "One",
                Collections.<KeybindStep>singletonList(new ActionStep((short) 7,
                        ItemSelector.toolbeltSlot(2), TargetSpec.hoverType("pickaxe"), "Use")));
        KeybindVariant second = new KeybindVariant("two", "Two",
                Collections.<KeybindStep>singletonList(new ConsoleCommandStep("say two", true)));
        KeybindRecord source = new KeybindRecord("source", "Original", "MOUSE_WHEEL_UP",
                Arrays.asList(first, second), "two");
        source.setHudMulti(true);
        source.setOriginalKey("F1");
        source.setOriginalCommand("EXAMINE");
        source.setPreviousManagedCommand("old");
        registry.add(source, null, 10);

        KeybindRecord copy = registry.duplicate(source.getId());
        List<KeybindRecord> records = registry.snapshot();
        assertSame(copy, records.get(1));
        assertNotEquals(source.getId(), copy.getId());
        assertNotEquals(source.getVariants().get(0).getId(), copy.getVariants().get(0).getId());
        assertEquals(1, copy.getVariants().indexOf(copy.getActiveVariant()));
        assertTrue(copy.isHudMulti());
        assertEquals(ItemSelector.toolbeltSlot(2),
                ((ActionStep) copy.getVariants().get(0).getSteps().get(0)).getSource());
        assertFalse(copy.isEnabled());
        assertTrue(DisableReason.display(copy.getDisabledReason()).contains("free key"));
        assertEquals("", copy.getOriginalKey());
        assertEquals("", copy.getOriginalCommand());
        assertEquals("", copy.getPreviousManagedCommand());
    }

    @Test public void extractRemovesCurrentVariantAndInsertsDisabledStandaloneAfterParent()
            throws Exception {
        KeybindRegistry registry = registry();
        KeybindVariant first = variant("first", "Default choice", "first");
        KeybindVariant extractedVariant = variant("second", "Smelt", "second");
        KeybindRecord source = new KeybindRecord("source", "Metal work",
                "MOUSE_WHEEL_UP", Arrays.asList(first, extractedVariant), "second");
        source.setHudMulti(true);
        source.setOriginalKey("F4");
        source.setOriginalCommand("EXAMINE");
        registry.add(source, null, 20);
        KeybindRecord tail = new KeybindRecord("tail", "Tail", "MOUSE_WHEEL_DOWN",
                Collections.<KeybindStep>singletonList(new ConsoleCommandStep("say tail")));
        registry.add(tail, null, 20);

        KeybindRecord extracted = registry.extractVariant(source.getId(), source.getName(),
                source.getKey(), source.getVariants(), source.getActiveVariantId(),
                source.isHudMulti(), extractedVariant.getId(), source.getCreatedByUser(),
                source.getCreatedOnServer(), null, null, 20);

        List<KeybindRecord> records = registry.snapshot();
        KeybindRecord parent = records.get(0);
        assertSame(extracted, records.get(1));
        assertSame(tail, records.get(2));
        assertEquals(1, parent.getVariants().size());
        assertEquals("first", parent.getVariants().get(0).getId());
        assertEquals("first", parent.getActiveVariantId());
        assertFalse(parent.isMultiPurpose());
        assertFalse(parent.isHudMulti());
        assertEquals("Metal work", parent.getName());
        assertEquals("F4", parent.getOriginalKey());
        assertEquals("Smelt extracted from Metal work", extracted.getName());
        assertEquals(source.getKey(), extracted.getKey());
        assertFalse(extracted.isEnabled());
        assertTrue(DisableReason.display(extracted.getDisabledReason()).contains("shares key"));
        assertEquals(1, extracted.getVariants().size());
        assertEquals("Smelt", extracted.getVariants().get(0).getSubName());
        assertNotEquals(extractedVariant.getId(), extracted.getVariants().get(0).getId());
        assertEquals("say second", ((ConsoleCommandStep) extracted.getKeybindSteps().get(0))
                .getCommand());
        assertEquals("", extracted.getOriginalKey());
        assertEquals("User", extracted.getCreatedByUser());
        assertEquals("Server", extracted.getCreatedOnServer());
    }

    @Test public void disablingBlockerClearsStaleConflictStatusWithoutEnablingDisplacedRecord()
            throws Exception {
        KeybindRegistry registry = registry();
        KeybindRecord displaced = new KeybindRecord("old", "Old", "MOUSE_WHEEL_UP",
                Collections.<KeybindStep>singletonList(new ConsoleCommandStep("say old")));
        KeybindRecord blocker = new KeybindRecord("new", "New", "MOUSE_WHEEL_UP",
                Collections.<KeybindStep>singletonList(new ConsoleCommandStep("say new")));
        registry.add(displaced, null, 10);
        registry.add(blocker, null, 10);
        assertFalse(displaced.isEnabled());
        assertTrue(DisableReason.blocksEnable(displaced.getDisabledReason()));

        registry.setEnabled(blocker.getId(), false, null, 10);

        assertFalse(displaced.isEnabled());
        assertFalse(DisableReason.blocksEnable(displaced.getDisabledReason()));
        assertEquals("disabled by user", DisableReason.display(displaced.getDisabledReason()));
    }

    @Test public void mergeAppendsDeepCopiedVariantsAndKeepsDestinationModeAndActive() throws Exception {
        KeybindRegistry registry = registry();
        KeybindVariant d1 = variant("d1", "First", "destination");
        KeybindVariant d2 = variant("d2", "Active", "destination active");
        KeybindRecord destination = new KeybindRecord("destination", "Destination",
                "MOUSE_WHEEL_UP", Arrays.asList(d1, d2), "d2");
        destination.setHudMulti(true);
        KeybindRecord source = new KeybindRecord("source", "Source", "MOUSE_WHEEL_DOWN",
                Arrays.asList(variant("s1", "", "source one"),
                        variant("s2", "First", "source two")), "s1");
        registry.add(destination, null, 10);
        registry.add(source, null, 10);

        KeybindRecord merged = registry.merge("source", "destination", null);
        assertEquals(4, merged.getVariants().size());
        assertEquals("d2", merged.getActiveVariantId());
        assertTrue(merged.isHudMulti());
        assertEquals("Source", merged.getVariants().get(2).getSubName());
        assertEquals("Source — First", merged.getVariants().get(3).getSubName());
        assertNull(registry.find("source"));
    }

    @Test public void portableImportSkipsDuplicatesAndMarksExactObjectsForReview() throws Exception {
        KeybindRegistry registry = registry();
        KeybindRecord exact = new KeybindRecord(null, "Exact", "E",
                Collections.<KeybindStep>singletonList(new ActionStep((short) 1,
                        ItemSelector.exactObject(99L, "hammer"),
                        TargetSpec.simple(TargetKind.HOVER), "Use")));
        PortableKeybindDefinition definition = PortableKeybindDefinition.fromRecord(exact);

        TransferImportResult result = registry.importPortable(Arrays.asList(definition, definition));
        assertEquals(1, result.getImported());
        assertEquals(1, result.getSkippedDuplicates());
        KeybindRecord imported = registry.snapshot().get(0);
        assertFalse(imported.isEnabled());
        assertTrue(DisableReason.display(imported.getDisabledReason()).contains("exact object ID"));
    }

    @Test public void mergeAcceptsFourteenAndFifteenButRejectsSixteenAtomically()
            throws Exception {
        for (int destinationCount : new int[] {7, 8}) {
            KeybindRegistry registry = registry();
            registry.add(multi("destination", "MOUSE_WHEEL_UP", destinationCount), null, 50);
            registry.add(multi("source", "MOUSE_WHEEL_DOWN", 7), null, 50);
            assertEquals(destinationCount + 7,
                    registry.merge("source", "destination", null).getVariants().size());
        }

        KeybindRegistry registry = registry();
        registry.add(multi("destination", "MOUSE_WHEEL_UP", 8), null, 50);
        registry.add(multi("source", "MOUSE_WHEEL_DOWN", 8), null, 50);
        try {
            registry.merge("source", "destination", null);
            fail("Expected 16 alternatives to be rejected");
        } catch (IllegalArgumentException expected) { }
        assertEquals(2, registry.snapshot().size());
        assertEquals(8, registry.find("destination").getVariants().size());
        assertEquals(8, registry.find("source").getVariants().size());
    }

    private static KeybindVariant variant(String id, String name, String command) {
        return new KeybindVariant(id, name,
                Collections.<KeybindStep>singletonList(new ConsoleCommandStep("say " + command)));
    }

    private static KeybindRecord multi(String id, String key, int count) {
        java.util.ArrayList<KeybindVariant> variants = new java.util.ArrayList<KeybindVariant>();
        for (int i = 0; i < count; i++)
            variants.add(variant(id + "-" + i, "Choice " + i, id + " " + i));
        return new KeybindRecord(id, id, key, variants, variants.get(0).getId());
    }

    private static KeybindRegistry registry() throws Exception {
        KeybindRegistry registry = new KeybindRegistry(new KeybindStore(
                Files.createTempDirectory("definition-registry").resolve("records.properties")),
                new VanillaBindService(), new CustomActionsImporter(),
                new ActionQueueCostCalculator(), new EventLogger(Logger.getAnonymousLogger()));
        registry.setCreationContext("User", "Server");
        return registry;
    }
}
