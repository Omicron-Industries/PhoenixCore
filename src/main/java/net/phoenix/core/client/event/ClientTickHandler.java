package net.phoenix.core.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.client.gui.WingFlightScreen;
import net.phoenix.core.client.keybind.PhoenixKeybinds;
import net.phoenix.core.common.data.item.PhoenixArmorItem;
import net.phoenix.core.integration.recipe_helper.RecipeBuilderMenu;
import net.phoenix.core.integration.recipe_helper.RecipeBuilderScreen;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientTickHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // ── Wing GUI Logic ────────────────────────────────────────────────
        while (PhoenixKeybinds.OPEN_WING_GUI.consumeClick()) {
            if (mc.screen == null &&
                    mc.player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof PhoenixArmorItem) {
                mc.setScreen(new WingFlightScreen());
            }
        }

        // ── Recipe Builder Logic ──────────────────────────────────────────
        while (PhoenixKeybinds.OPEN_RECIPE_BUILDER.consumeClick()) {
            if (mc.screen == null) {
                // Since it's a ContainerScreen, we must create the Menu first
                RecipeBuilderMenu menu = new RecipeBuilderMenu(0, mc.player.getInventory());

                // Now pass the menu, inventory, and a title to the Screen
                mc.setScreen(
                        new RecipeBuilderScreen(menu, mc.player.getInventory(), Component.literal("Recipe Builder")));
            }
        }
    }
}
