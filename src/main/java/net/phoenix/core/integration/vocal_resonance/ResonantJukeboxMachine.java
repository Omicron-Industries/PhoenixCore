package net.phoenix.core.integration.vocal_resonance;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.RecordItem;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ResonantJukeboxMachine extends WorkableElectricMultiblockMachine {

    private boolean hasDiscHatch, hasLibraryHatch, hasStreamHatch = false;

    @Persisted @NotNull
    private final NotifiableItemStackHandler discInventory = new NotifiableItemStackHandler(this, 1, IO.IN);

    @Persisted private String selectedLibrarySound = "";
    @Persisted private String currentStreamUrl = "";
    @Persisted private String streamTitle = "Ready...";

    private int totalSpeakerRange = 0;
    private float resonancePower = 1.0f;
    private static final int BASE_RANGE = 16;

    public ResonantJukeboxMachine(IMachineBlockEntity holder) {
        super(holder);
        // Force the machine to start in the "Enabled" state
        this.setWorkingEnabled(true);
    }

    public int getFinalRange() { return BASE_RANGE + totalSpeakerRange; }
    public void setStreamTitle(String title) { this.streamTitle = title; }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        var matchContext = getMultiblockState().getMatchContext();

        // Reset gates
        this.hasDiscHatch = false;
        this.hasLibraryHatch = false;
        this.hasStreamHatch = false;

        // Retrieve the list we populated in the Predicate
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

        // Handle speaker stats
        this.totalSpeakerRange = matchContext.getOrDefault("TotalSpeakerRange", 0);
        this.resonancePower = matchContext.getOrDefault("ResonancePower", 100) / 100.0f;
    }

    @Override
    public boolean isWorkingEnabled() {
        // If the machine is turned ON in the GUI and has a source to play
        return super.isWorkingEnabled() && (!currentStreamUrl.isEmpty() || !discInventory.getStackInSlot(0).isEmpty());
    }

    @Override
    public boolean isActive() {
        // This makes the machine play its active animation/overlay
        return isFormed() && isWorkingEnabled() && this.energyContainer.getEnergyStored() > 0;
    }

    @Override
    public boolean onWorking() {
        // 1. Calculate EU consumption
        long euConsumption = (long) (32 * resonancePower);

        // 2. Check if we have enough energy
        if (this.energyContainer.getEnergyStored() >= euConsumption) {
            // Drain the energy
            this.energyContainer.removeEnergy(euConsumption);

            // 3. Handle your streaming logic every 5 seconds
            if (getOffsetTimer() % 100 == 0 && hasStreamHatch && !currentStreamUrl.isEmpty()) {
                RadioStreamManager.loadAndPlay(currentStreamUrl, this);
            }

            // Return true to tell GT the machine is successfully "working"
            return true;
        } else {
            // Not enough energy - update the display and return false
            this.setStreamTitle("§cInsufficient Power");
            return false;
        }
    }

    private void handleDiscPlayback() {
        if (hasDiscHatch && !discInventory.getStackInSlot(0).isEmpty()) {
            var stack = discInventory.getStackInSlot(0);
            if (stack.getItem() instanceof RecordItem record) {
                // Logic to play the vanilla record sound via packet
            }
        }
    }

    // --- GUI AND UI ---

    @Persisted private String searchTerm = "";


    @Override
    public Widget createUIWidget() {
        // 1. Main Group with standard background to prevent black screen
        WidgetGroup group = new WidgetGroup(0, 0, 180, 180);
        group.setBackground(GuiTextures.BACKGROUND);

        // 2. Info Display Panel
        group.addWidget(new DraggableScrollableWidgetGroup(4, 4, 172, 60)
                .setBackground(getScreenTexture())
                .addWidget(new ComponentPanelWidget(4, 4, this::addDisplayText)));

        int y = 68;

        // 3. Searchable Sound Library
        if (hasLibraryHatch || hasDiscHatch) {
            group.addWidget(new LabelWidget(10, y, "§7Search Sounds:"));
            group.addWidget(new TextFieldWidget(10, y + 10, 160, 14,
                    () -> searchTerm,
                    val -> this.searchTerm = val));

            y += 28;

            DraggableScrollableWidgetGroup results = new DraggableScrollableWidgetGroup(10, y, 160, 40);
            results.setBackground(new com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup());

            var filteredSounds = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.keySet().stream()
                    .filter(loc -> loc.toString().contains(searchTerm.toLowerCase()))
                    .limit(20)
                    .toList();

            int buttonY = 0;
            for (net.minecraft.resources.ResourceLocation res : filteredSounds) {
                // Create a sub-group to layer the Button and the Label
                WidgetGroup btnGroup = new WidgetGroup(0, buttonY, 150, 12);
                btnGroup.addWidget(new com.lowdragmc.lowdraglib.gui.widget.ButtonWidget(0, 0, 150, 12,
                        com.gregtechceu.gtceu.api.gui.GuiTextures.VANILLA_BUTTON, (clickData) -> {
                    this.selectedLibrarySound = res.toString();
                }));
                btnGroup.addWidget(new LabelWidget(4, 2, res.getPath()));

                results.addWidget(btnGroup);
                buttonY += 13;
            }
            group.addWidget(results);
            y += 45;
        }

        // 4. Web Stream Input & Control
        if (hasStreamHatch) {
            group.addWidget(new TextFieldWidget(10, y, 135, 20,
                    () -> currentStreamUrl,
                    val -> this.currentStreamUrl = val)
                    .setHoverTooltips(Component.literal("Paste URL here")));

            // Play/Stop Toggle layered button
            WidgetGroup playBtnGroup = new WidgetGroup(150, y, 20, 20);
            playBtnGroup.addWidget(new com.lowdragmc.lowdraglib.gui.widget.ButtonWidget(0, 0, 20, 20,
                    com.gregtechceu.gtceu.api.gui.GuiTextures.VANILLA_BUTTON, (clickData) -> {
                if (!currentStreamUrl.isEmpty()) {
                    RadioStreamManager.loadAndPlay(currentStreamUrl, this);
                }
            }));
            playBtnGroup.addWidget(new LabelWidget(6, 6, isActive() ? "§c■" : "§a▶"));

            group.addWidget(playBtnGroup);
        }

        return group;
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

        if (hasStreamHatch) {
            textList.add(Component.literal("§eNow Playing: §f" + streamTitle));
        }

        textList.add(Component.literal("§7Radius: §a" + getFinalRange() + "m §8| §7Power: §d" + resonancePower + "x"));
    }

    private String getGateStatus(String name, boolean active) {
        return (active ? "  §a✔ " : "  §c✘ ") + "§7" + name;
    }
}