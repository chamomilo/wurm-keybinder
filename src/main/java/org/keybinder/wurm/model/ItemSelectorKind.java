package org.keybinder.wurm.model;

/** Portable ways to resolve the source item for one custom action. */
public enum ItemSelectorKind {
    CURRENT_ACTIVE,
    EMPTY_HAND,
    HOVERED_ITEM,
    TOOLBELT_SLOT,
    EQUIPMENT_SLOT,
    EXACT_OBJECT
}
