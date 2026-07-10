package net.phoenix.core.integration.vocal_resonance.ingredient;

import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.AbstractMapIngredient;

import net.minecraft.resources.ResourceLocation;

import net.phoenix.core.integration.vocal_resonance.ingredient.SoundIngredient;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.phoenix.core.integration.vocal_resonance.recipe.lookup.MapSoundIngredient;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SoundRecipeCapability extends RecipeCapability<SoundIngredient> {

    public static final SoundRecipeCapability CAP = new SoundRecipeCapability();

    protected SoundRecipeCapability() {
        // FIXED FOR 8.0.0: Replaced legacy string name with explicit ResourceLocation identifier
        super(new ResourceLocation("phoenixcore", "sound"), 0x00FFFF, false, 14, SoundIngredient.Serializer.INSTANCE);
    }

    @Override
    public SoundIngredient copyWithModifier(SoundIngredient content, ContentModifier modifier) {
        // Keeps the baseline copy pattern from your working Source capability setup
        return content.copy();
    }

    @Override
    public SoundIngredient copyInner(SoundIngredient content) {
        return content.copy();
    }

    @Override
    public @Nullable List<AbstractMapIngredient> getDefaultMapIngredient(Object ingredient) {
        List<AbstractMapIngredient> ingredients = new ObjectArrayList<>(1);
        if (ingredient instanceof SoundIngredient s) {
            // FIXED FOR 8.0.0: Maps cleanly to the abstract recipe map lookups
            ingredients.add(new MapSoundIngredient(s));
        }
        return ingredients;
    }
}