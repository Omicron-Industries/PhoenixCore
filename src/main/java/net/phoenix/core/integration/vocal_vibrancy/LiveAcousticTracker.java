package net.phoenix.core.integration.vocal_vibrancy;

import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.network.packet.S2CSoundMetadataPacket;

@OnlyIn(Dist.CLIENT)
public class LiveAcousticTracker {

    private float lastSentBass = 0.0f;

    public void tick(BlockPos machinePos) {
        FrequencyAnalyzer analyzer = VocalVibrancyClient.getLiveAnalyzer();

        // Only send a packet when bass intensity changes noticeably,
        // avoiding per-tick network spam.
        if (Math.abs(analyzer.bass - lastSentBass) > 0.1f) {
            this.lastSentBass = analyzer.bass;

            // duration == -1 signals the machine to only update bass, not reset
            // the remainingSoundTicks counter (handled in syncAcousticData).
            PhoenixNetwork.CHANNEL.sendToServer(
                    new S2CSoundMetadataPacket(machinePos, -1, analyzer.bass));
        }
    }
}
