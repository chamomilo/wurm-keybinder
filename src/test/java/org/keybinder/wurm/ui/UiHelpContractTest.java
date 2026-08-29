package org.keybinder.wurm.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import org.junit.After;
import org.junit.Test;
import org.keybinder.wurm.i18n.Language;
import org.keybinder.wurm.i18n.Messages;

public class UiHelpContractTest {
    @After public void restoreEnglish() { Messages.select("en"); }

    @Test public void everyRequiredHelpEntryExistsInEverySupportedLanguage() {
        for (Language language : Language.values()) {
            Messages.select(language.getCode());
            for (String key : UiHelpContract.requiredMessageKeys()) {
                String value = Messages.text(key);
                assertFalse(language.getCode() + " " + key, value.trim().isEmpty());
                assertNotEquals(language.getCode() + " " + key, key, value);
            }
        }
    }
}
