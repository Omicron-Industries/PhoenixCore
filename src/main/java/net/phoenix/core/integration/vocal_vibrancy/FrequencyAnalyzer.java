package net.phoenix.core.integration.vocal_vibrancy;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class FrequencyAnalyzer {

    public float bass, mid, treble;
    public int bpm = 120;

    private static final int HISTORY_SIZE = 43;
    private final float[] bassHistory = new float[HISTORY_SIZE];
    private int historyIndex = 0;

    private long lastBeatTimeMs = 0;
    private float bpmBuffer = 120.0f;
    private static final long DEBOUNCE_MS = 250;

    public void reset() {
        this.bass = 0f;
        this.mid = 0f;
        this.treble = 0f;
        this.bpm = 120;
        this.bpmBuffer = 120.0f;
        this.historyIndex = 0;
        this.lastBeatTimeMs = 0;
        java.util.Arrays.fill(bassHistory, 0f);
    }

    public void processBuffer(ByteBuffer data, int sampleRate) {
        data.order(ByteOrder.LITTLE_ENDIAN);
        int samples = data.remaining() / 2;
        if (samples <= 0) return;

        int n = Integer.highestOneBit(samples);
        if (n <= 0) return;

        float[] real = new float[n];
        float[] imag = new float[n];

        for (int i = 0; i < n; i++) {
            real[i] = data.getShort() / 32768.0f;
        }

        fft(real, imag, n);

        float b = 0, m = 0, t = 0;
        int bassEnd = Math.max(1, (int) (250.0 * n / sampleRate));
        int midEnd = Math.max(bassEnd + 1, (int) (4000.0 * n / sampleRate));

        for (int i = 0; i < n / 2; i++) {
            float mag = (float) Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);
            if (i < bassEnd) b += mag;
            else if (i < midEnd) m += mag;
            else t += mag;
        }

        this.bass = b / bassEnd;
        this.mid = m / (midEnd - bassEnd);
        this.treble = t / (Math.max(1, n / 2 - midEnd));

        detectBeatAndCalculateBPM(this.bass);
    }

    private void detectBeatAndCalculateBPM(float currentBassEnergy) {
        float historyAverage = 0.0f;
        for (float val : bassHistory) {
            historyAverage += val;
        }
        historyAverage /= HISTORY_SIZE;

        float variance = 0.0f;
        for (float val : bassHistory) {
            variance += (float) Math.pow(val - historyAverage, 2);
        }
        variance /= HISTORY_SIZE;

        float dynamicC = (-0.0025714f * variance) + 1.5142857f;
        dynamicC = Math.max(1.3f, Math.min(dynamicC, 1.65f));

        long currentTimeMs = System.currentTimeMillis();

        if (currentBassEnergy > (dynamicC * historyAverage)) {
            long timeGap = currentTimeMs - lastBeatTimeMs;

            if (timeGap > DEBOUNCE_MS && timeGap < 2000) {
                float rawBpm = 60000.0f / timeGap;
                bpmBuffer = (bpmBuffer * 0.85f) + (rawBpm * 0.15f);
                this.bpm = Math.round(bpmBuffer);
                this.lastBeatTimeMs = currentTimeMs;
            }
        }

        bassHistory[historyIndex] = currentBassEnergy;
        historyIndex = (historyIndex + 1) % HISTORY_SIZE;
    }

    private void fft(float[] real, float[] imag, int n) {
        int j = 0;
        for (int i = 0; i < n; i++) {
            if (i < j) {
                float temp = real[i];
                real[i] = real[j];
                real[j] = temp;
                temp = imag[i];
                imag[i] = imag[j];
                imag[j] = temp;
            }
            int m = n >> 1;
            while (m >= 1 && j >= m) {
                j -= m;
                m >>= 1;
            }
            j += m;
        }

        for (int len = 2; len <= n; len <<= 1) {
            double ang = 2 * Math.PI / len;
            float wreal = (float) Math.cos(ang);
            float wimag = (float) Math.sin(ang);
            for (int i = 0; i < n; i += len) {
                float ureal = 1, uimag = 0;
                for (int k = 0; k < len / 2; k++) {
                    int a = i + k;
                    int b = i + k + len / 2;
                    float vreal = real[b] * ureal - imag[b] * uimag;
                    float vimag = real[b] * uimag + imag[b] * ureal;
                    real[b] = real[a] - vreal;
                    imag[b] = imag[a] - vimag;
                    real[a] += vreal;
                    imag[a] += vimag;
                    float next_ureal = ureal * wreal - uimag * wimag;
                    uimag = ureal * wimag + uimag * wreal;
                    ureal = next_ureal;
                }
            }
        }
    }
}
