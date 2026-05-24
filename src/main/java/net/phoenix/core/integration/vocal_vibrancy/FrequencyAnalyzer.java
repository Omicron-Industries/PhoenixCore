package net.phoenix.core.integration.vocal_vibrancy;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class FrequencyAnalyzer {

    public float bass, mid, treble;
    private float[] window;

    public void processBuffer(ByteBuffer data, int sampleRate) {
        // 1. Convert byte buffer to float array (PCM)
        data.order(ByteOrder.LITTLE_ENDIAN);
        int samples = data.remaining() / 2; // 16-bit audio
        int n = Integer.highestOneBit(samples); // FFT needs power of 2

        float[] real = new float[n];
        float[] imag = new float[n];

        for (int i = 0; i < n; i++) {
            real[i] = data.getShort() / 32768.0f; // Normalize to -1.0 to 1.0
        }

        // 2. Perform FFT
        fft(real, imag, n);

        // 3. Calculate magnitudes and categorize
        float b = 0, m = 0, t = 0;
        int bassEnd = (int) (250.0 * n / sampleRate);
        int midEnd = (int) (4000.0 * n / sampleRate);

        for (int i = 0; i < n / 2; i++) {
            float mag = (float) Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);
            if (i < bassEnd) b += mag;
            else if (i < midEnd) m += mag;
            else t += mag;
        }

        this.bass = b / bassEnd;
        this.mid = m / (midEnd - bassEnd);
        this.treble = t / (n / 2 - midEnd);
    }

    // Classic Radix-2 FFT implementation
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
