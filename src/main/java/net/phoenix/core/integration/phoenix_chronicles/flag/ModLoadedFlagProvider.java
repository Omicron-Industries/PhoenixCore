package net.phoenix.core.integration.phoenix_chronicles.flag;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;

public class ModLoadedFlagProvider implements QuestFlagProvider {

    @Override
    public String prefix() {
        return "mod";
    }

    @Override
    public boolean evaluate(String expression, @Nullable MinecraftServer server) {
        
        return ModList.get().isLoaded(expression.trim());
    }
}
