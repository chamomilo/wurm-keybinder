package org.keybinder.wurm.bind;

import java.util.Locale;

/** Prevents per-frame profile reloads while allowing a delayed player name. */
public final class AccountActivationGate {
    private String applied = "";
    private String failed = "";
    private long retryAt;

    public synchronized boolean shouldApply(String account, long now) {
        String normalized = normalize(account);
        if (normalized.isEmpty() || normalized.equals(applied)) return false;
        return !normalized.equals(failed) || now >= retryAt;
    }

    public synchronized boolean isApplied(String account) {
        String normalized = normalize(account);
        return !normalized.isEmpty() && normalized.equals(applied);
    }

    public synchronized void applied(String account) {
        applied = normalize(account);
        failed = "";
        retryAt = 0L;
    }

    public synchronized void failed(String account, long now, long retryDelayMillis) {
        failed = normalize(account);
        retryAt = now + Math.max(0L, retryDelayMillis);
    }

    public synchronized void clear() {
        applied = "";
        failed = "";
        retryAt = 0L;
    }

    private static String normalize(String account) {
        return account == null ? "" : account.trim().toLowerCase(Locale.ENGLISH);
    }
}
