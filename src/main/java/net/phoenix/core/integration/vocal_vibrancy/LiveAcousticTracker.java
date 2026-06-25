package net.phoenix.core.integration.vocal_vibrancy;

import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.network.packet.C2SSoundMetadataPacket;

/**
 * Per-sensor bandwidth throttle.
 *
 * Compares the latest analyzer readings against what was last sent and only
 * fires a packet when something meaningful changed, keeping network traffic low
 * while still giving the server up-to-date data for recipe matching.
 */
@OnlyIn(Dist.CLIENT)
public class LiveAcousticTracker {

    private float lastBass = 0f;
    private float lastMid = 0f;
    private float lastTreble = 0f;
    private int lastBpm = 0;

    /**
     * @param soundPos   world position the sound originated from
     * @param soundRange propagation radius of the sound (blocks)
     * @param analyzer   the per-sensor FrequencyAnalyzer with current FFT data
     */
    public void tick(BlockPos soundPos, float soundRange, FrequencyAnalyzer analyzer) {
        boolean bassChanged = Math.abs(analyzer.bass - lastBass) > 0.05f;
        boolean midChanged = Math.abs(analyzer.mid - lastMid) > 0.05f;
        boolean trebleChanged = Math.abs(analyzer.treble - lastTreble) > 0.05f;
        boolean bpmChanged = analyzer.bpm != lastBpm;

        if (bassChanged || midChanged || trebleChanged || bpmChanged) {
            lastBass = analyzer.bass;
            lastMid = analyzer.mid;
            lastTreble = analyzer.treble;
            lastBpm = analyzer.bpm;

            // -1 duration = live update only; don't reset any countdown on the server
            PhoenixNetwork.CHANNEL.sendToServer(new C2SSoundMetadataPacket(
                    soundPos, soundRange, -1,
                    analyzer.bass, analyzer.mid, analyzer.treble, analyzer.bpm));
        }
    }
}
