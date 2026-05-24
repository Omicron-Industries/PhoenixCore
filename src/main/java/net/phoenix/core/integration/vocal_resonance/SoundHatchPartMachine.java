package net.phoenix.core.integration.vocal_resonance;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredPartMachine;

import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import lombok.Getter;

public class SoundHatchPartMachine extends TieredPartMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(SoundHatchPartMachine.class,
            TieredPartMachine.MANAGED_FIELD_HOLDER);

    @Getter
    @Persisted
    @DescSynced
    private final SoundHatchType soundType;

    public SoundHatchPartMachine(IMachineBlockEntity holder, int tier, SoundHatchType type) {
        super(holder, tier); // TieredPartMachine doesn't need IO
        this.soundType = type;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public Widget createUIWidget() {
        // 1. Create the main group with a standard size
        WidgetGroup group = new WidgetGroup(0, 0, 176, 90);

        // 2. SET THE BACKGROUND - This fixes the black screen
        group.setBackground(GuiTextures.BACKGROUND);

        // 3. Add Info Widgets
        group.addWidget(new LabelWidget(10, 10, "§b" + soundType.name() + " INTERFACE"));
        group.addWidget(
                new LabelWidget(10, 30, "§7Hardware Tier: §f" + com.gregtechceu.gtceu.api.GTValues.VNF[getTier()]));

        String desc = switch (soundType) {
            case DISC -> "Local playback for standard Music Discs.";
            case LIBRARY -> "Internal sound registry browser enabled.";
            case STREAM -> "External stream buffer active.";
        };

        group.addWidget(new LabelWidget(10, 50, "§8" + desc));
        group.addWidget(new LabelWidget(10, 70, "§a✔ System Operational"));

        return group;
    }

    public enum SoundHatchType {
        DISC,
        LIBRARY,
        STREAM
    }
}
