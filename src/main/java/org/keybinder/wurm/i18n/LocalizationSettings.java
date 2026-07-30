package org.keybinder.wurm.i18n;

import java.util.Properties;

public final class LocalizationSettings {
    public static final String LANGUAGE_KEY = "language";

    private LocalizationSettings() {}

    public static String load(Properties properties) {
        return properties == null ? Language.ENGLISH.getCode()
                : Language.fromCode(properties.getProperty(LANGUAGE_KEY)).getCode();
    }

    public static void save(Properties properties, String code) {
        properties.setProperty(LANGUAGE_KEY, Language.fromCode(code).getCode());
    }
}
