package com.wurmonline.client.renderer.gui;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.Opcode;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class KeybinderTagWindowTest {
    @Test
    public void rightClickIsDeclaredAsNoOpOnTagItself() throws Exception {
        Method declared = KeybinderTagWindow.class.getDeclaredMethod(
                "rightPressed", int.class, int.class, int.class);
        assertEquals(KeybinderTagWindow.class, declared.getDeclaringClass());

        ClassPool pool = ClassPool.getDefault();
        CtClass type = pool.get(KeybinderTagWindow.class.getName());
        CtMethod method = type.getDeclaredMethod("rightPressed",
                new CtClass[]{CtClass.intType, CtClass.intType, CtClass.intType});
        CodeAttribute code = method.getMethodInfo().getCodeAttribute();
        assertArrayEquals(new byte[]{(byte) Opcode.RETURN}, code.getCode());
    }
}
