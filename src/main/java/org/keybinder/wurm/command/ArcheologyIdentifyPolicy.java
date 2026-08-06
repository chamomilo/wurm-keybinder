package org.keybinder.wurm.command;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import org.keybinder.wurm.i18n.Messages;

import java.util.regex.Pattern;

/** Pure target and required-tool rules for one archaeology Identify action. */
final class ArcheologyIdentifyPolicy {
    static final short UNIDENTIFIED_FRAGMENT_IMAGE = 1460;
    static final short METAL_BRUSH_IMAGE = 882;
    static final short STONE_CHISEL_IMAGE = 1201;

    private static final Pattern UNIDENTIFIED_FRAGMENT_NAME = Pattern.compile(
            "^unidentified(?: (?:weapon|armour|tool|statue|container|rift|metal|wooden))? fragment$");

    private ArcheologyIdentifyPolicy() { }

    static boolean isUnidentifiedFragment(InventoryMetaItem item) {
        if (item == null || item.getType() != UNIDENTIFIED_FRAGMENT_IMAGE) return false;
        String base = ImproveTargetFingerprint.normalize(item.getBaseName());
        return UNIDENTIFIED_FRAGMENT_NAME.matcher(base).matches();
    }

    static ResourceRequirement requiredTool(InventoryMetaItem target) {
        if (!isUnidentifiedFragment(target))
            throw new StepUnavailableException(Messages.text(
                    "archeology.target_not_fragment", displayName(target)));
        short icon = target.getImproveIconId();
        if (icon == METAL_BRUSH_IMAGE)
            return new ResourceRequirement(RequirementFamily.METAL_BRUSH,
                    null, METAL_BRUSH_IMAGE, false, "metal brush");
        if (icon == STONE_CHISEL_IMAGE)
            return new ResourceRequirement(RequirementFamily.STONE_CHISEL,
                    null, STONE_CHISEL_IMAGE, false, "stone chisel");
        throw new StepUnavailableException(Messages.text(
                "archeology.unknown_tool_icon", icon, displayName(target)));
    }

    static String displayName(InventoryMetaItem item) {
        if (item == null) return Messages.text("archeology.generic_fragment");
        String display = item.getDisplayName();
        if (display != null && !display.trim().isEmpty()) return display.trim();
        String base = item.getBaseName();
        return base == null || base.trim().isEmpty()
                ? Messages.text("archeology.generic_fragment") : base.trim();
    }
}
