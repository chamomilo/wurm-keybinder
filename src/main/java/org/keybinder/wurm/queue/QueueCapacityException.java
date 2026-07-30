package org.keybinder.wurm.queue;

/** Indicates that no keybind steps were sent because the queue had no room. */
public final class QueueCapacityException extends IllegalStateException {
    public QueueCapacityException(String message) {
        super(message);
    }
}
