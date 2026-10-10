package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.PickData;
import org.chamomilo.wurm.ui.v1.UiColor;

/** Native label input/tooltip semantics with the kit's shared body fonts. */
class KeybinderUiLabel extends WurmLabel {
    private String caption;
    KeybinderUiLabel(String label) { super(label); initialize(label); }
    KeybinderUiLabel(String label, String tip) { super(label, tip); initialize(label); }
    KeybinderUiLabel(String label, String tip, boolean filled) { super(label, tip, filled); initialize(label); }
    private void initialize(String label) {
        KeybinderUi.fonts(this); caption = label; super.setLabel(label);
        setSize(text.getWidth(label) + 8, text.getHeight() + 4);
    }
    static KeybinderUiLabel header(String caption) {
        KeybinderUiLabel label = new KeybinderUiLabel(caption);
        label.text = KeybinderUi.heading(); label.textBold = label.text;
        label.setLabel(caption); label.setSize(label.text.getWidth(caption) + 12, 32);
        label.sizeFlags = FIXED_HEIGHT;
        return label;
    }
    @Override void setLabel(String value) { caption = value; super.setLabel(value); }
    @Override void setLabel(String value, String tip) { caption = value; super.setLabel(value, tip); }
    @Override protected void renderComponent(Queue queue, float ignoredAlpha) {
        String display = KeybinderUi.fit(text, caption, Math.max(0, width - 8));
        text.moveTo(x + 4, y + (height - text.getHeight()) / 2 + text.getAscent());
        text.paint(queue, display, UiColor.TEXT.red, UiColor.TEXT.green, UiColor.TEXT.blue, 1f);
    }
    @Override public void pick(PickData data, int mx, int my) {
        super.pick(data, mx, my);
        if (contains(mx, my) && text.getWidth(caption) > width - 8) data.addText(caption);
    }
}
