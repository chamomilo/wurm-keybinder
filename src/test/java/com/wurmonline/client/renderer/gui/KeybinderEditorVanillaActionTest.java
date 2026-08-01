package com.wurmonline.client.renderer.gui;

import javassist.ClassPool;
import javassist.CtMethod;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Opcode;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void doneFlushesLatestDropdownSelectionBeforeBuildingSteps() throws Exception {
        CtMethod save = ClassPool.getDefault().get(KeybinderEditorWindow.class.getName())
                .getDeclaredMethod("save");
        CodeIterator code = save.getMethodInfo().getCodeAttribute().iterator();
        ConstPool constants = save.getMethodInfo().getConstPool();
        boolean synchronizes = false;
        while (code.hasNext()) {
            int position = code.next();
            int opcode = code.byteAt(position);
            if (opcode != Opcode.INVOKESPECIAL && opcode != Opcode.INVOKEVIRTUAL) continue;
            int methodRef = code.u16bitAt(position + 1);
            if (KeybinderEditorWindow.class.getName().equals(
                    constants.getMethodrefClassName(methodRef))
                    && "updateEditorState".equals(constants.getMethodrefName(methodRef))) {
                synchronizes = true;
                break;
            }
        }
        assertTrue("Done must flush a just-selected Nearby value before save", synchronizes);
    }
}
