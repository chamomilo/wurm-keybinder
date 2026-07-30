package com.wurmonline.client.resources.textures;

import com.wurmonline.client.resources.KeybinderFileResourceUrl;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class KeybinderTextureFactory {
    private static final String ASSET_DIRECTORY = "mods/keybinder/assets/keybinder/";
    private static final Logger LOGGER = Logger.getLogger(KeybinderTextureFactory.class.getName());

    private KeybinderTextureFactory() {}

    public static ResourceTexture load(String path) {
        try {
            KeybinderFileResourceUrl url = new KeybinderFileResourceUrl(ASSET_DIRECTORY + path);
            if (!url.exists()) throw new IllegalArgumentException("Missing image " + url.getFilePath());
            ResourceTexture texture = ResourceTextureLoader.getInternalTexture(
                    url, TextureLoader.Filter.LINEAR, false, false, false);
            if (texture == null) throw new IllegalStateException("Texture loader returned no texture for " + path);
            return texture;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unable to load Keybinder texture " + path, e);
            return ResourceTextureLoader.getTexture("img.texture.broken");
        }
    }
}
