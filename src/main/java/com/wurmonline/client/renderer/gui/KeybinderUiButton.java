package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.gui.text.ChamomiloUiV1Fonts;
import com.wurmonline.client.renderer.gui.text.TextFont;
import org.chamomilo.wurm.ui.v1.*;
import java.awt.Rectangle;
import java.util.List;
import org.keybinder.wurm.i18n.LocalizedText;

/** Native completed clicks with SDK artwork, motion and explicitly coordinated caption groups. */
class KeybinderUiButton extends WButton implements KeybinderLocalized {
    protected final ChamomiloUiV1Canvas canvas = new ChamomiloUiV1Canvas(this);
    protected final UiButtonMotion motion = new UiButtonMotion();
    private boolean ready;
    private int pixels = 18, baseline = 22;
    private int ceiling = 18;
    private String display;
    private String question, message;
    private String groupId;
    private List<KeybinderUiButton> peers;
    private LocalizedText captionSource, tipSource, questionSource, messageSource;

    KeybinderUiButton(String caption, final ButtonListener listener) {
        super(caption);
        ready = true;
        captionSource = LocalizedText.capture(caption);
        setButtonListener(new ButtonListener() {
            private boolean armed;
            @Override public void buttonPressed(WButton button) {
                armed = isEnabled();
                if (armed) motion.pointerPressed(System.nanoTime());
                if (armed && listener != null) listener.buttonPressed(button);
            }
            @Override public void buttonClicked(WButton button) {
                boolean fire = armed && isEnabled(); armed = false;
                if (!fire || listener == null) return;
                if (question == null) listener.buttonClicked(button);
                else new KeybinderUiConfirmWindow(question, message,
                        () -> { if (isEnabled()) listener.buttonClicked(button); }).show(hud);
            }
        });
        setSize(Math.max(64, captionWidth(caption, 18) + 12), 32);
        sizeFlags = FIXED_WIDTH | FIXED_HEIGHT;
    }

    void confirmation(String question, String message) {
        this.question = question; this.message = message;
        questionSource = LocalizedText.capture(question); messageSource = LocalizedText.capture(message);
    }
    @Override public void setHoverString(String value) { tipSource = LocalizedText.capture(value); super.setHoverString(value); }
    @Override public void relocalize() {
        if (captionSource != null) setLocalizedCaption(captionSource.resolve());
        if (tipSource != null) setHoverString(tipSource.resolve());
        if (questionSource != null) question = questionSource.resolve();
        if (messageSource != null) message = messageSource.resolve();
    }
    void captionCeiling(int pixels) { ceiling = pixels; refit(); }
    /** Explicit locale changes may alter natural geometry; tick-time labels must not. */
    void setLocalizedCaption(String caption) {
        setLabel(caption);
        setSize(Math.max(64, captionWidth(caption, ceiling) + 12), height);
    }
    @Override void setSize(int width, int height) {
        int flags = sizeFlags;
        sizeFlags = 0; // Native fixed flags otherwise retain WButton's original padded size.
        super.setSize(width, height);
        sizeFlags = flags;
        if (ready) refit();
    }
    @Override void setLabel(String value) { setLabel(value, false); }
    @Override void setLabel(String value, boolean resize) {
        captionSource = LocalizedText.capture(value);
        if (value.equals(label)) return;
        super.setLabel(value, false);
        if (ready) refit();
    }

    private void refit() {
        fitGroup(groupId == null ? name : groupId,
                peers == null ? java.util.Collections.singletonList(this) : peers);
    }

    static void fitGroup(String id, List<KeybinderUiButton> buttons) {
        int chosen = 128, top = 0, bottom = 0;
        for (KeybinderUiButton button : buttons) {
            UiButtonLayout fit = UiButtonLayout.fit(new String[]{button.label}, Math.max(32, button.width),
                    Math.max(32, button.height), UiDensity.HIGH, UiScale.BASE, true);
            chosen = Math.min(chosen, Math.min(button.ceiling, fit.fontPixels));
        }
        for (KeybinderUiButton button : buttons) {
            String caption = button.label;
            int available = Math.max(0, button.width - 8);
            while (!caption.isEmpty() && captionWidth(caption + (caption.equals(button.label) ? "" : "..."), chosen) > available)
                caption = caption.substring(0, caption.offsetByCodePoints(caption.length(), -1));
            button.display = caption.equals(button.label) ? caption : caption + "...";
            for (boolean bold : new boolean[]{false, true}) {
                Rectangle ink = UiTypography.ink(button.display, chosen, bold, UiDensity.HIGH);
                top = Math.min(top, ink.y); bottom = Math.max(bottom, ink.y + ink.height);
            }
        }
        for (KeybinderUiButton button : buttons) {
            button.groupId = id; button.peers = buttons; button.pixels = chosen;
            button.baseline = (button.height - (bottom - top)) / 2 - top;
            button.text = ChamomiloUiV1Fonts.caption(chosen, false, UiDensity.HIGH);
            button.textBold = ChamomiloUiV1Fonts.caption(chosen, true, UiDensity.HIGH);
            if (!button.display.equals(button.label) && button.getHoverString() == null)
                button.setHoverString(button.label);
        }
    }
    private static int captionWidth(String caption, int pixels) {
        int width = 0;
        for (boolean bold : new boolean[]{false, true}) {
            Rectangle ink = UiTypography.ink(caption, pixels, bold, UiDensity.HIGH);
            width = Math.max(width, Math.max(UiTypography.width(caption, pixels, bold, UiDensity.HIGH),
                    ink.x + ink.width) - Math.min(0, ink.x));
        }
        return width;
    }
    protected void paintSurface(Queue queue) {
        motion.update(isEnabled(), hovered, isCloseHovered, System.nanoTime());
        UiPainter.button(canvas.begin(queue), motion.brightness(), motion.depth(), motion.hover(),
                UiScale.BASE, 1f, x, y, width, height);
    }
    @Override protected void renderComponent(Queue queue, float ignoredAlpha) {
        paintSurface(queue);
        boolean bold = isEnabled() && hovered;
        TextFont font = bold ? textBold : text;
        Rectangle ink = UiTypography.ink(display, pixels, bold, UiDensity.HIGH);
        int left = Math.min(0, ink.x);
        int extent = Math.max(UiTypography.width(display, pixels, bold, UiDensity.HIGH), ink.x + ink.width) - left;
        int depth = Math.round(motion.depth());
        font.moveTo(x + (width - extent) / 2 - left + depth, y + baseline + depth);
        UiColor color = UiPainter.buttonCaptionColor(isEnabled(), motion.hover());
        font.paint(queue, display, color.red, color.green, color.blue, 1f);
    }
}
