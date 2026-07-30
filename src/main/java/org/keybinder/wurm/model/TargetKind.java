package org.keybinder.wurm.model;

/** Native Keybinder target kinds. Legacy command tokens are decoded at system boundaries. */
public enum TargetKind {
    HOVER,
    BODY,
    ACTIVE_TOOL,
    SELECTED,
    TILE,
    AREA,
    TOOLBELT_SLOT,
    EQUIPMENT_SLOT,
    NEARBY_RADIUS,
    NEARBY_TYPE,
    EXACT_OBJECT,
    CURRENT_RIDE,
    EMPTY_HAND,
    UNRESOLVED
}
