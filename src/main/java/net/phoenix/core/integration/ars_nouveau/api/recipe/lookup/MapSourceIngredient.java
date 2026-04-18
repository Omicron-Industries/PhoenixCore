package net.phoenix.core.integration.ars_nouveau.api.recipe.lookup;

import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.AbstractMapIngredient;

import net.phoenix.core.integration.ars_nouveau.common.data.recipe.custom.SourceIngredient;

import java.util.Collections;
import java.util.List;

public class MapSourceIngredient extends AbstractMapIngredient {

    public final SourceIngredient ingredient;

    public MapSourceIngredient(SourceIngredient ingredient) {
        this.ingredient = ingredient;
    }

    @Override
    protected int hash() {
        return ingredient.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MapSourceIngredient other)) return false;
        return other.ingredient.equals(this.ingredient);
    }

    public static List<AbstractMapIngredient> convertToMapIngredient(SourceIngredient ingredient) {
        return Collections.singletonList(new MapSourceIngredient(ingredient));
    }
}
