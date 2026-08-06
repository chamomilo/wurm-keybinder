package org.keybinder.wurm.command;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * One selected world item's Examine-derived state. This is metadata capture,
 * not an execution lock: Improve requests never wait for or reserve this state.
 */
public final class WorldImproveTracker {
    private static final long NONE = Long.MIN_VALUE;
    private long targetId = NONE;
    private RequirementFamily requirement;
    private boolean damaged;
    private Float quality;
    private String examineText;
    private byte rarity;
    private float rarityRuneModifier;
    private final Set<String> rarityRuneMessages = new HashSet<String>();
    private boolean descriptionConfirmed;

    public synchronized void examineSent(long id) {
        if (id <= 0) {
            clear();
            return;
        }
        // Wurm may send the double-click Examine just before SelectBar finishes
        // switching to the clicked item. The Event and execution paths perform
        // the authoritative selected-ID check after that transition.
        targetId = id;
        requirement = null;
        damaged = false;
        quality = null;
        examineText = null;
        rarity = 0;
        rarityRuneModifier = 0.0f;
        rarityRuneMessages.clear();
        descriptionConfirmed = false;
    }

    public synchronized void selectionChanged(long selectedId) {
        // SelectBar may represent a hitched vehicle through a different client
        // object than the ground-item ID used by Examine/sendAction. The outgoing
        // Examine target remains authoritative; execution validates that exact ID.
    }

    public synchronized void event(long selectedId, String context, String message) {
        event(context, message);
    }

    public synchronized void event(String context, String message) {
        if (context == null || !":event".equalsIgnoreCase(context.trim())) return;
        if (targetId == NONE) return;
        WorldImproveEventParser.Parsed parsed = WorldImproveEventParser.parse(message);
        if (!descriptionConfirmed) {
            // DEFAULT_ACTION is only a possible Examine. Most world-object
            // descriptions carry Ql/Dam, but portable items placed in the
            // world can omit both and report only their standard Improve
            // requirement (for example a butchering knife needing water).
            if (!WorldImproveEventParser.isExamineDescription(message)
                    && parsed.getRequirement() == null) return;
            descriptionConfirmed = true;
            examineText = message;
        }
        if (parsed.getRequirement() != null) requirement = parsed.getRequirement();
        if (parsed.getDamaged() != null) damaged = parsed.getDamaged();
        if (parsed.getQuality() != null) quality = parsed.getQuality();
        if (parsed.getRarity() != null) rarity = parsed.getRarity();
        if (parsed.getRarityRuneModifier() != null) {
            String key = message.trim().toLowerCase(Locale.ENGLISH);
            if (rarityRuneMessages.add(key))
                rarityRuneModifier += parsed.getRarityRuneModifier();
        }
    }

    public synchronized Snapshot snapshot(long selectedId) {
        if (!descriptionConfirmed || targetId == NONE || selectedId != targetId
                || requirement == null) return null;
        return new Snapshot(targetId, requirement, damaged, quality, examineText,
                rarity, rarityRuneModifier);
    }

    public synchronized Snapshot currentSnapshot() {
        if (!descriptionConfirmed || targetId == NONE || requirement == null)
            return null;
        return new Snapshot(targetId, requirement, damaged, quality, examineText,
                rarity, rarityRuneModifier);
    }

    /** Repair is deterministic in WU; update local state when it is sent. */
    public synchronized void repaired(long id) {
        if (targetId == id) damaged = false;
    }

    public synchronized void clear() {
        targetId = NONE;
        requirement = null;
        damaged = false;
        quality = null;
        examineText = null;
        rarity = 0;
        rarityRuneModifier = 0.0f;
        rarityRuneMessages.clear();
        descriptionConfirmed = false;
    }

    public static final class Snapshot {
        private final long targetId;
        private final RequirementFamily requirement;
        private final boolean damaged;
        private final Float quality;
        private final String examineText;
        private final byte rarity;
        private final float rarityRuneModifier;

        Snapshot(long targetId, RequirementFamily requirement, boolean damaged,
                 Float quality, String examineText, byte rarity,
                 float rarityRuneModifier) {
            this.targetId = targetId;
            this.requirement = requirement;
            this.damaged = damaged;
            this.quality = quality;
            this.examineText = examineText;
            this.rarity = rarity;
            this.rarityRuneModifier = rarityRuneModifier;
        }

        public long getTargetId() { return targetId; }
        public RequirementFamily getRequirement() { return requirement; }
        public boolean isDamaged() { return damaged; }
        public Float getQuality() { return quality; }
        public String getExamineText() { return examineText; }
        public byte getRarity() { return rarity; }
        public float getRarityRuneModifier() { return rarityRuneModifier; }
    }
}
