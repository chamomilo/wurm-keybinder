package org.keybinder.wurm.integration;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/** Bounded-per-tick queue for mutations that must run on the HUD thread. */
public final class DeferredUiQueue {
    private final Queue<Runnable> pending = new ConcurrentLinkedQueue<Runnable>();
    private final int maximumPerDrain;
    private final Consumer<Throwable> failureHandler;

    public DeferredUiQueue(int maximumPerDrain, Consumer<Throwable> failureHandler) {
        if (maximumPerDrain < 1)
            throw new IllegalArgumentException("maximumPerDrain must be positive");
        this.maximumPerDrain = maximumPerDrain;
        this.failureHandler = failureHandler;
    }

    public void defer(Runnable operation) {
        if (operation != null) pending.offer(operation);
    }

    public int drain() {
        int processed = 0;
        Runnable operation;
        while (processed < maximumPerDrain && (operation = pending.poll()) != null) {
            processed++;
            try {
                operation.run();
            } catch (Throwable failure) {
                if (failureHandler != null) failureHandler.accept(failure);
            }
        }
        return processed;
    }

    public void clear() { pending.clear(); }
    int size() { return pending.size(); }
}
