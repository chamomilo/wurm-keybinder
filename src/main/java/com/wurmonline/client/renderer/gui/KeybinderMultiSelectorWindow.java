package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.WurmClientBase;
import org.keybinder.wurm.KeybinderMod;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindVariant;

import java.util.LinkedHashMap;
import java.util.Map;

/** Small HUD selector shown after holding a multi-purpose keybind. */
public final class KeybinderMultiSelectorWindow extends WWindow implements ButtonListener {
    private static final int WINDOW_HORIZONTAL_CHROME = 6;
    private static final int WINDOW_VERTICAL_CHROME = 25;
    private final String recordId;
    private final Map<WButton, String> variants = new LinkedHashMap<WButton, String>();
    private boolean centered;

    public KeybinderMultiSelectorWindow(KeybindRecord record) {
        super("keybinder.multi.selector", false);
        this.recordId = record.getId();
        setTitle(record.getName());
        resizable = false;
        WurmArrayPanel<FlexComponent> content =
                new WurmArrayPanel<FlexComponent>("keybinder.multi.options",
                        WurmArrayPanel.DIR_VERTICAL, true);
        content.componentWidthOffset = 2;
        WurmLabel prompt = new WurmLabel("Pick desired action:");
        content.addComponent(prompt);
        int widest = Math.max(prompt.width, new WurmLabel(record.getName()).width + 30);
        int index = 0;
        for (KeybindVariant variant : record.getVariants()) {
            String label = variant.getSubName().trim();
            if (label.isEmpty()) label = index == 0 ? "Default action" : "Alternative action " + index;
            if (variant.getId().equals(record.getActiveVariantId())) label = "> " + label;
            WButton button = new WButton(label, this);
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
        }
    }

    @Override public void buttonClicked(WButton button) {
        String variantId = variants.get(button);
        if (variantId != null) KeybinderMod.chooseMultiVariant(recordId, variantId);
    }

    @Override protected void closePressed() {
        KeybinderMod.closeMultiSelector();
    }
}
