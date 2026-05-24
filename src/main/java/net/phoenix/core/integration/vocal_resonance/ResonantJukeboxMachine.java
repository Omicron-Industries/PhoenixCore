package net.phoenix.core.integration.vocal_resonance;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;

import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PacketDistributor;
import net.phoenix.core.integration.vocal_resonance.ingredient.NotifiableSoundHandler;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.network.packet.C2SSelectSoundPacket;
import net.phoenix.core.network.packet.S2CPlaySoundPacket;
import net.phoenix.core.network.packet.S2CPlayStreamPacket;

import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ResonantJukeboxMachine extends WorkableElectricMultiblockMachine {

    private boolean hasDiscHatch = false;
    private boolean hasLibraryHatch = false;
    private boolean hasStreamHatch = false;

    @Persisted
    @NotNull
    private final NotifiableItemStackHandler discInventory = new NotifiableItemStackHandler(this, 1, IO.IN);

    @Persisted
    public String selectedLibrarySound = "";

    @Persisted
    private String lastPlayingStreamUrl = "";
    @Persisted
    public String currentStreamUrl = "";
    @Setter
    @Persisted
    public String streamTitle = "Ready...";
    @Persisted
    public String searchTerm = "";

    private int totalSpeakerRange = 0;
    private float resonancePower = 1.0f;
    private static final int BASE_RANGE = 16;
    private static final long MUSIC_ENERGY_DRAIN = 32L; // Flat 32 EU/t operational requirement

    @Persisted
    private int remainingSoundTicks = -1;
    @Persisted
    public float currentLiveBass = 1.0f;
    private int lastKnownDuration = 0;
    private NotifiableSoundHandler soundHandler;

    private int customServerTicks = 0;

    public ResonantJukeboxMachine(IMachineBlockEntity holder) {
        super(holder);
        this.setWorkingEnabled(true);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.soundHandler == null) {
            this.soundHandler = new NotifiableSoundHandler(this, IO.IN);
        }
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

        this.hasLibraryHatch = true; // Debug bypasses
        this.hasStreamHatch = true;
        this.totalSpeakerRange = matchContext.getOrDefault("TotalSpeakerRange", 0);
        this.resonancePower = matchContext.getOrDefault("ResonancePower", 100) / 100.0f;
        this.remainingSoundTicks = -1;
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        this.customServerTicks = 0;
        this.lastPlayingStreamUrl = "";
        if (recipeLogic.isActive()) {
            recipeLogic.setStatus(RecipeLogic.Status.IDLE);
        }
    }

    /**
     * Managed Working Cycle Override
     * Forces active EU extraction out of the energy hatches to prevent
     * the machine logic from dropping into an IDLE state.
     */
    @Override
    public boolean onWorking() {
        if (getLevel() == null || getLevel().isClientSide || !isFormed()) {
            return super.onWorking();
        }

        // Determine if an audio track or network link is configured
        boolean hasActiveSelection = (!selectedLibrarySound.isEmpty() && currentStreamUrl.isEmpty()) ||
                !currentStreamUrl.isEmpty();
        boolean shouldPlay = hasActiveSelection && isWorkingEnabled();

        if (shouldPlay) {
            // FIXED: Calls the multiblock's built-in energy container manager directly
            var energyContainer = this.getEnergyContainer();

            if (energyContainer != null && energyContainer.getEnergyStored() >= MUSIC_ENERGY_DRAIN) {

                // Drain EU from internal hatch matrix every tick
                energyContainer.changeEnergy(-MUSIC_ENERGY_DRAIN);

                // Explicitly retain WORKING status so the tick processing loop stays hot
                if (recipeLogic.getStatus() != RecipeLogic.Status.WORKING) {
                    recipeLogic.setStatus(RecipeLogic.Status.WORKING);
                }

                customServerTicks++;

            } else {
                // Instantly silent processing if power requirements break down
                if (recipeLogic.getStatus() != RecipeLogic.Status.IDLE) {
                    recipeLogic.setStatus(RecipeLogic.Status.IDLE);
                }
                return false;
            }
        } else {
            // Shift back to native passive handler cascades if no sound inputs are configured
            if (recipeLogic.getStatus() != RecipeLogic.Status.IDLE) {
                recipeLogic.setStatus(RecipeLogic.Status.IDLE);
            }
            return super.onWorking();
        }

        // 1. Process Live URL Streaming Channels
        if (hasStreamHatch && !currentStreamUrl.isEmpty()) {
            if (!currentStreamUrl.equals(lastPlayingStreamUrl)) {
                playStreamSound();
                lastPlayingStreamUrl = currentStreamUrl;
            }
        } else {
            lastPlayingStreamUrl = "";
        }

        // 2. Process File Audio Streams (Fires only if live stream channels are unassigned)
        if (hasLibraryHatch && !selectedLibrarySound.isEmpty() && currentStreamUrl.isEmpty()) {
            if (remainingSoundTicks > 0) {
                remainingSoundTicks--;
            } else {
                playLibrarySound();
                remainingSoundTicks = lastKnownDuration > 0 ? lastKnownDuration : 100;
            }
        }

        return true;
    }

    public void syncAcousticData(int duration, float bass) {
        if (duration > 0) {
            this.lastKnownDuration = duration;
            if (this.remainingSoundTicks < duration) {
                this.remainingSoundTicks = duration;
            }
        }
        this.currentLiveBass = bass;
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
                new S2CPlayStreamPacket(currentStreamUrl, getPos(), getFinalRange()));
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
        textList.add(Component.literal("§7Radius: §a" + getFinalRange() + "m"));
        textList.add(Component.literal("§7Usage: §e" + MUSIC_ENERGY_DRAIN + " EU/t"));
    }

    private String getGateStatus(String name, boolean active) {
        return (active ? "  §a✔ " : "  §c✘ ") + "§7" + name;
    }
}
