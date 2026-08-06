package org.keybinder.wurm.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class KeybindRecordTest {
    @Test public void hudExecutionViewSelectsVariantWithoutChangingStoredDefault() {
        KeybindVariant first = new KeybindVariant("first", "First",
                Collections.<KeybindStep>emptyList());
        KeybindVariant second = new KeybindVariant("second", "Second",
                Collections.<KeybindStep>emptyList());
        KeybindRecord stored = new KeybindRecord("record", "(HUD) (Multi) Work", "R",
                Arrays.asList(first, second), first.getId());
        stored.setHudMulti(true);
        stored.setValuePack(true);

        KeybindRecord execution = stored.executionViewForVariant(second.getId());

        assertEquals(first.getId(), stored.getActiveVariantId());
        assertEquals(second.getId(), execution.getActiveVariantId());
        assertEquals("Work-Second", KeybindNamePrefixes.baseName(execution.getDisplayName()));
        assertTrue(execution.isHudMulti());
        assertTrue(execution.isValuePack());
    }

    @Test public void oneVariantCanBeAnObservedHudKeybind() {
        KeybindRecord record = new KeybindRecord("record", "Open journal", "R",
                Collections.<KeybindStep>emptyList());

        record.setHudMulti(true);

        assertFalse(record.isMultiPurpose());
        assertTrue(record.isHudMulti());
        assertTrue(record.isSelectorKeybind());
    }
}
