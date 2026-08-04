package org.keybinder.wurm.command;

import java.util.Locale;

/** Conservative recognition of standard immediate server permission denials. */
final class BulkTransferRejectionMessage {
    private BulkTransferRejectionMessage() { }

    static boolean matches(String context, String message) {
        if (context == null || !":event".equalsIgnoreCase(context.trim())
                || message == null) return false;
        String value = message.trim().toLowerCase(Locale.ENGLISH);
        return value.startsWith("that would be illegal here.")
                || value.startsWith("you are not allowed to do that")
                || value.startsWith("you do not have permission to")
                || value.startsWith("only ingredients that are used to make food can be put onto a roasting dish.");
    }
}
