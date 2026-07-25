package net.phoenix.core.mixin.gtceu;

import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.phoenix.core.conflux.research.ResearchTeamHelper;
import net.phoenix.core.conflux.research.ResearchTreeRegistry;
import net.phoenix.core.conflux.research.WorldResearchData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(value = MultiblockControllerMachine.class, remap = false)
public abstract class MultiblockResearchGateMixin {

    @Inject(method = "formStructure", at = @At("HEAD"), cancellable = true)
    private void conflux$gateMultiblockFormation(String structureName, CallbackInfo ci) {
        MultiblockControllerMachine machine = (MultiblockControllerMachine) (Object) this;
        if (!(machine.getLevel() instanceof ServerLevel level)) return;

        ResourceLocation machineId = machine.getDefinition().getId();
        if (!ResearchTreeRegistry.INSTANCE.getGatedMultiblocks().contains(machineId)) return;

        ServerPlayer nearest = (ServerPlayer) level.getNearestPlayer(
                machine.getBlockPos().getX() + 0.5,
                machine.getBlockPos().getY() + 0.5,
                machine.getBlockPos().getZ() + 0.5,
                64, p -> !p.isSpectator() && p instanceof ServerPlayer);

        UUID teamId = nearest != null ? ResearchTeamHelper.getTeamId(nearest) : null;

        if (teamId == null || !WorldResearchData.get(level).canFormMultiblock(teamId, machineId)) {
            if (nearest != null) {
                nearest.sendSystemMessage(Component.translatable(
                        "phoenixcore.research.multiblock_locked", machineId.getPath()));
            }
            ci.cancel();
        }
    }
}
