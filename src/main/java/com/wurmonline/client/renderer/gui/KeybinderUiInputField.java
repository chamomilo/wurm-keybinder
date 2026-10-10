package com.wurmonline.client.renderer.gui;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Kit field around Wurm's real editing child: focus, selection and clipboard stay native. */
final class KeybinderUiInputField extends WurmBorderPanel {
    private final ChamomiloUiV1TextField control;
    KeybinderUiInputField(String id, KeybinderUiInputListener listener) {
        super(id);
        control = new ChamomiloUiV1TextField(id + ".field", 180, 2048,
                value -> listener.handleInputChanged(this, value), listener::handleInput);
        setComponent(control, CENTER);
        KeybinderUi.theme(this);
        for (WurmComponent child : KeybinderUi.tree(control)) if (child instanceof WurmInputField) {
            WurmInputField input = (WurmInputField) child;
            input.setMaxLines(1);
            try {
                Field field = WurmInputField.class.getDeclaredField("inputFieldListener");
                if (field.getType() != InputFieldListener.class) throw new NoSuchFieldException("inputFieldListener signature");
                field.setAccessible(true);
                final InputFieldListener delegate = (InputFieldListener) field.get(input);
                field.set(input, new InputFieldListener() {
                    @Override public void handleInput(String value) { delegate.handleInput(value); }
                    @Override public void handleInputChanged(WurmInputField field, String value) { delegate.handleInputChanged(field, value); }
                    @Override public void handleEscape(WurmInputField field) {
                        delegate.handleEscape(field); listener.handleEscape(KeybinderUiInputField.this);
                    }
                });
            } catch (ReflectiveOperationException | RuntimeException failure) {
                Logger.getLogger(KeybinderUiInputField.class.getName()).log(Level.WARNING,
                        "Cannot adapt Keybinder field Escape; native editing remains available", failure);
            }
        }
        control.resize(180, 32);
        setSize(180, 32); sizeFlags = FIXED_HEIGHT;
    }
    String getText() { return control.value(); }
    void setText(String value) { control.setText(value); }
    void setInvalid(boolean value) { control.setInvalid(value); }
    void setMaxInput(int maximum) {
        for (WurmComponent child : KeybinderUi.tree(control)) if (child instanceof WurmInputField)
            ((WurmInputField) child).setMaxInput(maximum);
    }
    @Override void componentResized() {
        if (control != null) control.resize(Math.max(32, width), Math.max(32, height));
        super.componentResized();
    }
}

interface KeybinderUiInputListener {
    void handleInput(String input);
    void handleInputChanged(KeybinderUiInputField field, String input);
    void handleEscape(KeybinderUiInputField field);
}
