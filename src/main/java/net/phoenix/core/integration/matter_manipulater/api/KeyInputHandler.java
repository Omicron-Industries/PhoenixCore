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

        while (PhoenixKeybinds.MANIPULATOR_MENU.consumeClick()) {
            ItemStack stack = mc.player.getMainHandItem();

            if (stack.getItem() instanceof PhoenixManipulatorItem) {
                mc.setScreen(new PhoenixRadialMenu());
            }
        }
        while (PhoenixKeybinds.MANIPULATOR_MENU.consumeClick()) {
            ItemStack stack = mc.player.getMainHandItem();

            if (stack.getItem() instanceof PhoenixManipulatorItem) {

                if (mc.player.getOffhandItem().isEmpty()) {
                    PhoenixInventoryService.findMatchingPipe(mc.player, ItemStack.EMPTY)
                            .ifPresent(foundStack -> {

                            });
                }

                mc.setScreen(new PhoenixRadialMenu());
            }
        }
    }
}
