package org.keybinder.wurm.bind;

import java.util.Objects;

/** Immutable presentation model for one row in the vanilla-import review. */
public final class VanillaImportCandidate {
    public enum Type {
        ACTION_CHAIN,
        SMART_IMPROVE,
        VANILLA_COMMAND,
        RAW_VANILLA_COMMAND
    }

    public enum Status {
        READY,
        NEEDS_REVIEW,
        CONFLICT,
        INVALID
    }

    private final BindSnapshot binding;
    private final Type type;
    private final Status status;
    private final String detail;
    private final boolean selectedByDefault;

    public VanillaImportCandidate(BindSnapshot binding, Type type, Status status,
                                  String detail, boolean selectedByDefault) {
        this.binding = Objects.requireNonNull(binding, "binding");
        this.type = Objects.requireNonNull(type, "type");
        this.status = Objects.requireNonNull(status, "status");
        this.detail = detail == null ? "" : detail;
        this.selectedByDefault = selectedByDefault;
    }

    public BindSnapshot getBinding() { return binding; }
    public Type getType() { return type; }
    public Status getStatus() { return status; }
    public String getDetail() { return detail; }
    public boolean isSelectedByDefault() { return selectedByDefault; }
    public boolean isImportable() { return status != Status.CONFLICT && status != Status.INVALID; }
}
