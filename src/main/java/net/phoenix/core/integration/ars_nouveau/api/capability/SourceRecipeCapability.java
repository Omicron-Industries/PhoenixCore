package net.phoenix.core.integration.ars_nouveau.api.capability;

import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.AbstractMapIngredient;

import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.phoenix.core.integration.ars_nouveau.api.recipe.lookup.MapSourceIngredient;
import net.phoenix.core.integration.ars_nouveau.common.data.recipe.custom.SourceIngredient;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SourceRecipeCapability extends RecipeCapability<SourceIngredient> {

    public static final SourceRecipeCapability CAP = new SourceRecipeCapability();

    protected SourceRecipeCapability() {
        super("source", 0xC85CCFFF, false, 13, SourceIngredient.Serializer.INSTANCE); // false, not true
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

    @Override
    public void addXEIInfo(WidgetGroup group, int xOffset, GTRecipe recipe, List<Content> contents,
                           boolean perTick, boolean isInput, MutableInt yOffset) {

        // 1. Handle the vertical buffer for conditions
        // If the condition text is now a single line, 12-14 is usually enough.
        if (!recipe.conditions.isEmpty()) {
            yOffset.add(12);
        }

        for (var content : contents) {
            var ingredient = SourceRecipeCapability.CAP.of(content.getContent());

            // 2. Fix the Horizontal Shift
            // Instead of a hardcoded 5, use (3 - xOffset).
            // GTCEu uses negative xOffsets in some EMI views to center text;
            // 3 - xOffset usually aligns it to the left margin of the "extra info" area.
            int xPos = 3 - xOffset;

            // 3. Add the widget
            group.addWidget(new LabelWidget(xPos, yOffset.addAndGet(10),
                    (isInput ? "Source Used: " : "Source Gen: ") + ingredient.getSource()));
        }
    }
}
