package net.phoenix.core.conflux.research;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeCondition;
import com.gregtechceu.gtceu.api.recipe.condition.RecipeConditionType;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@NoArgsConstructor
public class AxiomResearchCondition extends RecipeCondition<AxiomResearchCondition> {

    public static final String CONDITION_ID = "phoenixcore:axiom_research";

    public static final Codec<AxiomResearchCondition> CODEC = RecordCodecBuilder
            .create(instance -> RecipeCondition.isReverse(instance)
                    .and(Codec.STRING.fieldOf("flag").forGetter(AxiomResearchCondition::getFlag))
                    .apply(instance, AxiomResearchCondition::new));

    public static RecipeConditionType<AxiomResearchCondition> TYPE;

    private static final double CHECK_RADIUS = 24.0;

    @Getter
    private @NotNull String flag = "";

    public AxiomResearchCondition(boolean isReverse, @NotNull String flag) {
        super(isReverse);
        this.flag = flag;
    }

    public static AxiomResearchCondition of(String flag) {
        return new AxiomResearchCondition(false, flag);
    }

    @Override
    protected boolean testCondition(@NotNull GTRecipe recipe, @NotNull RecipeLogic recipeLogic) {
        MetaMachine machine = recipeLogic.getMachine();
        if (!(machine.getLevel() instanceof ServerLevel level)) return false;

        ServerPlayer nearestPlayer = level.getNearestPlayer(
                machine.getBlockPos().getX() + 0.5,
                machine.getBlockPos().getY() + 0.5,
                machine.getBlockPos().getZ() + 0.5,
                CHECK_RADIUS,
                p -> !p.isSpectator() && p instanceof ServerPlayer) instanceof ServerPlayer sp ? sp : null;

        if (nearestPlayer == null) return false;

        UUID teamId = ResearchTeamHelper.getTeamId(nearestPlayer);
        return WorldResearchData.get(level).hasFlag(teamId, flag);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Component getTooltips() {
        return Component.translatable("phoenixcore.condition.axiom_research", flag);
    }

    @Override
    public AxiomResearchCondition createTemplate() {
        return new AxiomResearchCondition();
    }

    @Override
    public RecipeConditionType<AxiomResearchCondition> getType() {
        if (TYPE == null) throw new IllegalStateException("AxiomResearchCondition.TYPE not registered yet!");
        return TYPE;
    }
}
