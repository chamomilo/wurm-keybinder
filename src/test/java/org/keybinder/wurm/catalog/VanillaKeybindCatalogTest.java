package org.keybinder.wurm.catalog;

import com.wurmonline.client.console.ActionClass;
import com.wurmonline.client.options.keybinding.PlayerKeybind;
import com.wurmonline.client.options.keybinding.PlayerKeybindCategory;
import org.junit.Test;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.junit.Assert.*;

public class VanillaKeybindCatalogTest {
    @Test
    public void includesEveryVisibleVanillaEntryInClientOrder() {
        VanillaKeybindCatalog catalog = new VanillaKeybindCatalog();
        int expected = 0;
        Set<String> visibleCategories = new HashSet<String>();
        for (PlayerKeybindCategory category : PlayerKeybindCategory.values())
            if (category.getIsVisible()) visibleCategories.add(category.name());
        for (PlayerKeybind keybind : PlayerKeybind.values())
            if (visibleCategories.contains(keybind.getCategory().name())
                    && keybind.getCommand() != null && !keybind.getCommand().trim().isEmpty())
                expected++;

        int actual = 0;
        for (VanillaKeybindCatalog.Category category : catalog.getCategories()) {
            assertTrue(visibleCategories.contains(category.getId()));
            actual += category.getEntries().size();
            for (VanillaKeybindCatalog.Entry entry : category.getEntries()) {
                assertSame(entry, catalog.find(entry.getCommand()));
                String command = entry.getCommand();
                if (!(command.startsWith("\"") && command.endsWith("\"")))
                    assertNotNull(ActionClass.valueOf(command.toUpperCase(Locale.ENGLISH)));
            }
        }
        assertEquals(expected, actual);
    }

    @Test
    public void containsKnownHudAndAllToolbeltSlots() {
        VanillaKeybindCatalog catalog = new VanillaKeybindCatalog();
        assertEquals("Main menu", catalog.find("MAIN_MENU").getDisplayName());
        for (int slot = 1; slot <= 10; slot++)
            assertNotNull(catalog.find("ACTIVATE_TOOL" + slot));
    }
}
