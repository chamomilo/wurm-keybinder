package org.keybinder.wurm.integration;

import com.wurmonline.client.renderer.gui.CreationListItem;
import com.wurmonline.client.renderer.gui.CreationListWindow;
import com.wurmonline.client.comm.ServerConnectionListenerClass;
import com.wurmonline.client.comm.SimpleServerConnectionClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class CreationSkillClientContractTest {
    @Test
    public void pinnedClientExposesServerCreationNamesAndSkills() throws Exception {
        Field items = CreationListWindow.class.getDeclaredField("createItemList");
        Method name = CreationListItem.class.getDeclaredMethod("getName");
        Method skill = CreationListItem.class.getMethod("getSkill");
        Method received = ServerConnectionListenerClass.class.getDeclaredMethod(
                "addItemToCreationList", CreationListItem.class);
        Method request = SimpleServerConnectionClass.class.getMethod(
                "sendRequestFullCreateItemList");

        assertEquals(List.class, items.getType());
        assertEquals(String.class, name.getReturnType());
        assertEquals(String.class, skill.getReturnType());
        assertEquals(void.class, received.getReturnType());
        assertEquals(void.class, request.getReturnType());
    }

    @Test
    public void snapshotIncludesRecipesNestedUnderCreationCategories() {
        CreationListItem category = new CreationListItem(
                "Containers", "", 0, (short) 0, (short) 1, true);
        category.addChild(new CreationListItem(
                "huge tub", "Carpentry", 1, (short) 0, (short) 1, false));

        java.util.List<org.keybinder.wurm.catalog.CreationSkillEntry> entries =
                ClientAccess.snapshotCreationSkills(
                        java.util.Collections.singletonList(category));

        assertEquals(2, entries.size());
        assertEquals("huge tub", entries.get(1).getItemName());
        assertEquals("Carpentry", entries.get(1).getSkillName());
    }
}
