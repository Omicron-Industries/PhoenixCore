package net.phoenix.core.integration.vocal_resonance.ingredient;

import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.AbstractMapIngredient;

import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.integration.vocal_resonance.recipe.lookup.MapSoundIngredient;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SoundRecipeCapability extends RecipeCapability<SoundIngredient> {

    public static final SoundRecipeCapability CAP = new SoundRecipeCapability();

    protected SoundRecipeCapability() {
        super(new ResourceLocation("phoenixcore", "sound"), 0x00FFFF, false, 14, SoundIngredient.Serializer.INSTANCE);
    }

    @Override
    public SoundIngredient copyWithModifier(SoundIngredient content, ContentModifier modifier) {
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

            ingredients.add(new MapSoundIngredient(s));
        }
        return ingredients;
    }
}
