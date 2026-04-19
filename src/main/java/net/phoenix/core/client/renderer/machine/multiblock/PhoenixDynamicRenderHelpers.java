package net.phoenix.core.client.renderer.machine.multiblock;

import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;

import net.phoenix.core.client.renderer.machine.*;
import net.phoenix.core.integration.phoenix_tesla_network.client.renderer.machine.TeslaTowerRenderer;

public class PhoenixDynamicRenderHelpers {

    public static DynamicRender<?, ?> getEyeOfHarmonyRender() {
        return EyeOfHarmonyRender.INSTANCE;
    }

    public static DynamicRender<?, ?> getArtificialStarRender() {
        return ArtificialStarRender.INSTANCE;
    }

    public static DynamicRender<?, ?> getPlasmaArcFurnaceRenderer() {
        return PlasmaArcFurnaceRender.INSTANCE;
    }

    public static DynamicRender<?, ?> getCustomFluidRenderer() {
        return CustomFluidRender.INSTANCE;
    }

    public static DynamicRender<?, ?> getHelicalFusionRenderer() {
        return HelicalFusionRenderer.INSTANCE;
    }

    public static DynamicRender<?, ?> getHoneyChamberRenderer() {
        return HoneyChamberDynamicRender.INSTANCE;
    }

    public static DynamicRender<?, ?> getTeslaTowerRenderer() {
        return TeslaTowerRenderer.INSTANCE;
    }

    public static DynamicRender<?, ?> getEngineGearboxRenderer() {
        return EngineGearboxRenderer.INSTANCE;
    }
}
