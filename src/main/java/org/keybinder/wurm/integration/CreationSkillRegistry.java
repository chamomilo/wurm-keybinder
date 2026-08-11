package org.keybinder.wurm.integration;

import com.wurmonline.client.renderer.gui.CreationListItem;
import com.wurmonline.client.renderer.gui.KeybinderCreationListBridge;
import org.keybinder.wurm.catalog.CreationSkillEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/** Captures the item-to-primary-skill rows as the client receives recipes. */
public final class CreationSkillRegistry {
    private static final Logger LOGGER = Logger.getLogger("Chamomilo.Keybinder");
    private static final Map<String, CreationSkillEntry> ENTRIES =
            new LinkedHashMap<String, CreationSkillEntry>();
    private static long revision;

    private CreationSkillRegistry() { }

    public static synchronized void observe(CreationListItem item) {
        if (item == null) return;
        String name = KeybinderCreationListBridge.itemName(item);
        String skill = item.getSkill();
        if (blank(name) || blank(skill)) return;
        name = name.trim();
        skill = skill.trim();
        String key = name + '\u0000' + skill;
        if (ENTRIES.containsKey(key)) return;
        ENTRIES.put(key, new CreationSkillEntry(name, skill));
        revision++;
        int size = ENTRIES.size();
        if (size == 1 || size % 100 == 0)
            LOGGER.info("[Keybinder] [Smart Improve diagnostic] received "
                    + size + " creation-skill rows; latest=\"" + name
                    + "\" -> \"" + skill + "\"");
    }

    public static synchronized List<CreationSkillEntry> snapshot() {
        return new ArrayList<CreationSkillEntry>(ENTRIES.values());
    }

    /**
     * Returns an immutable snapshot only when the catalog changed. This lets
     * Smart Improve reuse its normalized lookup instead of rebuilding every
     * recipe row on each key press.
     */
    public static synchronized Snapshot snapshotAfter(long knownRevision) {
        if (knownRevision == revision) return null;
        return new Snapshot(revision, new ArrayList<CreationSkillEntry>(
                ENTRIES.values()));
    }

    public static synchronized int size() {
        return ENTRIES.size();
    }

    public static synchronized void clear() {
        ENTRIES.clear();
        revision++;
    }

    public static final class Snapshot {
        private final long revision;
        private final List<CreationSkillEntry> entries;

        private Snapshot(long revision, List<CreationSkillEntry> entries) {
            this.revision = revision;
            this.entries = Collections.unmodifiableList(entries);
        }

        public long getRevision() { return revision; }
        public List<CreationSkillEntry> getEntries() { return entries; }
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
