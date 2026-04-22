package net.phoenix.core.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
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
import net.phoenix.core.network.packet.SelectColorPacket;

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
        if (PhoenixKeybinds.SPRAY_CAN_MENU.consumeClick()) {
            ItemStack stack = mc.player.getMainHandItem();
            if (stack.getItem() instanceof ChameleonSprayCanItem) {
                mc.setScreen(new ColorRadialMenuScreen(InteractionHand.MAIN_HAND));
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

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();

        // 1. Only run if the player exists and NO GUI is open
        if (mc.player == null || mc.screen != null) return;

        if (mc.options.keyShift.isDown()) {
            // 2. Check if holding the Chameleon Spray Can
            ItemStack stack = mc.player.getMainHandItem();
            if (stack.getItem() instanceof ChameleonSprayCanItem) {
                double scrollDelta = event.getScrollDelta();

                // 3. Cancel the default hotbar switching
                event.setCanceled(true);

                // 4. Calculate new color
                // -1 = Solvent, 0-15 = DyeColors
                int currentColor = stack.getOrCreateTag().getInt("color");
                if (!stack.getOrCreateTag().contains("color")) currentColor = -1;

                int direction = scrollDelta > 0 ? 1 : -1;
                int nextColor = currentColor + direction;

                // Wrap around logic: 16 total DyeColors (0-15) + Solvent (-1) = 17 states
                if (nextColor < -1) nextColor = 15;
                if (nextColor > 15) nextColor = -1;

                // 5. Sync to server
                // We use the same packet you just redesigned!
                PhoenixNetwork.CHANNEL.sendToServer(new SelectColorPacket(InteractionHand.MAIN_HAND, nextColor));

                // 6. Optional: Visual/Audio feedback
                mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.1f, 1.5f + (nextColor * 0.05f));
            }
        }
    }

    @SubscribeEvent
    public static void onRightClick(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Only care about right-click with spray can in main hand
        if (!event.isUseItem()) return;
        ItemStack stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof ChameleonSprayCanItem)) return;

        // Only on shift + air
        if (!mc.player.isShiftKeyDown()) return;
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.MISS) return;

        event.setCanceled(true); // prevent GregTech from eating it
        mc.setScreen(new ColorRadialMenuScreen(InteractionHand.MAIN_HAND));
    }
}
