package org.keybinder.wurm.command;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * One world item's Examine-derived state, including freshness and silent
 * automatic-Examine state used by the Smart Improve continuation.
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
    private boolean stale = true;
    private boolean silentExamine;

    public synchronized void examineSent(long id) {
        examineSent(id, false);
    }

    public synchronized void examineSent(long id, boolean silent) {
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
        stale = true;
        silentExamine = silent;
    }

    public synchronized void selectionChanged(long selectedId) {
        // SelectBar may represent a hitched vehicle through a different client
        // object than the ground-item ID used by Examine/sendAction. The outgoing
        // Examine target remains authoritative; execution validates that exact ID.
    }

    public synchronized boolean event(long selectedId, String context, String message) {
        return event(context, message);
    }

    public synchronized boolean event(String context, String message) {
        if (context == null || !":event".equalsIgnoreCase(context.trim())) return false;
        if (targetId == NONE) return false;
        WorldImproveEventParser.Parsed parsed = WorldImproveEventParser.parse(message);
        boolean recognized = WorldImproveEventParser.isExamineDescription(message)
                || parsed.getRequirement() != null || parsed.getDamaged() != null
                || parsed.getQuality() != null || parsed.getRarity() != null
                || parsed.getRarityRuneModifier() != null;
        if (!descriptionConfirmed) {
            // DEFAULT_ACTION is only a possible Examine. Most world-object
            // descriptions carry Ql/Dam, but portable items placed in the
            // world can omit both and report only their standard Improve
            // requirement (for example a butchering knife needing water).
            if (!WorldImproveEventParser.isExamineDescription(message)
                    && parsed.getRequirement() == null) return false;
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
        if (descriptionConfirmed && requirement != null) stale = false;
        boolean suppress = silentExamine && recognized;
        if (descriptionConfirmed && requirement != null) silentExamine = false;
        return suppress;
    }

    public synchronized Snapshot snapshot(long selectedId) {
        if (stale || !descriptionConfirmed || targetId == NONE || selectedId != targetId
                || requirement == null) return null;
        return new Snapshot(targetId, requirement, damaged, quality, examineText,
                rarity, rarityRuneModifier);
    }

    public synchronized Snapshot currentSnapshot() {
        if (stale || !descriptionConfirmed || targetId == NONE || requirement == null)
            return null;
        return new Snapshot(targetId, requirement, damaged, quality, examineText,
                rarity, rarityRuneModifier);
    }

    public synchronized Snapshot snapshotIncludingStale(long requestedId) {
        if (!descriptionConfirmed || targetId == NONE || requestedId != targetId
                || requirement == null) return null;
        return new Snapshot(targetId, requirement, damaged, quality, examineText,
                rarity, rarityRuneModifier);
    }

    public synchronized boolean isFresh(long requestedId) {
        return snapshot(requestedId) != null;
    }

    /** Any queued Repair or Improve can change damage, QL, and the next resource. */
    public synchronized void invalidate(long id) {
        if (targetId == id) stale = true;
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
        stale = true;
        silentExamine = false;
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
