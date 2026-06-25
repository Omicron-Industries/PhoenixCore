package net.phoenix.core.integration.phoenix_chronicles.flag;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;

/**
 * Checks whether a mod is currently loaded.
 *
 * Quest SNBT:
 * enable_if: "mod:refinedstorage"
 * enable_if: "mod:create,mod:mekanism" // both must be loaded
 *
 * The check is static after game launch — mods cannot load/unload at runtime,
 * so this is evaluated once and cached per expression.
 */
public class ModLoadedFlagProvider implements QuestFlagProvider {

    @Override
    public String prefix() {
        return "mod";
    }

    @Override
    public boolean evaluate(String expression, @Nullable MinecraftServer server) {
        // expression is just the mod ID; no operators needed (mod is either loaded or not)
        return ModList.get().isLoaded(expression.trim());
    }
}
