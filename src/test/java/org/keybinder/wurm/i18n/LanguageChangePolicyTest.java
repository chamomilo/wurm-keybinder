package org.keybinder.wurm.i18n;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LanguageChangePolicyTest {
    @Test
    public void repeatedCurrentLanguageDoesNotRebuild() {
        assertEquals(LanguageChangePolicy.Decision.UNCHANGED,
                LanguageChangePolicy.decide("en", "en", false, null));
    }

    @Test
    public void languageChangeRelocalizesExistingHud() {
        assertEquals(LanguageChangePolicy.Decision.APPLY,
                LanguageChangePolicy.decide("en", "pt-BR", false, null));
    }

    @Test
    public void editorWithUnsavedStateDefersRebuild() {
        assertEquals(LanguageChangePolicy.Decision.DEFER,
                LanguageChangePolicy.decide("en", "pt-BR", true, null));
    }

    @Test
    public void pendingLanguageCanBeChangedBackBeforeEditorCloses() {
        assertEquals(LanguageChangePolicy.Decision.CANCEL_PENDING,
                LanguageChangePolicy.decide("en", "en", true, "pt-BR"));
    }
}
