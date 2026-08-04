package org.keybinder.wurm.model;

/** A server-side inventory/container identity captured from a live client window. */
public final class InventoryReference {
    private final long id;
    private final String name;

    public InventoryReference(long id, String name) {
        this.id = id;
        this.name = name == null ? "" : name.trim();
    }

    public long getId() { return id; }
    public String getName() { return name; }

    public static InventoryReference copyOf(InventoryReference value) {
        return value == null ? null : new InventoryReference(value.id, value.name);
    }

    @Override public boolean equals(Object other) {
        if (!(other instanceof InventoryReference)) return false;
        InventoryReference reference = (InventoryReference) other;
        return id == reference.id && name.equals(reference.name);
    }

    @Override public int hashCode() {
        return 31 * Long.valueOf(id).hashCode() + name.hashCode();
    }
}
