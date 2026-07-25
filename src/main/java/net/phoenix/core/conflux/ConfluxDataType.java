package net.phoenix.core.conflux;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public enum ConfluxDataType {

    MATERIAL("Material", "mat", ChatFormatting.GOLD, false),
    BIOLOGICAL("Biological", "bio", ChatFormatting.GREEN, false),
    ENERGETIC("Energetic", "nrg", ChatFormatting.AQUA, false),
    COMPUTATIONAL("Computational", "cpu", ChatFormatting.LIGHT_PURPLE, false),
    ARCANE("Arcane", "arc", ChatFormatting.DARK_PURPLE, true);

    public final String displayName;

    public final String tag;

    public final ChatFormatting color;

    public final boolean softDep;

    ConfluxDataType(String displayName, String tag, ChatFormatting color, boolean softDep) {
        this.displayName = displayName;
        this.tag = tag;
        this.color = color;
        this.softDep = softDep;
    }

    public boolean isAvailable() {
        if (!softDep) return true;
        if (this == ARCANE) {
            return net.minecraftforge.fml.ModList.get().isLoaded("ars_nouveau");
        }
        return true;
    }

    public MutableComponent displayComponent() {
        return Component.literal(displayName).withStyle(color);
    }

    public String id() {
        return name().toLowerCase();
    }
}
