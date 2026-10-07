package org.keybinder.wurm.command;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.shared.constants.PlayerAction;

import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.integration.ActionSourceOverride;
import org.keybinder.wurm.integration.ClientAccess;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.ActivateToolStep;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.TargetKind;
import org.keybinder.wurm.model.TargetSpec;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class KeybindPreflightProbe {
    public static void main(String[] args) throws Exception {
        KeybindPreflightProbe probe = new KeybindPreflightProbe();
        if ("order".equals(args[0])) probe.allUnavailableStepsAreReportedBeforeFirstSendAndDoNotUseCapacity();
        else if ("snapshot".equals(args[0])) probe.laterActionsUseThePressTimeTargetEvenWhenEarlierDispatchChangesHud();
        else if ("activation".equals(args[0])) probe.simulatesEmptyHandActivation();
        else probe.missingActiveItemNeverReachesTheSendPath();
    }
    private final List<Long> sent = new ArrayList<Long>();
    public void allUnavailableStepsAreReportedBeforeFirstSendAndDoNotUseCapacity()
            throws Exception {
        List<String> events = new ArrayList<String>();
        HeadsUpDisplay hud = hud(41L);
        ClientAccess access = access();
        KeybindExecutionService service = service(access, events, false);
        ActionStep valid = action(TargetKind.ACTIVE_TOOL);
        ActionStep unavailable = action(TargetKind.UNRESOLVED);
        service.execute(new KeybindRecord("test", "Test", "R",
                Arrays.asList(valid, unavailable, action(TargetKind.ACTIVE_TOOL))), hud, 2);

        assertEquals(Arrays.asList(41L, 41L), sent);
        assertEquals(3, events.size());
        assertTrue(events.get(0), events.get(0).contains("skipping"));
        assertEquals("send", events.get(1));
        assertEquals("send", events.get(2));
    }

    public void laterActionsUseThePressTimeTargetEvenWhenEarlierDispatchChangesHud()
            throws Exception {
        List<String> events = new ArrayList<String>();
        HeadsUpDisplay hud = hud(72L);

        KeybindExecutionService service = service(access(), events, true);
        service.execute(new KeybindRecord("snapshot", "Test", "R", Arrays.asList(
                action(TargetKind.ACTIVE_TOOL), action(TargetKind.ACTIVE_TOOL))), hud, 2);

        assertEquals(Arrays.asList(72L, 72L), sent);
        assertEquals(Arrays.asList("send", "send"), events);
    }

    public void missingActiveItemNeverReachesTheSendPath() throws Exception {
        List<String> events = new ArrayList<String>();
        HeadsUpDisplay hud = hud(0L);
        KeybindExecutionService service = service(access(), events, false);
        service.execute(new KeybindRecord("missing", "Test", "R", Arrays.asList(
                action(TargetKind.ACTIVE_TOOL), action(TargetKind.ACTIVE_TOOL))), hud, 2);
        assertTrue(sent.isEmpty());
        assertEquals(2, events.size());
    }

    public void simulatesEmptyHandActivation() throws Exception {
        List<String> events = new ArrayList<String>();
        HeadsUpDisplay hud = hud(88L);
        KeybindExecutionService service = service(access(), events, false);
        service.execute(new KeybindRecord("activation", "Test", "R", Arrays.asList(
                action(TargetKind.ACTIVE_TOOL),
                new ActivateToolStep(TargetSpec.simple(TargetKind.EMPTY_HAND)),
                action(TargetKind.ACTIVE_TOOL))), hud, 2);
        assertEquals(Arrays.asList(88L), sent);
        assertEquals(2, events.size());
        assertTrue(events.get(0).contains("skipping"));
        assertEquals("send", events.get(1));
        Field active = HeadsUpDisplay.class.getDeclaredField("activeToolItem");
        active.setAccessible(true);
        assertEquals(null, active.get(hud));
    }

    private static ActionStep action(TargetKind kind) {
        return new ActionStep(PlayerAction.EXAMINE.getId(), TargetSpec.simple(kind));
    }

    private static ClientAccess access() throws Exception {
        ClientAccess access = new ClientAccess();
        Field active = HeadsUpDisplay.class.getDeclaredField("activeToolItem");
        active.setAccessible(true);
        Field adapter = ClientAccess.class.getDeclaredField("activeToolItem");
        adapter.setAccessible(true);
        adapter.set(access, active);
        for (String name : Arrays.asList("setActiveToolItem", "removeActiveToolItem")) {
            java.lang.reflect.Method method = HeadsUpDisplay.class.getDeclaredMethod(
                    name, InventoryMetaItem.class);
            method.setAccessible(true);
            Field adapterMethod = ClientAccess.class.getDeclaredField(name);
            adapterMethod.setAccessible(true);
            adapterMethod.set(access, method);
        }
        return access;
    }

    private KeybindExecutionService service(ClientAccess access, List<String> events, boolean clearToolAfterSend) {
        ActionSourceOverride.markHookAvailable();
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override public void publish(LogRecord record) { events.add(record.getMessage()); }
            @Override public void flush() { }
            @Override public void close() { }
        });
        return new KeybindExecutionService(new ActionExecutor(access, id -> "Action " + id,
                new ActionExecutor.ActionSender() {
                    @Override public void send(HeadsUpDisplay targetHud, PlayerAction action, long target) {
                        try {
                            // Preflight itself must not have activated or cleared a tool.
                            if (sent.isEmpty()) assertTrue(access.activeTool(targetHud) != null);
                        } catch (ReflectiveOperationException failure) {
                            throw new AssertionError(failure);
                        }
                        sent.add(target);
                        events.add("send");
                        if (clearToolAfterSend) {
                            try {
                                Field active = HeadsUpDisplay.class.getDeclaredField("activeToolItem");
                                active.setAccessible(true);
                                active.set(targetHud, null);
                            } catch (ReflectiveOperationException failure) {
                                throw new AssertionError(failure);
                            }
                        }
                    }
                    @Override public void send(HeadsUpDisplay targetHud, PlayerAction action, long[] targets) {
                        for (long target : targets) send(targetHud, action, target);
                    }
                }), access,
                new EventLogger(logger));
    }

    private static HeadsUpDisplay hud(long itemId) throws Exception {
        Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) field.get(null);
        HeadsUpDisplay hud = (HeadsUpDisplay) unsafe.allocateInstance(HeadsUpDisplay.class);


        if (itemId > 0L) {
            InventoryMetaItem item = (InventoryMetaItem) unsafe.allocateInstance(InventoryMetaItem.class);
            Field id = InventoryMetaItem.class.getDeclaredField("id");
            id.setAccessible(true);
            id.setLong(item, itemId);
            Field active = HeadsUpDisplay.class.getDeclaredField("activeToolItem");
            active.setAccessible(true);
            active.set(hud, item);
        }
        return hud;
    }

}
