package net.phoenix.core.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.phantasia.PhantasiaKeybind;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class PhantasiaClientEvents {

    private PhantasiaClientEvents() {}

    /**
     * Updated to .Pre to match PhantasiaKeybind's logic.
     * This allows the UI to render on the highest layer (PLAYER_LIST)
     * and ensures it stays above maps/other HUD elements.
     */
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        PhantasiaKeybind.onRenderOverlay(event);
    }
}
