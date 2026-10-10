package com.wurmonline.client.renderer.gui;

/** Composes the final SDK dropdown while retaining selectable-row pointer hooks. */
class KeybinderUiDropDown extends WurmBorderPanel {
    private final ChamomiloUiV1DropDown control;
    KeybinderUiDropDown(String id, int selected, String[] options) {
        super(id);
        int desired = 48;
        for (String option : options) desired = Math.max(desired, KeybinderUi.body().getWidth(option) + 30);
        control = new ChamomiloUiV1DropDown(id + ".choice", desired, options, value -> {});
        control.selectIndex(selected);
        setComponent(control, CENTER);
        KeybinderUi.theme(this);
        setSize(desired, 32);
        sizeFlags = FIXED_HEIGHT;
    }
    int getValue() { return control.selectedIndex(); }
    void setValue(int value) { control.selectIndex(value); }
    @Override public FlexComponent getComponentAt(int mx, int my) { return contains(mx, my) ? this : null; }
    @Override protected void leftPressed(int mx, int my, int count) {
        control.leftPressed(mx, my, count);
        KeybinderUi.themePopup(control);
    }
    @Override protected void mouseMoved(int mx, int my) { control.mouseMoved(mx, my); }
    @Override int getMouseCursor(int mx, int my) { return MOUSE_CURSOR_HAND; }
}
