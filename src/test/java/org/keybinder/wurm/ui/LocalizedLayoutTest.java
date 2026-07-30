package org.keybinder.wurm.ui;

import org.junit.After;
import org.junit.Test;
import org.keybinder.wurm.i18n.Messages;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LocalizedLayoutTest {
    @After public void restoreEnglish() { Messages.select("en"); }

    @Test
    public void buttonWidthUsesMeasuredPixelsForBothLanguages() {
        final Map<String, Integer> measured = new HashMap<String, Integer>();
        measured.put("Edit", 31);
        measured.put("Delete", 41);
        measured.put("Editar", 39);
        measured.put("Excluir", 44);
        LocalizedLayout.TextMeasurer font = measured::get;

        Messages.select("en");
        assertEquals(58, LocalizedLayout.controlWidth(
                Messages.text("common.edit"), 58, 18, font));
        assertEquals(68, LocalizedLayout.controlWidth(
                Messages.text("common.delete"), 68, 18, font));

        Messages.select("pt-BR");
        assertEquals(58, LocalizedLayout.controlWidth(
                Messages.text("common.edit"), 58, 18, font));
        assertEquals(68, LocalizedLayout.controlWidth(
                Messages.text("common.delete"), 68, 18, font));
    }

    @Test
    public void dropdownWidthUsesWidestMeasuredOptionRatherThanCharacterCount() {
        final Map<String, Integer> measured = new HashMap<String, Integer>();
        measured.put("iii", 12);
        measured.put("WW", 28);
        int width = LocalizedLayout.maximumOptionWidth(
                new String[]{"iii", "WW"}, 30, measured::get);
        assertEquals(58, width);
        assertTrue(width >= measured.get("WW") + 30);
    }
}
