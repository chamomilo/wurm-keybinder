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
            if (!"com.wurmonline.client.renderer.gui.KeybinderUiArrayPanel".equals(
                    constants.getMethodrefClassName(methodRef))) continue;
            String descriptor = constants.getMethodrefType(methodRef);
            if ("(Ljava/lang/String;I)V".equals(descriptor)) ordinaryVerticalPanel = true;
            if ("(Ljava/lang/String;IZ)V".equals(descriptor)) autoWidthPanel = true;
        }
        assertTrue(ordinaryVerticalPanel);
        assertFalse(autoWidthPanel);
        // The themed adapter retains the exact non-auto-width native constructor.
        javassist.CtConstructor constructor = ClassPool.getDefault()
                .get(KeybinderUiArrayPanel.class.getName())
                .getDeclaredConstructor(new javassist.CtClass[]{ClassPool.getDefault().get("java.lang.String"), javassist.CtClass.intType});
        CodeIterator adapter = constructor.getMethodInfo().getCodeAttribute().iterator();
        ConstPool adapterConstants = constructor.getMethodInfo().getConstPool();
        boolean nativeOrdinary = false;
        while (adapter.hasNext()) {
            int position = adapter.next();
            if (adapter.byteAt(position) != Opcode.INVOKESPECIAL) continue;
            int reference = adapter.u16bitAt(position + 1);
            if ("com.wurmonline.client.renderer.gui.WurmArrayPanel".equals(adapterConstants.getMethodrefClassName(reference)))
                nativeOrdinary |= "(Ljava/lang/String;I)V".equals(adapterConstants.getMethodrefType(reference));
        }
        assertTrue(nativeOrdinary);
    }
}
