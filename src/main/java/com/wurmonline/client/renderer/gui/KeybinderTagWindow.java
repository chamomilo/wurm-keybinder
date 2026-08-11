package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.resources.textures.KeybinderTextureFactory;
import com.wurmonline.client.resources.textures.ResourceTexture;
import com.wurmonline.client.settings.WindowPosition;
import org.keybinder.wurm.ui.TagController;

/**
 * A compact launcher built directly on Wurm's static/drag component. It must
 * not extend TargetWindow: loading that class before every mod has completed
 * preInit freezes it and prevents compatible mods from installing target-name
 * hooks. The launcher deliberately has no WWindow title bar or close button.
 */
public final class KeybinderTagWindow extends StaticComponent implements WindowSerializer {
    private static final int TAG_SIZE = 70;
    private static final int IMAGE_SIZE = 64;
    private static final int CLICK_SLOP = 3;
    private final TagController controller;
    private final DragController dragger;
    private final ResourceTexture texture = KeybinderTextureFactory.load("kb-tag.png");
    private int pressX;
    private int pressY;
    private boolean pressed;
    private boolean dragged;

    public KeybinderTagWindow(TagController controller) {
        super("Keybinder tag");
        this.controller = controller;
        dragger = new DragController(this);
        setSize(TAG_SIZE, TAG_SIZE);
    }

    @Override
    public void gameTick() {
        // The launcher has no tick-driven state.
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
        dragger.leftPressed(mouseX, mouseY, clickCount);
    }

    @Override
    protected void rightPressed(int mouseX, int mouseY, int clickCount) {
        // A right click is intentionally not an interaction for this launcher.
    }

    @Override
    protected void mouseDragged(int mouseX, int mouseY) {
        if (pressed && (Math.abs(mouseX - pressX) > CLICK_SLOP
                || Math.abs(mouseY - pressY) > CLICK_SLOP))
            dragged = true;
        dragger.mouseDragged(mouseX, mouseY);
    }

    @Override
    protected void leftReleased(int mouseX, int mouseY) {
        dragger.leftReleased(mouseX, mouseY);
        boolean open = pressed && !dragged
                && Math.abs(mouseX - pressX) <= CLICK_SLOP
                && Math.abs(mouseY - pressY) <= CLICK_SLOP;
        pressed = false;
        if (open) controller.openFromTag();
    }

    @Override
    public void restorePositionHints(WindowPosition position) {
        setPosition(position.x, position.y);
        dragger.setDisabled((position.flags & 1) != 0);
        hud.toggleComponent(this, (position.flags & 2) == 0);
    }

    @Override
    public WindowPosition createPositionHints() {
        int flags = 0;
        if (dragger.isDisabled()) flags |= 1;
        if (!hud.isComponentEnabled(this)) flags |= 2;
        return new WindowPosition(x, y, flags);
    }
}
