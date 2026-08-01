package org.keybinder.wurm.integration;

import org.junit.Test;

import java.util.function.Consumer;

import static org.junit.Assert.assertNotNull;

public class TransferFileChooserTest {
    @Test
    public void importAndExportExposeFailureCallbacksForTheAwtThread() throws Exception {
        assertNotNull(TransferFileChooser.class.getMethod(
                "chooseImport", Consumer.class, Consumer.class));
        assertNotNull(TransferFileChooser.class.getMethod(
                "chooseExport", Consumer.class, Consumer.class));
    }
}
