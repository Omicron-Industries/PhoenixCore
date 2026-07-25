package net.phoenix.core.conflux.pipe;

import com.gregtechceu.gtceu.api.pipenet.IPipeType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.conflux.ConfluxDataType;

import java.util.Locale;

public enum ConfluxPipeType implements IPipeType<ConfluxPipeData>, StringRepresentable {

    MATERIAL,
    BIOLOGICAL,
    ENERGETIC,
    COMPUTATIONAL,
    ARCANE;

    private static final float THICKNESS = 0.375f;

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

    @Override
    public ResourceLocation type() {
        return PhoenixCore.id(dataType().id() + "_data_pipe");
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
