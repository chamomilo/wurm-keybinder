package org.keybinder.wurm.ui;

import org.junit.Test;
import org.keybinder.wurm.i18n.DisableReason;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.KeybindRecord;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class KeybindStatusResolverTest {
    @Test public void oneEnabledRecordBlocksEveryDisabledRecordOnTheSameChord() {
        KeybindRecord owner = record("Owner", "R", true);
        KeybindRecord first = record("First", "r", false);
        KeybindRecord second = record("Second", "R", false);
        List<KeybindRecord> records = Arrays.asList(first, owner, second);

        assertSame(owner, KeybindStatusResolver.enabledManagedBlocker(first, records));
        assertSame(owner, KeybindStatusResolver.enabledManagedBlocker(second, records));
    }

    @Test public void modifierMakesASeparateChord() {
        KeybindRecord owner = record("Owner", "R", true);
        KeybindRecord disabled = record("Disabled", "SHIFT+R", false);

        assertNull(KeybindStatusResolver.enabledManagedBlocker(
                disabled, Arrays.asList(owner, disabled)));
    }

    @Test public void disablingOwnerImmediatelyUnblocksEverySiblingStatus() {
        KeybindRecord owner = record("Owner", "R", true);
        KeybindRecord first = record("First", "R", false);
        KeybindRecord second = record("Second", "R", false);
        List<KeybindRecord> records = Arrays.asList(owner, first, second);

        owner.setEnabled(false);

        assertNull(KeybindStatusResolver.enabledManagedBlocker(first, records));
        assertNull(KeybindStatusResolver.enabledManagedBlocker(second, records));
    }

    private static KeybindRecord record(String name, String key, boolean enabled) {
        KeybindRecord record = KeybindRecord.actionChain(name, key,
                Collections.<ActionStep>emptyList());
        record.setEnabled(enabled);
        record.setDisabledReason(enabled ? "" : DisableReason.value("disabled_by_user"));
        return record;
    }
}
