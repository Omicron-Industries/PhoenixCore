package net.phoenix.core.integration.vocal_resonance;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.phoenix.core.PhoenixCore;

import org.jcodec.codecs.aac.AACDecoder;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.AudioBuffer;
import org.jcodec.common.model.Packet;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;

import javax.sound.sampled.*;

@OnlyIn(Dist.CLIENT)
public class RadioClientAudio extends AbstractSoundInstance implements TickableSoundInstance {

    private final String rawUrl;
    private final float maxRange;
    private final float baseVolume;

    private volatile boolean stopped = false;
    private volatile boolean playing = true;

    private Thread streamThread;
    private SourceDataLine outputLine;

    private volatile float currentVolume = 1.0f;
    private int debugLogTick = 0;

    public RadioClientAudio(String url, BlockPos pos, float range, float baseVolume) {

        super(new ResourceLocation("minecraft", "intentionally_empty"),
                SoundSource.RECORDS, RandomSource.create());
        this.rawUrl = url;
        this.maxRange = range;
        this.baseVolume = baseVolume;
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.5;
        this.z = pos.getZ() + 0.5;
        this.volume = 1.0f;     
        this.attenuation = Attenuation.NONE;
        this.looping = false;

        streamThread = new Thread(this::resolveAndStream, "VocalResonance-Stream");
        streamThread.setDaemon(true);
        streamThread.start();
    }

    public float getMaxRange() {
        return maxRange;
    }

    @Override
    public void tick() {

        if (stopped || !playing) return;
        var player = Minecraft.getInstance().player;
        if (player == null) stopStreaming();
    }

    public void updateVolume(net.minecraft.world.entity.player.Player player) {
        if (stopped || !playing) return;
        double dx = player.getX() - x, dy = player.getY() - y, dz = player.getZ() - z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        float effectiveBase = Math.min(1.0f, baseVolume);
        float newVol = distance >= maxRange ? 0.0f : effectiveBase * (1.0f - (float) (distance / maxRange));
        currentVolume = newVol;
        if (++debugLogTick % 40 == 0) {
            PhoenixCore.LOGGER.info("VR volume: dist={} maxRange={} vol={} line={}",
                    String.format("%.1f", distance), maxRange, String.format("%.3f", newVol),
                    outputLine != null && outputLine.isOpen() ? "open" : "null/closed");
        }
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }

    private void resolveAndStream() {
        PhoenixCore.LOGGER.info("VocalResonance: opening stream: {}", rawUrl);
        try {
            openStream(rawUrl);
        } catch (Exception e) {
            PhoenixCore.LOGGER.warn("VocalResonance stream error: {}", e.getMessage());
            stopStreaming();
        }
    }

    private void openStream(String streamUrl) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(streamUrl).openConnection();
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(0);
            conn.connect();

            int code = conn.getResponseCode();
            if (code != 200 && code != 206) {
                PhoenixCore.LOGGER.error("VocalResonance: stream HTTP {} for URL: {}", code, streamUrl);
                stopStreaming();
                return;
            }

            String contentType = conn.getContentType();
            PhoenixCore.LOGGER.info("VocalResonance: stream Content-Type='{}' url={}", contentType,
                    streamUrl.length() > 80 ? streamUrl.substring(0, 80) + "..." : streamUrl);

            boolean isM4A = contentType != null &&
                    (contentType.startsWith("audio/mp4") || contentType.startsWith("video/mp4"));
            boolean isMp3 = contentType != null &&
                    (contentType.contains("mpeg") || contentType.contains("mp3"));

