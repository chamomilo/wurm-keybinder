package org.keybinder.wurm.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class KeybindConflictTest {
    @Test public void retainsManagedBindOrigin() {
        KeybindConflict conflict = new KeybindConflict(
                "E", "WoA on E", "keybinder_run 123", "Chamomilo", "Sklotopolis - Novus");
        assertEquals("Chamomilo", conflict.getCreatedByUser());
        assertEquals("Sklotopolis - Novus", conflict.getCreatedOnServer());
    }
}
