package org.keybinder.wurm.command;

import java.util.Locale;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.model.ObjectTypeNormalizer;

/** Portable inventory lookup target identified only by normalized item type. */
public final class InventoryFilterTarget {
    public static final String OPTION = "inventory+filter";
    public static final String PREFIX = OPTION + " ";

    private InventoryFilterTarget() {}

    public static boolean isInventoryFilter(String target) {
        return target != null
                && target.toLowerCase(Locale.ENGLISH).startsWith(PREFIX);
    }

    public static String encode(String itemType) {
        return PREFIX + ObjectTypeNormalizer.normalizeType(itemType);
    }

    public static String type(String target) {
        if (!isInventoryFilter(target))
            throw new IllegalArgumentException(
                    Messages.text("validation.inventory_filter_missing"));
        String value = target.substring(PREFIX.length()).trim();
        if (value.isEmpty())
            throw new IllegalArgumentException(
                    Messages.text("validation.inventory_filter_missing"));
        return ObjectTypeNormalizer.normalizeType(value);
    }
}
