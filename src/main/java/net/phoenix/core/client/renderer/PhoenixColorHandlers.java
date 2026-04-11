package net.phoenix.core.client.renderer;

import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.api.block.PhoenixMaterialContent;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class PhoenixColorHandlers {

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        PhoenixMaterialContent.CRYSTAL_ROSES.forEach((mat, block) -> event
                .register((state, world, pos, tintIndex) -> tintIndex == 0 ? mat.getMaterialRGB() : -1, block.get()));
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        PhoenixMaterialContent.CRYSTAL_ROSES.forEach((mat, block) -> event
                .register((stack, tintIndex) -> tintIndex == 0 ? mat.getMaterialRGB() : -1, block.get()));
    }
}
