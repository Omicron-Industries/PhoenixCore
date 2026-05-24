package net.phoenix.core.integration.vocal_vibrancy;

import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class VocalVibrancyClient {

    private static final FrequencyAnalyzer LIVE_ANALYZER = new FrequencyAnalyzer();
    private static final Map<BlockPos, LiveAcousticTracker> ACTIVE_TRACKERS = new HashMap<>();

    public static FrequencyAnalyzer getLiveAnalyzer() {
        return LIVE_ANALYZER;
    }

    /**
     * Returns true if a machine at this position is currently being tracked.
     * Used by VibrancyEvents to filter out irrelevant sounds.
     */
    public static boolean isTracking(BlockPos pos) {
        if (ACTIVE_TRACKERS.isEmpty()) return false;
        // Check within a small radius — the sound position (int-cast) may be
        // up to 0.5 blocks off from the exact machine pos.
        for (BlockPos tracked : ACTIVE_TRACKERS.keySet()) {
            if (tracked.distSqr(pos) <= 4) return true; // within ~2 blocks
        }
        return false;
    }

    /** Start tracking a specific machine's audio. */
    public static void startTracking(BlockPos pos) {
        ACTIVE_TRACKERS.put(pos, new LiveAcousticTracker());
    }

    /** Called every client tick to process ongoing audio data. */
    public static void tick() {
        ACTIVE_TRACKERS.forEach((pos, tracker) -> tracker.tick(pos));
    }

    public static void stopTracking(BlockPos pos) {
        ACTIVE_TRACKERS.remove(pos);
    }
}
