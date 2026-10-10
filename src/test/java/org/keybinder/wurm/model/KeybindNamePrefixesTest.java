package org.keybinder.wurm.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class KeybindNamePrefixesTest {
    @Test public void derivesPrefixesFromVariantCountAndHudModeWithoutDuplicates() {
        assertEquals("Forge", KeybindNamePrefixes.apply("Forge", 1, false));
        assertEquals("(Multi) Forge", KeybindNamePrefixes.apply("Forge", 2, false));
        assertEquals("(Quick) Forge",
                KeybindNamePrefixes.apply("Multi Forge", 2, true));
        assertEquals("(Multi) Forge",
                KeybindNamePrefixes.apply("HUD Multi Forge", 3, false));
        assertEquals("(Quick) Forge", KeybindNamePrefixes.apply("HUD Multi Forge", 1, true));
        assertEquals("Forge", KeybindNamePrefixes.baseName("(HUD) (Multi) Forge"));
        assertEquals("(Quick) Forge", KeybindNamePrefixes.apply("(HUD) (Multi) Forge", 2, true));
        assertEquals("(Quick) Forge", KeybindNamePrefixes.apply("(Quick) Forge", 2, true));
        assertEquals("(Multi) Forge", KeybindNamePrefixes.apply("(Quick) Forge", 2, false));
        assertEquals("Forge", KeybindNamePrefixes.apply("(Quick) Forge", 1, false));
    }

    @Test public void keepsGeneratedNameInsideRecordLimit() {
        StringBuilder base = new StringBuilder();
        for (int i = 0; i < 100; i++) base.append('x');
        String result = KeybindNamePrefixes.apply(base.toString(), 2, true);
        assertEquals(KeybindLimits.MAX_RECORD_NAME_LENGTH, result.length());
        assertTrue(result.startsWith("(Quick) "));
    }
}
