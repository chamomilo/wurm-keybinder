package org.keybinder.wurm.i18n;

/**
 * Pure language-switch decision used by the HUD controller and unit tests.
 * Captions update in place, including an editor with unsaved work.
 */
public final class LanguageChangePolicy {
    public enum Decision { UNCHANGED, APPLY }

    private LanguageChangePolicy() {}

    public static Decision decide(
            String currentCode, String requestedCode) {
        String current = Language.fromCode(currentCode).getCode();
        String requested = Language.fromCode(requestedCode).getCode();
        return requested.equals(current) ? Decision.UNCHANGED : Decision.APPLY;
    }
}
