package org.keybinder.wurm.bind;

/** Pure press/repeat/release controller for ordinary and HUD multi keybinds. */
public final class MultiKeyController {
    public enum Mode { ORDINARY, HUD }
    public enum Event {
        EXECUTE_ACTIVE,
        OPEN_ORDINARY_SELECTOR,
        OPEN_HUD_SELECTOR,
        CONSUME,
        NONE
    }

    private String recordId;
    private int key = -1;
    private Mode mode;
    private long pressedAt;
    private boolean selectorOpened;

    public synchronized Event press(String requestedRecordId, int requestedKey,
                                    Mode requestedMode, long nowNanos) {
        if (recordId != null) {
            return recordId.equals(requestedRecordId) && key == requestedKey
                    ? Event.CONSUME : Event.NONE;
        }
        recordId = requestedRecordId;
        key = requestedKey;
        mode = requestedMode;
        pressedAt = nowNanos;
        selectorOpened = requestedMode == Mode.HUD;
        return requestedMode == Mode.HUD ? Event.OPEN_HUD_SELECTOR : Event.CONSUME;
    }

    public synchronized Event release(int releasedKey) {
        if (recordId == null || key != releasedKey) return Event.NONE;
        Event result = mode == Mode.ORDINARY && !selectorOpened
                ? Event.EXECUTE_ACTIVE : Event.CONSUME;
        clearInternal();
        return result;
    }

    public synchronized Event threshold(long nowNanos, long thresholdNanos) {
        if (recordId == null || mode != Mode.ORDINARY || selectorOpened
                || nowNanos - pressedAt < thresholdNanos) return Event.NONE;
        selectorOpened = true;
        return Event.OPEN_ORDINARY_SELECTOR;
    }

    public synchronized String getRecordId() { return recordId; }

    public synchronized int getKey() { return key; }

    public synchronized void clear() { clearInternal(); }

    private void clearInternal() {
        recordId = null;
        key = -1;
        mode = null;
        pressedAt = 0L;
        selectorOpened = false;
    }
}
