package org.keybinder.wurm.ui;

import org.keybinder.wurm.catalog.InputKeyCatalog;
import org.keybinder.wurm.model.KeybindRecord;

import java.util.List;

/** Derives list status from the current set of managed records. */
public final class KeybindStatusResolver {
    private KeybindStatusResolver() {}

    /** Returns the enabled managed record that currently owns the target's chord. */
    public static KeybindRecord enabledManagedBlocker(KeybindRecord target,
                                                       List<KeybindRecord> records) {
        if (target == null || target.isEnabled() || records == null) return null;
        String chord = InputKeyCatalog.normalizeChord(target.getKey());
        if (chord.isEmpty()) return null;
        for (KeybindRecord candidate : records) {
            if (candidate != null && candidate != target && candidate.isEnabled()
                    && chord.equals(InputKeyCatalog.normalizeChord(candidate.getKey())))
                return candidate;
        }
        return null;
    }
}
