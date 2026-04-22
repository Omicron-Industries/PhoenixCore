package net.phoenix.core.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.phoenix.core.common.item.ChameleonSprayCanBehaviour;
import net.phoenix.core.common.item.ChameleonSprayCanItem;

public class SprayCanHudOverlay {

    public static final IGuiOverlay HUD_SPRAY_CAN = (gui, guiGraphics, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack stack = mc.player.getMainHandItem();
        // Check offhand if main hand isn't the spray can
        if (!(stack.getItem() instanceof ChameleonSprayCanItem)) {
            stack = mc.player.getOffhandItem();
        }

        if (stack.getItem() instanceof ChameleonSprayCanItem) {
            DyeColor color = ChameleonSprayCanBehaviour.getColor(stack);
            Component text;

            if (color != null) {
                Component colorName = Component.translatable("color.minecraft." + color.getSerializedName());
                text = Component.translatable("behaviour.paintspray.chameleon.status.color", colorName);
            } else {
                text = Component.translatable("behaviour.paintspray.chameleon.status.solvent");
            }

            int x = width / 2;
            int y = height - 53; // Positioned slightly above the hotbar/health labels

            // Draw centered text with a shadow
            int textWidth = mc.font.width(text);
            guiGraphics.drawString(mc.font, text, x - (textWidth / 2), y, 0xFFFFFF);
        }
    };
}
