package org.keybinder.wurm.command;

public final class ResourceMatch {
    private static final ResourceMatch ACCEPTED = new ResourceMatch(true, "accepted");
    private final boolean accepted;
    private final String reason;

    private ResourceMatch(boolean accepted, String reason) {
        this.accepted = accepted;
        this.reason = reason;
    }

    public static ResourceMatch accepted() { return ACCEPTED; }
    public static ResourceMatch rejected(String reason) {
        return new ResourceMatch(false, reason == null ? "rejected" : reason);
    }

    public boolean isAccepted() { return accepted; }
    public String getReason() { return reason; }
}
