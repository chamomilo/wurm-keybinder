package org.keybinder.wurm.bind;

import com.wurmonline.client.console.WurmConsole;
import org.keybinder.wurm.catalog.InputKeyCatalog;
import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.i18n.DisableReason;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.queue.ActionQueueCostCalculator;
import org.keybinder.wurm.queue.QueueCost;
import org.keybinder.wurm.validation.KeybindValidator;

import java.util.List;
import java.util.function.Function;

/** Rebuilds account-local live dispatcher bindings as one transactional workflow. */
public final class AccountBindingRestorer {
    private final ManagedBindAccess binds;
    private final AccountBindingCoordinator accountBindings;
    private final ActionQueueCostCalculator costs;
    private final EventLogger log;

    public AccountBindingRestorer(ManagedBindAccess binds,
                                  AccountBindingCoordinator accountBindings,
                                  ActionQueueCostCalculator costs, EventLogger log) {
        this.binds = binds;
        this.accountBindings = accountBindings;
        this.costs = costs;
        this.log = log;
    }

    public boolean restore(String account, List<KeybindRecord> records,
                           WurmConsole console, int limit,
                           Function<KeybindRecord, String> commandFor) {
        if (account == null || account.trim().isEmpty() || console == null) return false;
        int removed = 0;
        ManagedBindTransaction reset = new ManagedBindTransaction(binds, console);
        for (KeybindRecord record : records) {
            if (record.getKey().trim().isEmpty()) continue;
            String command = commandFor.apply(record);
            try {
                BindSnapshot live = findLive(console, record.getKey());
                if (live != null && live.getCommand().equalsIgnoreCase(command)
                        && reset.removeOwned(record.getKey(), command))
                    removed++;
            } catch (Exception failure) {
                reset.rollback(failure);
                log.error(Messages.text("registry.restore_failed",
                        record.getName(), account), failure);
                return false;
            }
        }

        String activeAccount = accountBindings.activate(account, records);
        if (activeAccount.isEmpty()) {
            reset.rollback(new IllegalStateException(
                    "Account activation profile was not available"));
            return false;
        }

        int restored = 0;
        int conflicts = 0;
        for (KeybindRecord record : records) {
            String command = commandFor.apply(record);
            try {
                if (record.isEnabled()) {
                    try {
                        KeybindValidator.validateConfigured(record);
                        applyLimit(record, limit);
                    } catch (RuntimeException invalid) {
                        record.setEnabled(false);
                        record.setDisabledReason(KeybindValidator.disabledReason(record));
                    }
                }

                BindSnapshot live = record.getKey().trim().isEmpty()
                        ? null : findLive(console, record.getKey());
                if (record.isEnabled()) {
                    if (live == null) {
                        installLive(console, record.getKey(), command);
                        restored++;
                    } else if (!live.getCommand().equalsIgnoreCase(command)) {
                        record.setEnabled(false);
                        record.setDisabledReason(DisableReason.value("key_used",
                                record.getKey(), live.getCommand()));
                        conflicts++;
                        log.warning(Messages.text("registry.restore_conflict",
                                record.getName(), record.getKey(), live.getCommand()));
                    }
                }
            } catch (Exception failure) {
                record.setEnabled(false);
                record.setDisabledReason(DisableReason.value("restore_failed",
                        failure.getClass().getSimpleName()));
                conflicts++;
                log.error(Messages.text("registry.restore_failed",
                        record.getName(), activeAccount), failure);
            }
        }
        accountBindings.persist(records);
        if (restored > 0 || removed > 0 || conflicts > 0)
            log.info(Messages.text("registry.restore_summary",
                    activeAccount, restored, removed, conflicts));
        return true;
    }

    private BindSnapshot findLive(WurmConsole console, String key)
            throws ReflectiveOperationException {
        return InputKeyCatalog.isVirtual(key) ? null : binds.findByKey(console, key);
    }

    private void installLive(WurmConsole console, String key, String command) {
        if (!InputKeyCatalog.isVirtual(key)) binds.install(console, key, command);
    }

    private void applyLimit(KeybindRecord record, int limit) {
        QueueCost cost = costs.keybindCost(record);
        if (limit > 0 && cost.getKind() == QueueCost.Kind.FIXED
                && cost.getValue() > limit) {
            record.setEnabled(false);
            record.setDisabledReason(
                    DisableReason.value("queue_exceeded", cost.getValue(), limit));
        }
    }
}
