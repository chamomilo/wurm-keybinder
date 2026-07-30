package org.keybinder.wurm.i18n;

import org.junit.Test;

import static org.junit.Assert.*;

public class ExistingWindowRelocalizerTest {
    @Test
    public void switchReusesWindowAndPreservesUiState() {
        FakeWindow window = new FakeWindow();
        FakeWindow identity = window;
        final int[] activations = {0};

        assertTrue(ExistingWindowRelocalizer.apply(
                "en", "pt-BR", () -> activations[0]++, window::relocalize));

        assertSame(identity, window);
        assertEquals(1, activations[0]);
        assertEquals(1, window.relocalizations);
        assertEquals(720, window.width);
        assertEquals(460, window.height);
        assertEquals(120, window.x);
        assertEquals(80, window.y);
        assertTrue(window.open);
        assertEquals("keybinds", window.selectedPage);
        assertTrue(window.skipIntro);
        assertEquals("unsaved command", window.unsavedEditorText);
    }

    @Test
    public void repeatedSelectionDoesNotRelocalize() {
        final int[] calls = {0};
        assertFalse(ExistingWindowRelocalizer.apply(
                "pt-BR", "pt-BR", () -> calls[0]++, () -> calls[0]++));
        assertEquals(0, calls[0]);
    }

    private static final class FakeWindow {
        private int width = 720;
        private int height = 460;
        private int x = 120;
        private int y = 80;
        private boolean open = true;
        private String selectedPage = "keybinds";
        private boolean skipIntro = true;
        private String unsavedEditorText = "unsaved command";
        private int relocalizations;

        private void relocalize() {
            relocalizations++;
        }
    }
}
