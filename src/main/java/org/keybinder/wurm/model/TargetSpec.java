package org.keybinder.wurm.model;

import java.util.Objects;
import org.keybinder.wurm.command.ObjectTypeNormalizer;
import org.keybinder.wurm.i18n.Messages;

/** Immutable, typed target used by every native Keybinder step. */
public final class TargetSpec {
    private final TargetKind kind;
    private final int slot;
    private final int dx;
    private final int dy;
    private final float radius;
    private final long objectId;
    private final String text;

    private TargetSpec(TargetKind kind, int slot, int dx, int dy, float radius,
                       long objectId, String text) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.slot = slot;
        this.dx = dx;
        this.dy = dy;
        this.radius = radius;
        this.objectId = objectId;
        this.text = text == null ? "" : text.trim();
    }

    public static TargetSpec simple(TargetKind kind) {
        if (kind == TargetKind.TILE || kind == TargetKind.TOOLBELT_SLOT
                || kind == TargetKind.EQUIPMENT_SLOT || kind == TargetKind.NEARBY_RADIUS
                || kind == TargetKind.NEARBY_TYPE || kind == TargetKind.HOVER_TYPE
                || kind == TargetKind.EXACT_OBJECT)
            throw new IllegalArgumentException(
                    Messages.text("validation.target_parameters", kind));
        return new TargetSpec(kind, 0, 0, 0, 0f, 0L, "");
    }

    public static TargetSpec tile(int dx, int dy) {
        if (dx < -1 || dx > 1 || dy < -1 || dy > 1)
            throw new IllegalArgumentException(Messages.text("validation.tile_offset"));
        return new TargetSpec(TargetKind.TILE, 0, dx, dy, 0f, 0L, "");
    }

    public static TargetSpec toolbeltSlot(int oneBasedSlot) {
        if (oneBasedSlot < 1 || oneBasedSlot > 10)
            throw new IllegalArgumentException(Messages.text("validation.toolbelt_slot"));
        return new TargetSpec(TargetKind.TOOLBELT_SLOT, oneBasedSlot, 0, 0, 0f, 0L, "");
    }

    public static TargetSpec equipmentSlot(int slot) {
        if (slot < 0 || slot > Byte.MAX_VALUE)
            throw new IllegalArgumentException(Messages.text("validation.equipment_slot"));
        return new TargetSpec(TargetKind.EQUIPMENT_SLOT, slot, 0, 0, 0f, 0L, "");
    }

    public static TargetSpec nearbyRadius(float radius) {
        if (!Float.isFinite(radius) || radius <= 0f)
            throw new IllegalArgumentException(Messages.text("validation.nearby_radius"));
        return new TargetSpec(TargetKind.NEARBY_RADIUS, 0, 0, 0, radius, 0L, "");
    }

    public static TargetSpec nearbyType(String type) {
        String value = ObjectTypeNormalizer.normalizeType(
                requireText(type, Messages.text("validation.nearby_type")));
        return new TargetSpec(TargetKind.NEARBY_TYPE, 0, 0, 0, 0f, 0L, value);
    }

    public static TargetSpec hoverType(String type) {
        String value = ObjectTypeNormalizer.normalizeType(
                requireText(type, Messages.text("validation.nearby_type")));
        return new TargetSpec(TargetKind.HOVER_TYPE, 0, 0, 0, 0f, 0L, value);
    }

    public static TargetSpec exactObject(long id, String name) {
        return new TargetSpec(TargetKind.EXACT_OBJECT, 0, 0, 0, 0f, id,
                name == null ? "" : name);
    }

    public static TargetSpec copyOf(TargetSpec target) {
        Objects.requireNonNull(target, "target");
        return new TargetSpec(target.kind, target.slot, target.dx, target.dy, target.radius,
                target.objectId, target.text);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(message);
        return value.trim();
    }

    public TargetKind getKind() { return kind; }
    public int getSlot() { return slot; }
    public int getDx() { return dx; }
    public int getDy() { return dy; }
    public float getRadius() { return radius; }
    public long getObjectId() { return objectId; }
    public String getText() { return text; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TargetSpec)) return false;
        TargetSpec that = (TargetSpec) other;
        return kind == that.kind && slot == that.slot && dx == that.dx && dy == that.dy
                && Float.compare(radius, that.radius) == 0 && objectId == that.objectId
                && text.equals(that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, slot, dx, dy, radius, objectId, text);
    }
}
