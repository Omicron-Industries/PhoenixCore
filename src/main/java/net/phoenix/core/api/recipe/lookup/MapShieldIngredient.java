package net.phoenix.core.api.recipe.lookup;

import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.AbstractMapIngredient;

import net.phoenix.core.common.machine.multiblock.Shield.ShieldTypes;

import java.util.List;

public class MapShieldIngredient extends AbstractMapIngredient {

    public final ShieldTypes shield;

    public MapShieldIngredient(ShieldTypes shield) {
        this.shield = shield;
    }

    @Override
    protected int hash() {
        return shield.key;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MapShieldIngredient other)) {
            return false;
        }
        return other.shield == shield;
    }

    public static List<AbstractMapIngredient> from(ShieldTypes ingredient) {
        return List.of(new MapShieldIngredient(ingredient));
    }
}
