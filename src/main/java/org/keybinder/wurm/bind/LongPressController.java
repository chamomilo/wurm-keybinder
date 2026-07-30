package org.keybinder.wurm.bind;

/** Atomic state machine for one currently held multi-purpose key. */
public final class LongPressController {
    public static final class Release {
        private final String recordId;
        private final boolean tap;

        private Release(String recordId, boolean tap) {
            this.recordId = recordId;
            this.tap = tap;
        }

        public String getRecordId() { return recordId; }
        public boolean isTap() { return tap; }
    }

    private String recordId;
    private int key = -1;
    private long pressedAt;
    private boolean triggered;

    public synchronized boolean press(String requestedRecordId, int requestedKey, long nowNanos) {
        if (recordId != null) return recordId.equals(requestedRecordId) && key == requestedKey;
        recordId = requestedRecordId;
        key = requestedKey;
        pressedAt = nowNanos;
        triggered = false;
        return true;
    }

    public synchronized Release release(int releasedKey) {
        if (recordId == null || key != releasedKey) return null;
        Release result = new Release(recordId, !triggered);
        clearInternal();
        return result;
    }

    public synchronized String triggerIfElapsed(long nowNanos, long thresholdNanos) {
        if (recordId == null || triggered || nowNanos - pressedAt < thresholdNanos) return null;
        triggered = true;
        return recordId;
    }

    public synchronized void clear() {
        clearInternal();
    }

    private void clearInternal() {
        recordId = null;
        key = -1;
        pressedAt = 0L;
        triggered = false;
    }
}
