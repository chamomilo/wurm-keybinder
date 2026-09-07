package org.keybinder.wurm.command;

import org.junit.Test;
import org.keybinder.wurm.catalog.CreationSkillEntry;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ImproveSkillCatalogTest {
    @Test
    public void resolvesMaterialAndRarityDecoratedTargetName() {
        ImproveSkillCatalog catalog = new ImproveSkillCatalog(Arrays.asList(
                new CreationSkillEntry("pickaxe", "Blacksmithing"),
                new CreationSkillEntry("wooden shield", "Shields")));

        assertEquals("Blacksmithing",
                catalog.skillFor("rare pickaxe (glowing), steel"));
        assertEquals("Shields", catalog.skillFor("cedarwood wooden shield"));
        assertNull(catalog.skillFor("unknown thing"));
    }

    @Test
    public void resolvesEveryLiveBeeHiveStateToItsRecipeSkill() {
        ImproveSkillCatalog catalog = new ImproveSkillCatalog(Arrays.asList(
                new CreationSkillEntry("bee hive", "Fine carpentry")));

        assertEquals("Fine carpentry",
                catalog.skillFor("empty bee hive, oakenwood"));
        assertEquals("Fine carpentry",
                catalog.skillFor("active bee hive, cedarwood"));
        assertEquals("Fine carpentry",
                catalog.skillFor("dormant bee hive, pinewood"));
        assertEquals("Fine carpentry",
                catalog.skillFor("noisy bee hive, birchwood"));
    }

    @Test
    public void refusesAnItemNameThatMapsToDifferentSkills() {
        ImproveSkillCatalog catalog = new ImproveSkillCatalog(Arrays.asList(
                new CreationSkillEntry("special tool", "Carpentry"),
                new CreationSkillEntry("special tool", "Blacksmithing")));

        assertNull(catalog.skillFor("special tool"));
    }

    @Test
    public void resolvesRenamedWorldItemFromItsExamineDescription() {
        ImproveSkillCatalog catalog = new ImproveSkillCatalog(Arrays.asList(
                new CreationSkillEntry("wagon", "Fine carpentry"),
                new CreationSkillEntry("pelt", "Tailoring"),
                new CreationSkillEntry("forge", "Blacksmithing"),
                new CreationSkillEntry("stone chisel", "Stone cutting")));

        assertEquals("Fine carpentry", catalog.skillFor("Raritet",
                "A fairly large wagon designed to be dragged by four animals. "
                        + "You will want to polish the Raritet with a pelt."));
        assertEquals("Blacksmithing", catalog.skillFor("My forge",
                "A forge made from stone bricks and clay. The forge has some "
                        + "irregularities that must be removed with a stone chisel."));
    }

    @Test
    public void examineDescriptionUsesWholeItemNamesOnly() {
        ImproveSkillCatalog catalog = new ImproveSkillCatalog(Arrays.asList(
                new CreationSkillEntry("log", "Carpentry")));

        assertNull(catalog.skillFor("renamed", "A catalogue of rare items."));
    }
}
