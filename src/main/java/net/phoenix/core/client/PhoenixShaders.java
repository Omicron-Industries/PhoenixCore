package net.phoenix.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.data.item.SoulLensItem;
import net.phoenix.core.mixin.accessor.PostChainAccessor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PhoenixShaders {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final ResourceLocation SOUL_SHADER = new ResourceLocation(PhoenixCore.MOD_ID,
            "shaders/post/soul_vision.json");
    private static boolean isEffectActive = false;

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(stack.getItem() instanceof SoulLensItem)) {
            stack = player.getItemInHand(InteractionHand.OFF_HAND);
        }

        if (stack.getItem() instanceof SoulLensItem) {
            float current = stack.getOrCreateTag().getFloat("CurrentSoul");
            float max = stack.getOrCreateTag().getFloat("MaxSoul");
            float ratio = (max > 0) ? (current / max) : 1.0f;

            if (ratio < 0.95f) {
                if (!isEffectActive) {
                    LOGGER.info("Loading soul vision shader.");
                    mc.gameRenderer.loadEffect(SOUL_SHADER);
                    isEffectActive = true;
                }

                if (mc.gameRenderer.currentEffect() instanceof PostChainAccessor accessor) {
                    var passes = accessor.getPasses();
                    if (!passes.isEmpty()) {
                        LOGGER.info("Setting Saturation uniform to: {}", ratio);
                        passes.get(0).getEffect().getUniform("Saturation").set(ratio);
                    } else {
                        LOGGER.warn("Shader passes are empty, cannot set uniform.");
                    }
                }
            } else {
                disableEffect(mc);
            }
        } else {
            disableEffect(mc);
        }
    }

    private static void disableEffect(Minecraft mc) {
        if (isEffectActive) {
            LOGGER.info("Disabling soul vision shader.");
            mc.gameRenderer.shutdownEffect();
            isEffectActive = false;
        }
    }
}
