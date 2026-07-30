package org.keybinder.wurm.i18n;

/**
 * Applies a dictionary change to the existing HUD window. It deliberately has
 * no window factory, so localization cannot create duplicate HUD components.
 */
public final class ExistingWindowRelocalizer {
    private ExistingWindowRelocalizer() {}

    public static boolean apply(
            String currentCode, String selectedCode,
            Runnable activateDictionary, Runnable relocalizeExistingWindow) {
        String current = Language.fromCode(currentCode).getCode();
        String selected = Language.fromCode(selectedCode).getCode();
        if (current.equals(selected)) return false;
        activateDictionary.run();
        if (relocalizeExistingWindow != null) relocalizeExistingWindow.run();
        return true;
    }
}
