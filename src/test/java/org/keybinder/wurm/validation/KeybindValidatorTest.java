package org.keybinder.wurm.validation;

import org.junit.Test;
import org.keybinder.wurm.i18n.DisableReason;
import org.keybinder.wurm.model.ConsoleCommandStep;
import org.keybinder.wurm.model.KeybindLimits;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.KeybindVariant;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class KeybindValidatorTest {
    @Test
    public void configuredRecordRequiresAnActiveStep() {
        KeybindRecord draft = new KeybindRecord("draft", "Draft", "R",
                Collections.<KeybindStep>emptyList());

        expectInvalid(new Runnable() {
            @Override public void run() { KeybindValidator.validateConfigured(draft); }
        });
        assertEquals(DisableReason.value("invalid_steps"),
                KeybindValidator.disabledReason(draft));
    }

    @Test
    public void pendingVariantsRequireStepsInEveryAlternative() {
        KeybindVariant valid = new KeybindVariant("valid", "Valid",
                Collections.<KeybindStep>singletonList(new ConsoleCommandStep("say ready")));
        KeybindVariant empty = new KeybindVariant("empty", "Empty",
                Collections.<KeybindStep>emptyList());

        expectInvalid(new Runnable() {
            @Override public void run() {
                KeybindValidator.validatePendingVariants(
                        "Multiple", "R", Arrays.asList(valid, empty));
            }
        });
    }

    @Test
    public void storageValidationAllowsAnUnconfiguredDraft() {
        KeybindRecord draft = new KeybindRecord("draft", "Draft", "",
                Collections.<KeybindStep>emptyList());

        KeybindValidator.validateStorage(Collections.singletonList(draft));
    }

    @Test
    public void commandLimitIsSharedByConfiguredAndStorageValidation() {
        String tooLong = repeat('x', KeybindLimits.MAX_COMMAND_LENGTH + 1);
        KeybindRecord record = new KeybindRecord("long", "Long", "R",
                Collections.<KeybindStep>singletonList(new ConsoleCommandStep(tooLong)));

        expectInvalid(new Runnable() {
            @Override public void run() { KeybindValidator.validateConfigured(record); }
        });
        expectInvalid(new Runnable() {
            @Override public void run() {
                KeybindValidator.validateStorage(Collections.singletonList(record));
            }
        });
    }

    private static void expectInvalid(Runnable operation) {
        try {
            operation.run();
            fail("Expected validation failure");
        } catch (IllegalArgumentException expected) { }
    }

    private static String repeat(char value, int count) {
        StringBuilder result = new StringBuilder(count);
        for (int i = 0; i < count; i++) result.append(value);
        return result.toString();
    }
}
