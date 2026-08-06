package org.keybinder.wurm.integration;

/**
 * Tracks the authoritative server name delivered by the current connection.
 * A Wurm {@code World} survives shard transfers and can temporarily retain the
 * previous shard name, so it must not be used while a handoff is in progress.
 */
public final class CurrentServerTracker {
    private String reportedName = "";
    private boolean awaitingServerInformation;

    public synchronized void serverInformation(String serverName) {
        reportedName = clean(serverName);
        awaitingServerInformation = reportedName.isEmpty();
    }

    public synchronized void connectionChanging() {
        reportedName = "";
        awaitingServerInformation = true;
    }

    public synchronized String currentOrWorld(String worldServerName) {
        if (!reportedName.isEmpty()) return reportedName;
        return awaitingServerInformation ? "" : clean(worldServerName);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
