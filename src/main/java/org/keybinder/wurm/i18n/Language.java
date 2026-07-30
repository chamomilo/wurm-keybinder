package org.keybinder.wurm.i18n;

public enum Language {
    ENGLISH("en", "language.en"),
    PORTUGUESE_BRAZIL("pt-BR", "language.pt_BR");

    private final String code;
    private final String displayKey;

    Language(String code, String displayKey) {
        this.code = code;
        this.displayKey = displayKey;
    }

    public String getCode() { return code; }
    public String getDisplayName() { return Messages.text(displayKey); }

    public static Language fromCode(String code) {
        if (code != null)
            for (Language language : values())
                if (language.code.equalsIgnoreCase(code.trim())) return language;
        return ENGLISH;
    }

    public static String[] displayNames() {
        Language[] values = values();
        String[] result = new String[values.length];
        for (int i = 0; i < values.length; i++) result[i] = values[i].getDisplayName();
        return result;
    }
}
