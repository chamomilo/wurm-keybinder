package org.keybinder.wurm.command;

/** Closed set of Improve requirements that Keybinder can identify safely. */
public enum RequirementFamily {
    SHARD(RequirementKind.CONSUMABLE),
    LUMP(RequirementKind.CONSUMABLE),
    LEATHER(RequirementKind.CONSUMABLE),
    CLAY(RequirementKind.CONSUMABLE),
    LOG(RequirementKind.CONSUMABLE),
    STRING(RequirementKind.CONSUMABLE),
    PELT(RequirementKind.TOOL),
    FILE(RequirementKind.TOOL),
    MALLET(RequirementKind.TOOL),
    CARVING_KNIFE(RequirementKind.TOOL),
    STONE_CHISEL(RequirementKind.TOOL),
    METAL_BRUSH(RequirementKind.TOOL),
    NEEDLE(RequirementKind.TOOL),
    AWL(RequirementKind.TOOL),
    LEATHER_KNIFE(RequirementKind.TOOL),
    SCISSORS(RequirementKind.TOOL),
    WHETSTONE(RequirementKind.TOOL),
    HAMMER(RequirementKind.TOOL),
    CLAY_SHAPER(RequirementKind.TOOL),
    SPATULA(RequirementKind.TOOL),
    BODY_HAND(RequirementKind.BUILT_IN),
    WATER(RequirementKind.WATER),
    UNKNOWN(RequirementKind.UNKNOWN);

    private final RequirementKind kind;

    RequirementFamily(RequirementKind kind) {
        this.kind = kind;
    }

    public RequirementKind getKind() {
        return kind;
    }
}
