package net.phoenix.core.common.data.recipeConditions;

import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
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
            float currentSoul = SoulSavedData.get(level).getMultiplier(new ChunkPos(recipeLogic.getMachine().getPos()));
            return currentSoul >= minSoul;
        }
        return false;
    }

    @Override
    public Component getTooltips() {
        String header = isReverse ? "§dRequires Soul Resonance Below:" : "§dRequires Soul Resonance:";
        return Component.literal(header + "\n§l" + minSoul);
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
