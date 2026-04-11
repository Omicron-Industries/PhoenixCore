package net.phoenix.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.phoenix.core.common.data.item.SoulLensItem;

public class SoulVisionHandler {

    private static final ResourceLocation GRAYSCALE_SHADER = new ResourceLocation("minecraft",
            "shaders/post/desaturate.json");
    private static boolean effectActive = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Check if player is holding the Soul Lens
        ItemStack stack = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
        boolean holdingLens = stack.getItem() instanceof SoulLensItem;

        if (holdingLens) {
            float current = stack.getOrCreateTag().getFloat("CurrentSoul");
            float max = stack.getOrCreateTag().getFloat("MaxSoul");
            float ratio = (max > 0) ? (current / max) : 1.0f;

            // Trigger grayscale if soul levels are low (e.g., below 40%)
            if (ratio < 0.4f && !effectActive) {
                mc.gameRenderer.loadEffect(GRAYSCALE_SHADER);
                effectActive = true;
            } else if (ratio >= 0.4f && effectActive) {
                mc.gameRenderer.shutdownEffect();
                effectActive = false;
            }
        } else if (effectActive) {
            mc.gameRenderer.shutdownEffect();
            effectActive = false;
        }
    }
}
