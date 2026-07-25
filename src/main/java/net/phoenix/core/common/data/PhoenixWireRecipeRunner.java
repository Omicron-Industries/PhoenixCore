package net.phoenix.core.common.data;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.data.recipes.FinishedRecipe;
import net.phoenix.core.common.data.recipe.generated.PhoenixWireRecipeHandler;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class PhoenixWireRecipeRunner {

    public static void init(@NotNull Consumer<FinishedRecipe> provider) {
        for (Material material : GTRegistries.MATERIALS) {
            PhoenixWireRecipeHandler.run(provider, material);
        }
    }
}
