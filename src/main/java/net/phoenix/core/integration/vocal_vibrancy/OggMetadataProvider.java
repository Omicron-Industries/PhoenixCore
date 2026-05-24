package net.phoenix.core.integration.vocal_vibrancy;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

public class OggMetadataProvider {

    /**
     * Reads the OGG footer to find the total PCM samples.
     * Ticks = (Samples / SampleRate) * 20
     */
    public static int getExactDurationTicks(ResourceManager manager, ResourceLocation soundLoc) {
        // 1. Convert sound ID to file path (e.g., minecraft:music_disc.pigstep ->
        // sounds/music/game/records/pigstep.ogg)
        ResourceLocation fileLoc = convertToPath(soundLoc);
        Optional<Resource> resource = manager.getResource(fileLoc);

        if (resource.isEmpty()) return 20; // Fallback 1s

        try (InputStream is = resource.get().open()) {
            byte[] allBytes = is.readAllBytes();
            if (allBytes.length < 28) return 20;

            // OGG stores the "Granule Position" (total samples) in the last page header
            // We search for the last occurrence of "OggS" (the page magic header)
            for (int i = allBytes.length - 28; i >= 0; i--) {
                if (allBytes[i] == 'O' && allBytes[i + 1] == 'g' && allBytes[i + 2] == 'g' && allBytes[i + 3] == 'S') {
                    // Granule position is at offset 6 in the header (8 bytes, little endian)
                    long granulePos = ByteBuffer.wrap(allBytes, i + 6, 8)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .getLong();

                    // Standard Minecraft sample rate is 44100Hz
                    return (int) ((granulePos / 44100.0) * 20);
                }
            }
        } catch (IOException e) {
            return 20;
        }
        return 20;
    }

    private static ResourceLocation convertToPath(ResourceLocation loc) {
        // If loc is 'minecraft:music_disc.5'
        // This turns it into 'minecraft:sounds/music_disc/5.ogg'
        // Note: Some sounds are nested deeper, but this is the standard for most records.
        String path = loc.getPath().replace(".", "/");
        return new ResourceLocation(loc.getNamespace(), "sounds/" + path + ".ogg");
    }
}
