package org.keybinder.wurm.model;

import java.util.Objects;
import org.keybinder.wurm.i18n.Messages;

/** A native Wurm key action identified by its stable ActionClass command name. */
public final class VanillaActionStep implements KeybindStep {
    private final String command;

    public VanillaActionStep(String command) {
        String value = command == null ? "" : command.trim();
        if (value.isEmpty())
            throw new IllegalArgumentException(Messages.text("validation.vanilla_missing"));
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
