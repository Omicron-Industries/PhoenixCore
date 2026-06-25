package net.phoenix.core.integration.vocal_resonance;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;

import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PacketDistributor;
import net.phoenix.core.integration.vocal_resonance.ingredient.NotifiableSoundHandler;
import net.phoenix.core.integration.vocal_vibrancy.WorldAcousticSensor;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.network.packet.C2SSelectSoundPacket;
import net.phoenix.core.network.packet.S2CPlaySoundPacket;
import net.phoenix.core.network.packet.S2CPlayStreamPacket;

import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ResonantJukeboxMachine extends WorkableElectricMultiblockMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            ResonantJukeboxMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    private boolean hasDiscHatch = false;
    private boolean hasLibraryHatch = false;
    private boolean hasStreamHatch = false;

    @Persisted
    @NotNull
    private final NotifiableItemStackHandler discInventory = new NotifiableItemStackHandler(this, 1, IO.IN);

    /**
     * Registered as a machine trait in the constructor so GTCEu's recipe pipeline
     * can find it via getRecipeHandlers() → traits scan. No override needed.
     */
    @NotNull
    private final NotifiableSoundHandler soundHandler = new NotifiableSoundHandler(this, IO.IN);

    @Persisted
    @DescSynced
    public String selectedLibrarySound = "";

    @Persisted
    private String lastPlayingStreamUrl = "";
    private String lastPlayingLibrarySound = "";
    @Persisted
    @DescSynced
    public String currentStreamUrl = "";

    @Setter
    @Persisted
    @DescSynced
    public String streamTitle = "Ready...";

    @Persisted
    public String searchTerm = "";

    private int totalSpeakerRange = 0;
    private float resonancePower = 1.0f;
    private static final int BASE_RANGE = 16;
    private static final long MUSIC_ENERGY_DRAIN = 32L;

    @Persisted
    @DescSynced
    private int remainingSoundTicks = -1;

    @Persisted
    @DescSynced
    public float currentLiveBass = 0.0f;

    @Persisted
    @DescSynced
    public int currentLiveBPM = 0;

    private int lastKnownDuration = 0;

    // The core Tick Subscription Handler
    private final ConditionalSubscriptionHandler acousticTickHandler;

    public ResonantJukeboxMachine(IMachineBlockEntity holder) {
        super(holder);
        // Initialize the tick sub to point to our acoustic update loop, running only when formed
        this.acousticTickHandler = new ConditionalSubscriptionHandler(this, this::acousticStateMachineTick,
                this::isFormed);
    }

    @Override
    public @NotNull ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        var matchContext = getMultiblockState().getMatchContext();
        this.hasDiscHatch = false;
        this.hasLibraryHatch = false;
        this.hasStreamHatch = false;

        List<SoundHatchPartMachine> soundHatches = matchContext.get("SoundHatches");
        if (soundHatches != null) {
            for (SoundHatchPartMachine hatch : soundHatches) {
                switch (hatch.getSoundType()) {
                    case DISC -> this.hasDiscHatch = true;
                    case LIBRARY -> this.hasLibraryHatch = true;
                    case STREAM -> this.hasStreamHatch = true;
                }
            }
        }

        this.hasLibraryHatch = true;
        this.hasStreamHatch = true;
        this.totalSpeakerRange = matchContext.getOrDefault("TotalSpeakerRange", 0);
        // ResonancePower is stored as integer (amplifier × 100) by tieredSpeakers() predicate.
        // Default 100 → /100 = 1.0f base volume when no speakers are present.
        this.resonancePower = matchContext.getOrDefault("ResonancePower", 100) / 100.0f;
        // Force the tick loop to re-send packets after reload or re-formation.
        this.lastPlayingStreamUrl = "";
        this.lastPlayingLibrarySound = "";
        this.resetAcousticData();

        // Activate/Update the tick subscription loop state
        this.acousticTickHandler.updateSubscription();
        // Register as a world-acoustic sensor so any sound in range feeds NotifiableSoundHandler
        WorldAcousticSensor.register(getPos(), getFinalRange());
    }

    @Override
    public void onStructureInvalid() {
        killAllMachineAudio(); // stop clients before clearing state
        super.onStructureInvalid();
        this.lastPlayingStreamUrl = "";
        this.resetAcousticData();

        // Terminate the tick subscription loop when broken
        this.acousticTickHandler.updateSubscription();
        WorldAcousticSensor.unregister(getPos());
    }

    /**
     * THE TICK SUB LOOP
     * Runs independently of recipe processing state. Gated purely by active UI toggles and physical energy
     * availability.
     */
    public void acousticStateMachineTick() {
        if (getLevel() == null || getLevel().isClientSide) return;

        boolean hasActiveSelection = (!selectedLibrarySound.isEmpty() && currentStreamUrl.isEmpty()) ||
                !currentStreamUrl.isEmpty();
        boolean canPlay = hasActiveSelection && isWorkingEnabled();

        // If the machine is turned off via UI or has nothing selected, make sure we clean up noise
        if (!canPlay) {
            if (!lastPlayingStreamUrl.isEmpty() || remainingSoundTicks != -1) {
                killAllMachineAudio();
            }
            return;
        }

        // EU Gating: Manually poll and draw energy out of internal power containers
        var energyContainer = this.getEnergyContainer();
        if (energyContainer == null || energyContainer.getEnergyStored() < MUSIC_ENERGY_DRAIN) {
            // Out of power! Halt audio updates and stop audio until power returns
            killAllMachineAudio();
            return;
        }

        // Safely draw operational costs
        energyContainer.changeEnergy(-MUSIC_ENERGY_DRAIN);

        // ── Stream Audio Handling ──
        if (hasStreamHatch && !currentStreamUrl.isEmpty()) {
            if (!currentStreamUrl.equals(lastPlayingStreamUrl)) {
                this.selectedLibrarySound = "";
                this.lastPlayingLibrarySound = "";
                this.remainingSoundTicks = -1;

                boolean isYt = currentStreamUrl.contains("youtube.com") || currentStreamUrl.contains("youtu.be");
                String label = isYt ? "YT Audio" :
                        (currentStreamUrl.length() > 30 ? currentStreamUrl.substring(0, 27) + "..." : currentStreamUrl);
                this.streamTitle = "§7Status: §aStreaming §f" + label;
                playStreamSound();
                lastPlayingStreamUrl = currentStreamUrl;
            }
        } else {
            // User cleared the stream URL — kill the active stream and mark it gone
            if (!lastPlayingStreamUrl.isEmpty()) {
                killAllMachineAudio();
                this.lastPlayingStreamUrl = "";
            }
        }

        // ── Standard Resource Library Audio Handling ──
        // Only send a play packet when the selection changes. The client instance loops
        // indefinitely (looping=true), so repeated packets just cause audible restarts.
        if (hasLibraryHatch && !selectedLibrarySound.isEmpty() && currentStreamUrl.isEmpty()) {
            if (!selectedLibrarySound.equals(lastPlayingLibrarySound)) {
                playLibrarySound();
                lastPlayingLibrarySound = selectedLibrarySound;
            }
            this.markDirty();
        }
    }

    /**
     * Utility method to completely silence client execution tracks and clear backend state fields.
     */
    private void killAllMachineAudio() {
        var level = getLevel();
        if (level == null || level.isClientSide) return;

        // volume=0 signals ClientSoundHandler.playSound() to call stopSoundAt() instead of playing
        PhoenixNetwork.CHANNEL.send(
                PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(getPos())),
                new S2CPlaySoundPacket(getPos(), new ResourceLocation("minecraft", "empty"), 0.0f, 1.0f,
                        (float) getFinalRange()));

        // Do NOT reset lastPlayingStreamUrl here — callers that want a re-send must do it explicitly.
        // Resetting it here caused a kill→restart loop every tick whenever conditions briefly failed.
        this.lastPlayingLibrarySound = "";
        this.resetAcousticData();
        this.markDirty();
    }

    @Override
    public boolean onWorking() {
        return super.onWorking();
    }

    public void syncAndGeneralUpdate() {
        if (this.getLevel() != null && this.getLevel().isClientSide) {
            PhoenixNetwork.CHANNEL.sendToServer(new C2SSelectSoundPacket(
                    this.getPos(), this.selectedLibrarySound, this.currentStreamUrl));
        }
    }

    public int getFinalRange() {
        return BASE_RANGE + totalSpeakerRange;
    }

    public float getResonancePower() {
        return resonancePower;
    }

    public void syncAcousticData(int duration, float bass, int bpm) {
        if (duration > 0) {
            this.lastKnownDuration = duration;
            if (this.remainingSoundTicks <= 0 || duration > this.remainingSoundTicks) {
                this.remainingSoundTicks = duration;
            }
        }
        this.currentLiveBass = bass;
        this.currentLiveBPM = bpm;
        this.markDirty();
    }

    public void resetAcousticData() {
        this.currentLiveBass = 0.0f;
        this.currentLiveBPM = 0;
        this.remainingSoundTicks = -1;
        this.lastKnownDuration = 0;
    }

    private void playLibrarySound() {
        var level = getLevel();
        if (level == null || level.isClientSide) return;
        if (selectedLibrarySound == null || selectedLibrarySound.isEmpty()) return;

        ResourceLocation soundLoc = ResourceLocation.tryParse(selectedLibrarySound);
        if (soundLoc == null) return;

        PhoenixNetwork.CHANNEL.send(
                PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(getPos())),
                new S2CPlaySoundPacket(getPos(), soundLoc, resonancePower, 1.0f, (float) getFinalRange()));
    }

    private void playStreamSound() {
        var level = getLevel();
        if (level == null || level.isClientSide) return;

        PhoenixNetwork.CHANNEL.send(
                PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(getPos())),
                new S2CPlayStreamPacket(currentStreamUrl, getPos(), getFinalRange(), resonancePower));
    }

    @Override
    public Widget createUIWidget() {
        WidgetGroup group = new WidgetGroup(0, 0, 180, 120);
        group.addWidget(new DraggableScrollableWidgetGroup(4, 4, 172, 80)
                .setBackground(GuiTextures.DISPLAY)
                .addWidget(new ComponentPanelWidget(4, 4, this::addDisplayText)));

        TextTexture buttonText = new TextTexture("§b[ ACCESS CONSOLE ]");
        IGuiTexture buttonVisual = new GuiTextureGroup(GuiTextures.BUTTON, buttonText);

        group.addWidget(new ButtonWidget(4, 88, 172, 20, buttonVisual, (clickData) -> {
            if (this.getLevel() != null && this.getLevel().isClientSide) {
                openResonanceConsole();
            }
        }));
        return group;
    }

    @OnlyIn(Dist.CLIENT)
    private void openResonanceConsole() {
        net.minecraft.client.Minecraft.getInstance().setScreen(new ResonanceConsoleScreen(this));
    }

    @Override
    public void addDisplayText(@NotNull List<Component> textList) {
        super.addDisplayText(textList);
        if (!isFormed()) return;
        textList.add(Component.literal("§7-".repeat(15)));
        textList.add(Component.literal("§bAcoustic Capabilities:"));
        textList.add(Component.literal(getGateStatus("Physical Discs", hasDiscHatch)));
        textList.add(Component.literal(getGateStatus("Sound Library", hasLibraryHatch)));
        textList.add(Component.literal(getGateStatus("YT Streaming", hasStreamHatch)));
        textList.add(Component.literal("§7Radius: §a" + getFinalRange() + "m §8(base " + BASE_RANGE + " + §7" +
                totalSpeakerRange + "§8 speaker bonus)"));

        boolean isAcousticallyActive = (!selectedLibrarySound.isEmpty() || !currentStreamUrl.isEmpty()) &&
                isWorkingEnabled();
        long activeDrain = isAcousticallyActive ? MUSIC_ENERGY_DRAIN : 0L;
        textList.add(Component.literal("§7Usage: §e" + activeDrain + " EU/t"));

        if (currentLiveBPM > 0) {
            textList.add(Component.literal("§7BPM detected: §f" + currentLiveBPM));
        } else {
            textList.add(Component.literal("§7BPM detected: §8Awaiting Analysis..."));
        }
    }

    private String getGateStatus(String name, boolean active) {
        return (active ? "  §a✔ " : "  §c✘ ") + "§7" + name;
    }
}
