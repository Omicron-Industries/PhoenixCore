package net.phoenix.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.phoenix.core.PhoenixCore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PhoenixShaders {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final ResourceLocation SOUL_SHADER = new ResourceLocation(PhoenixCore.MOD_ID, "shaders/post/soul_vision.json");
    private static boolean isEffectActive = false;

    /**
     * This method is now only used by the mixin for the always-on test.
     * It will always return 0.0f to force a grayscale effect.
     */
    public static float getCurrentSoulRatio() {
        return 0.0f;
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // --- ALWAYS-ON SHADER TEST ---
        // Unconditionally load the shader once and leave it on.
        if (!isEffectActive) {
            LOGGER.info("Attempting to load ALWAYS-ON soul vision shader.");
            mc.gameRenderer.loadEffect(SOUL_SHADER);
            isEffectActive = true;
        }
    }
}