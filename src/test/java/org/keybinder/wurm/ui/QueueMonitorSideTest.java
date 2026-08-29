package org.keybinder.wurm.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class QueueMonitorSideTest {
    @Test
    public void settingIsParsedDefensively() {
        assertEquals(QueueMonitorSide.RIGHT, QueueMonitorSide.fromSetting(null));
        assertEquals(QueueMonitorSide.RIGHT, QueueMonitorSide.fromSetting("unknown"));
        assertEquals(QueueMonitorSide.RIGHT, QueueMonitorSide.fromSetting(" RIGHT "));
        assertEquals(QueueMonitorSide.LEFT, QueueMonitorSide.fromSetting("Left"));
    }
}
