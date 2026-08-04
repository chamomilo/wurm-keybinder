package org.keybinder.wurm.model;

/**
 * Exact counted transfer from a bulk-storage row.
 *
 * <p>The source IDs intentionally remain server identities. The source window may
 * be closed after capture; the server resolves the saved bulk item ID when the
 * move request is executed.</p>
 */
public final class BulkTransferStep implements KeybindStep {
    private final BulkStorageItem source;
    private final int quantity;
    private final BulkDestinationKind destinationKind;
    private final InventoryReference capturedDestination;

    public BulkTransferStep(BulkStorageItem source, int quantity,
                            BulkDestinationKind destinationKind,
                            InventoryReference capturedDestination) {
        this.source = source;
        this.quantity = quantity;
        this.destinationKind = destinationKind;
        this.capturedDestination = capturedDestination;
    }

    @Override public StepKind getKind() { return StepKind.BULK_TRANSFER; }
    public BulkStorageItem getSource() { return source; }
    public int getQuantity() { return quantity; }
    public BulkDestinationKind getDestinationKind() { return destinationKind; }
    public InventoryReference getCapturedDestination() { return capturedDestination; }
}
