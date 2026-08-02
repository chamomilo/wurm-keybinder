package org.keybinder.wurm.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class KeybindDefinitionServiceTest {
    private final KeybindDefinitionService definitions = new KeybindDefinitionService();

    @Test
    public void replacementPreservesIdentityOwnershipAndValuePackProvenance() {
        KeybindRecord existing = record("record", "Old", "R", "say old");
        existing.setOriginalKey("F1");
        existing.setOriginalCommand("EXAMINE");
        existing.setPreviousManagedCommand("old dispatcher");
        existing.setValuePack(true);

        KeybindRecord replacement = definitions.replaceSteps(existing, "New", "T",
                Collections.<KeybindStep>singletonList(new ConsoleCommandStep("say new")),
                "New user", "New server");

        assertEquals(existing.getId(), replacement.getId());
        assertEquals("F1", replacement.getOriginalKey());
        assertEquals("EXAMINE", replacement.getOriginalCommand());
        assertEquals("old dispatcher", replacement.getPreviousManagedCommand());
        assertEquals("New user", replacement.getCreatedByUser());
        assertEquals("New server", replacement.getCreatedOnServer());
        assertTrue(replacement.isValuePack());
    }

    @Test
    public void duplicateGetsIndependentIdentityAndDropsPackProvenance() {
        KeybindRecord source = record("source", "Source", "R", "say source");
        source.setValuePack(true);

        KeybindRecord duplicate = definitions.duplicate(source, "Source copy", "User", "Server");

        assertNotEquals(source.getId(), duplicate.getId());
        assertNotEquals(source.getVariants().get(0).getId(), duplicate.getVariants().get(0).getId());
        assertFalse(duplicate.isEnabled());
        assertFalse(duplicate.isValuePack());
        assertEquals("User", duplicate.getCreatedByUser());
    }

    @Test
    public void extractionBuildsParentAndStandaloneWithoutRuntimeMutation() {
        KeybindVariant first = variant("first", "Default", "say first");
        KeybindVariant second = variant("second", "Second", "say second");
        KeybindRecord source = new KeybindRecord("source", "Multiple", "R",
                Arrays.asList(first, second), second.getId());
        source.setHudMulti(true);
        source.setValuePack(true);

        KeybindDefinitionService.Extraction extraction = definitions.extract(
                source, source.getName(), source.getKey(), source.getVariants(),
                source.getActiveVariantId(), source.isHudMulti(), second.getId(),
                "Creator", "Origin", "Current", "Current server", null);

        assertEquals(2, source.getVariants().size());
        assertEquals(1, extraction.getParent().getVariants().size());
        assertTrue(extraction.getParent().isValuePack());
        assertFalse(extraction.getExtracted().isValuePack());
        assertNotEquals(second.getId(), extraction.getExtracted().getVariants().get(0).getId());
        assertEquals("Current", extraction.getExtracted().getCreatedByUser());
    }

    private static KeybindRecord record(String id, String name, String key, String command) {
        return new KeybindRecord(id, name, key,
                Collections.<KeybindStep>singletonList(new ConsoleCommandStep(command)));
    }

    private static KeybindVariant variant(String id, String name, String command) {
        return new KeybindVariant(id, name,
                Collections.<KeybindStep>singletonList(new ConsoleCommandStep(command)));
    }
}
