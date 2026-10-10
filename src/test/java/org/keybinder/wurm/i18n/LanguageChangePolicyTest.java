package org.keybinder.wurm.i18n;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LanguageChangePolicyTest {
    @Test public void repeatedCurrentLanguageDoesNotRebuild() {
        assertEquals(LanguageChangePolicy.Decision.UNCHANGED,
                LanguageChangePolicy.decide("ru", "RU"));
    }

    @Test
    public void languageChangeRelocalizesExistingHud() {
        assertEquals(LanguageChangePolicy.Decision.APPLY,
                LanguageChangePolicy.decide("en", "pt-BR"));
    }

    @Test
    public void allLanguagesApplyImmediately() {
        for (Language current : Language.values())
            for (Language requested : Language.values())
                assertEquals(current == requested ? LanguageChangePolicy.Decision.UNCHANGED
                                : LanguageChangePolicy.Decision.APPLY,
                        LanguageChangePolicy.decide(current.getCode(), requested.getCode()));
    }
}
