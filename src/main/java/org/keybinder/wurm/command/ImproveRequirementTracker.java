package org.keybinder.wurm.command;

import com.wurmonline.client.renderer.PickableUnit;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ImproveRequirementTracker {
    private static final long EXPECTATION_TTL_MILLIS = 10_000L;
    private static final long REQUIREMENT_TTL_MILLIS = 30L * 60L * 1000L;
    private static final int MAX_REQUIREMENTS = 512;
    private final Map<Long, Requirement> requirements = new ConcurrentHashMap<Long, Requirement>();
    private volatile long expectedTargetId = Long.MIN_VALUE;
    private volatile long expectedUntil;

    public void expect(long targetId) {
        expectedTargetId = targetId;
        expectedUntil = System.currentTimeMillis() + EXPECTATION_TTL_MILLIS;
    }

    public void observe(HeadsUpDisplay hud, String context, String message) {
        if (hud == null || message == null || context == null
                || !":event".equalsIgnoreCase(context)) return;
        long now = System.currentTimeMillis();
        long targetId = expectedUntil >= now ? expectedTargetId : Long.MIN_VALUE;
        if (targetId == Long.MIN_VALUE) {
            PickableUnit hovered = hud.getWorld().getCurrentHoveredObject();
            if (hovered == null) return;
            targetId = hovered.getId();
        }
        String lower = message.toLowerCase(java.util.Locale.ENGLISH);
        Requirement current = requirements.get(targetId);
        if (current == null) current = new Requirement();
        if (lower.contains("damage the") || lower.contains("before you try to improve")
                || damageAboveZero(lower)) current.damaged = true;
        String tool = toolFrom(lower);
        if (tool != null) current.toolName = tool;
        current.updatedAt = now;
        requirements.put(targetId, current);
        if (requirements.size() > MAX_REQUIREMENTS) removeExpired(now);
    }

    public String toolName(long id) {
        Requirement value = requirements.get(id);
        return expired(value) ? null : value.toolName;
    }

    public boolean damaged(long id) {
        Requirement value = requirements.get(id);
        return !expired(value) && value.damaged;
    }

    public void repaired(long id) {
        Requirement value = requirements.get(id);
        if (value != null) value.damaged = false;
    }

    public void clear() {
        requirements.clear();
        expectedTargetId = Long.MIN_VALUE;
        expectedUntil = 0L;
    }

    private void removeExpired(long now) {
        for (Map.Entry<Long, Requirement> entry : requirements.entrySet())
            if (now - entry.getValue().updatedAt > REQUIREMENT_TTL_MILLIS)
                requirements.remove(entry.getKey(), entry.getValue());
    }

    private static boolean expired(Requirement value) {
        return value == null
                || System.currentTimeMillis() - value.updatedAt > REQUIREMENT_TTL_MILLIS;
    }

    private static boolean damageAboveZero(String message) {
        int marker = message.indexOf(", dam: ");
        if (marker < 0) return false;
        String rest = message.substring(marker + 7).trim();
        int end = 0;
        while (end < rest.length() && (Character.isDigit(rest.charAt(end)) || rest.charAt(end) == '.')) end++;
        if (end == 0) return false;
        try { return Float.parseFloat(rest.substring(0, end)) > 0.0f; }
        catch (NumberFormatException ignored) { return false; }
    }

    private static String toolFrom(String text) {
        if (text.contains("with a log") || text.contains("more log")) return "log";
        if (text.contains("rock shards")) return "rock shards";
        if (text.contains("with a string") || text.contains("more string")) return "string";
        if (text.contains("use a mallet")) return "mallet";
        if (text.contains("use a file")) return "file";
        if (text.contains("polish")) return "pelt";
        if (text.contains("carve away")) return "carving knife";
        if (text.contains("stone chisel")) return "stone chisel";
        if (text.contains("stains") || text.contains("temper")) return "water";
        if (text.contains("backstitched") || text.contains("slipstitching")) return "needle";
        if (text.contains("cut away")) return "scissors";
        if (text.contains("sharpened")) return "whetstone";
        if (text.contains("flattened")) return "hammer";
        if (text.contains("with a lump") || text.contains("more lump")) return "lump";
        return null;
    }

    private static final class Requirement {
        private volatile String toolName;
        private volatile boolean damaged;
        private volatile long updatedAt;
    }
}
