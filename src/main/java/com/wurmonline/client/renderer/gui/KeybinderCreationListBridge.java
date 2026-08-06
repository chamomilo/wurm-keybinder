package com.wurmonline.client.renderer.gui;

/** Package-access bridge for the server-supplied crafting catalog. */
public final class KeybinderCreationListBridge {
    private KeybinderCreationListBridge() { }

    public static String itemName(CreationListItem item) {
        return item == null ? null : item.getName();
    }
}
