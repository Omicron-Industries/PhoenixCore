package net.phoenix.core.integration.vocal_vibrancy;

import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side acoustic tracking.
 *
 * Each tracked sensor position gets its own {@link FrequencyAnalyzer} so that
 * sounds near machine A don't bleed into machine B's readings. The mixin
 * ({@link net.phoenix.core.mixin.minecraft.MixinOggAudioStream}) routes each
 * PCM buffer to whichever analyzer "owns" the currently-playing sound.
 *
 * The currently-active sound's origin position is stored in {@link #currentSoundPos}
 * and {@link #currentSoundRange} so the mixin can look up the right analyzer and
 * so {@link LiveAcousticTracker} can include the sound position in its packets.
 */
@OnlyIn(Dist.CLIENT)
public class VocalVibrancyClient {

    // ── Per-sensor state ────────────────────────────────────────────────────

    /** One analyzer per tracked sensor position. */
    private static final Map<BlockPos, FrequencyAnalyzer> ANALYZERS = new HashMap<>();
    private static final Map<BlockPos, LiveAcousticTracker> TRACKERS = new HashMap<>();

    // ── Currently-playing sound context (set by VibrancyEvents) ────────────

    /**
     * Position of the sound currently being decoded by {@link MixinOggAudioStream}.
     * Null when no relevant sound is active.
     */
    private static BlockPos currentSoundPos = null;
    private static float currentSoundRange = 0f;

    // ── Public API ──────────────────────────────────────────────────────────

    /** Fast-path used by MixinOggAudioStream to skip analysis when nobody is listening. */
    public static boolean isAnyTracking() {
        return !ANALYZERS.isEmpty();
    }

    /**
     * Returns true if there is at least one sensor within range of {@code soundPos}.
     * Used by {@link VibrancyEvents} to decide whether to set up PCM routing.
     */
    public static boolean hasSensorNear(BlockPos soundPos, float soundRange) {
        if (ANALYZERS.isEmpty()) return false;
        float rangeSq = soundRange * soundRange;
        for (BlockPos sensor : ANALYZERS.keySet()) {
            float effectiveSq = Math.max(rangeSq, 0);
            if (sensor.distSqr(soundPos) <= effectiveSq + 256) { // +16 block slop
                return true;
            }
        }
        return false;
    }

    /**
     * Called by {@link VibrancyEvents} when a sound starts playing.
     * Routes subsequent PCM buffers (via MixinOggAudioStream) to the right analyzers.
     */
    public static void onSoundStarted(BlockPos soundPos, float soundRange) {
        currentSoundPos = soundPos;
        currentSoundRange = soundRange;
    }

    /** Called by {@link VibrancyEvents} when the active sound stops. */
    public static void onSoundStopped() {
        currentSoundPos = null;
        currentSoundRange = 0f;
    }

    /**
     * Called by {@link MixinOggAudioStream} for each decoded PCM buffer.
     * Feeds every sensor whose range overlaps the current sound's position.
     */
    public static void onPCMBuffer(java.nio.ByteBuffer data, int sampleRate) {
        if (currentSoundPos == null || ANALYZERS.isEmpty()) return;
        float rangeSq = currentSoundRange * currentSoundRange;
        for (var entry : ANALYZERS.entrySet()) {
            BlockPos sensorPos = entry.getKey();
            // Feed this analyzer if the sound is within range of this sensor
            float effectiveSq = Math.max(rangeSq, 256f); // min 16-block sensor radius
            if (sensorPos.distSqr(currentSoundPos) <= effectiveSq) {
                entry.getValue().processBuffer(data.duplicate(), sampleRate);
            }
        }
    }

    /** Start listening at a sensor position (called by any machine wanting world audio). */
    public static void startTracking(BlockPos pos) {
        ANALYZERS.putIfAbsent(pos, new FrequencyAnalyzer());
        TRACKERS.putIfAbsent(pos, new LiveAcousticTracker());
    }

    /** Stop listening — call on structure break / GUI close / machine unload. */
    public static void stopTracking(BlockPos pos) {
        FrequencyAnalyzer removed = ANALYZERS.remove(pos);
        TRACKERS.remove(pos);
        if (removed != null && ANALYZERS.isEmpty()) {
            currentSoundPos = null;
        }
    }

    /** @return the analyzer for {@code pos}, or {@code null} if not tracked. */
    public static FrequencyAnalyzer getAnalyzer(BlockPos pos) {
        return ANALYZERS.get(pos);
    }

    /** Called every client tick to push live data to the server for all sensors. */
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
