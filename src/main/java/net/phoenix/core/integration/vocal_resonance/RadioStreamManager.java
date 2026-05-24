package net.phoenix.core.integration.vocal_resonance;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.PacketDistributor;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.network.packet.S2CPlayStreamPacket;

import java.net.URL;
import java.net.URLConnection;

public class RadioStreamManager {

    public static String convertToStream(String url) {
        if (url == null || url.isEmpty()) return "";

        if (url.contains("youtube.com") || url.contains("youtu.be") || url.contains("music.youtube.com")) {
            String videoId = "";
            try {
                if (url.contains("v=")) {
                    videoId = url.split("v=")[1].split("&")[0];
                } else if (url.contains("youtu.be/")) {
                    videoId = url.split("youtu.be/")[1].split("\\?")[0];
                } else if (url.contains("embed/")) {
                    videoId = url.split("embed/")[1].split("\\?")[0];
                }

                if (!videoId.isEmpty()) {
                    return "https://invidious.projectsegfau.lt/latest_version?id=" + videoId + "&itag=140";
                }
            } catch (Exception e) {
                return url;
            }
        }
        return url;
    }

    public static void loadAndPlay(String url, ResonantJukeboxMachine controller) {
        if (url == null || url.isEmpty()) return;

        String streamUrl = convertToStream(url);

        Thread t = new Thread(() -> {
            try {
                // Pre-verify the link before sending any packets
                URLConnection connection = new URL(streamUrl).openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.connect();

                int radius = controller.getFinalRange();

                PhoenixNetwork.CHANNEL.send(
                        PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                                controller.getPos().getX(),
                                controller.getPos().getY(),
                                controller.getPos().getZ(),
                                radius,
                                controller.getLevel().dimension())),
                        new S2CPlayStreamPacket(streamUrl, controller.getPos(), radius));

                controller.setStreamTitle("§aStreaming: §f" + (url.length() > 20 ? "YT Audio" : url));

            } catch (Exception e) {
                // Expected: unreachable URL, timeout, bad redirect, etc.
                controller.setStreamTitle("§cStream Error (Inaccessible)");
            } catch (Throwable t2) {
                // Safety net: never let an uncaught Throwable escape a background
                // server thread — that would crash the entire server.
                controller.setStreamTitle("§cStream Error (Fatal)");
                net.minecraftforge.fml.ModLoader.get();  // keep class ref to avoid import strip
                org.apache.logging.log4j.LogManager.getLogger("VocalResonance")
                        .error("Fatal error in RadioStreamManager thread", t2);
            }
        }, "VocalResonance-Stream");
        t.setDaemon(true);
        t.start();
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
