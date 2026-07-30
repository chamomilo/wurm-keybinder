package org.keybinder.wurm.catalog;

import com.wurmonline.client.console.ActionClass;
import com.wurmonline.client.options.keybinding.PlayerKeybind;
import com.wurmonline.client.options.keybinding.PlayerKeybindCategory;
import com.wurmonline.shared.constants.PlayerAction;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    @Test
    public void matchesGameplayCommandToCurrentPlayerActionBindWithoutDisplayingId() {
        VanillaKeybindCatalog.Entry examine = new VanillaKeybindCatalog().find("EXAMINE");

        assertNotNull(examine);
        assertEquals(Short.valueOf(PlayerAction.EXAMINE.getId()), examine.getActionId());
        assertEquals(PlayerKeybind.EXAMINE.getDisplayName(), examine.getDisplayName());
        assertFalse(examine.getDisplayName().contains(
                "(" + PlayerAction.EXAMINE.getId() + ")"));
    }

    @Test
    public void followsClientCommandAliasesWithoutCopiedNumericIds() {
        VanillaKeybindCatalog catalog = new VanillaKeybindCatalog();

        assertAlias(catalog, PlayerKeybind.PLANT_SIGN, PlayerAction.PLANT_SIGN);
        assertAlias(catalog, PlayerKeybind.SIT, PlayerAction.SIT_ANY);
        assertAlias(catalog, PlayerKeybind.PICK_FLOWERS, PlayerAction.PICK_SPROUT);
        assertAlias(catalog, PlayerKeybind.MINE_SURFACE, PlayerAction.MINE_FORWARD);
        assertAlias(catalog, PlayerKeybind.FUNGUS_SPELL, PlayerAction.FUNGUS);
    }

    @Test
    public void activateIsMarkedAsExistingLocalActivationInsteadOfInventingActionId() {
        VanillaKeybindCatalog.Entry activate =
                new VanillaKeybindCatalog().find(PlayerKeybind.ACTIVATE.getCommand());

        assertNotNull(activate);
        assertTrue(activate.isActivateTool());
        assertNull(activate.getActionId());
    }

    @Test
    public void unknownAndAmbiguousBindingsAreNotResolved() {
        Map<String, List<Short>> candidates = new LinkedHashMap<String, List<Short>>();
        candidates.put("EXAMINE", Arrays.asList((short) 1, (short) 2));
        candidates.put("OPEN", Arrays.asList((short) 3, (short) 3));

        Map<String, Short> resolved = VanillaKeybindCatalog.uniqueActionIds(candidates);

        assertFalse(resolved.containsKey("EXAMINE"));
        assertEquals(Short.valueOf((short) 3), resolved.get("OPEN"));
        assertNull(new VanillaKeybindCatalog(Collections.<String, Short>emptyMap())
                .find("EXAMINE").getActionId());
    }

    private static void assertAlias(VanillaKeybindCatalog catalog,
                                    PlayerKeybind keybind, PlayerAction action) {
        VanillaKeybindCatalog.Entry entry = catalog.find(keybind.getCommand());
        assertNotNull(entry);
        assertEquals(Short.valueOf(action.getId()), entry.getActionId());
    }
}
