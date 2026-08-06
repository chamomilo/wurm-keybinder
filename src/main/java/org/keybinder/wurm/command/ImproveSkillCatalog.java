package org.keybinder.wurm.command;

import org.keybinder.wurm.catalog.CreationSkillEntry;
import org.keybinder.wurm.model.ObjectTypeNormalizer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Resolves an improve target to its server-supplied primary crafting skill. */
final class ImproveSkillCatalog {
    private final Map<String, String> skills = new HashMap<String, String>();
    private final Set<String> ambiguous = new HashSet<String>();

    ImproveSkillCatalog(List<CreationSkillEntry> entries) {
        if (entries == null) return;
        for (CreationSkillEntry entry : entries) {
            if (entry == null || blank(entry.getSkillName())) continue;
            String type;
            try {
                type = ObjectTypeNormalizer.normalizeType(entry.getItemName());
            } catch (RuntimeException invalidName) {
                continue;
            }
            String previous = skills.get(type);
            String skill = entry.getSkillName().trim();
            if (previous == null) skills.put(type, skill);
            else if (!previous.equals(skill)) ambiguous.add(type);
        }
    }

    String skillFor(String itemName) {
        return skillFor(itemName, null);
    }

    String skillFor(String itemName, String examineText) {
        String type;
        try {
            type = ObjectTypeNormalizer.normalizeType(itemName);
        } catch (RuntimeException invalidName) {
            return null;
        }
        String direct = ambiguous.contains(type) ? null : skills.get(type);
        if (direct != null || blank(examineText)) return direct;

        String text = normalizeText(examineText);
        String bestType = null;
        int bestIndex = Integer.MAX_VALUE;
        for (String candidate : skills.keySet()) {
            if (ambiguous.contains(candidate)) continue;
            int index = wholePhraseIndex(text, candidate);
            if (index < 0) continue;
            if (index < bestIndex || (index == bestIndex
                    && (bestType == null || candidate.length() > bestType.length()))) {
                bestIndex = index;
                bestType = candidate;
            }
        }
        return bestType == null ? null : skills.get(bestType);
    }

    int size() { return skills.size(); }

    private static String normalizeText(String value) {
        return value.replace('\u00a0', ' ').replace('\u2007', ' ')
                .replace('\u202f', ' ').toLowerCase(java.util.Locale.ENGLISH)
                .replaceAll("\\s+", " ");
    }

    private static int wholePhraseIndex(String text, String phrase) {
        int from = 0;
        while (from <= text.length() - phrase.length()) {
            int index = text.indexOf(phrase, from);
            if (index < 0) return -1;
            int end = index + phrase.length();
            boolean left = index == 0 || !Character.isLetterOrDigit(
                    text.charAt(index - 1));
            boolean right = end == text.length() || !Character.isLetterOrDigit(
                    text.charAt(end));
            if (left && right) return index;
            from = index + 1;
        }
        return -1;
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
