package net.phoenix.core.conflux.pipe;

import com.gregtechceu.gtceu.api.pipenet.IPipeType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.conflux.ConfluxDataType;

import java.util.Locale;

/**
 * One enum value per {@link ConfluxDataType}; ordinal matches the data-type ordinal.
 * Each value produces its own {@link ResourceLocation} so GTCEu's pipe-net treats
 * them as separate network types and they never merge across disciplines.
 */
public enum ConfluxPipeType implements IPipeType<ConfluxPipeData>, StringRepresentable {

    MATERIAL,
    BIOLOGICAL,
    ENERGETIC,
    COMPUTATIONAL,
    ARCANE;

    /** Pipe cross-section thickness in the 0–1 range (matches GTCEu normal pipes). */
    private static final float THICKNESS = 0.375f;

    /** The {@link ConfluxDataType} that corresponds to this pipe variant. */
    public ConfluxDataType dataType() {
        return ConfluxDataType.values()[ordinal()];
    }

    @Override
    public float getThickness() {
        return THICKNESS;
    }

    @Override
    public ConfluxPipeData modifyProperties(ConfluxPipeData base) {
        return base;
    }

    @Override
    public boolean isPaintable() {
        return true;
    }

    /** Unique RL per variant — used by GTCEu to key separate pipe-net types. */
    @Override
    public ResourceLocation type() {
        return PhoenixCore.id(dataType().id() + "_data_pipe");
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
