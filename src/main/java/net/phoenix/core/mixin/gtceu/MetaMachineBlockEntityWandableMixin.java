package net.phoenix.core.mixin.gtceu;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.phoenix.core.integration.ars_nouveau.common.data.multiblock.part.source.SourceHatchPartMachine;

import com.hollingsworth.arsnouveau.api.item.IWandable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = MetaMachineBlockEntity.class, remap = false)
public abstract class MetaMachineBlockEntityWandableMixin implements IWandable {

    @Unique
    private MetaMachine phoenix$getMachine() {
        return ((MetaMachineBlockEntity) (Object) this).getMetaMachine();
    }

    @Unique
    private boolean phoenix$isSourceHatch(MetaMachine m) {
        return m instanceof SourceHatchPartMachine;
    }

    @Override
    public void onFinishedConnectionFirst(BlockPos storedPos, LivingEntity entity, Player player) {
        MetaMachine m = phoenix$getMachine();
        if (!phoenix$isSourceHatch(m)) return;

        if (m instanceof IWandable wandable) {
            wandable.onFinishedConnectionFirst(storedPos, entity, player);
        }
    }

    @Override
    public void onFinishedConnectionLast(BlockPos storedPos, LivingEntity entity, Player player) {
        MetaMachine m = phoenix$getMachine();
        if (!phoenix$isSourceHatch(m)) return;

        if (m instanceof IWandable wandable) {
            wandable.onFinishedConnectionLast(storedPos, entity, player);
        }
    }
}
