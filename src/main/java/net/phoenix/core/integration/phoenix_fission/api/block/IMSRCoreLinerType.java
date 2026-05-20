package net.phoenix.core.integration.phoenix_fission.api.block;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

public interface IMSRCoreLinerType extends StringRepresentable {

    String getName();

    int getTier(); // 1 = Baseline, 2 = EV, 3 = IV, 4 = LuV, etc.

    ResourceLocation getTexture();

    // New MSR Registry Parameters
    int getFluidFlowRate();

    double getHeatPerMb();

    String getInputFluidId();

    String getOutputFluidId();
}
