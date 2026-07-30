package org.keybinder.wurm.bind;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class WheelInputHandlerTest {
    @Test public void mapsSignsAndRunsOnceRegardlessOfMagnitude() throws Exception {
        Environment environment = new Environment(false, false, false, false);
        RecordingDispatcher dispatcher = new RecordingDispatcher();
        assertTrue(WheelInputHandler.handle(environment, dispatcher, 1, 2, -3));
        assertTrue(WheelInputHandler.handle(environment, dispatcher, 1, 2, 3));
        assertEquals(2, dispatcher.chords.size());
        assertEquals("MOUSE_WHEEL_UP", dispatcher.chords.get(0));
        assertEquals("MOUSE_WHEEL_DOWN", dispatcher.chords.get(1));
    }

    @Test public void zeroAndHudComponentDoNotRun() throws Exception {
        RecordingDispatcher dispatcher = new RecordingDispatcher();
        assertFalse(WheelInputHandler.handle(
                new Environment(false, false, false, false), dispatcher, 1, 2, 0));
        assertFalse(WheelInputHandler.handle(
                new Environment(true, false, false, false), dispatcher, 1, 2, -1));
        assertTrue(dispatcher.chords.isEmpty());
    }

    @Test public void usesExactCanonicalModifierCombinationWithoutFallback() throws Exception {
        RecordingDispatcher dispatcher = new RecordingDispatcher();
        WheelInputHandler.handle(new Environment(false, true, true, true),
                dispatcher, 1, 2, -1);
        assertEquals(1, dispatcher.chords.size());
        assertEquals("CTRL+SHIFT+ALT+MOUSE_WHEEL_UP", dispatcher.chords.get(0));
    }

    @Test(expected = Exception.class)
    public void exposesDispatcherFailureToFailOpenHookBoundary() throws Exception {
        WheelInputHandler.handle(new Environment(false, false, false, false),
                new WheelInputHandler.Dispatcher() {
                    @Override public boolean executeExact(String chord) throws Exception {
                        throw new Exception("boom");
                    }
                }, 1, 2, -1);
    }

    private static final class Environment implements WheelInputHandler.Environment {
        private final boolean component, ctrl, shift, alt;
        private Environment(boolean component, boolean ctrl, boolean shift, boolean alt) {
            this.component = component; this.ctrl = ctrl; this.shift = shift; this.alt = alt;
        }
        @Override public boolean isOverHudComponent(int x, int y) { return component; }
        @Override public boolean isControlDown() { return ctrl; }
        @Override public boolean isShiftDown() { return shift; }
        @Override public boolean isAltDown() { return alt; }
    }

    private static final class RecordingDispatcher implements WheelInputHandler.Dispatcher {
        private final List<String> chords = new ArrayList<String>();
        @Override public boolean executeExact(String chord) {
            chords.add(chord);
            return true;
        }
    }
}
