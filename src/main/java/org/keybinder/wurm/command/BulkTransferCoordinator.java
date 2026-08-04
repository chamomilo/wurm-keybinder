package org.keybinder.wurm.command;

import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.model.BulkTransferStep;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.function.LongSupplier;

/** Serializes exact move requests and correlates each asynchronous quantity form. */
public final class BulkTransferCoordinator {
    public interface MoveSender {
        void send(long destinationId, long bulkItemId);
    }

    public interface ResponseSender {
        void send(Map<String, String> fields, String buttonId);
    }

    /** Completes the asynchronous barrier; false aborts the remaining chain. */
    public interface Completion {
        void complete(boolean proceed);
    }

    private static final long RESPONSE_TIMEOUT_MILLIS = 30000L;

    private final EventLogger log;
    private final LongSupplier clock;
    private Pending pending;
    private final ArrayDeque<Pending> queued = new ArrayDeque<Pending>();

    public BulkTransferCoordinator(EventLogger log) {
        this(log, System::currentTimeMillis);
    }

    BulkTransferCoordinator(EventLogger log, LongSupplier clock) {
        this.log = log;
        this.clock = clock;
    }

    public synchronized void requireIdle() {
        expireIfNeeded();
        if (pending != null)
            throw new StepUnavailableException(Messages.text(
                    "unavailable.bulk_pending", pending.itemId, pending.destinationId));
    }

    public synchronized void begin(BulkTransferStep step, long destinationId,
                                   MoveSender sender) {
        begin(step, destinationId, sender, null);
    }

    public synchronized void begin(BulkTransferStep step, long destinationId,
                                   MoveSender sender, Completion completion) {
        long storageId = step.getSource().getStorage().getId();
        long itemId = step.getSource().getItem().getId();
        Pending request = new Pending(storageId, itemId, destinationId,
                step.getSource().getItem().getName(), step.getQuantity(),
                sender, completion);
        expireIfNeeded();
        if (pending != null) {
            queued.addLast(request);
            log.info(Messages.text("event.bulk_move_queued",
                    itemId, destinationId, queued.size()));
            log.diagnostic("bulk-transfer queued: storageId=" + storageId
                    + ", itemId=" + itemId + ", itemName='" + request.itemName
                    + "', destinationId=" + destinationId
                    + ", queuedBehindCurrent=" + queued.size());
            return;
        }
        pending = request;
        RuntimeException failure = sendCurrent();
        if (failure != null) throw failure;
    }

    /** Returns true only when the matching form was answered and must be suppressed. */
    public synchronized boolean intercept(String title, String bml,
                                          ResponseSender sender) {
        expireIfNeeded();
        if (pending == null) return false;
        BulkTransferBmlResponse response = BulkTransferBmlResponse.parse(title, bml);
        if (response == null) {
            log.diagnostic("bulk-transfer pending BML did not match: title='"
                    + safe(title) + "', bmlLength=" + (bml == null ? 0 : bml.length())
                    + ", preview='" + preview(bml) + "'");
            return false;
        }
        Pending accepted = pending;
        try {
            sender.send(response.fieldsForQuantity(accepted.quantity), "submit");
            pending = null;
            log.info(Messages.text("event.bulk_quantity_answered",
                    response.getQuestionId(), accepted.quantity,
                    accepted.itemId, accepted.destinationId));
            log.diagnostic("bulk-transfer BML answered: questionId="
                    + response.getQuestionId() + ", button=submit, numstext="
                    + accepted.quantity + ", items=1"
                    + ", itemId=" + accepted.itemId + ", storageId=" + accepted.storageId
                    + ", destinationId=" + accepted.destinationId
                    + ", remainingQueued=" + queued.size());
            complete(accepted, true);
            if (pending == null) dispatchNext();
            return true;
        } catch (RuntimeException failure) {
            // The native form is about to be shown as a fail-open fallback.
            // Forget this correlation so another RemoveItemQuestion cannot be
            // auto-answered on behalf of the failed request.
            pending = null;
            cancelQueued("quantity response failed");
            complete(accepted, false);
            log.error(Messages.text("error.bulk_bml_answer"), failure);
            return false;
        }
    }

    public synchronized void tick() { expireIfNeeded(); }

