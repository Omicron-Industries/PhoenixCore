package net.phoenix.core.integration.vocal_vibrancy;

import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side registry of machines that want to "hear" the world around them.
 *
 * Any machine (not just the jukebox) can register here with a listen radius.
 * {@link C2SSoundMetadataPacket} will update the stored data for whichever
 * sensor pos it targets, and {@link NotifiableSoundHandler} reads it from here.
 *
 * Registration: call {@link #register} from your machine's onStructureFormed / onLoad.
 * Deregistration: call {@link #unregister} from onStructureInvalid / onUnload.
 *
 * Thread-safety: uses ConcurrentHashMap; reads/writes from server tick and
 * network thread are both safe.
 */
public class WorldAcousticSensor {

    public static final class SensorData {

        /** Blocks — how far this sensor listens. Set by the registering machine. */
        public final int listenRadius;

        /** Latest bass energy reported by the client, 0.0–10.0. */
        public volatile float bass = 0f;
        /** Latest BPM detected by the client. */
        public volatile int bpm = 0;
        /** Latest mid-range energy. */
        public volatile float mid = 0f;
        /** Latest treble energy. */
        public volatile float treble = 0f;
        /** Track duration in ticks as reported when the sound started (-1 = unknown). */
        public volatile int durationTicks = -1;

        public SensorData(int listenRadius) {
            this.listenRadius = listenRadius;
        }

        public void update(int durationTicks, float bass, float mid, float treble, int bpm) {
            if (durationTicks >= 0) this.durationTicks = durationTicks;
            this.bass = bass;
            this.mid = mid;
            this.treble = treble;
            this.bpm = bpm;
        }

        public void reset() {
            this.bass = 0f;
            this.mid = 0f;
            this.treble = 0f;
            this.bpm = 0;
            this.durationTicks = -1;
        }
    }

    // Keyed by dimension resource key string + BlockPos for multi-world safety.
    // Most callers will only need BlockPos, so we offer both lookup styles.
    private static final Map<BlockPos, SensorData> SENSORS = new ConcurrentHashMap<>();

    /** Register a machine as a world-acoustic sensor with the given listen radius. */
    public static void register(BlockPos pos, int listenRadius) {
        SENSORS.putIfAbsent(pos, new SensorData(listenRadius));
    }

    /** Remove a sensor — call on structure break / machine unload. */
    public static void unregister(BlockPos pos) {
        SENSORS.remove(pos);
    }

    /** @return the sensor at {@code pos}, or {@code null} if not registered. */
    public static SensorData get(BlockPos pos) {
        return SENSORS.get(pos);
    }

    /** @return an unmodifiable view of all active sensors (for client-side range checks). */
    public static Map<BlockPos, SensorData> all() {
        return Collections.unmodifiableMap(SENSORS);
    }

    /**
     * Find all registered sensors within {@code soundRange} blocks of {@code soundPos}
     * and update their data. Called from {@link C2SSoundMetadataPacket} on the server.
     *
     * @param soundPos      world position the sound originated from
     * @param soundRange    how far the sound reaches (blocks)
     * @param durationTicks track length in ticks; -1 for a live-update-only call
     * @param bass          FFT bass energy
     * @param mid           FFT mid energy
     * @param treble        FFT treble energy
     * @param bpm           detected BPM
     */
    public static void onSoundData(BlockPos soundPos, float soundRange,
                                   int durationTicks, float bass, float mid, float treble, int bpm) {
        float rangeSq = soundRange * soundRange;
        for (var entry : SENSORS.entrySet()) {
            BlockPos sensorPos = entry.getKey();
            SensorData data = entry.getValue();

            // The sensor hears the sound if the sound is within EITHER the sound's own
            // propagation range OR the sensor's listen radius, whichever is larger.
            float effectiveRangeSq = Math.max(rangeSq,
                    (float) data.listenRadius * data.listenRadius);

            if (sensorPos.distSqr(soundPos) <= effectiveRangeSq) {
                data.update(durationTicks, bass, mid, treble, bpm);
            }
        }
    }

    /** Clear all sensors — e.g. on server stop. */
    public static void clear() {
        SENSORS.clear();
    }
}
