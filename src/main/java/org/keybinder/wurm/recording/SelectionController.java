package org.keybinder.wurm.recording;

import org.keybinder.wurm.command.ExactObjectTarget;
import org.keybinder.wurm.command.NearbyTypeTarget;
import org.keybinder.wurm.model.ObjectTypeNormalizer;
import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.model.BulkStorageItem;
import org.keybinder.wurm.model.InventoryReference;
import org.keybinder.wurm.integration.BulkInventoryDestinationPolicy;

public final class SelectionController {
    public enum Mode {
        NONE, TOOLBELT, EQUIPMENT, EXACT_OBJECT, NEARBY_TYPE, HOVER_TYPE,
        INVENTORY_FILTER, BULK_SOURCE, BULK_DESTINATION
    }
    private enum ResultKind { NONE, TARGET, BULK_SOURCE, BULK_DESTINATION }

    private final EventLogger log;
    private volatile Mode mode = Mode.NONE;
    private volatile String selectedTarget = "tile";
    private volatile boolean selectionComplete;
    private volatile ResultKind resultKind = ResultKind.NONE;
    private volatile BulkStorageItem selectedBulkSource;
    private volatile InventoryReference selectedBulkDestination;

    public SelectionController(EventLogger log) {
        this.log = log;
    }

    public void requestToolbelt() {
        begin(Mode.TOOLBELT);
        log.info(Messages.text("event.select_toolbelt"));
    }

    public void requestEquipment() {
        begin(Mode.EQUIPMENT);
        log.info(Messages.text("event.select_equipment"));
    }

    public void requestExactObject() {
        begin(Mode.EXACT_OBJECT);
        log.info(Messages.text("event.select_object"));
    }

    public void requestNearbyType() {
        begin(Mode.NEARBY_TYPE);
        log.info(Messages.text("event.select_nearby_type"));
    }

    public void requestHoverType() {
        begin(Mode.HOVER_TYPE);
        log.info(Messages.text("event.select_hover_type"));
    }

    public void requestInventoryFilter() {
        begin(Mode.INVENTORY_FILTER);
        log.info(Messages.text("event.select_inventory_filter"));
    }

    public void requestBulkSource() {
        begin(Mode.BULK_SOURCE);
        log.info(Messages.text("event.select_bulk_source"));
    }

    public void requestBulkDestination() {
        begin(Mode.BULK_DESTINATION);
        log.info(Messages.text("event.select_bulk_destination"));
    }

    public boolean acceptToolbelt(int zeroBasedSlot) {
        if (mode != Mode.TOOLBELT || zeroBasedSlot < 0 || zeroBasedSlot >= 10) return false;
        selectedTarget = "@tb" + (zeroBasedSlot + 1);
        selectionComplete = true;
        resultKind = ResultKind.TARGET;
        mode = Mode.NONE;
        log.info(Messages.text("event.target_selected", selectedTarget));
        return true;
    }

    public boolean acceptEquipment(byte slot) {
        if (mode != Mode.EQUIPMENT || slot < 0) return false;
        selectedTarget = "@eq" + slot;
        selectionComplete = true;
        resultKind = ResultKind.TARGET;
        mode = Mode.NONE;
        log.info(Messages.text("event.target_selected", selectedTarget));
        return true;
    }

    public boolean acceptExactObject(long id, String name) {
        if (mode != Mode.EXACT_OBJECT) return false;
        selectedTarget = ExactObjectTarget.encode(id, name);
        selectionComplete = true;
        resultKind = ResultKind.TARGET;
        mode = Mode.NONE;
        log.info(Messages.text("event.exact_selected",
                ExactObjectTarget.display(selectedTarget)));
        return true;
    }

    public boolean acceptNearbyType(String name) {
        if (mode != Mode.NEARBY_TYPE) return false;
        selectedTarget = NearbyTypeTarget.encode(name);
        selectionComplete = true;
        resultKind = ResultKind.TARGET;
        mode = Mode.NONE;
        log.info(Messages.text("event.target_selected", selectedTarget));
        return true;
    }

