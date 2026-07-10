package net.phoenix.core.integration.vocal_resonance;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.multiblock.pattern.PatternState;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.TextWidget;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
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

public class ResonantJukeboxMachine extends WorkableElectricMultiblockMachine implements IMuiMachine {

    private boolean hasDiscHatch = false;
    private boolean hasLibraryHatch = false;
    private boolean hasStreamHatch = false;

    @SaveField
    @NotNull
// Pass: slots, inputIO, outputIO
    private final NotifiableItemStackHandler discInventory = attachTrait(new NotifiableItemStackHandler(1, IO.IN, IO.NONE));

    @NotNull
// This one remains correct because your custom constructor expects (controller, io)
    private final NotifiableSoundHandler soundHandler = attachTrait(new NotifiableSoundHandler(this, IO.IN));


    @SaveField
    @SyncToClient
    public String selectedLibrarySound = "";

    @SaveField
    private String lastPlayingStreamUrl = "";
    private String lastPlayingLibrarySound = "";

    @SaveField
    @SyncToClient
    public String currentStreamUrl = "";

    @Setter
    @SaveField
    @SyncToClient
    public String streamTitle = "Ready...";

    @SaveField
    public String searchTerm = "";

    private int totalSpeakerRange = 0;
    private float resonancePower = 1.0f;
    private static final int BASE_RANGE = 16;
    private static final long MUSIC_ENERGY_DRAIN = 32L;

    @SaveField
    @SyncToClient
    private int remainingSoundTicks = -1;

    @SaveField
    @SyncToClient
    public float currentLiveBass = 0.0f;

    @SaveField
    @SyncToClient
    public int currentLiveBPM = 0;

    private int lastKnownDuration = 0;

    private TickableSubscription acousticSub;

    public ResonantJukeboxMachine(BlockEntityCreationInfo holder) {
        super(holder);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            this.acousticSub = subscribeServerTick(this::acousticStateMachineTick);
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (this.acousticSub != null) {
            unsubscribe(this.acousticSub);
            this.acousticSub = null;
        }
    }

    @Override
    public void formStructure(@NotNull String substructureName) {
        super.formStructure(substructureName);

        // Safety hook: initialize data parsing only on the server logical frame
        if (getLevel() == null || getLevel().isClientSide) return;

        PatternState patternState = this.getPatternState(substructureName);
        if (patternState == null || patternState.getCache() == null) return;

        this.hasDiscHatch = false;
        this.hasLibraryHatch = false;
        this.hasStreamHatch = false;
        this.totalSpeakerRange = 0;

        int totalSpeakerValue = 0;
        int resonancePowerPercentage = 100;

        // FIXED: Replaced getMultiblockState() by processing the native block cache safely
        for (var entry : patternState.getCache().long2ObjectEntrySet()) {
            BlockEntity blockEntity = entry.getValue().getBlockEntity();

            // Collect any valid active SoundHatch multi-parts from the structure frame
            if (blockEntity instanceof SoundHatchPartMachine hatch) {
                if (hatch.getSoundType() != null) {
                    switch (hatch.getSoundType()) {
                        case DISC -> this.hasDiscHatch = true;
                        case LIBRARY -> this.hasLibraryHatch = true;
                        case STREAM -> this.hasStreamHatch = true;
                    }
                }
            }

            // If your custom contextual properties are attached to custom components:
            // Extract values via direct match checks or cast properties here if needed.
        }

        // Apply properties safely with runtime overrides
        this.totalSpeakerRange = totalSpeakerValue;
        this.resonancePower = resonancePowerPercentage / 100.0f;
        this.lastPlayingStreamUrl = "";
        this.lastPlayingLibrarySound = "";
        this.resetAcousticData();

        WorldAcousticSensor.register(getBlockPos(), getFinalRange());
    }

    @Override
    public void invalidateStructure(String substructureName) {
        killAllMachineAudio();
        super.invalidateStructure(substructureName);
        this.lastPlayingStreamUrl = "";
        this.resetAcousticData();

        WorldAcousticSensor.unregister(getBlockPos());
    }

