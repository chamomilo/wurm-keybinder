package org.keybinder.wurm.command;

import java.util.Locale;
import java.util.Objects;

/** Detects reuse of a Wurm runtime target ID within one client session. */
public final class ImproveTargetFingerprint {
    private final int clientType;
    private final String canonicalName;

    public ImproveTargetFingerprint(int clientType, String canonicalName) {
        this.clientType = clientType;
        this.canonicalName = normalize(canonicalName);
    }

    public int getClientType() { return clientType; }
    public String getCanonicalName() { return canonicalName; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ImproveTargetFingerprint)) return false;
        ImproveTargetFingerprint that = (ImproveTargetFingerprint) other;
        return clientType == that.clientType
                && canonicalName.equals(that.canonicalName);
    }

    @Override public int hashCode() {
        return Objects.hash(clientType, canonicalName);
    }

    @Override public String toString() {
        return clientType + ":" + canonicalName;
    }

    static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ENGLISH)
                .replaceAll("[\\s\\u00a0]+", " ").trim();
    }
}
