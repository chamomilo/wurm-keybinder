package org.keybinder.wurm.command;

import com.wurmonline.shared.util.MaterialUtilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable candidate snapshot used by deterministic resource matching. */
public final class ImproveResourceCandidate {
    private final long id;
    private final String baseName;
    private final String displayName;
    private final byte materialId;
    private final short imageId;
    private final float r;
    private final float g;
    private final float b;
    private final byte temperature;
    private final List<ImproveResourceCandidate> children;

    public ImproveResourceCandidate(long id, String baseName, String displayName,
                                    byte materialId, short imageId,
                                    float r, float g, float b, byte temperature,
                                    List<ImproveResourceCandidate> children) {
        this.id = id;
        this.baseName = normalize(baseName);
        this.displayName = displayName == null ? "" : displayName.trim();
        this.materialId = materialId;
        this.imageId = imageId;
        this.r = r;
        this.g = g;
        this.b = b;
        this.temperature = temperature;
        this.children = children == null ? Collections.<ImproveResourceCandidate>emptyList()
                : Collections.unmodifiableList(
                        new ArrayList<ImproveResourceCandidate>(children));
    }

    public long getId() { return id; }
    public String getBaseName() { return baseName; }
    public String getDisplayName() { return displayName; }
    public byte getMaterialId() { return materialId; }
    public short getImageId() { return imageId; }
    public byte getTemperature() { return temperature; }
    public List<ImproveResourceCandidate> getChildren() { return children; }

    public String dragonLeatherColour() {
        return MaterialUtilities.getDragonLeatherMaterialNameFromColour(r, g, b);
    }

    private static String normalize(String value) {
        return ImproveTargetFingerprint.normalize(value);
    }
}
