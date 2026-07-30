package org.keybinder.wurm.bind;

import org.keybinder.wurm.catalog.InputKeyCatalog;

/**
 * Pure wheel-event gate used by the client hook. One delivered event can
 * resolve and execute at most one exact chord.
 */
public final class WheelInputHandler {
    public interface Environment {
        boolean isOverHudComponent(int x, int y);
        boolean isControlDown();
        boolean isShiftDown();
        boolean isAltDown();
    }

    public interface Dispatcher {
        boolean executeExact(String chord) throws Exception;
    }

    private WheelInputHandler() {}

    public static boolean handle(Environment environment, Dispatcher dispatcher,
                                 int x, int y, int delta) throws Exception {
        if (environment == null || dispatcher == null || delta == 0
                || environment.isOverHudComponent(x, y)) return false;
        String chord = InputKeyCatalog.wheelChord(delta, environment.isControlDown(),
                environment.isShiftDown(), environment.isAltDown());
        return dispatcher.executeExact(chord);
    }
}
