package net.phoenix.core.mixin.ars;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.phoenix.core.integration.ars_nouveau.api.capability.ISourceProviderCapability;

import com.hollingsworth.arsnouveau.common.items.DominionWand;
import com.hollingsworth.arsnouveau.common.items.DominionWand.DominionData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DominionWand.class)
public abstract class DominionWandMixin {

    @Unique
    private static final ThreadLocal<Boolean> phoenix$hadStoredData = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(method = "useOn", at = @At("HEAD"))
    private void phoenix$captureStoredData(UseOnContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = ctx.getItemInHand();
        boolean had = new DominionData(stack).hasStoredData();
        phoenix$hadStoredData.set(had);
    }

    @Inject(method = "useOn", at = @At("RETURN"), cancellable = true)
    private void phoenix$consumeClickOnCapEndpoint(UseOnContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
        try {
            Level level = ctx.getLevel();
            if (level.isClientSide) return;

            if (!Boolean.TRUE.equals(phoenix$hadStoredData.get())) return;

            BlockEntity be = level.getBlockEntity(ctx.getClickedPos());
            if (be == null) return;

            if (be.getCapability(ISourceProviderCapability.CAPABILITY).isPresent() &&
                    cir.getReturnValue() == InteractionResult.PASS) {
                cir.setReturnValue(InteractionResult.CONSUME);
            }
        } finally {
            phoenix$hadStoredData.remove();
        }
    }
}
