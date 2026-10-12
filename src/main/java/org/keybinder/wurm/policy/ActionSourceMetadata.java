package org.keybinder.wurm.policy;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Complete explicit source flags from the server build paired with the pinned client. */
final class ActionSourceMetadata {
    private static final Map<Short, Boolean> SELECTABLE_TOOLS = load();

    private ActionSourceMetadata() { }

    /** Null means optional/unknown: retain the audited command rule or manual source. */
    static Boolean selectableTool(short actionId) {
        return SELECTABLE_TOOLS.get(actionId);
    }

    private static Map<Short, Boolean> load() {
        try (InputStream input = ActionSourceMetadata.class.getResourceAsStream(
                "/keybinder/action-source-metadata.properties")) {
            if (input == null) throw new IllegalStateException("Missing action source metadata");
            Properties flags = new Properties();
            flags.load(input);
            Map<Short, Boolean> result = new LinkedHashMap<Short, Boolean>();
            read(flags, "alwaysUseActiveItem", true, result);
            read(flags, "neverUseActiveItem", false, result);
            return Collections.unmodifiableMap(result);
        } catch (Exception failure) {
            Logger.getLogger("Chamomilo.Keybinder").log(Level.WARNING,
                    "Unable to load action source metadata; retaining command rules", failure);
            return Collections.emptyMap();
        }
    }

    private static void read(Properties flags, String key, boolean selectable,
                             Map<Short, Boolean> result) {
        String value = flags.getProperty(key);
        if (value == null || value.trim().isEmpty())
            throw new IllegalArgumentException("Missing action source group: " + key);
        for (String token : value.split(",")) {
            short id = Short.parseShort(token.trim());
            if (id < 0 || result.put(id, selectable) != null)
                throw new IllegalArgumentException("Invalid or duplicate action source ID: " + id);
        }
    }
}
