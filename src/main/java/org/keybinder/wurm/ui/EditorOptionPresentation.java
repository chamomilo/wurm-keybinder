package org.keybinder.wurm.ui;

import org.keybinder.wurm.command.ExactObjectTarget;
import org.keybinder.wurm.command.NearbyTypeTarget;
import org.keybinder.wurm.command.InventoryFilterTarget;
import org.keybinder.wurm.command.TargetCodec;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.model.ItemSelectorKind;

/** Text and option mapping for editor source/target controls. */
public final class EditorOptionPresentation {
    private EditorOptionPresentation() {}

    public static String targetDisplay(String target) {
        if (target.startsWith("@tb"))
            return Messages.text("target.slot.toolbelt", target.substring(3));
        if (target.startsWith("@eq"))
            return Messages.text("target.slot.equipment", target.substring(3));
        if (target.startsWith("@nearby")) {
            try {
                return TargetCodec.display(TargetCodec.decode(target));
            } catch (IllegalArgumentException invalid) {
                return target;
            }
        }
        if (ExactObjectTarget.isExact(target)) return ExactObjectTarget.display(target);
        if (NearbyTypeTarget.isNearbyType(target)) return target;
        if (InventoryFilterTarget.isInventoryFilter(target))
            return Messages.text("target.inventory_filter_named",
                    InventoryFilterTarget.type(target));
        if (target.startsWith("hover-type "))
            return Messages.text("target.hover_type_named",
                    target.substring("hover-type ".length()));
        if (target.equals("tile")) return Messages.text("target.tile", "C");
        if (target.startsWith("tile_"))
            return Messages.text("target.tile", target.substring(5).toUpperCase());
        if (target.equals("area")) return Messages.text("target.area");
        return targetLabel(target);
    }

    public static String targetLabel(String token) {
        if ("hover".equals(token)) return Messages.text("target.hover");
        if ("body".equals(token)) return Messages.text("target.body");
        if ("tool".equals(token)) return Messages.text("target.tool");
        if ("selected".equals(token)) return Messages.text("target.selected");
        if ("current ride".equals(token)) return Messages.text("target.current_ride");
        if ("tiles".equals(token)) return Messages.text("target.tiles");
        if ("toolbelt".equals(token)) return Messages.text("target.toolbelt");
        if ("equipment".equals(token)) return Messages.text("target.equipment");
        if ("exact object".equals(token)) return Messages.text("target.exact_object");
        if ("nearby".equals(token)) return Messages.text("target.nearby");
        if ("nearby by type".equals(token)) return Messages.text("target.nearby_type");
        if ("hover by type".equals(token)) return Messages.text("target.hover_type");
        if (InventoryFilterTarget.OPTION.equals(token))
            return Messages.text("target.inventory_filter");
        if ("hand".equals(token)) return Messages.text("target.hand");
        return token;
    }

    public static String[] targetOptions(String target, String[] baseOptions) {
        if (!EditorTargetValue.isConcrete(target)) {
            String[] labels = new String[baseOptions.length];
            for (int i = 0; i < baseOptions.length; i++) labels[i] = targetLabel(baseOptions[i]);
            return labels;
        }
        String[] options = new String[baseOptions.length + 1];
        options[0] = targetDisplay(target);
        for (int i = 0; i < baseOptions.length; i++)
            options[i + 1] = targetLabel(baseOptions[i]);
        return options;
    }

    public static int sourceOptionFor(ItemSelectorKind kind) {
        switch (kind) {
            case EMPTY_HAND: return 1;
            case HOVERED_ITEM: return 2;
            case TOOLBELT_SLOT: return 3;
            case EQUIPMENT_SLOT: return 4;
            case EXACT_OBJECT: return 5;
            default: return 0;
        }
    }

    public static String sourceLabel(String value) {
        if ("current-active".equals(value)) return Messages.text("source.current_active");
        if ("empty-hand".equals(value)) return Messages.text("source.empty_hand");
        if ("hovered-item".equals(value)) return Messages.text("source.hovered_item");
        if ("toolbelt".equals(value)) return Messages.text("source.toolbelt");
        if ("equipment".equals(value)) return Messages.text("source.equipment");
        if ("exact-object".equals(value)) return Messages.text("source.exact_item");
        return value;
    }

    public static String sourceHelpKey(ItemSelectorKind kind) {
        return "help.source." + kind.name().toLowerCase(java.util.Locale.ENGLISH);
    }
}
