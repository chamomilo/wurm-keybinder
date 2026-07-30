package org.keybinder.wurm.queue;

import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.gui.MindLogicCalculator;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;

import java.lang.reflect.Field;

public final class QueueLimitService {
    private Field calculatorField;

    public int readLimit(HeadsUpDisplay hud) {
        if (hud == null) return -1;
        try {
            if (calculatorField == null) {
                calculatorField = HeadsUpDisplay.class.getDeclaredField("mindLogicCalculator");
            }
            MindLogicCalculator calculator = ReflectionUtil.getPrivateField(hud, calculatorField);
            return calculator == null ? -1 : calculator.getMaxNumberOfActions();
        } catch (ReflectiveOperationException e) {
            return -1;
        }
    }
}