    /** Releases a rejected request immediately instead of waiting for its absent form. */
    public synchronized boolean observeEvent(String context, String message) {
        if (pending == null
                || !BulkTransferRejectionMessage.matches(context, message)) return false;
        Pending rejected = pending;
        pending = null;
        log.warning(Messages.text("event.bulk_move_rejected",
                rejected.itemId, rejected.destinationId, safe(message)));
        log.diagnostic("bulk-transfer rejected by server event: storageId="
                + rejected.storageId + ", itemId=" + rejected.itemId
                + ", destinationId=" + rejected.destinationId
                + ", remainingQueued=" + queued.size()
                + ", message='" + safe(message) + "'");
        complete(rejected, true);
        if (pending == null) dispatchNext();
        return true;
    }

    public synchronized void clear(String reason) {
        if (pending != null || !queued.isEmpty())
            log.diagnostic("bulk-transfer pending request cleared: " + safe(reason)
                    + ", itemId=" + (pending == null ? 0L : pending.itemId)
                    + ", destinationId=" + (pending == null ? 0L : pending.destinationId)
                    + ", queued=" + queued.size());
        Pending active = pending;
        pending = null;
        complete(active, false);
        while (!queued.isEmpty()) complete(queued.removeFirst(), false);
    }

    synchronized boolean hasPending() { return pending != null || !queued.isEmpty(); }
    synchronized int queuedCount() { return queued.size(); }

    private void expireIfNeeded() {
        if (pending == null || clock.getAsLong() <= pending.deadline) return;
        Pending expired = pending;
        pending = null;
        log.warning(Messages.text("event.bulk_bml_timeout",
                expired.itemId, expired.destinationId));
        log.diagnostic("bulk-transfer timeout: storageId=" + expired.storageId
                + ", itemId=" + expired.itemId
                + ", destinationId=" + expired.destinationId
                + ", queued=" + queued.size());
        cancelQueued("previous quantity form timed out");
        complete(expired, false);
    }

    private void dispatchNext() {
        Pending next = queued.pollFirst();
        if (next == null) return;
        pending = next;
        // The previous form has already been answered. A failure starting the
        // next request must not make that form visible again.
        sendCurrent();
    }

    private RuntimeException sendCurrent() {
        Pending current = pending;
        if (current == null) return null;
        current.deadline = clock.getAsLong() + RESPONSE_TIMEOUT_MILLIS;
        log.info(Messages.text("event.bulk_move_sent",
                current.itemId, current.storageId, current.destinationId,
                current.quantity));
        log.diagnostic("bulk-transfer request: storageId=" + current.storageId
                + ", itemId=" + current.itemId + ", itemName='" + current.itemName
                + "', destinationId=" + current.destinationId + ", quantity="
                + current.quantity
                + ", responseTimeoutMs=" + RESPONSE_TIMEOUT_MILLIS
                + ", remainingQueued=" + queued.size());
        try {
            current.sender.send(current.destinationId, current.itemId);
            return null;
        } catch (RuntimeException failure) {
            pending = null;
            cancelQueued("move request failed");
            log.error(Messages.text("error.bulk_move_send"), failure);
            return failure;
        }
    }

    private void cancelQueued(String reason) {
        int count = queued.size();
        while (!queued.isEmpty()) complete(queued.removeFirst(), false);
        if (count <= 0) return;
        log.warning(Messages.text("event.bulk_queue_cancelled", count));
        log.diagnostic("bulk-transfer queue cancelled: count=" + count
                + ", reason='" + safe(reason) + "'");
    }

    private static String preview(String value) {
        if (value == null) return "";
        String clean = value.replace('\r', ' ').replace('\n', ' ');
        return clean.length() <= 320 ? clean : clean.substring(0, 320) + "...";
    }

    private static String safe(String value) { return value == null ? "" : value; }

    private void complete(Pending request, boolean proceed) {
        if (request == null || request.completion == null) return;
        try {
            request.completion.complete(proceed);
        } catch (RuntimeException failure) {
            log.error(Messages.text("error.bulk_continuation"), failure);
        }
    }

    private static final class Pending {
        private final long storageId;
        private final long itemId;
        private final long destinationId;
        private final String itemName;
        private final int quantity;
        private final MoveSender sender;
        private final Completion completion;
        private long deadline;

        private Pending(long storageId, long itemId, long destinationId,
                        String itemName, int quantity, MoveSender sender,
                        Completion completion) {
            this.storageId = storageId;
            this.itemId = itemId;
            this.destinationId = destinationId;
            this.itemName = itemName == null ? "" : itemName;
            this.quantity = quantity;
            this.sender = sender;
            this.completion = completion;
        }
    }
}
