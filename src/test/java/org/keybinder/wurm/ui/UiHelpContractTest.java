package org.keybinder.wurm.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import org.junit.After;
import org.junit.Test;
import org.keybinder.wurm.i18n.Messages;

public class UiHelpContractTest {
    @After public void restoreEnglish() { Messages.select("en"); }

    @Test public void everyRequiredHelpEntryExistsInEverySupportedLanguage() {
        for (String language : new String[] {"en", "pt-BR"}) {
            Messages.select(language);
            for (String key : UiHelpContract.requiredMessageKeys()) {
                String value = Messages.text(key);
                assertFalse(language + " " + key, value.trim().isEmpty());
                assertNotEquals(language + " " + key, key, value);
            }
        }
    }
}
