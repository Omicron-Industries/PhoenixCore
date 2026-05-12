package net.phoenix.core.integration.vocal_resonance;

import net.minecraftforge.network.PacketDistributor;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.network.packet.S2CPlayStreamPacket;

import java.net.URL;
import java.net.URLConnection;

public class RadioStreamManager {

    public static String convertToStream(String url) {
        if (url == null || url.isEmpty()) return "";

        // Support for Standard YT, Mobile YT, and YT Music
        if (url.contains("youtube.com") || url.contains("youtu.be") || url.contains("music.youtube.com")) {
            String videoId = "";
            try {
                if (url.contains("v=")) {
                    // Splits at 'v=', then takes the ID before any following '&' parameters
                    videoId = url.split("v=")[1].split("&")[0];
                } else if (url.contains("youtu.be/")) {
                    // Splits at the slash for shortened links
                    videoId = url.split("youtu.be/")[1].split("\\?")[0];
                }

                if (!videoId.isEmpty()) {
                    // itag 140 is the high-quality AAC audio-only stream (works best for streaming)
                    return "https://invidious.projectsegfau.lt/latest_version?id=" + videoId + "&itag=140";
                }
            } catch (Exception e) {
                // If parsing fails, return original and hope it's a direct link
                return url;
            }
        }
        return url;
    }

    public static void loadAndPlay(String url, ResonantJukeboxMachine controller) {
        String streamUrl = convertToStream(url);

        new Thread(() -> {
            try {
                URLConnection connection = new URL(streamUrl).openConnection();
                connection.setConnectTimeout(5000);
                connection.connect();

                // Success! Broadcast to nearby players
                int radius = controller.getFinalRange();

                PhoenixNetwork.CHANNEL.send(
                        PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                                controller.getPos().getX(), controller.getPos().getY(), controller.getPos().getZ(),
                                radius, controller.getLevel().dimension())),
                        new S2CPlayStreamPacket(streamUrl, controller.getPos(), radius)
                );

                controller.setStreamTitle("Streaming: " + url);
            } catch (Exception e) {
                controller.setStreamTitle("§cStream Failed (Link Invalid)");
            }
        }).start();
    }
}