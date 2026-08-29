package org.keybinder.wurm.queue;

/** Indicates that one resolved keybind step had no room in the live queue. */
public final class QueueCapacityException extends IllegalStateException {
    public QueueCapacityException(String message) {
        super(message);
    }
}
