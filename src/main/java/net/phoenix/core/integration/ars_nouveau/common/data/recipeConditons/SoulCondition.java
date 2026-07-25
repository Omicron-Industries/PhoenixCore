package net.phoenix.core.integration.ars_nouveau.common.data.recipeConditons;

import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeCondition;
import com.gregtechceu.gtceu.api.recipe.condition.RecipeConditionType;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.phoenix.core.saveddata.SoulSavedData;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public class SoulCondition extends RecipeCondition<SoulCondition> {

    public static final Codec<SoulCondition> CODEC = RecordCodecBuilder
            .create(instance -> RecipeCondition.isReverse(instance)
                    .and(Codec.FLOAT.fieldOf("minSoul").forGetter(SoulCondition::getMinSoul))
                    .apply(instance, SoulCondition::new));

    @Getter
    private float minSoul;

    public SoulCondition() {
        super(false);
    }

    public SoulCondition(boolean isReverse, float minSoul) {
        super(isReverse);
        this.minSoul = minSoul;
    }

    @Override
    protected boolean testCondition(@NotNull GTRecipe recipe, @NotNull RecipeLogic recipeLogic) {
        if (recipeLogic.getMachine().getLevel() instanceof ServerLevel level) {
            float currentSoul = SoulSavedData.get(level)
                    .getMultiplier(new ChunkPos(recipeLogic.getMachine().getBlockPos()));
            return currentSoul >= minSoul;
        }
        return false;
    }

    @Override
    public Component getTooltips() {
        String header = isReverse ? "§dRequires Soul Below: " : "§dRequires Soul: ";
        return Component.literal(header + minSoul);
    }

    @Override
    public SoulCondition createTemplate() {
        return new SoulCondition();
    }

    @Override
    public RecipeConditionType<SoulCondition> getType() {
        return TYPE;
    }

    public static RecipeConditionType<SoulCondition> TYPE;
}
