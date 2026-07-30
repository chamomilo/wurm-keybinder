package org.keybinder.wurm.catalog;

import org.junit.After;
import org.junit.Test;
import org.keybinder.wurm.i18n.Messages;

import java.util.HashSet;
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

    @Test public void localizedMouseLabelsDoNotChangePersistedKeys() {
        Messages.select("pt-BR");
        InputKeyCatalog catalog = InputKeyCatalog.system();
        assertEquals("Roda do mouse para cima", catalog.displayName("MOUSE_WHEEL_UP"));
        assertEquals("MOUSE_WHEEL_UP", catalog.persistedName("Roda do mouse para cima"));
        assertEquals("MOUSE2", catalog.persistedName("Botão da roda do mouse"));
    }
}
