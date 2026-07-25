package net.phoenix.core.api.recipe;

import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;

import net.phoenix.core.integration.ars_nouveau.api.capability.SourceRecipeCapability;

import java.util.ArrayList;

public class PhoenixRecipeModifier {

    public static class Builder {

        private final ModifierFunction.FunctionBuilder gtmBuilder = ModifierFunction.builder();
        private double sourceMultiplier = 1.0;
        private double euOutputMultiplier = 1.0;

        public Builder durationMultiplier(double multiplier) {
            gtmBuilder.durationMultiplier(multiplier);
            return this;
        }

        public Builder sourceMultiplier(double multiplier) {
            this.sourceMultiplier = multiplier;
            return this;
        }

        public Builder euOutputMultiplier(double multiplier) {
            this.euOutputMultiplier = multiplier;
            return this;
        }

        public ModifierFunction build() {
            ModifierFunction baseModifier = gtmBuilder.build();

            return recipe -> {
                GTRecipe modified = baseModifier.apply(recipe);
                if (modified == null) return null;

                if (sourceMultiplier != 1.0 && modified.inputs.containsKey(SourceRecipeCapability.CAP)) {
                    var sourceContents = new ArrayList<>(modified.inputs.get(SourceRecipeCapability.CAP));
                    sourceContents.replaceAll(content -> content.copy(SourceRecipeCapability.CAP,
                            ContentModifier.multiplier(sourceMultiplier)));
                    modified.inputs.put(SourceRecipeCapability.CAP, sourceContents);
                }

                if (sourceMultiplier != 1.0 && modified.outputs.containsKey(SourceRecipeCapability.CAP)) {
                    var sourceContents = new ArrayList<>(modified.outputs.get(SourceRecipeCapability.CAP));
                    sourceContents.replaceAll(content -> content.copy(SourceRecipeCapability.CAP,
                            ContentModifier.multiplier(sourceMultiplier)));
                    modified.outputs.put(SourceRecipeCapability.CAP, sourceContents);
                }

                if (euOutputMultiplier != 1.0 && modified.tickOutputs.containsKey(EURecipeCapability.CAP)) {
                    var euContents = new ArrayList<>(modified.tickOutputs.get(EURecipeCapability.CAP));
                    euContents.replaceAll(content -> content.copy(EURecipeCapability.CAP,
                            ContentModifier.multiplier(euOutputMultiplier)));
                    modified.tickOutputs.put(EURecipeCapability.CAP, euContents);
                }

                return modified;
            };
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
