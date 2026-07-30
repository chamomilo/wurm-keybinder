package com.wurmonline.client.renderer.gui;

import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertFalse;

public class KeybinderEditorVanillaActionTest {
    @Test
    public void editorHasNoManualNumericActionIdInput() {
        for (Class<?> nested : KeybinderEditorWindow.class.getDeclaredClasses()) {
            if (!"ActionRow".equals(nested.getSimpleName())) continue;
            for (Field field : nested.getDeclaredFields())
                assertFalse("Numeric action identity must remain internal, not an input field",
                        "id".equals(field.getName())
                                && WurmInputField.class.isAssignableFrom(field.getType()));
        }
    }
}
