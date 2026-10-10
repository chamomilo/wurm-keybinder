package com.wurmonline.client.renderer.gui;

/** Wurm omits diagnostic component names in normal builds; retain logical typography group IDs. */
class KeybinderUiArrayPanel<T extends FlexComponent> extends WurmArrayPanel<T> {
    private final int direction;
    KeybinderUiArrayPanel(String id, int direction) { super(id, direction); this.direction = direction; KeybinderUi.identify(this, id); }
    KeybinderUiArrayPanel(String id, int direction, boolean autoWidth) { super(id, direction, autoWidth); this.direction = direction; KeybinderUi.identify(this, id); }
    KeybinderUiArrayPanel(String id, int direction, int width, int height) { super(id, direction, width, height); this.direction = direction; KeybinderUi.identify(this, id); }

    @Override void performLayout() {
        super.performLayout();
        // Native horizontal layout places fixed-height controls at the top.
        // Centre after every layout, including moves caused by wheel scrolling.
        if (direction == DIR_HORIZONTAL && components != null)
            for (FlexComponent child : components)
                child.setPosition(child.x, y + Math.max(0, (height - child.height) / 2));
    }
}
