package org.keybinder.wurm.command;

import org.junit.Test;
import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.model.BulkDestinationKind;
import org.keybinder.wurm.model.BulkStorageItem;
import org.keybinder.wurm.model.BulkTransferStep;
import org.keybinder.wurm.model.InventoryReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BulkTransferCoordinatorTest {
    private static final String FORM =
            "passthrough{id='id';text='88'};"
                    + "text{text=\"How many items do you wish to remove?\"};"
                    + "input{id='numstext'};radio{group='items';id='1'}";

    @Test public void correlatesMoveWithMatchingQuantityFormAndSuppressesIt() {
        AtomicLong now = new AtomicLong(1000L);
        BulkTransferCoordinator coordinator = new BulkTransferCoordinator(
                new EventLogger(Logger.getAnonymousLogger()), now::get);
        final long[] move = new long[2];
        coordinator.begin(step(), 303L, (destination, item) -> {
            move[0] = destination;
            move[1] = item;
        });

        final Map<?, ?>[] answer = new Map<?, ?>[1];
        final String[] button = new String[1];
        assertTrue(coordinator.intercept("Removing items", FORM, (fields, id) -> {
            answer[0] = fields;
            button[0] = id;
        }));

        assertEquals(303L, move[0]);
        assertEquals(202L, move[1]);
        assertEquals("1", answer[0].get("numstext"));
        assertEquals("submit", button[0]);
        assertFalse(coordinator.hasPending());
    }

    @Test public void answersEachQueuedFormWithItsOwnQuantity() {
        BulkTransferCoordinator coordinator = new BulkTransferCoordinator(
                new EventLogger(Logger.getAnonymousLogger()), () -> 1000L);
        coordinator.begin(step(202L, 43), 303L, (destination, item) -> { });
        final String[] answer = new String[1];

        assertTrue(coordinator.intercept("Removing items", FORM,
                (fields, id) -> answer[0] = fields.get("numstext")));

        assertEquals("43", answer[0]);
    }

    @Test public void continuationWaitsForQuantityAnswer() {
        BulkTransferCoordinator coordinator = new BulkTransferCoordinator(
                new EventLogger(Logger.getAnonymousLogger()), () -> 1000L);
        final List<String> order = new ArrayList<String>();
        coordinator.begin(step(), 303L,
                (destination, item) -> order.add("move"),
                proceed -> order.add("continue:" + proceed));

        assertEquals(java.util.Arrays.asList("move"), order);
        assertTrue(coordinator.intercept("Removing items", FORM,
                (fields, id) -> order.add("answer:" + fields.get("numstext"))));
        assertEquals(java.util.Arrays.asList(
                "move", "answer:1", "continue:true"), order);
    }

    @Test public void timeoutAbortsContinuation() {
        AtomicLong now = new AtomicLong(1000L);
        BulkTransferCoordinator coordinator = new BulkTransferCoordinator(
                new EventLogger(Logger.getAnonymousLogger()), now::get);
        final List<Boolean> completion = new ArrayList<Boolean>();
        coordinator.begin(step(), 303L, (destination, item) -> { },
                completion::add);

        now.set(32000L);
        coordinator.tick();

        assertEquals(java.util.Arrays.asList(Boolean.FALSE), completion);
    }

    @Test public void unrelatedFormDoesNotConsumePendingRequest() {
        BulkTransferCoordinator coordinator = new BulkTransferCoordinator(
                new EventLogger(Logger.getAnonymousLogger()), () -> 1000L);
        coordinator.begin(step(), 303L, (destination, item) -> { });

        assertFalse(coordinator.intercept("Settlement", FORM, (fields, id) -> { }));
        assertTrue(coordinator.hasPending());
    }

    @Test public void pendingRequestExpiresAndAllowsAnotherMove() {
        AtomicLong now = new AtomicLong(1000L);
        BulkTransferCoordinator coordinator = new BulkTransferCoordinator(
                new EventLogger(Logger.getAnonymousLogger()), now::get);
        coordinator.begin(step(), 303L, (destination, item) -> { });
        now.set(32000L);

        coordinator.tick();

        assertFalse(coordinator.hasPending());
        coordinator.requireIdle();
    }

    @Test public void responseFailureFallsBackToNativeFormAndDropsCorrelation() {
        BulkTransferCoordinator coordinator = new BulkTransferCoordinator(
                new EventLogger(Logger.getAnonymousLogger()), () -> 1000L);
        coordinator.begin(step(), 303L, (destination, item) -> { });

        assertFalse(coordinator.intercept("Removing items", FORM, (fields, id) -> {
            throw new IllegalStateException("send failed");
        }));

        assertFalse(coordinator.hasPending());
        coordinator.requireIdle();
    }

    @Test public void multipleTransfersAreSentOneQuantityFormAtATime() {
        BulkTransferCoordinator coordinator = new BulkTransferCoordinator(
                new EventLogger(Logger.getAnonymousLogger()), () -> 1000L);
        final List<Long> sentItems = new ArrayList<Long>();
        BulkTransferCoordinator.MoveSender moves =
                (destination, item) -> sentItems.add(item);

        coordinator.begin(step(202L), 303L, moves);
        coordinator.begin(step(203L), 303L, moves);
        coordinator.begin(step(204L), 303L, moves);

        assertEquals(java.util.Arrays.asList(202L), sentItems);
        assertEquals(2, coordinator.queuedCount());
        assertTrue(coordinator.intercept("Removing items", FORM,
                (fields, id) -> { }));
        assertEquals(java.util.Arrays.asList(202L, 203L), sentItems);
        assertEquals(1, coordinator.queuedCount());
        assertTrue(coordinator.intercept("Removing items", FORM,
                (fields, id) -> { }));
        assertEquals(java.util.Arrays.asList(202L, 203L, 204L), sentItems);
        assertEquals(0, coordinator.queuedCount());
        assertTrue(coordinator.intercept("Removing items", FORM,
                (fields, id) -> { }));
        assertFalse(coordinator.hasPending());
    }

    @Test public void timeoutCancelsRemainingTransfersWithoutSendingThem() {
        AtomicLong now = new AtomicLong(1000L);
        BulkTransferCoordinator coordinator = new BulkTransferCoordinator(
                new EventLogger(Logger.getAnonymousLogger()), now::get);
        final List<Long> sentItems = new ArrayList<Long>();
        BulkTransferCoordinator.MoveSender moves =
                (destination, item) -> sentItems.add(item);
        coordinator.begin(step(202L), 303L, moves);
        coordinator.begin(step(203L), 303L, moves);
        now.set(32000L);

        coordinator.tick();

        assertEquals(java.util.Arrays.asList(202L), sentItems);
        assertFalse(coordinator.hasPending());
        assertEquals(0, coordinator.queuedCount());
    }

    @Test public void responseFailureCancelsRemainingTransfers() {
        BulkTransferCoordinator coordinator = new BulkTransferCoordinator(
                new EventLogger(Logger.getAnonymousLogger()), () -> 1000L);
        final List<Long> sentItems = new ArrayList<Long>();
        BulkTransferCoordinator.MoveSender moves =
                (destination, item) -> sentItems.add(item);
        coordinator.begin(step(202L), 303L, moves);
        coordinator.begin(step(203L), 303L, moves);

        assertFalse(coordinator.intercept("Removing items", FORM,
                (fields, id) -> { throw new IllegalStateException("send failed"); }));

        assertEquals(java.util.Arrays.asList(202L), sentItems);
        assertFalse(coordinator.hasPending());
        assertEquals(0, coordinator.queuedCount());
    }

    @Test public void permissionDenialImmediatelyContinuesWithNextTransfer() {
        BulkTransferCoordinator coordinator = new BulkTransferCoordinator(
                new EventLogger(Logger.getAnonymousLogger()), () -> 1000L);
        final List<Long> sentItems = new ArrayList<Long>();
        BulkTransferCoordinator.MoveSender moves =
                (destination, item) -> sentItems.add(item);
        coordinator.begin(step(202L), 303L, moves);
        coordinator.begin(step(203L), 303L, moves);

        assertTrue(coordinator.observeEvent(":Event",
                "That would be illegal here. You can check the settlement token for the local laws."));

        assertEquals(java.util.Arrays.asList(202L, 203L), sentItems);
        assertTrue(coordinator.hasPending());
        assertEquals(0, coordinator.queuedCount());
    }

    @Test public void incompatibleDestinationImmediatelyReleasesPendingTransfer() {
        BulkTransferCoordinator coordinator = new BulkTransferCoordinator(
                new EventLogger(Logger.getAnonymousLogger()), () -> 1000L);
        coordinator.begin(step(202L), 303L, (destination, item) -> { });

        assertTrue(coordinator.observeEvent(":Event",
                "Only ingredients that are used to make food can be put onto a roasting dish."));

        assertFalse(coordinator.hasPending());
        coordinator.requireIdle();
    }

    @Test public void serverRejectionContinuesMixedChain() {
        BulkTransferCoordinator coordinator = new BulkTransferCoordinator(
                new EventLogger(Logger.getAnonymousLogger()), () -> 1000L);
        final List<Boolean> completion = new ArrayList<Boolean>();
        coordinator.begin(step(), 303L, (destination, item) -> { },
                completion::add);

        coordinator.observeEvent(":Event",
                "Only ingredients that are used to make food can be put onto a roasting dish.");

        assertEquals(java.util.Arrays.asList(Boolean.TRUE), completion);
    }

    @Test public void unrelatedEventDoesNotReleasePendingTransfer() {
        BulkTransferCoordinator coordinator = new BulkTransferCoordinator(
                new EventLogger(Logger.getAnonymousLogger()), () -> 1000L);
        coordinator.begin(step(202L), 303L, (destination, item) -> { });

        assertFalse(coordinator.observeEvent(":Event", "You are too far away."));
        assertTrue(coordinator.hasPending());
    }

    @Test public void fourSessionsKeepBulkQueuesIndependent() {
        List<BulkTransferCoordinator> sessions = new ArrayList<BulkTransferCoordinator>();
        for (int i = 0; i < 4; i++) {
            BulkTransferCoordinator session = new BulkTransferCoordinator(
                    new EventLogger(Logger.getAnonymousLogger()), () -> 1000L);
            session.begin(step(202L + i, i + 1), 303L + i,
                    (destination, item) -> { });
            sessions.add(session);
        }

        assertTrue(sessions.get(0).intercept("Removing items", FORM,
                (fields, id) -> { }));

        assertFalse(sessions.get(0).hasPending());
        assertTrue(sessions.get(1).hasPending());
        assertTrue(sessions.get(2).hasPending());
        assertTrue(sessions.get(3).hasPending());
    }

    private static BulkTransferStep step() {
        return step(202L);
    }

    private static BulkTransferStep step(long itemId) {
        return step(itemId, 1);
    }

    private static BulkTransferStep step(long itemId, int quantity) {
        return new BulkTransferStep(new BulkStorageItem(
                new InventoryReference(101L, "bulk storage bin"),
                new InventoryReference(itemId, "barley (100x)")), quantity,
                BulkDestinationKind.CAPTURED_INVENTORY,
                new InventoryReference(303L, "small barrel"));
    }
}
