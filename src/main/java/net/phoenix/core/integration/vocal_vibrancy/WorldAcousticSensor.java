package net.phoenix.core.integration.vocal_vibrancy;

import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WorldAcousticSensor {

    public static final class SensorData {

        public final int listenRadius;

        public volatile float bass = 0f;

        public volatile int bpm = 0;

        public volatile float mid = 0f;

        public volatile float treble = 0f;

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

    private static final Map<BlockPos, SensorData> SENSORS = new ConcurrentHashMap<>();

    public static void register(BlockPos pos, int listenRadius) {
        SENSORS.putIfAbsent(pos, new SensorData(listenRadius));
    }

    public static void unregister(BlockPos pos) {
        SENSORS.remove(pos);
    }

    public static SensorData get(BlockPos pos) {
        return SENSORS.get(pos);
    }

    public static Map<BlockPos, SensorData> all() {
        return Collections.unmodifiableMap(SENSORS);
    }

    public static void onSoundData(BlockPos soundPos, float soundRange,
                                   int durationTicks, float bass, float mid, float treble, int bpm) {
        float rangeSq = soundRange * soundRange;
        for (var entry : SENSORS.entrySet()) {
            BlockPos sensorPos = entry.getKey();
            SensorData data = entry.getValue();

            float effectiveRangeSq = Math.max(rangeSq,
                    (float) data.listenRadius * data.listenRadius);

            if (sensorPos.distSqr(soundPos) <= effectiveRangeSq) {
                data.update(durationTicks, bass, mid, treble, bpm);
            }
        }
    }

    public static void clear() {
        SENSORS.clear();
    }
}