            if (isM4A) {
                openM4aStream(conn);
            } else if (isMp3) {
                openMp3Stream(conn);
            } else {
                openJavaxStream(conn);
            }

        } catch (Exception e) {
            PhoenixCore.LOGGER.error("VocalResonance: stream playback error", e);
            stopStreaming();
        } finally {
            if (outputLine != null) {
                outputLine.drain();
                outputLine.close();
            }
            if (conn != null) conn.disconnect();
        }
    }

    private void openM4aStream(HttpURLConnection conn) throws Exception {
        PhoenixCore.LOGGER.info("VocalResonance: opening M4A stream via jcodec");
        InputStream httpIn = new BufferedInputStream(conn.getInputStream(), 65536);
        HttpStreamingM4aChannel channel = new HttpStreamingM4aChannel(httpIn);

        MP4Demuxer demuxer;
        try {
            demuxer = MP4Demuxer.createMP4Demuxer(channel);
        } catch (Exception e) {
            PhoenixCore.LOGGER.error("VocalResonance: jcodec failed to parse M4A container", e);
            stopStreaming();
            return;
        }

        List<DemuxerTrack> audioTracks = demuxer.getAudioTracks();
        if (audioTracks.isEmpty()) {
            PhoenixCore.LOGGER.error("VocalResonance: no audio tracks found in M4A stream");
            stopStreaming();
            return;
        }

        DemuxerTrack track = audioTracks.get(0);
        java.nio.ByteBuffer codecPrivate = track.getMeta().getCodecPrivate();
        if (codecPrivate == null) {
            PhoenixCore.LOGGER.error("VocalResonance: M4A track has no codec private data (AAC config missing)");
            stopStreaming();
            return;
        }
        PhoenixCore.LOGGER.info("VocalResonance: codec private {} bytes, creating AACDecoder",
                codecPrivate.remaining());

        AACDecoder decoder;
        try {
            decoder = new AACDecoder(codecPrivate);
        } catch (Exception e) {
            PhoenixCore.LOGGER.error("VocalResonance: AACDecoder init failed", e);
            stopStreaming();
            return;
        }

        var audioMeta = track.getMeta().getAudioCodecMeta();
        int sampleRate = audioMeta != null && audioMeta.getSampleRate() > 0 ? audioMeta.getSampleRate() : 44100;
        int channels = audioMeta != null && audioMeta.getChannelCount() > 0 ? audioMeta.getChannelCount() : 2;

        AudioFormat pcmFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sampleRate, 16, channels, channels * 2, sampleRate, false);

        DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, pcmFormat);
        outputLine = (SourceDataLine) AudioSystem.getLine(lineInfo);
        outputLine.open(pcmFormat, 16384);
        outputLine.start();

        PhoenixCore.LOGGER.info("VocalResonance: M4A stream open — {}Hz {}ch, playing", sampleRate, channels);

        ByteBuffer decodeBuf = ByteBuffer.allocate(8192);
        Packet packet;
        while (playing && !stopped && (packet = track.nextFrame()) != null) {
            decodeBuf.clear();
            AudioBuffer audioBuf = decoder.decodeFrame(packet.getData(), decodeBuf);
            if (audioBuf == null) continue;
            ByteBuffer pcm = audioBuf.getData();
            byte[] arr = new byte[pcm.remaining()];
            pcm.get(arr);
            scalePcm(arr, arr.length);
            outputLine.write(arr, 0, arr.length);
        }
    }

    private static final class HttpStreamingM4aChannel implements SeekableByteChannel {

        private final InputStream http;
        private byte[] buf = new byte[131072]; 
        private int filled = 0;                
        private long pos = 0;
        private boolean done = false;

        HttpStreamingM4aChannel(InputStream http) {
            this.http = http;
        }

        private void fetchUpTo(long target) throws java.io.IOException {
            if (done || filled >= target) return;
            if (target > buf.length) {
                int newLen = Math.max((int) target, buf.length * 2);
                buf = java.util.Arrays.copyOf(buf, newLen);
            }
            while (filled < target && !done) {
                int want = (int) Math.min(8192, target - filled);
                int got = http.read(buf, filled, want);
                if (got == -1) {
                    done = true;
                } else {
                    filled += got;
                }
            }
        }

        @Override
        public int read(ByteBuffer dst) throws java.io.IOException {
            fetchUpTo(pos + dst.remaining());
            if (pos >= filled) return -1;
            int n = (int) Math.min(dst.remaining(), filled - pos);
            dst.put(buf, (int) pos, n);
            pos += n;
            return n;
        }

        @Override
        public int write(ByteBuffer src) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long position() {
            return pos;
        }

        @Override
        public SeekableByteChannel setPosition(long newPos) throws java.io.IOException {
            fetchUpTo(newPos);
            pos = newPos;
            return this;
        }

        @Override
        public long size() {
            return filled;
        }

        @Override
        public SeekableByteChannel truncate(long s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public void close() throws java.io.IOException {
            http.close();
        }
    }

    private void openMp3Stream(HttpURLConnection conn) throws Exception {
        InputStream raw = new BufferedInputStream(conn.getInputStream(), 65536);
        javazoom.jl.decoder.Bitstream bitstream = new javazoom.jl.decoder.Bitstream(raw);
        javazoom.jl.decoder.Decoder decoder = new javazoom.jl.decoder.Decoder();

        javazoom.jl.decoder.Header header = bitstream.readFrame();
        if (header == null) {
            PhoenixCore.LOGGER.error("VocalResonance: MP3 stream has no frames");
            stopStreaming();
            return;
        }

        int sampleRate = header.frequency();
        int channels = header.mode() == javazoom.jl.decoder.Header.SINGLE_CHANNEL ? 1 : 2;
        AudioFormat pcmFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED, sampleRate, 16, channels, channels * 2, sampleRate, false);

        DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, pcmFormat);
        if (!AudioSystem.isLineSupported(lineInfo)) {
            PhoenixCore.LOGGER.error("VocalResonance: SourceDataLine not supported for {}Hz {}ch", sampleRate,
                    channels);
            stopStreaming();
            return;
        }
        outputLine = (SourceDataLine) AudioSystem.getLine(lineInfo);
        outputLine.open(pcmFormat, 16384);
        outputLine.start();
        PhoenixCore.LOGGER.info("VocalResonance: MP3 stream open — {}Hz {}ch MASTER_GAIN_supported={}",
                sampleRate, channels, outputLine.isControlSupported(FloatControl.Type.MASTER_GAIN));

        int frameCount = 0;
        while (playing && !stopped) {
            javazoom.jl.decoder.SampleBuffer samples = (javazoom.jl.decoder.SampleBuffer) decoder.decodeFrame(header,
                    bitstream);
            bitstream.closeFrame();

            short[] buf = samples.getBuffer();
            int len = samples.getBufferLength();
            byte[] pcm = new byte[len * 2];
            float vol = currentVolume;
            if (++frameCount % 500 == 0) {
                PhoenixCore.LOGGER.info("VR MP3 decode: frame={} vol={}", frameCount, String.format("%.3f", vol));
            }
            for (int i = 0; i < len; i++) {
                short s = (short) (buf[i] * vol);
                pcm[i * 2] = (byte) (s & 0xFF);
                pcm[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
            }
            outputLine.write(pcm, 0, pcm.length);

            header = bitstream.readFrame();
            if (header == null) break;
        }
    }

    private void openJavaxStream(HttpURLConnection conn) throws Exception {
        try (InputStream raw = new BufferedInputStream(conn.getInputStream(), 65536)) {
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(raw);
            AudioFormat base = audioIn.getFormat();

            AudioFormat pcmFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    base.getSampleRate() > 0 ? base.getSampleRate() : 44100f,
                    16,
                    base.getChannels() > 0 ? base.getChannels() : 2,
                    base.getChannels() > 0 ? base.getChannels() * 2 : 4,
                    base.getSampleRate() > 0 ? base.getSampleRate() : 44100f,
                    false);

            AudioInputStream pcmIn = AudioSystem.getAudioInputStream(pcmFormat, audioIn);
            DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, pcmFormat);
            if (!AudioSystem.isLineSupported(lineInfo)) {
                PhoenixCore.LOGGER.error("VocalResonance: SourceDataLine not supported for this audio format");
                stopStreaming();
                return;
            }

            outputLine = (SourceDataLine) AudioSystem.getLine(lineInfo);
            outputLine.open(pcmFormat, 16384);
            outputLine.start();
            PhoenixCore.LOGGER.info("VocalResonance: stream open — {}Hz {}ch",
                    (int) pcmFormat.getSampleRate(), pcmFormat.getChannels());

            byte[] buf = new byte[4096];
            int read;
            while (playing && !stopped && (read = pcmIn.read(buf)) != -1) {
                scalePcm(buf, read);
                outputLine.write(buf, 0, read);
            }
        } catch (UnsupportedAudioFileException e) {
            PhoenixCore.LOGGER.error("VocalResonance: unsupported audio format — {}. " +
                    "Use a direct MP3/OGG stream URL, or M4A (handled by jcodec).", e.getMessage());
            stopStreaming();
        }
    }

    private void scalePcm(byte[] buf, int len) {
        float vol = currentVolume;
        for (int i = 0; i + 1 < len; i += 2) {
            short s = (short) ((buf[i] & 0xFF) | (buf[i + 1] << 8));
            s = (short) (s * vol);
            buf[i] = (byte) (s & 0xFF);
            buf[i + 1] = (byte) ((s >> 8) & 0xFF);
        }
    }

    public void stopStreaming() {
        playing = false;
        stopped = true;
        if (outputLine != null) outputLine.stop();
        if (streamThread != null) streamThread.interrupt();
        
        Minecraft.getInstance().submit(() -> Minecraft.getInstance().getSoundManager().stop(this));
    }
}
