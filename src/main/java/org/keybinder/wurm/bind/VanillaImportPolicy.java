package org.keybinder.wurm.bind;

import org.keybinder.wurm.catalog.VanillaKeybindCatalog;

/** Keeps fundamental native controls outside Keybinder ownership. */
public final class VanillaImportPolicy {
    private final VanillaKeybindCatalog catalog;

    public VanillaImportPolicy() {
        this(new VanillaKeybindCatalog());
    }

    VanillaImportPolicy(VanillaKeybindCatalog catalog) {
        this.catalog = catalog;
    }

    public boolean mayImport(String command) {
        VanillaKeybindCatalog.Category category = catalog.categoryFor(command);
        String unquoted = unquote(command);
        if (category == null) category = catalog.categoryFor(unquoted);
        if (category == null && !unquoted.isEmpty())
            category = catalog.categoryFor("\"" + unquoted + "\"");
        return category == null || !category.usesNativeCompatibility();
    }

    static String unquote(String command) {
        if (command == null) return "";
        String value = command.trim();
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\""))
            return value.substring(1, value.length() - 1);
        return value;
    }
}
