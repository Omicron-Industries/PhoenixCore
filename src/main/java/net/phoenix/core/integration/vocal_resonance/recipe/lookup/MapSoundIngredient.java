package net.phoenix.core.integration.vocal_resonance.recipe.lookup;

import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.AbstractMapIngredient;

import net.phoenix.core.integration.vocal_resonance.ingredient.SoundIngredient;

import java.util.Collections;
import java.util.List;

public class MapSoundIngredient extends AbstractMapIngredient {

    public final SoundIngredient ingredient;

    public MapSoundIngredient(SoundIngredient ingredient) {
        this.ingredient = ingredient;
    }

    @Override
    protected int hash() {
        return ingredient.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MapSoundIngredient other)) return false;
        return other.ingredient.equals(this.ingredient);
    }

    public static List<AbstractMapIngredient> convertToMapIngredient(SoundIngredient ingredient) {
        return Collections.singletonList(new MapSoundIngredient(ingredient));
    }
}
