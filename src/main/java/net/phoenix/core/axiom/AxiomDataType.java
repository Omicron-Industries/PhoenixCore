package net.phoenix.core.axiom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * The five disciplines of Axiom research.
 *
 * Each type is carried by its own pipe variant and produced by dedicated machines.
 * ARCANE is only reachable when Ars Nouveau is present — callers should check
 * {@link #isAvailable()} before exposing it in UIs or recipes.
 */
public enum AxiomDataType {

    MATERIAL("Material", "mat", ChatFormatting.GOLD, false),
    BIOLOGICAL("Biological", "bio", ChatFormatting.GREEN, false),
    ENERGETIC("Energetic", "nrg", ChatFormatting.AQUA, false),
    COMPUTATIONAL("Computational", "cpu", ChatFormatting.LIGHT_PURPLE, false),
    ARCANE("Arcane", "arc", ChatFormatting.DARK_PURPLE, true);

    /** Human-readable display name. */
    public final String displayName;
    /** Short 3-letter tag used in resource names (pipe blocks, items, etc.). */
    public final String tag;
    /** Chat color used in tooltips and GUI labels. */
    public final ChatFormatting color;
    /** Whether this type requires a soft-dep mod (Ars Nouveau for ARCANE). */
    public final boolean softDep;

    AxiomDataType(String displayName, String tag, ChatFormatting color, boolean softDep) {
        this.displayName = displayName;
        this.tag = tag;
        this.color = color;
        this.softDep = softDep;
    }

    /**
     * Returns false for soft-dep types when their required mod is absent.
     * Always true for base types; ARCANE checks for AN at runtime.
     */
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

    /** Lowercase resource-safe name, e.g. "material", "arcane". */
    public String id() {
        return name().toLowerCase();
    }
}
