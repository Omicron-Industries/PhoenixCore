package net.phoenix.core.integration.vocal_vibrancy;

import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class VocalVibrancyClient {

    private static final Map<BlockPos, FrequencyAnalyzer> ANALYZERS = new HashMap<>();
    private static final Map<BlockPos, LiveAcousticTracker> TRACKERS = new HashMap<>();

    private static BlockPos currentSoundPos = null;
    private static float currentSoundRange = 0f;

    public static boolean isAnyTracking() {
        return !ANALYZERS.isEmpty();
    }

    public static boolean hasSensorNear(BlockPos soundPos, float soundRange) {
        if (ANALYZERS.isEmpty()) return false;
        float rangeSq = soundRange * soundRange;
        for (BlockPos sensor : ANALYZERS.keySet()) {
            float effectiveSq = Math.max(rangeSq, 0);
            if (sensor.distSqr(soundPos) <= effectiveSq + 256) {
                return true;
            }
        }
        return false;
    }

    public static void onSoundStarted(BlockPos soundPos, float soundRange) {
        currentSoundPos = soundPos;
        currentSoundRange = soundRange;
    }

    public static void onSoundStopped() {
        currentSoundPos = null;
        currentSoundRange = 0f;
    }

    public static void onPCMBuffer(java.nio.ByteBuffer data, int sampleRate) {
        if (currentSoundPos == null || ANALYZERS.isEmpty()) return;
        float rangeSq = currentSoundRange * currentSoundRange;
        for (var entry : ANALYZERS.entrySet()) {
            BlockPos sensorPos = entry.getKey();

            float effectiveSq = Math.max(rangeSq, 256f);
            if (sensorPos.distSqr(currentSoundPos) <= effectiveSq) {
                entry.getValue().processBuffer(data.duplicate(), sampleRate);
            }
        }
    }

    public static void startTracking(BlockPos pos) {
        ANALYZERS.putIfAbsent(pos, new FrequencyAnalyzer());
        TRACKERS.putIfAbsent(pos, new LiveAcousticTracker());
    }

    public static void stopTracking(BlockPos pos) {
        FrequencyAnalyzer removed = ANALYZERS.remove(pos);
        TRACKERS.remove(pos);
        if (removed != null && ANALYZERS.isEmpty()) {
            currentSoundPos = null;
        }
    }

    public static FrequencyAnalyzer getAnalyzer(BlockPos pos) {
        return ANALYZERS.get(pos);
    }

    public static void tick() {
        if (currentSoundPos == null) return;
        TRACKERS.forEach((sensorPos, tracker) -> {
            FrequencyAnalyzer analyzer = ANALYZERS.get(sensorPos);
            if (analyzer != null) {
                tracker.tick(currentSoundPos, currentSoundRange, analyzer);
            }
        });
    }
}
