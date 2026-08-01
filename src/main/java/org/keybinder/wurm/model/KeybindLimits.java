package org.keybinder.wurm.model;

/** Shared defensive limits for persisted and portable keybind definitions. */
public final class KeybindLimits {
    public static final int MAX_VARIANTS = 15;
    public static final int MAX_STEPS_PER_VARIANT = 100;
    public static final int MAX_RECORDS_IN_TRANSFER = 1000;
    public static final int MAX_RECORD_NAME_LENGTH = 80;
    public static final int MAX_VARIANT_NAME_LENGTH = 80;
    public static final int MAX_COMMAND_LENGTH = 500;
    public static final int MAX_ENCODED_FIELD_LENGTH = 1024;
    public static final long MAX_TRANSFER_FILE_BYTES = 10L * 1024L * 1024L;

    private KeybindLimits() { }
}
