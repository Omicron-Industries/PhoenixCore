package net.phoenix.core.integration.vocal_resonance;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.PacketDistributor;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.network.packet.S2CPlayStreamPacket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RadioStreamManager {

    private static final Logger LOGGER = LogManager.getLogger("VocalResonance");

    public static void loadAndPlay(String url, ResonantJukeboxMachine controller) {
        if (url == null || url.isEmpty()) return;

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            LOGGER.warn("Rejecting non-HTTP stream URL: {}", url);
            controller.setStreamTitle("§cInvalid URL (must be http/https)");
            return;
        }

        int radius = controller.getFinalRange();

        PhoenixNetwork.CHANNEL.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        controller.getBlockPos().getX(),
                        controller.getBlockPos().getY(),
                        controller.getBlockPos().getZ(),
                        radius,
                        controller.getLevel().dimension())),
                new S2CPlayStreamPacket(url, controller.getBlockPos(), radius, controller.getResonancePower()));

        String label = isYoutube(url) ? "YT Audio" : shortenUrl(url);
        controller.setStreamTitle("§eResolving: §f" + label);
        LOGGER.info("Dispatched stream URL to clients in range {}: {}", radius, url);
    }

    private static boolean isYoutube(String url) {
        return url.contains("youtube.com") || url.contains("youtu.be") || url.contains("music.youtube.com");
    }

    private static String shortenUrl(String url) {
        return url.length() > 40 ? url.substring(0, 37) + "..." : url;
    }

    public static float getPropagationFactor(ResourceLocation sound) {
        String path = sound.getPath();
        if (path.contains("explosion") || path.contains("thunder") || path.contains("dragon")) return 8.0f;
        if (path.contains("warden") || path.contains("wither") || path.contains("bell")) return 5.0f;
        if (path.contains("shout") || path.contains("scream") || path.contains("roar")) return 3.0f;
        if (path.startsWith("music_disc")) return 2.5f;
        if (path.startsWith("block.note_block")) return 1.5f;
        if (path.contains("step") || path.contains("drip") || path.contains("ui.")) return 0.2f;
        if (path.contains("amethyst") || path.contains("bubble")) return 0.5f;
        return 1.0f;
    }
}
