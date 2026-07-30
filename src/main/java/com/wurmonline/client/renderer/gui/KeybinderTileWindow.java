package com.wurmonline.client.renderer.gui;

import org.keybinder.wurm.KeybinderMod;
import org.keybinder.wurm.ui.KeybindEditorController;
import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.resources.textures.KeybinderTextureFactory;
import com.wurmonline.client.resources.textures.ResourceTexture;
import org.keybinder.wurm.i18n.Messages;

public final class KeybinderTileWindow extends WWindow implements ButtonListener {
    private static final int SELECTOR_SIZE = 300;
    private static final String[] TARGETS = {
            "tile_nw", "tile_n", "tile_ne", "tile_w", "tile",
            "tile_e", "tile_sw", "tile_s", "tile_se"
    };
    private final KeybindEditorController controller;
    private final WButton area;

    public KeybinderTileWindow(KeybindEditorController controller) {
        super("keybinder.tiles", false);
        this.controller = controller;
        setTitle(Messages.text("tile.title"));

        TileSelector selector = new TileSelector();
        selector.setSize(SELECTOR_SIZE, SELECTOR_SIZE);
        area = new WButton(Messages.text("tile.area"), this);
        area.setHoverString(Messages.text("tile.area.tip"));
        area.setSize(SELECTOR_SIZE, area.height);

        WurmBorderPanel root = new WurmBorderPanel("keybinder.tiles.root");
        root.setComponent(selector, WurmBorderPanel.CENTER);
        root.setComponent(area, WurmBorderPanel.SOUTH);
        setComponent(root);
        setInitialSize(SELECTOR_SIZE + 34, SELECTOR_SIZE + 76, false);
    }

    private void select(String target) {
        controller.requestTargetSelection(target);
        KeybinderMod.deferUi(() -> hud.hideComponent(this));
    }

    @Override public void buttonPressed(WButton button) {}

    @Override public void buttonClicked(WButton button) {
        if (button == area) {
            select("area");
        }
    }

    @Override protected void closePressed() { KeybinderMod.deferUi(() -> hud.hideComponent(this)); }

    private final class TileSelector extends FlexComponent {
        private final ResourceTexture texture =
                KeybinderTextureFactory.load("tile-selector.png");
        private int hovered = -1;

        private TileSelector() {
            super("keybinder.tiles.image");
        }

        @Override
        protected void renderComponent(Queue queue, float alpha) {
            // WurmComponent UV coordinates use a fixed 0..256 range regardless
            // of the source image's pixel dimensions.
            drawTexture(queue, texture, 1f, 1f, 1f, 1f,
                    x, y, width, height, 0, 0, 256, 256);
            if (hovered >= 0) {
                int column = hovered % 3;
                int row = hovered / 3;
                int left = x + column * width / 3;
                int top = y + row * height / 3;
                int right = x + (column + 1) * width / 3;
                int bottom = y + (row + 1) * height / 3;
                fillRect(queue, 0.82f, 0.67f, 0.30f, 0.22f,
                        left, top, right - left, bottom - top);
            }
        }

        @Override
        void mouseMoved(int mouseX, int mouseY) {
            hovered = cellAt(mouseX, mouseY);
        }

        @Override
        void mouseExited() {
            hovered = -1;
        }

        @Override
        int getMouseCursor(int mouseX, int mouseY) {
            return cellAt(mouseX, mouseY) < 0 ? MOUSE_CURSOR_NORMAL : MOUSE_CURSOR_HAND;
        }

        @Override
        void leftPressed(int mouseX, int mouseY, int clickCount) {
            int cell = cellAt(mouseX, mouseY);
            if (cell >= 0) select(TARGETS[cell]);
        }

        private int cellAt(int mouseX, int mouseY) {
            if (mouseX < x || mouseY < y || mouseX >= x + width || mouseY >= y + height)
                return -1;
            int column = Math.min(2, (mouseX - x) * 3 / width);
            int row = Math.min(2, (mouseY - y) * 3 / height);
            return row * 3 + column;
        }
    }
}
