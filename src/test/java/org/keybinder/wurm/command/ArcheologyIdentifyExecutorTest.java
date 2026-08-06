package org.keybinder.wurm.command;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.shared.constants.PlayerAction;
import org.junit.Test;
import org.keybinder.wurm.integration.ExecutionOriginGuard;
import org.keybinder.wurm.model.ArcheologyIdentifySourceMode;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ArcheologyIdentifyExecutorTest {
    @Test public void acceptsEveryConfirmedUnidentifiedFragmentName() throws Exception {
        for (String name : Arrays.asList(
                "unidentified fragment",
                "unidentified weapon fragment",
                "unidentified armour fragment",
                "unidentified tool fragment",
                "unidentified statue fragment",
                "unidentified container fragment",
                "unidentified rift fragment",
                "unidentified metal fragment",
                "unidentified wooden fragment")) {
            InventoryMetaItem item = item(1L, name,
                    ArcheologyIdentifyPolicy.UNIDENTIFIED_FRAGMENT_IMAGE,
                    ArcheologyIdentifyPolicy.STONE_CHISEL_IMAGE);
            assertTrue(name, ArcheologyIdentifyPolicy.isUnidentifiedFragment(item));
        }
    }

    @Test public void rejectsIdentifiedWrongImageAndSubstringTargets() throws Exception {
        assertFalse(ArcheologyIdentifyPolicy.isUnidentifiedFragment(item(1L,
                "small statue fragment [1/10]", (short) 1465, (short) -1)));
        assertFalse(ArcheologyIdentifyPolicy.isUnidentifiedFragment(item(2L,
                "unidentified statue fragment", (short) 999, (short) 882)));
        assertFalse(ArcheologyIdentifyPolicy.isUnidentifiedFragment(item(3L,
                "box with unidentified statue fragment", (short) 1460, (short) 882)));
        assertFalse(ArcheologyIdentifyPolicy.isUnidentifiedFragment(item(4L,
                "unidentified statue fragment shard", (short) 1460, (short) 882)));
    }

    @Test public void resolvesBrushAndChiselFromRequiredToolIcon() throws Exception {
        ResourceRequirement brush = ArcheologyIdentifyPolicy.requiredTool(item(1L,
                "unidentified fragment", (short) 1460, (short) 882));
        ResourceRequirement chisel = ArcheologyIdentifyPolicy.requiredTool(item(2L,
                "unidentified statue fragment", (short) 1460, (short) 1201));

        assertEquals(RequirementFamily.METAL_BRUSH, brush.getFamily());
        assertEquals(Short.valueOf((short) 882), brush.getExactImageId());
        assertEquals(RequirementFamily.STONE_CHISEL, chisel.getFamily());
        assertEquals(Short.valueOf((short) 1201), chisel.getExactImageId());
    }

    @Test public void sharedChiselIconNeverAcceptsCarvingKnife() throws Exception {
        ResourceRequirement chisel = ArcheologyIdentifyPolicy.requiredTool(item(1L,
                "unidentified fragment", (short) 1460, (short) 1201));

        assertTrue(chisel.match(candidate(2L, "rare iron stone chisel (glowing)",
                (short) 1201)).isAccepted());
        assertFalse(chisel.match(candidate(3L, "carving knife",
                (short) 1201)).isAccepted());
        assertFalse(chisel.match(candidate(4L, "stone chisel blade",
                (short) 1201)).isAccepted());
    }

    @Test public void unknownToolIconAndOrdinaryObjectFailClosed() throws Exception {
        assertUnavailable(item(1L, "unidentified fragment", (short) 1460, (short) 777));
        assertUnavailable(item(2L, "stone chisel", (short) 1201, (short) 882));
    }

    @Test public void toolbeltWinsBeforeInventoryAndToolbeltOnlyHasNoFallback()
            throws Exception {
        ResourceRequirement requirement = ArcheologyIdentifyPolicy.requiredTool(item(1L,
                "unidentified fragment", (short) 1460, (short) 1201));
        ImproveResourceCandidate beltChisel = candidate(2L, "stone chisel", (short) 1201);
        ImproveResourceCandidate inventoryChisel = candidate(3L, "stone chisel", (short) 1201);
        ImproveResourceCandidate inventory = candidate(10L, "inventory", (short) 0,
                Collections.singletonList(inventoryChisel));

        ResolvedImproveResource selected = ArcheologyIdentifyExecutor.resolveTool(
                Collections.singletonList(beltChisel), inventory, requirement,
                ArcheologyIdentifySourceMode.TOOLBELT_THEN_INVENTORY, null);
        assertSame(beltChisel, selected.getCandidate());
        assertTrue(selected.isToolbelt());

        assertNull(ArcheologyIdentifyExecutor.resolveTool(
                Collections.<ImproveResourceCandidate>emptyList(), inventory, requirement,
                ArcheologyIdentifySourceMode.TOOLBELT_ONLY, null));
    }

    @Test public void inventoryFallbackRecursesIntoVisibleContainers() throws Exception {
        ResourceRequirement requirement = ArcheologyIdentifyPolicy.requiredTool(item(1L,
                "unidentified fragment", (short) 1460, (short) 882));
        ImproveResourceCandidate brush = candidate(4L, "metal brush", (short) 882);
        ImproveResourceCandidate satchel = candidate(3L, "satchel", (short) 2,
                Collections.singletonList(brush));
        ImproveResourceCandidate inventory = candidate(2L, "inventory", (short) 0,
                Collections.singletonList(satchel));

        ResolvedImproveResource selected = ArcheologyIdentifyExecutor.resolveTool(
                Collections.<ImproveResourceCandidate>emptyList(), inventory, requirement,
                ArcheologyIdentifySourceMode.TOOLBELT_THEN_INVENTORY, null);

        assertSame(brush, selected.getCandidate());
        assertTrue(selected.isNested());
        assertFalse(selected.isToolbelt());
    }

    @Test public void preparedExecutionSendsExactlyOneIdentifyUnderOriginGuard() {
        final int[] calls = {0};
        final long[] ids = new long[2];
        ArcheologyIdentifyExecutor executor = new ArcheologyIdentifyExecutor(
                null, null, new ArcheologyIdentifyExecutor.ActionSender() {
            @Override public void send(HeadsUpDisplay hud, long sourceId, long targetId) {
                assertTrue(ExecutionOriginGuard.isInternal());
                calls[0]++;
                ids[0] = sourceId;
                ids[1] = targetId;
            }
        });

        executor.executePrepared(null, 441L, 1307L);

        assertEquals(1, calls[0]);
        assertEquals(441L, ids[0]);
        assertEquals(1307L, ids[1]);
        assertFalse(ExecutionOriginGuard.isInternal());
        assertEquals(911, PlayerAction.IDENTIFY.getId());
    }

    @Test public void hoverBatchQueuesEveryExecutableFragmentInStableOrder()
            throws Exception {
        List<InventoryMetaItem> targets = Arrays.asList(
                item(8L, "unidentified statue fragment", (short) 1460, (short) 1201),
                item(3L, "unidentified fragment", (short) 1460, (short) 882),
                item(7L, "unidentified tool fragment", (short) 1460, (short) 1201),
                item(2L, "unidentified weapon fragment", (short) 1460, (short) 882),
                item(6L, "unidentified armour fragment", (short) 1460, (short) 1201),
                item(1L, "unidentified container fragment", (short) 1460, (short) 882),
                item(5L, "unidentified rift fragment", (short) 1460, (short) 1201),
                item(4L, "unidentified metal fragment", (short) 1460, (short) 882));
        List<ImproveResourceCandidate> tools = Arrays.asList(
                candidate(100L, "metal brush", (short) 882),
                candidate(101L, "stone chisel", (short) 1201));

        ArcheologyIdentifyExecutor.PreparedBatch batch =
                ArcheologyIdentifyExecutor.prepareCandidates(targets, tools, null,
                        ArcheologyIdentifySourceMode.TOOLBELT_ONLY,
                        20, true, null);

        assertEquals(8, batch.getQueueCost());
        List<Long> queued = new ArrayList<Long>();
        for (ArcheologyIdentifyExecutor.PreparedIdentify action : batch.getItems())
            queued.add(action.getTargetId());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L), queued);

        ArcheologyIdentifyExecutor.PreparedBatch capped =
                ArcheologyIdentifyExecutor.prepareCandidates(targets, tools, null,
                        ArcheologyIdentifySourceMode.TOOLBELT_ONLY,
                        5, true, null);
        List<Long> cappedTargets = new ArrayList<Long>();
        for (ArcheologyIdentifyExecutor.PreparedIdentify action : capped.getItems())
            cappedTargets.add(action.getTargetId());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), cappedTargets);
    }

    @Test public void hoverBatchSkipsUnavailableItemsAndStopsAtQueueBudget()
            throws Exception {
        List<InventoryMetaItem> targets = Arrays.asList(
                item(1L, "backpack", (short) 20, (short) -1),
                item(2L, "unidentified statue fragment", (short) 1460, (short) 1201),
                item(3L, "unidentified fragment", (short) 1460, (short) 777),
                item(4L, "unidentified weapon fragment", (short) 1460, (short) 882),
                item(5L, "unidentified tool fragment", (short) 1460, (short) 882));
        List<ImproveResourceCandidate> tools = Collections.singletonList(
                candidate(100L, "metal brush", (short) 882));

        ArcheologyIdentifyExecutor.PreparedBatch batch =
                ArcheologyIdentifyExecutor.prepareCandidates(targets, tools, null,
                        ArcheologyIdentifySourceMode.TOOLBELT_ONLY,
                        1, true, null);

        assertEquals(1, batch.getQueueCost());
        assertEquals(4L, batch.getItems().get(0).getTargetId());
        assertEquals(100L, batch.getItems().get(0).getTool().getCandidate().getId());
    }

    @Test public void preparedBatchSendsEveryQueuedIdentifyUnderOriginGuard()
            throws Exception {
        List<InventoryMetaItem> targets = Arrays.asList(
                item(1L, "unidentified fragment", (short) 1460, (short) 882),
                item(2L, "unidentified statue fragment", (short) 1460, (short) 1201));
        List<ImproveResourceCandidate> tools = Arrays.asList(
                candidate(100L, "metal brush", (short) 882),
                candidate(101L, "stone chisel", (short) 1201));
        ArcheologyIdentifyExecutor.PreparedBatch batch =
                ArcheologyIdentifyExecutor.prepareCandidates(targets, tools, null,
                        ArcheologyIdentifySourceMode.TOOLBELT_ONLY,
                        10, true, null);
        final List<Long> sentTargets = new ArrayList<Long>();
        ArcheologyIdentifyExecutor executor = new ArcheologyIdentifyExecutor(
                null, null, new ArcheologyIdentifyExecutor.ActionSender() {
            @Override public void send(HeadsUpDisplay hud, long sourceId, long targetId) {
                assertTrue(ExecutionOriginGuard.isInternal());
                sentTargets.add(targetId);
            }
        });

        executor.executePreparedBatch(null, batch);

        assertEquals(Arrays.asList(1L, 2L), sentTargets);
        assertFalse(ExecutionOriginGuard.isInternal());
    }

    private static void assertUnavailable(InventoryMetaItem item) {
        try {
            ArcheologyIdentifyPolicy.requiredTool(item);
            fail("Unavailable target was accepted: " + item.getBaseName());
        } catch (StepUnavailableException expected) {
            assertTrue(expected.getMessage().length() > 0);
        }
    }

    private static ImproveResourceCandidate candidate(long id, String name, short image) {
        return candidate(id, name, image,
                Collections.<ImproveResourceCandidate>emptyList());
    }

    private static ImproveResourceCandidate candidate(long id, String name, short image,
                                                       List<ImproveResourceCandidate> children) {
        return new ImproveResourceCandidate(id, name, name, (byte) 0, image,
                0f, 0f, 0f, (byte) 0, children);
    }

    private static InventoryMetaItem item(long id, String name, short type,
                                          short improveIcon) throws Exception {
        Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
        InventoryMetaItem item = (InventoryMetaItem) unsafe.allocateInstance(
                InventoryMetaItem.class);
        set(item, "id", id);
        set(item, "type", type);
        set(item, "improveIconId", improveIcon);
        set(item, "baseName", name);
        set(item, "displayName", name);
        set(item, "children", new java.util.ArrayList<InventoryMetaItem>());
        return item;
    }

    private static void set(InventoryMetaItem item, String fieldName, Object value)
            throws Exception {
        Field field = InventoryMetaItem.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(item, value);
    }
}
