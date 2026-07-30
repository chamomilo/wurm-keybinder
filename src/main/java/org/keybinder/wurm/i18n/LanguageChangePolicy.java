package org.keybinder.wurm.i18n;

/**
 * Pure language-switch decision used by the HUD controller and unit tests.
 * The editor is deliberately never rebuilt while it may contain unsaved work.
 */
public final class LanguageChangePolicy {
    public enum Decision { UNCHANGED, CANCEL_PENDING, APPLY, DEFER }

    private LanguageChangePolicy() {}

    public static Decision decide(
            String currentCode, String requestedCode, boolean editorOpen, String pendingCode) {
        String current = Language.fromCode(currentCode).getCode();
        String requested = Language.fromCode(requestedCode).getCode();
        if (requested.equals(current))
            return pendingCode == null ? Decision.UNCHANGED : Decision.CANCEL_PENDING;
        return editorOpen ? Decision.DEFER : Decision.APPLY;
    }
}
