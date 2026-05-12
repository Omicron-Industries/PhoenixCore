package net.phoenix.core.integration.vocal_resonance;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import java.io.InputStream;
import java.net.URL;

public class RadioClientAudio extends AbstractTickableSoundInstance {
    // Static reference for the packet to find and kill the current stream
    public static RadioClientAudio currentInstance = null;

    private final String streamUrl;
    private Thread streamThread;
    private volatile boolean playing = true;

    public RadioClientAudio(String url, BlockPos pos) {
        super(SoundEvents.MUSIC_DISC_5, SoundSource.RECORDS, net.minecraft.util.RandomSource.create());
        this.streamUrl = url;
        this.x = (float) pos.getX();
        this.y = (float) pos.getY();
        this.z = (float) pos.getZ();
        this.relative = false;

        startStreaming();
    }

    private void startStreaming() {
        streamThread = new Thread(() -> {
            AudioDevice device = null;
            try (InputStream in = new URL(streamUrl).openStream()) {
                Bitstream bitstream = new Bitstream(in);
                Decoder decoder = new Decoder();
                device = FactoryRegistry.systemRegistry().createAudioDevice();
                device.open(decoder);

                while (playing && !this.isStopped()) { // Check Minecraft's state too
                    javazoom.jl.decoder.Header frame = bitstream.readFrame();
                    if (frame == null) break;

                    javazoom.jl.decoder.SampleBuffer output = (javazoom.jl.decoder.SampleBuffer) decoder.decodeFrame(frame, bitstream);
                    short[] pcm = output.getBuffer();
                    int length = output.getBufferLength();

                    device.write(pcm, 0, length);
                    bitstream.closeFrame();
                }
            } catch (Exception e) {
                // If it fails, we want the sound instance to die
                this.stopStreaming();
            } finally {
                if (device != null) device.close();
            }
        }, "Radio-Stream-Thread");

        streamThread.setDaemon(true);
        streamThread.start();
    }

    @Override
    public void tick() {
        // Standard check: stop if the player is too far
        if (Minecraft.getInstance().player != null) {
            double distSq = Minecraft.getInstance().player.distanceToSqr(this.x, this.y, this.z);
            if (distSq > (128 * 128)) { // Range usually matches the packet range
                this.stopStreaming();
            }
        }

        // Check if Minecraft's engine stopped us (e.g. /stopsound or world change)
        if (this.isStopped()) {
            this.playing = false;
        }
    }

    public void stopStreaming() {
        this.playing = false;
        // This is the correct way to notify the sound engine to kill this instance
        Minecraft.getInstance().getSoundManager().stop(this);

        if (streamThread != null) {
            streamThread.interrupt();
        }
    }
}