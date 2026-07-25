package net.phoenix.core.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.client.gui.WingFlightScreen;
import net.phoenix.core.client.gui.screen.ColorRadialMenuScreen;
import net.phoenix.core.client.keybind.PhoenixKeybinds;
import net.phoenix.core.common.data.item.PhoenixArmorItem;
import net.phoenix.core.common.item.ChameleonSprayCanItem;
import net.phoenix.core.integration.recipe_helper.RecipeBuilderMenu;
import net.phoenix.core.integration.recipe_helper.RecipeBuilderScreen;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.network.client.ClientSoundHandler;
import net.phoenix.core.network.packet.C2STeslaDischargePacket;
import net.phoenix.core.network.packet.SelectColorPacket;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientTickHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        ClientSoundHandler.tickActiveStreams(mc.player);

        while (PhoenixKeybinds.OPEN_WING_GUI.consumeClick()) {
            if (mc.screen == null &&
                    mc.player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof PhoenixArmorItem) {
                mc.setScreen(new WingFlightScreen());
            }
        }
        if (PhoenixKeybinds.SPRAY_CAN_MENU.consumeClick()) {
            ItemStack stack = mc.player.getMainHandItem();
            if (stack.getItem() instanceof ChameleonSprayCanItem) {
                mc.setScreen(new ColorRadialMenuScreen(InteractionHand.MAIN_HAND));
            }
        }

        while (PhoenixKeybinds.OPEN_RECIPE_BUILDER.consumeClick()) {
            if (mc.screen == null) {
                RecipeBuilderMenu menu = new RecipeBuilderMenu(0, mc.player.getInventory());
                mc.setScreen(
                        new RecipeBuilderScreen(menu, mc.player.getInventory(), Component.literal("Recipe Builder")));
            }
        }

        while (PhoenixKeybinds.TESLA_MODE.consumeClick()) {
            ItemStack chestItem = mc.player.getItemBySlot(EquipmentSlot.CHEST);
            if (!chestItem.isEmpty() &&
                    chestItem.getItem() instanceof net.phoenix.core.common.data.item.PhoenixArmorItem) {
                PhoenixNetwork.CHANNEL.sendToServer(new net.phoenix.core.network.packet.C2SToggleTeslaModePacket());
            }
        }

        while (PhoenixKeybinds.TESLA_DISCHARGE.consumeClick()) {
            ItemStack bootsItem = mc.player.getItemBySlot(EquipmentSlot.FEET);

            if (!bootsItem.isEmpty() &&
                    bootsItem.getItem() instanceof net.phoenix.core.common.data.item.PhoenixArmorItem) {
                CompoundTag nbt = bootsItem.getOrCreateTag();

                if (nbt.getInt("dischargeCooldown") == 0) {
                    PhoenixNetwork.CHANNEL.sendToServer(new C2STeslaDischargePacket());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        if (mc.options.keyShift.isDown()) {
            ItemStack stack = mc.player.getMainHandItem();
            if (stack.getItem() instanceof ChameleonSprayCanItem) {
                double scrollDelta = event.getScrollDelta();
                event.setCanceled(true);

                int currentColor = stack.getOrCreateTag().getInt("color");
                if (!stack.getOrCreateTag().contains("color")) currentColor = -1;

                int direction = scrollDelta > 0 ? 1 : -1;
                int nextColor = currentColor + direction;

                if (nextColor < -1) nextColor = 15;
                if (nextColor > 15) nextColor = -1;

                PhoenixNetwork.CHANNEL.sendToServer(new SelectColorPacket(InteractionHand.MAIN_HAND, nextColor));
                mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.1f, 1.5f + (nextColor * 0.05f));
            }
        }
    }

    @SubscribeEvent
    public static void onRightClick(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (!event.isUseItem()) return;
        ItemStack stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof ChameleonSprayCanItem)) return;

        if (!mc.player.isShiftKeyDown()) return;
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.MISS) return;

        event.setCanceled(true);
        mc.setScreen(new ColorRadialMenuScreen(InteractionHand.MAIN_HAND));
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof ChameleonSprayCanItem) {

        }
    }
}
