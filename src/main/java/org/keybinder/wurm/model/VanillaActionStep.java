package org.keybinder.wurm.model;

import java.util.Objects;

/** A native Wurm key action identified by its stable ActionClass command name. */
public final class VanillaActionStep implements KeybindStep {
    private final String command;

    public VanillaActionStep(String command) {
        String value = command == null ? "" : command.trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Vanilla command is missing");
        this.command = value;
    }

    public String getCommand() {
        return command;
    }

    @Override
    public StepKind getKind() {
        return StepKind.VANILLA_ACTION;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof VanillaActionStep
                && command.equals(((VanillaActionStep) other).command);
    }

    @Override
    public int hashCode() {
        return Objects.hash(command);
    }
}
