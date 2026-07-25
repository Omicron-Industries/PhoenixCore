package net.phoenix.core.integration.phoenix_chronicles.flag;

import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;

public interface QuestFlagProvider {

    String prefix();

    boolean evaluate(String expression, @Nullable MinecraftServer server);
}
