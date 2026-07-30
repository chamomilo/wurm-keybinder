package org.keybinder.wurm.model;

public final class KeybindConflict {
    private final String key;
    private final String owner;
    private final String command;
    private final String createdByUser;
    private final String createdOnServer;

    public KeybindConflict(String key, String owner, String command) {
        this(key, owner, command, "", "");
    }

    public KeybindConflict(String key, String owner, String command,
                           String createdByUser, String createdOnServer) {
        this.key = key;
        this.owner = owner;
        this.command = command;
        this.createdByUser = createdByUser == null ? "" : createdByUser;
        this.createdOnServer = createdOnServer == null ? "" : createdOnServer;
    }

    public String getKey() { return key; }
    public String getOwner() { return owner; }
    public String getCommand() { return command; }
    public String getCreatedByUser() { return createdByUser; }
    public String getCreatedOnServer() { return createdOnServer; }
}
