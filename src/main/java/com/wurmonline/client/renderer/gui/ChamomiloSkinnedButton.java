package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.resources.ChamomiloResourceUrl;
import com.wurmonline.client.resources.textures.ResourceTexture;
import com.wurmonline.client.resources.textures.ResourceTextureLoader;
import com.wurmonline.client.resources.textures.TextureLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Native button input with a reusable, horizontally stretchable wood/iron skin. */
public final class ChamomiloSkinnedButton extends WButton {
    // Original 2172 x 724 artwork, unchanged. Only its transparent canvas padding
    // is excluded. Each end cap contains BOTH original corner plates and rails.
    private static final float[] U = {14f / 2172f, 154f / 2172f,
            2019f / 2172f, 2159f / 2172f};
    private static final float V_TOP = 96f / 724f;
    private static final float V_BOTTOM = 624f / 724f;
    private static final int FACE_HEIGHT = 528;
    private static final int CAP_WIDTH = 140;
    private static final int LABEL_GAP = 4;
    private static final Logger LOG = Logger.getLogger("Chamomilo.UpdateCoordinator");
    private static ResourceTexture skin;
    private static boolean skinAttempted;

    /** Width can grow in a parent layout; add FIXED_WIDTH for a fixed action column. */
    public ChamomiloSkinnedButton(String label, ButtonListener listener, int desiredWidth) {
        super(label, listener);
        sizeFlags = 0;
        setSize(desiredWidth, preferredHeight());
        sizeFlags = FIXED_HEIGHT;
    }

    private int preferredHeight() {
        return Math.max(28, text.getHeight() + 12);
    }

    private static int capWidth(int height) {
        return Math.max(1, Math.round(height * (float) CAP_WIDTH / FACE_HEIGHT));
    }

    @Override void setSize(int width, int height) {
        int targetHeight = Math.max(preferredHeight(), height);
        int minimumWidth = text.getWidth(label) + 2 * (capWidth(targetHeight) + LABEL_GAP);
        super.setSize(Math.max(minimumWidth, width), targetHeight);
    }

    private static void loadSkin() {
        if (skinAttempted) return;
        skinAttempted = true;
        try {
            skin = ResourceTextureLoader.getInternalTexture(new ChamomiloResourceUrl(
                    "/org/chamomilo/wurm/update/update-button.png"),
                    TextureLoader.Filter.LINEAR, false, false, false);
        } catch (Exception failure) {
            LOG.log(Level.WARNING, "Cannot load Chamomilo button skin; using dark button face", failure);
        }
    }

    @Override protected void renderComponent(Queue queue, float ignoredAlpha) {
        loadSkin();
        boolean enabled = isEnabled();
        float tint = !enabled ? .55f : isDown ? .72f : hovered ? 1.12f : 1f;
        if (skin != null) {
            int cap = Math.min(capWidth(height), width / 2);
            int[] xs = {x, x + cap, x + width - cap, x + width};
            // Three full-height strips: shrink each end uniformly, stretch only
            // the middle horizontally. No flipped, substituted or overlaid rail.
            for (int col = 0; col < 3; col++) {
                if (xs[col + 1] <= xs[col]) continue;
                Renderer.texturedQuadAlphaBlend(queue, skin, tint, tint, tint, 1f,
                        xs[col], y, xs[col + 1] - xs[col], height,
                        U[col], V_TOP, U[col + 1], V_BOTTOM);
            }
        } else {
            fillRect(queue, .10f * tint, .085f * tint, .061f * tint, 1f, x, y, width, height);
            fillRect(queue, .34f * tint, .29f * tint, .20f * tint, 1f, x, y, width, 2);
            fillRect(queue, .34f * tint, .29f * tint, .20f * tint, 1f, x, y + height - 2, width, 2);
        }
        int offset = enabled && isDown ? 1 : 0;
        text.moveTo(x + (width - text.getWidth(label)) / 2 + offset,
                y + (height - text.getHeight()) / 2 + text.getAscent() + offset);
        text.paint(queue, label, enabled ? .96f : .67f, enabled ? .91f : .65f,
                enabled ? .77f : .57f, 1f);
    }
}
