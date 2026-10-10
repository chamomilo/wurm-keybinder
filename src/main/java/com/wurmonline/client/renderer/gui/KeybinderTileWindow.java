package com.wurmonline.client.renderer.gui;

import org.keybinder.wurm.KeybinderMod;
import org.keybinder.wurm.ui.KeybindEditorController;
import com.wurmonline.client.renderer.backend.Queue;
import org.keybinder.wurm.i18n.Messages;

public final class KeybinderTileWindow extends KeybinderUiWindow implements ButtonListener {
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
        area = new KeybinderUiButton(Messages.text("tile.area"), this);
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
        private final KeybinderUiButton[] cells = new KeybinderUiButton[9];

        private TileSelector() {
            super("keybinder.tiles.grid");
            KeybinderUi.identify(this, "keybinder.tiles.grid");
            String[] captions = {"NW", "N", "NE", "W", "C", "E", "SW", "S", "SE"};
            for (int index = 0; index < cells.length; index++) {
                final int target = index;
                cells[index] = new KeybinderUiButton(captions[index], new ButtonListener() {
                    @Override public void buttonPressed(WButton button) {}
                    @Override public void buttonClicked(WButton button) { select(TARGETS[target]); }
                });
                cells[index].captionCeiling(32);
                cells[index].parent = this;
            }
        }

        @Override
        protected void renderComponent(Queue queue, float alpha) {
            for (KeybinderUiButton cell : cells) cell.render(queue, 1f);
        }
        @Override void componentResized() {
            if (cells == null || cells[0] == null) return;
            for (int index = 0; index < cells.length; index++) {
                int column = index % 3, row = index / 3;
                cells[index].setSize(Math.max(32, width / 3 - 4), Math.max(32, height / 3 - 4));
                cells[index].setPosition(x + column * width / 3 + 2, y + row * height / 3 + 2);
            }
            KeybinderUiButton.fitGroup("keybinder.tiles.directions", java.util.Arrays.asList(cells));
        }
        @Override public FlexComponent getComponentAt(int mx, int my) {
            for (KeybinderUiButton cell : cells) if (cell.contains(mx, my)) return cell;
            return contains(mx, my) ? this : null;
        }
    }
}
