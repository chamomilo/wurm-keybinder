package org.keybinder.wurm.model;

import java.util.Objects;

public final class ConsoleCommandStep implements KeybindStep {
    private final String command;
    private final boolean preserveExactText;

    public ConsoleCommandStep(String command) {
        this(command, false);
    }

    public ConsoleCommandStep(String command, boolean preserveExactText) {
        this.command = Objects.requireNonNull(command, "command");
        this.preserveExactText = preserveExactText;
    }

    @Override public StepKind getKind() { return StepKind.CONSOLE_COMMAND; }
    public String getCommand() { return command; }
    public boolean isPreserveExactText() { return preserveExactText; }
}
