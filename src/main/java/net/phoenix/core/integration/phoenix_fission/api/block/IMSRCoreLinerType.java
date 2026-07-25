package net.phoenix.core.integration.phoenix_fission.api.block;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

public interface IMSRCoreLinerType extends StringRepresentable {

    String getName();

    int getTier();

    ResourceLocation getTexture();

    int getFluidFlowRate();

    double getHeatPerMb();

    String getInputFluidId();

    String getOutputFluidId();
}
