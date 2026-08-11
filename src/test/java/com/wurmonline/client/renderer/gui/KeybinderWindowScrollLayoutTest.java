package com.wurmonline.client.renderer.gui;

import javassist.ClassPool;
import javassist.CtMethod;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Opcode;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class KeybinderWindowScrollLayoutTest {
    @Test
    public void scrollTableDoesNotUseReentrantAutoWidthLayout() throws Exception {
        CtMethod method = ClassPool.getDefault()
                .get(KeybinderWindow.class.getName())
                .getDeclaredMethod("createScrollableTable");
        CodeIterator code = method.getMethodInfo().getCodeAttribute().iterator();
        ConstPool constants = method.getMethodInfo().getConstPool();
        boolean ordinaryVerticalPanel = false;
        boolean autoWidthPanel = false;
        while (code.hasNext()) {
            int position = code.next();
            if (code.byteAt(position) != Opcode.INVOKESPECIAL) continue;
            int methodRef = code.u16bitAt(position + 1);
            if (!"com.wurmonline.client.renderer.gui.WurmArrayPanel".equals(
                    constants.getMethodrefClassName(methodRef))) continue;
            String descriptor = constants.getMethodrefType(methodRef);
            if ("(Ljava/lang/String;I)V".equals(descriptor)) ordinaryVerticalPanel = true;
            if ("(Ljava/lang/String;IZ)V".equals(descriptor)) autoWidthPanel = true;
        }
        assertTrue(ordinaryVerticalPanel);
        assertFalse(autoWidthPanel);
    }
}
