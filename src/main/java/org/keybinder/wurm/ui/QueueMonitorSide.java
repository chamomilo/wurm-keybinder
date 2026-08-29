package org.keybinder.wurm.ui;

import java.util.Locale;

/** Persisted screen edge used by the compact action-queue monitor. */
public enum QueueMonitorSide {
    RIGHT("right"),
    LEFT("left");

    private final String setting;

    QueueMonitorSide(String setting) {
        this.setting = setting;
    }

    public String getSetting() {
        return setting;
    }

    public static QueueMonitorSide fromSetting(String value) {
        if (value == null) return RIGHT;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (QueueMonitorSide side : values())
            if (side.setting.equals(normalized)) return side;
        return RIGHT;
    }
}
