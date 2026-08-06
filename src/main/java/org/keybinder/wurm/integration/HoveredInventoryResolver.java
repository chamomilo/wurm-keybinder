package org.keybinder.wurm.integration;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.gui.KeybinderInventorySelectionBridge;

/** Hides package-access GUI probing behind an integration-level result. */
public final class HoveredInventoryResolver {
    public static final class Result {
        private final InventoryMetaItem item;
        private final boolean playerInventoryWindowHit;
        private final String diagnostic;

        private Result(InventoryMetaItem item, boolean playerInventoryWindowHit,
                       String diagnostic) {
            this.item = item;
            this.playerInventoryWindowHit = playerInventoryWindowHit;
            this.diagnostic = diagnostic;
        }

        public InventoryMetaItem getItem() { return item; }
        public boolean isPlayerInventoryWindowHit() {
            return playerInventoryWindowHit;
        }
        public String getDiagnostic() { return diagnostic; }
    }

    public Result resolve(HeadsUpDisplay hud, int mouseX, int mouseY)
            throws ReflectiveOperationException {
        InventoryMetaItem item = KeybinderInventorySelectionBridge.itemUnderMouse(
                hud, mouseX, mouseY);
        if (item != null) return new Result(item, false, "not-probed");
        KeybinderInventorySelectionBridge.PlayerInventoryHit playerHit =
                KeybinderInventorySelectionBridge.playerInventoryUnderMouse(
                        hud, mouseX, mouseY);
        return new Result(playerHit.getItem(), playerHit.isHit(), playerHit.diagnostic());
    }
}
