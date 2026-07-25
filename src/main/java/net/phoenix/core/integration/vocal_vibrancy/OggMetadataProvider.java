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

    public static int getExactDurationTicks(ResourceManager manager, ResourceLocation soundLoc) {
        ResourceLocation fileLoc = convertToPath(soundLoc);
        Optional<Resource> resource = manager.getResource(fileLoc);

        if (resource.isEmpty()) return 20;

        try (InputStream is = resource.get().open()) {
            byte[] allBytes = is.readAllBytes();
            if (allBytes.length < 28) return 20;

            for (int i = allBytes.length - 28; i >= 0; i--) {
                if (allBytes[i] == 'O' && allBytes[i + 1] == 'g' && allBytes[i + 2] == 'g' && allBytes[i + 3] == 'S') {

                    long granulePos = ByteBuffer.wrap(allBytes, i + 6, 8)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .getLong();

                    return (int) ((granulePos / 44100.0) * 20);
                }
            }
        } catch (IOException e) {
            return 20;
        }
        return 20;
    }

    private static ResourceLocation convertToPath(ResourceLocation loc) {
        String path = loc.getPath().replace(".", "/");
        return new ResourceLocation(loc.getNamespace(), "sounds/" + path + ".ogg");
    }
}
