package org.keybinder.wurm.integration;

import java.util.List;
import java.util.Locale;

public final class ServerNameResolver {
    public String resolve(String worldServerName, List<String> browserNames) {
        String shortName = worldServerName == null ? "" : worldServerName.trim();
        if (shortName.isEmpty() || browserNames == null) return shortName;
        String suffix = " - " + shortName;
        String match = null;
        for (String value : browserNames) {
            if (value == null) continue;
            String candidate = value.trim();
            if (!candidate.equalsIgnoreCase(shortName)
                    && !candidate.toLowerCase(Locale.ENGLISH)
                    .endsWith(suffix.toLowerCase(Locale.ENGLISH))) continue;
            if (match != null && !match.equalsIgnoreCase(candidate)) return shortName;
            match = candidate;
        }
        return match == null ? shortName : match;
    }
}
