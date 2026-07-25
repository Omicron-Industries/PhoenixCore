package net.phoenix.core.integration.phoenix_chronicles.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.platform.NativeImage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class QuestIconCache {

    private static final Map<String, ResourceLocation> CACHE = new HashMap<>();
    private static final Map<String, int[]> DIMS_CACHE = new HashMap<>();
    private static final Map<String, Boolean> MISS_CACHE = new HashMap<>();

    private QuestIconCache() {}

    public static ResourceLocation get(String questPath) {
        if (MISS_CACHE.getOrDefault(questPath, false)) return null;
        ResourceLocation cached = CACHE.get(questPath);
        if (cached != null) return cached;

        Path iconFile = iconPath(questPath);
        if (!Files.exists(iconFile)) {
            MISS_CACHE.put(questPath, true);
            return null;
        }

        try (InputStream is = Files.newInputStream(iconFile)) {
            NativeImage img = NativeImage.read(is);
            int w = img.getWidth(), h = img.getHeight();
            DynamicTexture tex = new DynamicTexture(img);
            String texName = questPath.replace('/', '_').replace(':', '_').toLowerCase();
            ResourceLocation loc = new ResourceLocation("phoenixcore", "dynamic/quest_icon/" + texName);
            Minecraft.getInstance().getTextureManager().register(loc, tex);
            CACHE.put(questPath, loc);
            DIMS_CACHE.put(questPath, new int[] { w, h });
            return loc;
        } catch (IOException e) {
            System.err.println("[Phoenix Chronicles] Failed to load icon for '" + questPath + "': " + e.getMessage());
            MISS_CACHE.put(questPath, true);
            return null;
        }
    }

    public static int[] getDimensions(String questPath) {
        return DIMS_CACHE.getOrDefault(questPath, new int[] { 64, 64 });
    }

    public static void invalidate(String questPath) {
        CACHE.remove(questPath);
        DIMS_CACHE.remove(questPath);
        MISS_CACHE.remove(questPath);
    }

    public static void invalidateAll() {
        Minecraft mc = Minecraft.getInstance();
        for (ResourceLocation loc : CACHE.values()) {
            mc.getTextureManager().release(loc);
        }
        CACHE.clear();
        DIMS_CACHE.clear();
        MISS_CACHE.clear();
    }

    private static Path iconPath(String questPath) {
        String filename = questPath.replace('/', '_').replace(':', '_').toLowerCase() + ".png";
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve("phoenix_chronicles").resolve("icons").resolve(filename);
    }
}
