package net.phoenix.core.integration.vocal_resonance;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredPartMachine;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;

import net.minecraft.network.chat.Component;

import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.UISettings;
import brachy.modularui.utils.Alignment;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.TextWidget;
import brachy.modularui.widgets.layout.Flow;
import lombok.Getter;

public class SoundHatchPartMachine extends TieredPartMachine implements IMuiMachine {

    @Getter
    @SaveField
    @SyncToClient
    private final SoundHatchType soundType;

    public SoundHatchPartMachine(BlockEntityCreationInfo holder, int tier, SoundHatchType type) {
        super(holder, tier);
        this.soundType = type;
    }

    @Override
    public ModularPanel<?> buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        ModularPanel<?> panel = new ModularPanel<>("phoenix_core:sound_hatch_panel");

        panel.size(176, 110);

        Flow layout = Flow.column()
                .size(176, 110)
                .padding(10)
                .crossAxisAlignment(Alignment.CrossAxis.START);

        layout.child(new TextWidget<>(Component.literal("§b" + soundType.name() + " INTERFACE")).marginBottom(6));
        layout.child(new TextWidget<>(
                Component.literal("§7Hardware Tier: §f" + com.gregtechceu.gtceu.api.GTValues.VNF[getTier()]))
                .marginBottom(6));

        String desc = switch (soundType) {
            case DISC -> "Local playback for standard Music Discs.";
            case LIBRARY -> "Internal sound registry browser enabled.";
            case STREAM -> "External stream buffer active.";
        };
        layout.child(new TextWidget<>(Component.literal("§8" + desc)).marginBottom(10));

        layout.child(new TextWidget<>(Component.literal(
                isFormed() ? "§a✔ System Operational" : "§c✘ Not linked to controller")));

        panel.child(layout);
        return panel;
    }

    public enum SoundHatchType {
        DISC,
        LIBRARY,
        STREAM
    }
}