    public void acousticStateMachineTick() {
        if (getLevel() == null || getLevel().isClientSide || !isFormed()) return;

        boolean hasActiveSelection = (!selectedLibrarySound.isEmpty() && currentStreamUrl.isEmpty()) || !currentStreamUrl.isEmpty();
        boolean canPlay = hasActiveSelection && isWorkingEnabled();

        if (!canPlay) {
            if (!lastPlayingStreamUrl.isEmpty() || remainingSoundTicks != -1) {
                killAllMachineAudio();
            }
            return;
        }

        var energyContainer = this.getEnergyContainer();
        if (energyContainer == null || energyContainer.getEnergyStored() < MUSIC_ENERGY_DRAIN) {
            killAllMachineAudio();
            return;
        }

        energyContainer.changeEnergy(-MUSIC_ENERGY_DRAIN);

        // Stream Audio Handling
        if (hasStreamHatch && !currentStreamUrl.isEmpty()) {
            if (!currentStreamUrl.equals(lastPlayingStreamUrl)) {
                this.selectedLibrarySound = "";
                this.lastPlayingLibrarySound = "";
                this.remainingSoundTicks = -1;

                boolean isYt = currentStreamUrl.contains("youtube.com") || currentStreamUrl.contains("youtu.be");
                String label = isYt ? "YT Audio" : (currentStreamUrl.length() > 30 ? currentStreamUrl.substring(0, 27) + "..." : currentStreamUrl);
                this.streamTitle = "§7Status: §aStreaming §f" + label;
                playStreamSound();
                lastPlayingStreamUrl = currentStreamUrl;
            }
        } else {
            if (!lastPlayingStreamUrl.isEmpty()) {
                killAllMachineAudio();
                this.lastPlayingStreamUrl = "";
            }
        }

        // Standard Resource Library Audio Handling
        if (hasLibraryHatch && !selectedLibrarySound.isEmpty() && currentStreamUrl.isEmpty()) {
            if (!selectedLibrarySound.equals(lastPlayingLibrarySound)) {
                playLibrarySound();
                lastPlayingLibrarySound = selectedLibrarySound;
            }
            this.markAsChanged();
        }
    }

    private void killAllMachineAudio() {
        var level = getLevel();
        if (level == null || level.isClientSide) return;

        PhoenixNetwork.CHANNEL.send(
                PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(getBlockPos())),
                new S2CPlaySoundPacket(getBlockPos(), new ResourceLocation("minecraft", "empty"), 0.0f, 1.0f, (float) getFinalRange()));

        this.lastPlayingLibrarySound = "";
        this.resetAcousticData();
        this.markAsChanged();
    }

    public void syncAndGeneralUpdate() {
        if (this.getLevel() != null && this.getLevel().isClientSide) {
            PhoenixNetwork.CHANNEL.sendToServer(new C2SSelectSoundPacket(this.getBlockPos(), this.selectedLibrarySound, this.currentStreamUrl));
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
        this.markAsChanged();
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
                PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(getBlockPos())),
                new S2CPlaySoundPacket(getBlockPos(), soundLoc, resonancePower, 1.0f, (float) getFinalRange()));
    }

    private void playStreamSound() {
        var level = getLevel();
        if (level == null || level.isClientSide) return;

        PhoenixNetwork.CHANNEL.send(
                PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(getBlockPos())),
                new S2CPlayStreamPacket(currentStreamUrl, getBlockPos(), getFinalRange(), resonancePower));
    }

    @Override
    public List<IWidget> getWidgetsForDisplay(PanelSyncManager syncManager) {
        List<IWidget> widgets = super.getWidgetsForDisplay(syncManager);
        if (!isFormed()) return widgets;
        widgets.add(new TextWidget<>(Text.of(Component.literal("§bAcoustic Capabilities:"))));
        widgets.add(new TextWidget<>(Text.dynamic(() -> Component.literal(getGateStatus("Physical Discs", hasDiscHatch)))));
        widgets.add(new TextWidget<>(Text.dynamic(() -> Component.literal(getGateStatus("Sound Library", hasLibraryHatch)))));
        widgets.add(new TextWidget<>(Text.dynamic(() -> Component.literal(getGateStatus("YT Streaming", hasStreamHatch)))));
        widgets.add(new TextWidget<>(Text.dynamic(() ->
                Component.literal("§7Radius: §a" + getFinalRange() + "m §8(base " + BASE_RANGE + " + §7" + totalSpeakerRange + "§8 speaker bonus)"))));
        widgets.add(new TextWidget<>(Text.dynamic(() -> {
            boolean active = (!selectedLibrarySound.isEmpty() || !currentStreamUrl.isEmpty()) && isWorkingEnabled();
            return Component.literal("§7Usage: §e" + (active ? MUSIC_ENERGY_DRAIN : 0L) + " EU/t");
        })));
        widgets.add(new TextWidget<>(Text.dynamic(() ->
                currentLiveBPM > 0
                        ? Component.literal("§7BPM detected: §f" + currentLiveBPM)
                        : Component.literal("§7BPM detected: §8Awaiting Analysis..."))));
        return widgets;
    }

    private String getGateStatus(String name, boolean active) {
        return (active ? "  §a✔ " : "  §c✘ ") + "§7" + name;
    }
}