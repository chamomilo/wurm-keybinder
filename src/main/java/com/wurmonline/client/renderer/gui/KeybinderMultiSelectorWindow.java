package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.WurmClientBase;
import org.keybinder.wurm.KeybinderMod;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindVariant;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.ui.CursorWarpCoordinates;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import java.util.LinkedHashMap;
import java.util.Map;

/** Small HUD selector shown after holding a multi-purpose keybind. */
public final class KeybinderMultiSelectorWindow extends WWindow implements ButtonListener {
    private static final int WINDOW_HORIZONTAL_CHROME = 6;
    private static final int WINDOW_VERTICAL_CHROME = 25;
    private final String recordId;
    private final Map<WButton, String> variants = new LinkedHashMap<WButton, String>();
    private final boolean hudSelection;
    private final int originalMouseX;
    private final int originalMouseY;
    private WButton activeButton;
    private boolean centered;
    private boolean warpAttempted;

    public KeybinderMultiSelectorWindow(KeybindRecord record, boolean hudSelection,
                                        int originalMouseX, int originalMouseY) {
        super("keybinder.multi.selector", false);
        this.recordId = record.getId();
        this.hudSelection = hudSelection;
        this.originalMouseX = originalMouseX;
        this.originalMouseY = originalMouseY;
        setTitle(record.getName());
        resizable = false;
        WurmArrayPanel<FlexComponent> content =
                new WurmArrayPanel<FlexComponent>("keybinder.multi.options",
                        WurmArrayPanel.DIR_VERTICAL, true);
        content.componentWidthOffset = 2;
        WurmLabel prompt = new WurmLabel(Messages.text("multi.pick"));
        content.addComponent(prompt);
        int widest = Math.max(prompt.width, new WurmLabel(record.getName()).width + 30);
        int index = 0;
        for (KeybindVariant variant : record.getVariants()) {
            String label = variant.getSubName().trim();
            if (label.isEmpty()) label = hudSelection
                    ? Messages.text("multi.variant", index + 1)
                    : index == 0 ? Messages.text("multi.default")
                    : Messages.text("multi.alternative", index);
            boolean active = !hudSelection
                    && variant.getId().equals(record.getActiveVariantId());
            if (active) label = "> " + label;
            WButton button = new WButton(label, this);
            if (active) activeButton = button;
            widest = Math.max(widest, button.width);
            variants.put(button, variant.getId());
            content.addComponent(button);
            index++;
        }
        prompt.setSize(widest, prompt.height);
        for (WButton button : variants.keySet()) button.setSize(widest, button.height);
        content.componentResized();
        setComponent(content);
        setInitialSize(widest + WINDOW_HORIZONTAL_CHROME,
                content.calcHeight() + WINDOW_VERTICAL_CHROME, false);
    }

    @Override public void buttonPressed(WButton button) { }

    @Override public void gameTick() {
        super.gameTick();
        if (!centered && hud != null) {
            centered = true;
            setPosition(Math.max(0, (WurmClientBase.getGameWindow().getWidth() - width) / 2),
                    Math.max(0, (WurmClientBase.getGameWindow().getHeight() - height) / 2));
            return;
        }
        if (centered && !warpAttempted) {
            warpAttempted = true;
            try {
                WButton target = activeButton;
                if (target == null && !variants.isEmpty()) target = variants.keySet().iterator().next();
                if (target != null && Mouse.isCreated() && Mouse.isInsideWindow()) {
                    CursorWarpCoordinates.Point point = CursorWarpCoordinates.fromGuiCenter(
                            target.x, target.y, target.width, target.height,
                            Display.getWidth(), Display.getHeight());
                    Mouse.setCursorPosition(point.getX(), point.getY());
                }
            } catch (RuntimeException failure) {
                KeybinderMod.debugMultiPointerWarp(failure);
            }
        }
    }

    @Override public void buttonClicked(WButton button) {
        String variantId = variants.get(button);
        if (variantId != null)
            KeybinderMod.chooseMultiVariant(recordId, variantId, hudSelection);
    }

    @Override protected void closePressed() {
        KeybinderMod.closeMultiSelector();
    }

    public boolean selectsRecord(String id) {
        return id != null && id.equals(recordId);
    }

    /** Restores the world/inventory hover displaced by the selector's pointer warp. */
    public void restoreOriginalPointer() {
        if (!hudSelection || !Mouse.isCreated() || !Mouse.isInsideWindow()) return;
        CursorWarpCoordinates.Point point = CursorWarpCoordinates.fromGuiPoint(
                originalMouseX, originalMouseY, Display.getWidth(), Display.getHeight());
        Mouse.setCursorPosition(point.getX(), point.getY());
    }

    public int getOriginalMouseX() { return originalMouseX; }
    public int getOriginalMouseY() { return originalMouseY; }
}
