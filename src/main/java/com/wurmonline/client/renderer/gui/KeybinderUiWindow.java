package com.wurmonline.client.renderer.gui;

import org.chamomilo.wurm.ui.v1.UiBackground;
import com.wurmonline.client.renderer.backend.Queue;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.i18n.LocalizedText;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Every Keybinder dialog shares the kit frame, chrome and branded typography. */
class KeybinderUiWindow extends ChamomiloUiV1Window {
    // Includes detached/hidden dialogs without retaining closed HUD windows.
    private static final Set<KeybinderUiWindow> EXISTING =
            Collections.newSetFromMap(new WeakHashMap<KeybinderUiWindow, Boolean>());
    private boolean contentReady;
    private WButton maximize;
    private String displayedLanguage;
    private LocalizedText titleSource;
    private FlexComponent localizedContent;

    KeybinderUiWindow(String id, boolean resizable) {
        super(id, "Keybinder", org.keybinder.wurm.i18n.Messages.languageCode(), UiBackground.WALNUT, 5);
        this.resizable = resizable;
        KeybinderUi.identify(this, id);
        contentReady = true;
        displayedLanguage = Messages.languageCode();
        setHeaderHeight(40);
        setContentPadding(4);
        KeybinderUi.theme(this);
        maximize = KeybinderUi.maximizeControl(this);
        EXISTING.add(this);
    }

    static void refreshExistingLanguages() {
        for (KeybinderUiWindow window : new ArrayList<>(EXISTING)) {
            if (Messages.languageCode().equals(window.displayedLanguage)) continue;
            try { window.relocalize(); }
            catch (RuntimeException failure) {
                Logger.getLogger(KeybinderUiWindow.class.getName()).log(Level.WARNING,
                        "Unable to refresh Keybinder dialog language", failure);
            }
        }
    }

    void setLocalizedTitle(String value) { titleSource = LocalizedText.capture(value); super.setTitle(value); }

    public void relocalize() {
        setLanguage(Messages.languageCode());
        if (titleSource != null) setLocalizedTitle(titleSource.resolve());
        KeybinderUi.relocalize(this);
        if (!resizable && localizedContent instanceof WurmArrayPanel)
            setSize(Math.max(width, ((WurmArrayPanel<?>) localizedContent).calcWidth() + 32), height);
        if (!resizable && localizedContent instanceof KeybinderUiLabel)
            setSize(Math.max(width, ((KeybinderUiLabel) localizedContent).naturalWidth() + 32), height);
        displayedLanguage = Messages.languageCode();
    }

    @Override public void gameTick() {
        if (!Messages.languageCode().equals(displayedLanguage)) relocalize();
        super.gameTick();
    }

    @Override void setComponent(FlexComponent component) {
        if (!contentReady) { super.setComponent(component); return; }
        localizedContent = component;
        setContent(component);
        KeybinderUi.theme(this);
    }

    @Override void componentResized() {
        super.componentResized();
        if (contentReady) KeybinderUi.theme(this);
    }

    @Override public void toggleMaximized() {
        // These windows are registered by Keybinder's existing HUD/persistence integration.
        if (!resizable || hud == null) return;
        show(hud);
        super.toggleMaximized();
    }

    @Override public void setInitialSize(int width, int height, boolean relative) {
        // SDK geometry is expressed in pixels; the game's native font option must not rescale it.
        super.setInitialSize(width + (contentReady ? 24 : 0), height + (contentReady ? 32 : 0),
                false, false, .5f, .5f);
    }

    @Override protected void renderComponent(Queue queue, float ignoredAlpha) {
        if (maximize != null) maximize.setEnabled(resizable);
        super.renderComponent(queue, 1f);
    }
}
