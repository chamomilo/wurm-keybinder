package org.keybinder.wurm.bind;

public final class BindSnapshot {
    private final int metaCode;
    private final String key;
    private final String command;

    public BindSnapshot(int metaCode, String key, String command) {
        this.metaCode = metaCode;
        this.key = key;
        this.command = command;
    }

    public int getMetaCode() { return metaCode; }
    public String getKey() { return key; }
    public String getCommand() { return command; }
}
