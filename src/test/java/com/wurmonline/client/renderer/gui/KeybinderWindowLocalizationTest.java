package com.wurmonline.client.renderer.gui;

import javassist.ClassPool;
import javassist.CtMethod;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Opcode;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

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

    @Test
    public void wurmImportButtonUsesUiConfirmationInsteadOfConsoleProtocol() throws Exception {
        CtMethod method = ClassPool.getDefault()
                .get(KeybinderWindow.class.getName())
                .getDeclaredMethod("buttonClicked");
        CodeIterator code = method.getMethodInfo().getCodeAttribute().iterator();
        ConstPool constants = method.getMethodInfo().getConstPool();
        boolean confirmsImport = false;
        boolean startsConsoleReview = false;
        while (code.hasNext()) {
            int position = code.next();
            if (code.byteAt(position) != Opcode.INVOKEINTERFACE) continue;
            int methodRef = code.u16bitAt(position + 1);
            if (!"org.keybinder.wurm.ui.KeybinderUiController".equals(
                    constants.getInterfaceMethodrefClassName(methodRef))) continue;
            String name = constants.getInterfaceMethodrefName(methodRef);
            if ("confirmImport".equals(name)) confirmsImport = true;
            if ("requestImport".equals(name)) startsConsoleReview = true;
        }
        assertTrue(confirmsImport);
        assertFalse(startsConsoleReview);
    }
}
