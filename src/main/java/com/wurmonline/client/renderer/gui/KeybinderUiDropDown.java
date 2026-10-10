package com.wurmonline.client.renderer.gui;

import org.keybinder.wurm.i18n.LocalizedText;

/** Composes the final SDK dropdown while retaining selectable-row pointer hooks. */
class KeybinderUiDropDown extends WurmBorderPanel implements KeybinderLocalized {
    private final ChamomiloUiV1DropDown control;
    private String[] options;
    private final LocalizedText[] optionSources;
    KeybinderUiDropDown(String id, int selected, String[] options) {
        super(id);
        this.options = options.clone();
        optionSources = new LocalizedText[options.length];
        for (int i = 0; i < options.length; i++) optionSources[i] = LocalizedText.capture(options[i]);
        int desired = optionWidth(options);
        control = new ChamomiloUiV1DropDown(id + ".choice", desired, options, value -> {});
        control.selectIndex(selected);
        setComponent(control, CENTER);
        KeybinderUi.theme(this);
        setSize(desired, 32);
        sizeFlags = FIXED_HEIGHT;
    }
    int getValue() { return control.selectedIndex(); }
    static int optionWidth(String[] options) {
        int desired = 48;
        // The SDK reserves 36 px for field insets/arrow. Measure both weights
        // and retain a small margin, including detached lists' scrollbar.
        for (String option : options) desired = Math.max(desired,
                Math.max(KeybinderUi.body().getWidth(option), KeybinderUi.strong().getWidth(option)) + 40);
        return desired;
    }
    void setValue(int value) { control.selectIndex(value); }
    void setOptions(String[] values) {
        int selected = getValue();
        options = values.clone();
        for (int i = 0; i < values.length; i++) optionSources[i] = LocalizedText.capture(values[i]);
        control.setOptions(values);
        control.selectIndex(selected);
    }
    @Override public void relocalize() {
        KeybinderUi.closePopup(control);
        int selected = getValue();
        for (int i = 0; i < options.length; i++)
            if (optionSources[i] != null) options[i] = optionSources[i].resolve();
        control.setOptions(options); control.selectIndex(selected);
        setSize(optionWidth(options), height);
    }
    @Override public FlexComponent getComponentAt(int mx, int my) { return contains(mx, my) ? this : null; }
    @Override protected void leftPressed(int mx, int my, int count) {
        control.leftPressed(mx, my, count);
        KeybinderUi.themePopup(control);
    }
    @Override protected void mouseMoved(int mx, int my) { control.mouseMoved(mx, my); }
    @Override int getMouseCursor(int mx, int my) { return MOUSE_CURSOR_HAND; }
}
