package net.phoenix.core.client.renderer.machine.multiblock;

import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;

import net.phoenix.core.PhoenixCore;
import net.phoenix.core.client.renderer.machine.*;
import net.phoenix.core.integration.phoenix_tesla_network.client.renderer.machine.TeslaTowerRenderer;

public class PhoenixDynamicRenderHelpers {

    private static boolean registered = false;

    public static void registerAll() {
        if (registered) return;
        registered = true;

        DynamicRenderManager.register(PhoenixCore.id("eye_of_harmony"), EyeOfHarmonyRender.TYPE);
        DynamicRenderManager.register(PhoenixCore.id("artificial_star"), ArtificialStarRender.TYPE);
        DynamicRenderManager.register(PhoenixCore.id("plasma_arc_furnace"), PlasmaArcFurnaceRender.TYPE);
        DynamicRenderManager.register(PhoenixCore.id("custom_fluid"), CustomFluidRender.TYPE);
        DynamicRenderManager.register(PhoenixCore.id("helical_fusion"), HelicalFusionRenderer.TYPE);
        DynamicRenderManager.register(PhoenixCore.id("honey_chamber"), HoneyChamberDynamicRender.TYPE);
        DynamicRenderManager.register(PhoenixCore.id("tesla_tower"), TeslaTowerRenderer.TYPE);
        DynamicRenderManager.register(PhoenixCore.id("engine_gearbox"), EngineGearboxRenderer.TYPE);
    }

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
