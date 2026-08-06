package org.keybinder.wurm.integration;

import com.wurmonline.client.renderer.gui.CreationListItem;
import com.wurmonline.client.renderer.gui.KeybinderCreationListBridge;
import org.keybinder.wurm.catalog.CreationSkillEntry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/** Captures the item-to-primary-skill rows as the client receives recipes. */
public final class CreationSkillRegistry {
    private static final Logger LOGGER = Logger.getLogger("Chamomilo.Keybinder");
    private static final Map<String, CreationSkillEntry> ENTRIES =
            new LinkedHashMap<String, CreationSkillEntry>();

    private CreationSkillRegistry() { }

    public static synchronized void observe(CreationListItem item) {
        if (item == null) return;
        String name = KeybinderCreationListBridge.itemName(item);
        String skill = item.getSkill();
        if (blank(name) || blank(skill)) return;
        name = name.trim();
        skill = skill.trim();
        ENTRIES.put(name + '\u0000' + skill,
                new CreationSkillEntry(name, skill));
        int size = ENTRIES.size();
        if (size == 1 || size % 100 == 0)
            LOGGER.info("[Keybinder] [Smart Improve diagnostic] received "
                    + size + " creation-skill rows; latest=\"" + name
                    + "\" -> \"" + skill + "\"");
    }

    public static synchronized List<CreationSkillEntry> snapshot() {
        return new ArrayList<CreationSkillEntry>(ENTRIES.values());
    }

    public static synchronized int size() {
        return ENTRIES.size();
    }

    public static synchronized void clear() {
        ENTRIES.clear();
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
