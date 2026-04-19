package net.phoenix.core.integration.matter_manipulater.api;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.client.keybind.PhoenixKeybinds;
import net.phoenix.core.integration.matter_manipulater.common.data.item.PhoenixManipulatorItem;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, value = Dist.CLIENT)
public class KeyInputHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        // Check if our specific key was pressed
        while (PhoenixKeybinds.MANIPULATOR_MENU.consumeClick()) {
            ItemStack stack = mc.player.getMainHandItem();

            // Only open if holding the Manipulator
            if (stack.getItem() instanceof PhoenixManipulatorItem) {
                mc.setScreen(new PhoenixRadialMenu());
            }
        }
        while (PhoenixKeybinds.MANIPULATOR_MENU.consumeClick()) {
            ItemStack stack = mc.player.getMainHandItem();

            if (stack.getItem() instanceof PhoenixManipulatorItem) {
                // --- SMART SWAP LOGIC ---
                // If off-hand is empty, try to find the last used pipe or any pipe
                if (mc.player.getOffhandItem().isEmpty()) {
                    PhoenixInventoryService.findMatchingPipe(mc.player, ItemStack.EMPTY) // Pass EMPTY to find ANY pipe
                            .ifPresent(foundStack -> {
                                // Note: In a real mod, you'd send a packet to swap slots on the server
                                // For now, this just helps the player realize they have pipes available
                            });
                }

                mc.setScreen(new PhoenixRadialMenu());
            }
        }
    }
}
