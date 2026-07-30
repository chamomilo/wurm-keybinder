package org.keybinder.wurm.migration;

import org.keybinder.wurm.command.TargetCodec;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.ActivateToolStep;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.TargetSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One-way decoder from Custom Actions commands into native Keybinder steps. */
public final class CustomActionsImporter {
    public boolean supports(String command) {
        return command != null && command.trim().regionMatches(true, 0, "act ", 0, 4);
    }

    public List<KeybindStep> importCommand(String input) {
        if (input == null) throw new IllegalArgumentException("Command is missing");
        String command = input.trim();
        if (command.regionMatches(true, 0, "act ", 0, 4)) command = command.substring(4).trim();
        if (command.isEmpty()) return Collections.emptyList();

        List<KeybindStep> result = new ArrayList<KeybindStep>();
        for (String rawPart : command.split("\\|", -1)) {
            String part = rawPart.trim();
            if (part.isEmpty()) throw new IllegalArgumentException("Empty action in chain");
            String[] fields = part.split("\\s+");
            if (fields.length != 2)
                throw new IllegalArgumentException("Expected '<id> <target>': " + part);
            int parsed = parseActionId(fields[0]);
            if ("toolbelt".equalsIgnoreCase(fields[1])) {
                result.add(new ActivateToolStep(TargetSpec.toolbeltSlot(parsed)));
            } else {
                result.add(new ActionStep((short) parsed, TargetCodec.decode(fields[1])));
            }
        }
        return result;
    }

    private static int parseActionId(String value) {
        int parsed;
        try {
            parsed = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid action id: " + value, e);
        }
        if (parsed < Short.MIN_VALUE || parsed > Short.MAX_VALUE)
            throw new IllegalArgumentException("Action id is outside short range: " + parsed);
        return parsed;
    }
}
