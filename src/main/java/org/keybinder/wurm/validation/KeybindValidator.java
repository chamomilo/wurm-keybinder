package org.keybinder.wurm.validation;

import org.keybinder.wurm.i18n.DisableReason;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.model.ConsoleCommandStep;
import org.keybinder.wurm.model.KeybindLimits;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.KeybindVariant;
import org.keybinder.wurm.model.VanillaActionStep;

import java.util.List;

/** One definition-level validation policy shared by UI, registry and storage. */
public final class KeybindValidator {
    private KeybindValidator() { }

    public static void validateConfigured(KeybindRecord record) {
        if (record == null)
            throw new IllegalArgumentException(Messages.text("validation.step_missing"));
        validateIdentity(record.getName(), record.getKey());
        validateVariants(record.getVariants(), false);
        if (record.getKeybindSteps().isEmpty())
            throw new IllegalArgumentException(Messages.text("validation.step_missing"));
    }

    public static void validatePendingSteps(String name, String key,
                                            List<? extends KeybindStep> steps) {
        validateIdentity(name, key);
        if (steps == null || steps.isEmpty())
            throw new IllegalArgumentException(Messages.text("validation.step_missing"));
        validateStepList(steps);
    }

    public static void validatePendingVariants(String name, String key,
                                               List<KeybindVariant> variants) {
        validateIdentity(name, key);
        validateVariants(variants, true);
    }

    /** Structural limits for persisted drafts; missing key/steps remain allowed. */
    public static void validateStorage(List<KeybindRecord> records) {
        if (records == null)
            throw new IllegalArgumentException("Keybind records are missing");
        if (records.size() > KeybindLimits.MAX_RECORDS_IN_TRANSFER)
            throw new IllegalArgumentException("Too many Keybinder records");
        for (KeybindRecord record : records) {
            requireLength(record.getName(), KeybindLimits.MAX_RECORD_NAME_LENGTH,
                    Messages.text("validation.name_too_long",
                            KeybindLimits.MAX_RECORD_NAME_LENGTH));
            validateVariants(record.getVariants(), false);
        }
    }

    public static String disabledReason(KeybindRecord record) {
        if (record == null || record.getName() == null || record.getName().trim().isEmpty())
            return DisableReason.value("invalid_name");
        if (record.getKey() == null || record.getKey().trim().isEmpty())
            return DisableReason.value("invalid_key");
        if (record.getKeybindSteps().isEmpty())
            return DisableReason.value("invalid_steps");
        return DisableReason.value("invalid");
    }

    private static void validateIdentity(String name, String key) {
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException(Messages.text("validation.name_missing"));
        if (key == null || key.trim().isEmpty())
            throw new IllegalArgumentException(Messages.text("validation.key_missing"));
        requireLength(name, KeybindLimits.MAX_RECORD_NAME_LENGTH,
                Messages.text("validation.name_too_long", KeybindLimits.MAX_RECORD_NAME_LENGTH));
    }

    private static void validateVariants(List<KeybindVariant> variants,
                                         boolean requireEveryVariantStep) {
        if (variants == null || variants.isEmpty())
            throw new IllegalArgumentException(Messages.text("validation.variant_missing"));
        if (variants.size() > KeybindLimits.MAX_VARIANTS)
            throw new IllegalArgumentException(Messages.text("editor.max_variants",
                    KeybindLimits.MAX_VARIANTS));
        for (KeybindVariant variant : variants) {
            requireLength(variant.getSubName(), KeybindLimits.MAX_VARIANT_NAME_LENGTH,
                    Messages.text("validation.variant_name_too_long",
                            KeybindLimits.MAX_VARIANT_NAME_LENGTH));
            if (requireEveryVariantStep && variant.getSteps().isEmpty())
                throw new IllegalArgumentException(
                        Messages.text("validation.variant_step_missing"));
            validateStepList(variant.getSteps());
        }
    }

    private static void validateStepList(List<? extends KeybindStep> steps) {
        if (steps.size() > KeybindLimits.MAX_STEPS_PER_VARIANT)
            throw new IllegalArgumentException(Messages.text("validation.steps_too_many",
                    KeybindLimits.MAX_STEPS_PER_VARIANT));
        for (KeybindStep step : steps) {
            String command = step instanceof ConsoleCommandStep
                    ? ((ConsoleCommandStep) step).getCommand()
                    : step instanceof VanillaActionStep
                    ? ((VanillaActionStep) step).getCommand() : null;
            if (command != null && command.length() > KeybindLimits.MAX_COMMAND_LENGTH)
                throw new IllegalArgumentException(Messages.text("validation.command_too_long",
                        KeybindLimits.MAX_COMMAND_LENGTH));
        }
    }

    private static void requireLength(String value, int maximum, String message) {
        if (value != null && value.length() > maximum)
            throw new IllegalArgumentException(message);
    }
}