    public boolean acceptHoverType(String name) {
        if (mode != Mode.HOVER_TYPE) return false;
        selectedTarget = "hover-type " + ObjectTypeNormalizer.normalizeType(name);
        selectionComplete = true;
        resultKind = ResultKind.TARGET;
        mode = Mode.NONE;
        log.info(Messages.text("event.target_selected", selectedTarget));
        return true;
    }

    public boolean acceptInventoryFilter(String name) {
        if (mode != Mode.INVENTORY_FILTER) return false;
        selectedTarget = org.keybinder.wurm.command.InventoryFilterTarget.encode(name);
        selectionComplete = true;
        resultKind = ResultKind.TARGET;
        mode = Mode.NONE;
        log.info(Messages.text("event.target_selected", selectedTarget));
        log.diagnostic("inventory filter captured: rawType='" + safe(name)
                + "', normalizedType='"
                + org.keybinder.wurm.command.InventoryFilterTarget.type(selectedTarget)
                + "'");
        return true;
    }

    public boolean acceptBulkSource(long storageId, String storageName,
                                    long itemId, String itemName) {
        if (mode != Mode.BULK_SOURCE || storageId <= 0L || itemId <= 0L) return false;
        selectedBulkSource = new BulkStorageItem(
                new InventoryReference(storageId, storageName),
                new InventoryReference(itemId, itemName));
        selectionComplete = true;
        resultKind = ResultKind.BULK_SOURCE;
        mode = Mode.NONE;
        log.info(Messages.text("event.bulk_source_selected",
                itemName, itemId, storageName, storageId));
        log.diagnostic("bulk source captured: storageId=" + storageId
                + ", storageName='" + safe(storageName) + "', itemId=" + itemId
                + ", itemName='" + safe(itemName) + "'");
        return true;
    }

    public boolean acceptBulkDestination(long id, String name) {
        if (mode != Mode.BULK_DESTINATION
                || !BulkInventoryDestinationPolicy.isValid(id)) return false;
        selectedBulkDestination = new InventoryReference(id, name);
        selectionComplete = true;
        resultKind = ResultKind.BULK_DESTINATION;
        mode = Mode.NONE;
        log.info(Messages.text("event.bulk_destination_selected", name, id));
        log.diagnostic("bulk destination captured: destinationId=" + id
                + ", destinationName='" + safe(name) + "'");
        return true;
    }

    public void selectTile(String target) {
        selectedTarget = target;
        selectionComplete = true;
        resultKind = ResultKind.TARGET;
        mode = Mode.NONE;
        log.info(Messages.text("event.target_selected", selectedTarget));
    }

    public String getSelectedTarget() { return selectedTarget; }
    public String consumeSelectedTarget() {
        if (!selectionComplete || resultKind != ResultKind.TARGET) return null;
        completeConsumption();
        return selectedTarget;
    }
    public BulkStorageItem consumeSelectedBulkSource() {
        if (!selectionComplete || resultKind != ResultKind.BULK_SOURCE
                || selectedBulkSource == null) return null;
        BulkStorageItem result = selectedBulkSource;
        completeConsumption();
        selectedBulkSource = null;
        return result;
    }
    public InventoryReference consumeSelectedBulkDestination() {
        if (!selectionComplete || resultKind != ResultKind.BULK_DESTINATION
                || selectedBulkDestination == null) return null;
        InventoryReference result = selectedBulkDestination;
        completeConsumption();
        selectedBulkDestination = null;
        return result;
    }
    public Mode getMode() { return mode; }
    public void cancel() {
        mode = Mode.NONE;
        selectionComplete = false;
        resultKind = ResultKind.NONE;
        selectedBulkSource = null;
        selectedBulkDestination = null;
    }

    private void begin(Mode requestedMode) {
        mode = requestedMode;
        selectionComplete = false;
        resultKind = ResultKind.NONE;
        selectedBulkSource = null;
        selectedBulkDestination = null;
    }

    private void completeConsumption() {
        selectionComplete = false;
        resultKind = ResultKind.NONE;
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
