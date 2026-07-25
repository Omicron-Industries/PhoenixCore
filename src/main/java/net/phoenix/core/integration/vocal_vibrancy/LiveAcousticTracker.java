package net.phoenix.core.integration.vocal_vibrancy;

import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.network.packet.C2SSoundMetadataPacket;

@OnlyIn(Dist.CLIENT)
public class LiveAcousticTracker {

    private float lastBass = 0f;
    private float lastMid = 0f;
    private float lastTreble = 0f;
    private int lastBpm = 0;

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

            PhoenixNetwork.CHANNEL.sendToServer(new C2SSoundMetadataPacket(
                    soundPos, soundRange, -1,
                    analyzer.bass, analyzer.mid, analyzer.treble, analyzer.bpm));
        }
    }
}
