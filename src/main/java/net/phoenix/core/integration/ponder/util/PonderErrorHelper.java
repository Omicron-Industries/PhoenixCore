package net.phoenix.core.integration.ponder.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.phoenix.core.PhoenixCore;

public class PonderErrorHelper {

    public static void yeet(Throwable e) {
        PhoenixCore.LOGGER.error(e.getMessage(), e);
        Player clientPlayer = Minecraft.getInstance().player;
        if (clientPlayer != null) {
            MutableComponent first = Component.literal("[Phoenix Ponder ERROR] ").withStyle(ChatFormatting.DARK_RED);
            MutableComponent second = Component.literal(e.getMessage()).withStyle(ChatFormatting.RED);
            clientPlayer.sendSystemMessage(first.append(second));
        }
    }
}
