package org.keybinder.wurm.bind;

/**
 * One-shot lifecycle for a multi-variant selector, including a deferred open.
 * The selector remains active only until a variant is chosen or the next
 * unrelated keyboard/pointer action dismisses it.
 */
public final class SelectorSessionController {
    public enum KeyDecision {
        NONE,
        DISMISS,
        DISMISS_AND_SUPPRESS_TRIGGER
    }

    private long generation;
    private boolean active;
    private int triggerKey = -1;
    private boolean dismissalArmed;
    private String pressedVariantId;

    public synchronized long open(int key) {
        generation++;
        active = true;
        triggerKey = key;
        dismissalArmed = key < 0;
        pressedVariantId = null;
        return generation;
    }

    public synchronized boolean isCurrent(long token) {
        return active && generation == token;
    }

    public synchronized KeyDecision keyPressed(int key) {
        if (!active || !dismissalArmed) return KeyDecision.NONE;
        boolean suppress = triggerKey >= 0 && triggerKey == key;
        invalidate();
        return suppress ? KeyDecision.DISMISS_AND_SUPPRESS_TRIGGER
                : KeyDecision.DISMISS;
    }

    public synchronized boolean pointerPressed(int button, String variantId) {
        if (!active) return false;
        pressedVariantId = button == 0 ? clean(variantId) : null;
        if (pressedVariantId != null || !dismissalArmed) return false;
        invalidate();
        return true;
    }

    public synchronized boolean pointerReleased(int button, String variantId) {
        if (!active || pressedVariantId == null) return false;
        String released = button == 0 ? clean(variantId) : null;
        boolean selectedCandidate = pressedVariantId.equals(released);
        pressedVariantId = null;
        if (selectedCandidate) return false;
        if (!dismissalArmed) return false;
        invalidate();
        return true;
    }

    public synchronized boolean otherPointerAction() {
        if (!active || !dismissalArmed) return false;
        invalidate();
        return true;
    }

    /** Arms cancellation only after the physical key which opened the selector is released. */
    public synchronized boolean triggerReleased(int key) {
        if (!active || dismissalArmed || triggerKey < 0 || triggerKey != key) return false;
        dismissalArmed = true;
        return true;
    }

    public synchronized void close() {
        if (active) invalidate();
    }

    public synchronized boolean isActive() { return active; }

    private void invalidate() {
        generation++;
        active = false;
        triggerKey = -1;
        dismissalArmed = false;
        pressedVariantId = null;
    }

    private static String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
