package org.keybinder.wurm.bind;

import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.i18n.DisableReason;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.storage.AccountKeybindStateStore;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Owns account-local activation persistence independently of live bind repair. */
public final class AccountBindingCoordinator {
    private final AccountKeybindStateStore store;
    private final EventLogger log;
    private String activeAccount = "";

    public AccountBindingCoordinator(AccountKeybindStateStore store, EventLogger log) {
        this.store = store;
        this.log = log;
    }

    public String activate(String account, List<KeybindRecord> records) {
        if (account == null || account.trim().isEmpty()) return "";
        String requestedAccount = account.trim();
        if (!activeAccount.isEmpty()
                && !activeAccount.equalsIgnoreCase(requestedAccount))
            persist(records);
        if (store == null) {
            activeAccount = requestedAccount;
            return activeAccount;
        }
        try {
            AccountKeybindStateStore.State state = store.load(requestedAccount);
            activeAccount = requestedAccount;
            if (state.isPresent()) {
                for (KeybindRecord record : records) {
                    boolean enabled = state.getEnabledIds().contains(record.getId());
                    record.setEnabled(enabled);
                    record.setDisabledReason(enabled ? ""
                            : DisableReason.value("disabled_by_user"));
                }
            } else {
                persist(records);
            }
        } catch (Exception failure) {
            activeAccount = "";
            log.error(Messages.text("registry.account_load_failed", requestedAccount), failure);
            return "";
        }
        return activeAccount;
    }

    public void persist(List<KeybindRecord> records) {
        if (store == null || activeAccount.isEmpty()) return;
        Set<String> enabled = new HashSet<String>();
        for (KeybindRecord record : records)
            if (record.isEnabled()) enabled.add(record.getId());
        try {
            store.save(activeAccount, enabled);
        } catch (IOException failure) {
            log.error(Messages.text("registry.account_save_failed", activeAccount), failure);
        }
    }

    public String getActiveAccount() { return activeAccount; }
}
