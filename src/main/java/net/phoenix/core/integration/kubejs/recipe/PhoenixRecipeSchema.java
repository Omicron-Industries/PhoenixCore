package net.phoenix.core.integration.kubejs.recipe;

import com.gregtechceu.gtceu.common.recipe.condition.CleanroomCondition;
import com.gregtechceu.gtceu.integration.kjs.recipe.GTRecipeSchema;

import net.phoenix.core.api.capability.PhoenixRecipeCapabilities;
import net.phoenix.core.common.data.recipeConditions.FluidInHatchCondition;
import net.phoenix.core.common.machine.multiblock.Shield;
import net.phoenix.core.common.machine.multiblock.unique.BlazingCleanroom;

import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import lombok.experimental.Accessors;

import static com.gregtechceu.gtceu.integration.kjs.recipe.GTRecipeSchema.*;

public interface PhoenixRecipeSchema {

    @SuppressWarnings({ "unused", "UnusedReturnValue" })
    @Accessors(chain = true, fluent = true)
    class PhoenixRecipeJS extends GTRecipeSchema.GTRecipeJS {

        public GTRecipeJS cleanroom(BlazingCleanroom cleanroomType) {
            return addCondition(new CleanroomCondition(cleanroomType));
        }

        public GTRecipeJS fluidInHatch(String fluidId) {
            return addCondition(FluidInHatchCondition.of(fluidId));
        }

        public GTRecipeSchema.GTRecipeJS requiredShieldState(String stateName) {
            Shield.ShieldTypes requiredType = Shield.ShieldTypes.valueOf(stateName.toUpperCase());

            this.input(PhoenixRecipeCapabilities.SHIELDTYPES, requiredType);
            return this;
        }

        GTRecipeSchema.GTRecipeJS shieldHealthChange(int healthChange) {
            this.addData("shield_health_change", healthChange);
            return this;
        }
    }

    /**
     * Adds shield health damage (positive number) or repair (negative number)
     * to be applied after the recipe runs.
     */
    public

    RecipeSchema SCHEMA = new RecipeSchema(PhoenixRecipeJS.class, PhoenixRecipeJS::new,
            DURATION, DATA, CONDITIONS,
            ALL_INPUTS, ALL_TICK_INPUTS, ALL_OUTPUTS, ALL_TICK_OUTPUTS,
            INPUT_CHANCE_LOGICS, OUTPUT_CHANCE_LOGICS, TICK_INPUT_CHANCE_LOGICS, TICK_OUTPUT_CHANCE_LOGICS, CATEGORY)
            .constructor((recipe, schemaType, keys, from) -> recipe.id(from.getValue(recipe, ID)), ID)
            .constructor(DURATION, CONDITIONS, ALL_INPUTS, ALL_OUTPUTS, ALL_TICK_INPUTS, ALL_TICK_OUTPUTS);
}
