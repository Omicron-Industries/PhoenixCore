package net.phoenix.core.integration.ars_nouveau.api.capability;

import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.AbstractMapIngredient;

import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.integration.ars_nouveau.api.recipe.lookup.MapSourceIngredient;
import net.phoenix.core.integration.ars_nouveau.common.data.recipe.custom.SourceIngredient;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SourceRecipeCapability extends RecipeCapability<SourceIngredient> {

    public static final SourceRecipeCapability CAP = new SourceRecipeCapability();

    protected SourceRecipeCapability() {
        super(new ResourceLocation("phoenixcore", "source"), 0xC85CCFFF, false, 13,
                SourceIngredient.Serializer.INSTANCE);
    }

    @Override
    public SourceIngredient copyWithModifier(SourceIngredient content, ContentModifier modifier) {
        return new SourceIngredient((int) modifier.apply(content.getSource()));
    }

    @Override
    public SourceIngredient copyInner(SourceIngredient content) {
        return content.copy();
    }

    @Override
    public @Nullable List<AbstractMapIngredient> getDefaultMapIngredient(Object ingredient) {
        List<AbstractMapIngredient> ingredients = new ObjectArrayList<>(1);
        if (ingredient instanceof SourceIngredient s) ingredients.add(new MapSourceIngredient(s));
        return ingredients;
    }
}
