package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.resources.textures.KeybinderTextureFactory;
import com.wurmonline.client.resources.textures.ResourceTexture;
import org.keybinder.wurm.ui.KeybinderUiController;

/**
 * A compact launcher built on the same static/drag component used by Wurm's
 * Target window. It deliberately has no WWindow title bar or close button.
 */
public final class KeybinderTagWindow extends TargetWindow {
    private static final int TAG_SIZE = 70;
    private static final int IMAGE_SIZE = 64;
    private static final int CLICK_SLOP = 3;
    private final KeybinderUiController controller;
    private final ResourceTexture texture = KeybinderTextureFactory.load("kb-tag.png");
    private int pressX;
    private int pressY;
    private boolean pressed;
    private boolean dragged;

    public KeybinderTagWindow(KeybinderUiController controller) {
        super();
        this.controller = controller;
        setSize(TAG_SIZE, TAG_SIZE);
    }

    @Override
    public void gameTick() {
        // TargetWindow's renderer is intentionally unused by this launcher.
    }

    @Override
    protected void renderComponent(Queue queue, float ignoredAlpha) {
        // TargetClassicRenderer uses a black outer line and a muted GUI-colour
        // inner line. Keep that native target-window silhouette around the art.
        fillRect(queue, 0.0f, 0.0f, 0.0f, 1.0f, x, y, TAG_SIZE, TAG_SIZE);
        fillRect(queue, 0.26f, 0.23f, 0.18f, 1.0f,
                x + 1, y + 1, TAG_SIZE - 2, TAG_SIZE - 2);
        drawTexture(queue, texture, 1f, 1f, 1f, 1f,
                x + 3, y + 3, IMAGE_SIZE, IMAGE_SIZE, 0, 0, 256, 256);
    }

    @Override
    protected void leftPressed(int mouseX, int mouseY, int clickCount) {
        pressed = true;
        dragged = false;
        pressX = mouseX;
        pressY = mouseY;
        super.leftPressed(mouseX, mouseY, clickCount);
    }

    @Override
    protected void mouseDragged(int mouseX, int mouseY) {
        if (pressed && (Math.abs(mouseX - pressX) > CLICK_SLOP
                || Math.abs(mouseY - pressY) > CLICK_SLOP))
            dragged = true;
        super.mouseDragged(mouseX, mouseY);
    }

    @Override
    protected void leftReleased(int mouseX, int mouseY) {
        super.leftReleased(mouseX, mouseY);
        boolean open = pressed && !dragged
                && Math.abs(mouseX - pressX) <= CLICK_SLOP
                && Math.abs(mouseY - pressY) <= CLICK_SLOP;
        pressed = false;
        if (open) controller.openFromTag();
    }
}
