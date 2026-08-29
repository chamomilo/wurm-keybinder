package org.keybinder.wurm.catalog;

import org.junit.After;
import org.junit.Test;
import org.keybinder.wurm.i18n.Messages;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class InputKeyCatalogTest {
    @After public void restoreEnglish() { Messages.select("en"); }

    @Test public void normalizesWheelAndMouseButtonKeys() {
        assertEquals("MOUSE_WHEEL_UP", InputKeyCatalog.normalizeChord("mouse_wheel_up"));
        assertEquals("MOUSE_WHEEL_DOWN", InputKeyCatalog.normalizeChord("MOUSE_WHEEL_DOWN"));
        assertEquals("MOUSE2", InputKeyCatalog.normalizeChord("Mouse2"));
        assertEquals("CTRL+SHIFT+ALT+MOUSE_WHEEL_UP",
                InputKeyCatalog.normalizeChord("alt+shift+ctrl+mouse_wheel_up"));
    }

    @Test public void mapsDisplayNamesToPersistedNamesBothWays() {
        InputKeyCatalog catalog = InputKeyCatalog.system();
        assertEquals("Mouse Wheel Up", catalog.displayName("MOUSE_WHEEL_UP"));
        assertEquals("Mouse Wheel Down", catalog.displayName("MOUSE_WHEEL_DOWN"));
        assertEquals("Mouse Wheel Button", catalog.displayName("Mouse2"));
        assertEquals("MOUSE2", catalog.persistedName("Mouse Wheel Button"));
        assertEquals("MOUSE_WHEEL_UP", catalog.persistedName("Mouse Wheel Up"));
    }

    @Test public void runtimeSnapshotHasNoEmptyOrDuplicatePersistedNames() {
        Set<String> names = new HashSet<String>();
        for (InputKeyCatalog.Entry entry : InputKeyCatalog.system().entries()) {
            assertNotNull(entry.getPersistedName());
            assertFalse(entry.getPersistedName().isEmpty());
            assertTrue("duplicate " + entry.getPersistedName(), names.add(entry.getPersistedName()));
        }
        assertEquals(InputKeyCatalog.InputKind.MOUSE_BUTTON,
                InputKeyCatalog.system().findPersisted("MOUSE2").getInputKind());
        assertEquals(InputKeyCatalog.InputKind.MOUSE_WHEEL_DIRECTION,
                InputKeyCatalog.system().findPersisted("MOUSE_WHEEL_UP").getInputKind());
    }

    @Test public void followsTheVanillaKeyPickerOrder() {
        List<InputKeyCatalog.Entry> entries = InputKeyCatalog.system().entries();
        assertTrue(indexOf(entries, "A") < indexOf(entries, "Z"));
        assertTrue(indexOf(entries, "Z") < indexOf(entries, "UP"));
        assertTrue(indexOf(entries, "UP") < indexOf(entries, "NUMPAD1"));
        assertTrue(indexOf(entries, "NUMPAD1") < indexOf(entries, "F1"));
        assertTrue(indexOf(entries, "F1") < indexOf(entries, "1"));
        assertTrue(indexOf(entries, "1") < indexOf(entries, "SPACE"));
        assertTrue(indexOf(entries, "SPACE") < indexOf(entries, "MOUSE2"));
        assertTrue(indexOf(entries, "MOUSE2") < indexOf(entries, "PAUSE"));
        assertTrue(indexOf(entries, "PAUSE") < indexOf(entries, "GRAVE"));
    }

    private static int indexOf(List<InputKeyCatalog.Entry> entries, String persisted) {
        for (int i = 0; i < entries.size(); i++)
            if (persisted.equals(entries.get(i).getPersistedName())) return i;
        fail("Missing key " + persisted);
        return -1;
    }

    @Test public void localizedMouseLabelsDoNotChangePersistedKeys() {
        Messages.select("pt-BR");
        InputKeyCatalog catalog = InputKeyCatalog.system();
        assertEquals("Roda do mouse para cima", catalog.displayName("MOUSE_WHEEL_UP"));
        assertEquals("MOUSE_WHEEL_UP", catalog.persistedName("Roda do mouse para cima"));
        assertEquals("MOUSE2", catalog.persistedName("Botão da roda do mouse"));

        Messages.select("de");
        assertEquals("Mausrad nach oben", catalog.displayName("MOUSE_WHEEL_UP"));
        assertEquals("MOUSE_WHEEL_UP", catalog.persistedName("Mausrad nach oben"));
        assertEquals("MOUSE2", catalog.persistedName("Mausradtaste"));
    }
}
