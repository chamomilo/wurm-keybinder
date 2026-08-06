package org.keybinder.wurm.catalog;

/** One item-to-primary-skill row from the live server crafting catalog. */
public final class CreationSkillEntry {
    private final String itemName;
    private final String skillName;

    public CreationSkillEntry(String itemName, String skillName) {
        this.itemName = itemName;
        this.skillName = skillName;
    }

    public String getItemName() { return itemName; }
    public String getSkillName() { return skillName; }
}
