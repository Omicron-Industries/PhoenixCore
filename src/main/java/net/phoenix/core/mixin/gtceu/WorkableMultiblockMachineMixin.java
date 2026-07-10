package net.phoenix.core.mixin.gtceu;

import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import net.minecraft.server.level.ServerLevel;
import net.phoenix.core.integration.ars_nouveau.api.recipe.SoulGrowthHook;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WorkableMultiblockMachine.class, remap = false)
public abstract class WorkableMultiblockMachineMixin {

    @Inject(method = "afterWorking", at = @At("TAIL"))
    private void phoenix$afterWorkingSoulHook(CallbackInfo ci) {
        WorkableMultiblockMachine machine = (WorkableMultiblockMachine) (Object) this;

        var logic = machine.getRecipeLogic();
        GTRecipe recipe = logic.getLastRecipe();

        if (recipe == null ||
                (!recipe.data.contains("soul_growth_perm") && !recipe.data.contains("soul_growth_temp"))) {
            return;
        }

        if (machine.getLevel() instanceof ServerLevel serverLevel) {
            SoulGrowthHook.handle(recipe, serverLevel, machine.getBlockPos());
        }
    }
}
