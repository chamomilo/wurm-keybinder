package com.wurmonline.client.renderer.gui;

import javassist.ClassPool;
import javassist.CtMethod;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Opcode;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class KeybinderWindowLocalizationTest {
    @Test
    public void openingKeybindsRefreshesLabelsFromActiveDictionary() throws Exception {
        CtMethod method = ClassPool.getDefault()
                .get(KeybinderWindow.class.getName())
                .getDeclaredMethod("showKeybinds");
        CodeIterator code = method.getMethodInfo().getCodeAttribute().iterator();
        ConstPool constants = method.getMethodInfo().getConstPool();
        boolean rebuildsListTop = false;
        while (code.hasNext()) {
            int position = code.next();
            if (code.byteAt(position) != Opcode.INVOKESPECIAL) continue;
            int methodRef = code.u16bitAt(position + 1);
            if (KeybinderWindow.class.getName().equals(
                    constants.getMethodrefClassName(methodRef))
                    && "rebuildListTop".equals(constants.getMethodrefName(methodRef))) {
                rebuildsListTop = true;
                break;
            }
        }
        assertTrue(rebuildsListTop);
    }
}
