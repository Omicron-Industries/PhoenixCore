package net.phoenix.core.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

import java.util.Map;

public class RecipeRemover {

    public static void clean(Map<ResourceLocation, Recipe<?>> recipes) {
        recipes.entrySet().removeIf(entry -> RecipeBlacklist.shouldRemoveParsed(entry.getKey(), entry.getValue()));
    }
}
