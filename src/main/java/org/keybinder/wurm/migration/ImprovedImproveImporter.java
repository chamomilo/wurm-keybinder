package org.keybinder.wurm.migration;

import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.SmartImproveStep;
import org.keybinder.wurm.model.TargetKind;
import org.keybinder.wurm.model.TargetSpec;
import org.keybinder.wurm.i18n.Messages;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** One-way decoder for known commands used by the Improved Improve lineage. */
public final class ImprovedImproveImporter {
    public boolean supports(String command) {
        if (command == null) return false;
        String normalized = command.trim().toLowerCase(Locale.ENGLISH);
        return "improve".equals(normalized)
                || "i2improve".equals(normalized)
                || "improveitems".equals(normalized);
    }

    public List<KeybindStep> importCommand(String command) {
        if (!supports(command))
            throw new IllegalArgumentException(Messages.text("validation.not_improve_command"));
        return Collections.<KeybindStep>singletonList(
                new SmartImproveStep(TargetSpec.simple(TargetKind.HOVER)));
    }
}
