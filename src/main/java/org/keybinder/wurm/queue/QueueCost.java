package org.keybinder.wurm.queue;

public final class QueueCost {
    public enum Kind { FIXED, DYNAMIC, UNKNOWN }
    private final Kind kind;
    private final int value;

    private QueueCost(Kind kind, int value) {
        this.kind = kind;
        this.value = value;
    }

    public static QueueCost fixed(int value) { return new QueueCost(Kind.FIXED, value); }
    public static QueueCost dynamic() { return new QueueCost(Kind.DYNAMIC, -1); }
    public static QueueCost unknown() { return new QueueCost(Kind.UNKNOWN, -1); }
    public Kind getKind() { return kind; }
    public int getValue() { return value; }
}
