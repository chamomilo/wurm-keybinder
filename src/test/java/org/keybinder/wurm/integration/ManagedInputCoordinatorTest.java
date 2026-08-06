package org.keybinder.wurm.integration;

import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import org.junit.Test;
import org.keybinder.wurm.model.KeybindRecord;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ManagedInputCoordinatorTest {
    @Test public void triggerKeyDismissalIsSuppressedOnce() {
        StubEnvironment environment = new StubEnvironment();
        ManagedInputCoordinator coordinator = new ManagedInputCoordinator(environment);

        coordinator.openSelectorSession(42);
        coordinator.observeKeyReleased(42);
        coordinator.observeKeyPressed(42);

        assertTrue(environment.selectorClosed);
        assertTrue(coordinator.handleKeyToggle(null, 42, true));
        assertFalse(coordinator.handleKeyToggle(null, 42, true));
    }

    @Test public void outsidePointerPressClosesArmedSelector() {
        ManagedInputCoordinator coordinator =
                new ManagedInputCoordinator(new StubEnvironment());
        coordinator.openSelectorSession(-1);

        assertTrue(coordinator.pointerPressed(0, null));
    }

    private static final class StubEnvironment
            implements ManagedInputCoordinator.Environment {
        private boolean selectorClosed;

        @Override public HeadsUpDisplay hud() { return null; }
        @Override public KeybindRecord findEnabledByChord(String chord) { return null; }
        @Override public KeybindRecord findById(String id) { return null; }
        @Override public void execute(KeybindRecord record, HeadsUpDisplay hud) {}
        @Override public void openSelector(KeybindRecord record,
                                           boolean hudSelection, int triggerKey) {}
        @Override public void closeSelector() { selectorClosed = true; }
        @Override public void warning(String message, Throwable failure) {}
        @Override public void fine(String message, Throwable failure) {}
        @Override public void reportWheelFailure(String failure, Throwable cause) {}
        @Override public long nanoTime() { return 0L; }
        @Override public long currentTimeMillis() { return 0L; }
    }
}
