package org.keybinder.wurm.command;

/** Expected runtime condition: this step has no usable tool or target and should be skipped. */
public final class StepUnavailableException extends RuntimeException {
    public StepUnavailableException(String message) {
        super(message);
    }
}
