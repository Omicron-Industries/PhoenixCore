package net.phoenix.core.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.phantasia.PhantasiaKeybind;
import net.phoenix.core.integration.phantasia.PhantasiaScriptLoader;

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

    /**
     * Reset the lazy-reload flag when the player disconnects so that the next
     * world join re-runs the deferred script load. This matters if the player
     * connects to a server where different addons are loaded, or rejoins after
     * a /reload that re-registered machines.
     */
    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        PhantasiaScriptLoader.resetLazyReload();
    }
}
