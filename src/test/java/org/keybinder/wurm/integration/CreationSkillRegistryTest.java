package org.keybinder.wurm.integration;

import com.wurmonline.client.renderer.gui.CreationListItem;
import org.junit.After;
import org.junit.Test;
import org.keybinder.wurm.catalog.CreationSkillEntry;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class CreationSkillRegistryTest {
    @After public void clearRegistry() {
        CreationSkillRegistry.clear();
    }

    @Test public void capturesServerRecipeNameAndPrimarySkill() {
        CreationSkillRegistry.clear();
        CreationSkillRegistry.observe(new CreationListItem(
                "huge tub", "Fine carpentry", 0, (short) 1,
                (short) 2, false));

        List<CreationSkillEntry> entries = CreationSkillRegistry.snapshot();
        assertEquals(1, entries.size());
        assertEquals(1, CreationSkillRegistry.size());
        assertEquals("huge tub", entries.get(0).getItemName());
        assertEquals("Fine carpentry", entries.get(0).getSkillName());
    }

    @Test public void ignoresRecipeIngredientRowsWithoutASkill() {
        CreationSkillRegistry.clear();
        CreationSkillRegistry.observe(new CreationListItem(
                "plank", "", 1, (short) 1, (short) 2, false));

        assertEquals(0, CreationSkillRegistry.snapshot().size());
        assertEquals(0, CreationSkillRegistry.size());
    }

    @Test public void revisionSnapshotChangesOnlyForCatalogMutations() {
        CreationSkillRegistry.clear();
        CreationSkillRegistry.Snapshot empty =
                CreationSkillRegistry.snapshotAfter(Long.MIN_VALUE);
        assertNotNull(empty);
        assertNull(CreationSkillRegistry.snapshotAfter(empty.getRevision()));

        CreationListItem recipe = new CreationListItem(
                "huge tub", "Fine carpentry", 0, (short) 1,
                (short) 2, false);
        CreationSkillRegistry.observe(recipe);
        CreationSkillRegistry.Snapshot populated =
                CreationSkillRegistry.snapshotAfter(empty.getRevision());
        assertNotNull(populated);
        assertEquals(1, populated.getEntries().size());

        CreationSkillRegistry.observe(recipe);
        assertNull(CreationSkillRegistry.snapshotAfter(
                populated.getRevision()));
    }
}
