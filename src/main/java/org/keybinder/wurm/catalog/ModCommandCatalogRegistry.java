package org.keybinder.wurm.catalog;

import org.gotti.wurmunlimited.modloader.interfaces.ModEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/** Collects optional command catalogs from every initialized client mod. */
public final class ModCommandCatalogRegistry {
    private static final Map<String, ModCommandCatalog> CATALOGS =
            new LinkedHashMap<String, ModCommandCatalog>();

    private ModCommandCatalogRegistry() { }

    /** Feed this from Keybinder's ModListener callback. Unsupported mods are ignored. */
    public static synchronized void modInitialized(ModEntry<?> entry) {
        if (entry == null || entry.getWurmMod() == null) return;
        Object mod = entry.getWurmMod();
        ModCommandCatalog catalog = ModCommandCatalog.load(
                mod.getClass(), displayName(entry));
        if (catalog.isVisible())
            CATALOGS.put(catalog.getProviderClassName(), catalog);
    }

    /** Stable, sorted snapshot used for the lifetime of one editor window. */
    public static synchronized List<ModCommandCatalog> snapshot() {
        List<ModCommandCatalog> result =
                new ArrayList<ModCommandCatalog>(CATALOGS.values());
        Collections.sort(result, new Comparator<ModCommandCatalog>() {
            @Override public int compare(ModCommandCatalog left, ModCommandCatalog right) {
                int byName = left.getDisplayName().compareToIgnoreCase(right.getDisplayName());
                return byName != 0 ? byName
                        : left.getProviderClassName().compareTo(right.getProviderClassName());
            }
        });
        return Collections.unmodifiableList(result);
    }

    static synchronized void clearForTests() { CATALOGS.clear(); }

    private static String displayName(ModEntry<?> entry) {
        Properties properties = entry.getProperties();
        String name = properties == null ? "" : trim(properties.getProperty("updateName"));
        if (name.isEmpty()) name = trim(entry.getName());
        return name;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
