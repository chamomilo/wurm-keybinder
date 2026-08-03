package org.keybinder.wurm.command;

import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.i18n.DisableReason;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.KeybindRecord;

import java.util.Collections;
import java.util.Locale;

/** Routes Keybinder console commands without depending on the mod entry point. */
public final class KeybinderCommandRouter {
    public interface Context {
        void ensureReady();
        KeybindRecord find(String id);
        void execute(KeybindRecord record) throws Exception;
        String selectedTarget();
        void add(KeybindRecord record) throws Exception;
        int importAllReviewed() throws Exception;
        boolean delete(String id) throws Exception;
        void restoreOriginalBindings();
        void refreshWindow();
    }

    private final EventLogger events;
    public KeybinderCommandRouter(EventLogger events) {
        this.events = events;
    }

    public boolean route(String command, String[] data, Context context) {
        if (!isKeybinderCommand(command)) return false;
        try {
            if ("keybinder_run".equalsIgnoreCase(command)) {
                context.ensureReady();
                requireLength(data, 2, "command.usage.run");
                KeybindRecord record = context.find(data[1]);
                if (record == null)
                    throw new IllegalArgumentException(
                            Messages.text("event.record_missing", data[1]));
                if (!record.isEnabled()) {
                    events.warning(Messages.text("event.record_disabled", record.getName(),
                            DisableReason.display(record.getDisabledReason())));
                    return true;
                }
                context.execute(record);
                return true;
            }
            if ("keybinder_add_selected".equalsIgnoreCase(command)) {
                context.ensureReady();
                requireLength(data, 4, "command.usage.add_selected");
                int parsed = parseActionId(data[3]);
                KeybindRecord record = KeybindRecord.actionChain(
                        data[2].replace('_', ' '), data[1],
                        Collections.singletonList(new ActionStep((short) parsed,
                                TargetCodec.decode(context.selectedTarget()))));
                context.add(record);
                context.refreshWindow();
                return true;
            }
            if ("keybinder_import_confirm".equalsIgnoreCase(command)) {
                context.ensureReady();
                if (data == null || data.length != 2 || !"CONFIRM".equals(data[1]))
                    throw new IllegalArgumentException(
                            Messages.text("command.usage.import_confirm"));
                int imported = context.importAllReviewed();
                events.info(Messages.text("event.import_complete", imported));
                context.refreshWindow();
                return true;
            }
            if ("keybinder_delete".equalsIgnoreCase(command)) {
                context.ensureReady();
                requireLength(data, 2, "command.usage.delete");
                if (!context.delete(data[1]))
                    events.warning(Messages.text("event.record_missing", data[1]));
                context.refreshWindow();
                return true;
            }
            if ("keybinder_restore_originals".equalsIgnoreCase(command)) {
                context.ensureReady();
                if (data == null || data.length != 2 || !"CONFIRM".equals(data[1]))
                    throw new IllegalArgumentException(
                            Messages.text("command.usage.restore_originals"));
                context.restoreOriginalBindings();
                return true;
            }
            return false;
        } catch (Throwable error) {
            events.error(error.getMessage() == null
                    ? Messages.text("error.command_failed") : error.getMessage(), error);
            return true;
        }
    }

    private static boolean isKeybinderCommand(String command) {
        return command != null && command.toLowerCase(Locale.ENGLISH).startsWith("keybinder_");
    }

    private static void requireLength(String[] data, int length, String usageKey) {
        if (data == null || data.length != length)
            throw new IllegalArgumentException(Messages.text(usageKey));
    }

    private static int parseActionId(String value) {
        final int parsed;
        try {
            parsed = Integer.parseInt(value);
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException(Messages.text("validation.action_id_invalid", value));
        }
        if (parsed < Short.MIN_VALUE || parsed > Short.MAX_VALUE)
            throw new IllegalArgumentException(
                    Messages.text("validation.action_id_range") + ": " + parsed);
        return parsed;
    }
}
