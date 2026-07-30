package org.keybinder.wurm.command;

import org.keybinder.wurm.model.TargetKind;
import org.keybinder.wurm.model.TargetSpec;

/** Converts external/editor/legacy target tokens to the native target model. */
public final class TargetCodec {
    private TargetCodec() {}

    public static TargetSpec decode(String token) {
        if (token == null || token.trim().isEmpty())
            throw new IllegalArgumentException("Target is missing");
        String value = token.trim();
        switch (value.toLowerCase(java.util.Locale.ENGLISH)) {
            case "hover": return TargetSpec.simple(TargetKind.HOVER);
            case "body": return TargetSpec.simple(TargetKind.BODY);
            case "tool": return TargetSpec.simple(TargetKind.ACTIVE_TOOL);
            case "selected": return TargetSpec.simple(TargetKind.SELECTED);
            case "current ride": return TargetSpec.simple(TargetKind.CURRENT_RIDE);
            case "tile": return TargetSpec.tile(0, 0);
            case "tile_n": return TargetSpec.tile(0, -1);
            case "tile_ne": return TargetSpec.tile(1, -1);
            case "tile_e": return TargetSpec.tile(1, 0);
            case "tile_se": return TargetSpec.tile(1, 1);
            case "tile_s": return TargetSpec.tile(0, 1);
            case "tile_sw": return TargetSpec.tile(-1, 1);
            case "tile_w": return TargetSpec.tile(-1, 0);
            case "tile_nw": return TargetSpec.tile(-1, -1);
            case "area": return TargetSpec.simple(TargetKind.AREA);
            case "hand": return TargetSpec.simple(TargetKind.EMPTY_HAND);
            case "unresolved": return TargetSpec.simple(TargetKind.UNRESOLVED);
            default:
        }
        if (value.startsWith("@tb"))
            return TargetSpec.toolbeltSlot(parseInt(value.substring(3), "toolbelt slot"));
        if (value.startsWith("@eq"))
            return TargetSpec.equipmentSlot(parseInt(value.substring(3), "equipment slot"));
        if (value.startsWith("@nearby")) {
            try {
                return TargetSpec.nearbyRadius(Float.parseFloat(value.substring(7)));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid nearby radius", e);
            }
        }
        if (NearbyTypeTarget.isNearbyType(value))
            return TargetSpec.nearbyType(NearbyTypeTarget.type(value));
        if (ExactObjectTarget.isExact(value))
            return TargetSpec.exactObject(ExactObjectTarget.id(value), ExactObjectTarget.name(value));
        throw new IllegalArgumentException("Unknown target: " + value);
    }

    public static String encode(TargetSpec target) {
        switch (target.getKind()) {
            case HOVER: return "hover";
            case BODY: return "body";
            case ACTIVE_TOOL: return "tool";
            case SELECTED: return "selected";
            case TILE: return tileToken(target.getDx(), target.getDy());
            case AREA: return "area";
            case TOOLBELT_SLOT: return "@tb" + target.getSlot();
            case EQUIPMENT_SLOT: return "@eq" + target.getSlot();
            case NEARBY_RADIUS: return "@nearby" + trimFloat(target.getRadius());
            case NEARBY_TYPE: return NearbyTypeTarget.PREFIX + target.getText();
            case EXACT_OBJECT: return ExactObjectTarget.encode(target.getObjectId(), target.getText());
            case CURRENT_RIDE: return "current ride";
            case EMPTY_HAND: return "hand";
            case UNRESOLVED: return "unresolved";
            default: throw new IllegalArgumentException("Unsupported target " + target.getKind());
        }
    }

    public static String display(TargetSpec target) {
        switch (target.getKind()) {
            case TOOLBELT_SLOT: return "toolbelt slot " + target.getSlot();
            case EQUIPMENT_SLOT: return "equipment slot " + target.getSlot();
            case EXACT_OBJECT:
                return target.getText().isEmpty() ? "Exact object" : target.getText();
            case NEARBY_TYPE: return "nearby " + target.getText();
            case CURRENT_RIDE: return "current ride";
            default: return encode(target);
        }
    }

    private static int parseInt(String value, String name) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + name + ": " + value, e);
        }
    }

    private static String tileToken(int dx, int dy) {
        if (dx == 0 && dy == 0) return "tile";
        if (dx == 0 && dy == -1) return "tile_n";
        if (dx == 1 && dy == -1) return "tile_ne";
        if (dx == 1 && dy == 0) return "tile_e";
        if (dx == 1 && dy == 1) return "tile_se";
        if (dx == 0 && dy == 1) return "tile_s";
        if (dx == -1 && dy == 1) return "tile_sw";
        if (dx == -1 && dy == 0) return "tile_w";
        if (dx == -1 && dy == -1) return "tile_nw";
        throw new IllegalArgumentException("Invalid tile offset");
    }

    private static String trimFloat(float value) {
        if (value == (long) value) return Long.toString((long) value);
        return Float.toString(value);
    }
}
