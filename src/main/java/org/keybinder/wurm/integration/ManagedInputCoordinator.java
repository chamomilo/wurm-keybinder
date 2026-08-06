package org.keybinder.wurm.integration;

import com.wurmonline.client.console.KeyBinding;
import com.wurmonline.client.console.WurmConsole;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import org.keybinder.wurm.bind.MultiKeyController;
import org.keybinder.wurm.bind.SelectorSessionController;
import org.keybinder.wurm.bind.WheelInputHandler;
import org.keybinder.wurm.model.KeybindRecord;

import java.util.Locale;

/** Owns the transient input state shared by wheel and multi-purpose keybinds. */
public final class ManagedInputCoordinator {
    public interface Environment {
        HeadsUpDisplay hud();
        KeybindRecord findEnabledByChord(String chord);
        KeybindRecord findById(String id);
        void execute(KeybindRecord record, HeadsUpDisplay hud) throws Exception;
        void openSelector(KeybindRecord record, boolean hudSelection, int triggerKey);
        void closeSelector();
        void warning(String message, Throwable failure);
        void fine(String message, Throwable failure);
        void reportWheelFailure(String failure, Throwable cause);
        long nanoTime();
        long currentTimeMillis();
    }

    private static final String RUN_PREFIX = "keybinder_run ";
    private static final long LONG_PRESS_NANOS = 200_000_000L;
    private static final long FAILURE_REPORT_INTERVAL_MILLIS = 30_000L;

    private final Environment environment;
    private final MultiKeyController multiKey = new MultiKeyController();
    private final SelectorSessionController selectorSession =
            new SelectorSessionController();
    private final ThreadLocal<Integer> suppressedSelectorKey =
            new ThreadLocal<Integer>();
    private long lastWheelFailureAt;
    private String lastWheelFailure = "";

    public ManagedInputCoordinator(Environment environment) {
        if (environment == null) throw new IllegalArgumentException("environment");
        this.environment = environment;
    }

    public void handleMouseWheel(final int x, final int y, final int delta) {
        try {
            if (selectorSession.otherPointerAction()) {
                environment.closeSelector();
                return;
            }
            final HeadsUpDisplay currentHud = environment.hud();
            WheelInputHandler.handle(new WheelInputHandler.Environment() {
                @Override public boolean isOverHudComponent(int px, int py) {
                    if (currentHud == null) return true;
                    try {
                        return currentHud.getComponentAt(px, py) != null;
                    } catch (Throwable failure) {
                        environment.fine("Mouse wheel HUD hit-test failed open", failure);
                        return true;
                    }
                }

                @Override public boolean isControlDown() { return currentHud.isControlDown(); }
                @Override public boolean isShiftDown() { return currentHud.isShiftDown(); }
                @Override public boolean isAltDown() { return currentHud.isAltDown(); }
            }, new WheelInputHandler.Dispatcher() {
                @Override public boolean executeExact(String chord) throws Exception {
                    KeybindRecord record = environment.findEnabledByChord(chord);
                    if (record == null) return false;
                    if (record.isHudMulti()) environment.openSelector(record, true, -1);
                    else environment.execute(record, currentHud);
                    return true;
                }
            }, x, y, delta);
        } catch (Throwable failure) {
            String description = failure.getClass().getName() + ": "
                    + String.valueOf(failure.getMessage());
            environment.warning(
                    "Mouse wheel keybind hook failed open (" + description + ")", failure);
            long now = environment.currentTimeMillis();
            if (!description.equals(lastWheelFailure)
                    || now - lastWheelFailureAt >= FAILURE_REPORT_INTERVAL_MILLIS) {
                lastWheelFailure = description;
                lastWheelFailureAt = now;
                environment.reportWheelFailure(description, failure);
            }
        }
    }

    public boolean handleKeyToggle(WurmConsole console, int key, boolean pressed) {
        try {
            if (pressed) {
                Integer suppressed = suppressedSelectorKey.get();
                suppressedSelectorKey.remove();
                if (suppressed != null && suppressed == key) return true;
            }
            if (!pressed) {
                String heldId = multiKey.getRecordId();
                MultiKeyController.Event release = multiKey.release(key);
                if (release != MultiKeyController.Event.NONE) {
                    KeybindRecord held = environment.findById(heldId);
                    if (release == MultiKeyController.Event.EXECUTE_ACTIVE
                            && held != null && held.isEnabled())
                        environment.execute(held, environment.hud());
                    return true;
                }
            }
            KeyBinding binding = console.getCurrentBinding(key);
            if (binding == null || binding.getAction() != null) return false;
            String command = binding.getStrCommand();
            if (command == null
                    || !command.toLowerCase(Locale.ENGLISH).startsWith(RUN_PREFIX)) return false;
            String id = command.substring(RUN_PREFIX.length()).trim();
            KeybindRecord record = environment.findById(id);
            if (record == null || !record.isEnabled() || !record.isSelectorKeybind()) return false;
            if (pressed) {
                MultiKeyController.Event event = multiKey.press(id, key,
                        record.isHudMulti() ? MultiKeyController.Mode.HUD
                                : MultiKeyController.Mode.ORDINARY,
                        environment.nanoTime());
                if (event == MultiKeyController.Event.OPEN_HUD_SELECTOR)
                    environment.openSelector(record, true, key);
            }
            return true;
        } catch (Throwable failure) {
            environment.warning("Long-press key hook failed open", failure);
            clear();
            return false;
        }
    }

    public void pollLongPress() {
        MultiKeyController.Event event = multiKey.threshold(
                environment.nanoTime(), LONG_PRESS_NANOS);
        if (event != MultiKeyController.Event.OPEN_ORDINARY_SELECTOR) return;
        String id = multiKey.getRecordId();
        KeybindRecord record = environment.findById(id);
        if (record == null || !record.isEnabled() || !record.isMultiPurpose()) {
            clear();
            return;
        }
        environment.openSelector(record, false, multiKey.getKey());
    }

    public void observeKeyPressed(int key) {
        try {
            suppressedSelectorKey.remove();
            SelectorSessionController.KeyDecision decision = selectorSession.keyPressed(key);
            if (decision == SelectorSessionController.KeyDecision.NONE) return;
            if (decision == SelectorSessionController.KeyDecision.DISMISS_AND_SUPPRESS_TRIGGER)
                suppressedSelectorKey.set(key);
            environment.closeSelector();
        } catch (Throwable failure) {
            suppressedSelectorKey.remove();
            environment.fine("Unable to dismiss multi selector for key press", failure);
        }
    }

    public void observeKeyReleased(int key) {
        try {
            selectorSession.triggerReleased(key);
        } catch (Throwable failure) {
            environment.fine("Unable to arm multi selector cancellation", failure);
        }
    }

    public long openSelectorSession(int triggerKey) {
        return selectorSession.open(triggerKey);
    }

    public boolean isCurrentSelectorSession(long token) {
        return selectorSession.isCurrent(token);
    }

    public boolean pointerPressed(int button, String variantId) {
        return selectorSession.pointerPressed(button, variantId);
    }

    public boolean pointerReleased(int button, String variantId) {
        return selectorSession.pointerReleased(button, variantId);
    }

    public boolean isHoldingRecord(String recordId) {
        return recordId != null && recordId.equals(multiKey.getRecordId());
    }

    public void closeSelectorSession() {
        selectorSession.close();
    }

    public void clear() {
        multiKey.clear();
    }
}
