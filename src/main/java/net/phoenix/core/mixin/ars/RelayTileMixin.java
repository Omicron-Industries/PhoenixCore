package net.phoenix.core.mixin.ars;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.phoenix.core.integration.ars_nouveau.api.capability.ISourceProviderCapability;
import net.phoenix.core.mixin.accessor.RelayTileDisabledAccessor;

import com.hollingsworth.arsnouveau.api.source.AbstractSourceMachine;
import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import com.hollingsworth.arsnouveau.api.util.BlockUtil;
import com.hollingsworth.arsnouveau.client.particle.ParticleUtil;
import com.hollingsworth.arsnouveau.common.block.tile.RelayTile;
import com.hollingsworth.arsnouveau.common.items.DominionWand;
import com.hollingsworth.arsnouveau.common.util.PortUtil;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RelayTile.class, remap = false)
public abstract class RelayTileMixin {

    @Unique
    private RelayTile phoenix$self() {
        return (RelayTile) (Object) this;
    }

    @Unique
    @Nullable
    private static ISourceTile phoenix$resolve(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof AbstractSourceMachine asm) return asm;
        if (be == null) return null;

        var cap = be.getCapability(ISourceProviderCapability.CAPABILITY);
        if (cap.isPresent()) {
            return cap.resolve().map(ISourceProviderCapability::getSource).orElse(null);
        }
        return null;
    }

    @Unique
    private static boolean phoenix$invalidDistanceOrSelf(RelayTile relay, BlockPos pos) {
        return BlockUtil.distanceFrom(pos, relay.getBlockPos()) > relay.getMaxDistance() ||
                pos.equals(relay.getBlockPos());
    }

    @Inject(method = "setSendTo", at = @At("HEAD"), cancellable = true)
    private void phoenix$setSendTo(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        RelayTile relay = phoenix$self();
        Level level = relay.getLevel();
        if (level == null) return;

        if (phoenix$invalidDistanceOrSelf(relay, pos)) {
            cir.setReturnValue(false);
            return;
        }

        if (phoenix$resolve(level, pos) != null) {
            relay.setToPos(pos.immutable());
            relay.updateBlock();
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "onFinishedConnectionFirst", at = @At("HEAD"), cancellable = true)
    private void phoenix$onFinishedConnectionFirst(BlockPos pos, LivingEntity entity, Player player, CallbackInfo ci) {
        RelayTile relay = phoenix$self();
        Level level = relay.getLevel();
        if (level == null || level.isClientSide || pos == null) return;

        if (phoenix$invalidDistanceOrSelf(relay, pos)) return;
        if (phoenix$resolve(level, pos) == null) return;

        relay.setToPos(pos.immutable());
        relay.updateBlock();

        PortUtil.sendMessage(entity,
                Component.translatable("ars_nouveau.connections.send", DominionWand.getPosString(pos)));
        ParticleUtil.beam(pos, relay.getBlockPos(), level);

        ci.cancel();
    }

    @Inject(method = "onFinishedConnectionLast", at = @At("HEAD"), cancellable = true)
    private void phoenix$onFinishedConnectionLast(BlockPos pos, LivingEntity entity, Player player, CallbackInfo ci) {
        RelayTile relay = phoenix$self();
        Level level = relay.getLevel();
        if (level == null || pos == null) return;

        if (pos.equals(relay.getBlockPos())) return;
        if (level.getBlockEntity(pos) instanceof RelayTile) return;
        if (phoenix$resolve(level, pos) == null) return;

        if (relay.setTakeFrom(pos.immutable())) {
            PortUtil.sendMessage(entity,
                    Component.translatable("ars_nouveau.connections.take", DominionWand.getPosString(pos)));
        } else {
            PortUtil.sendMessage(entity, Component.translatable("ars_nouveau.connections.fail"));
        }

        ci.cancel();
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void phoenix$tick(CallbackInfo ci) {
        RelayTile relay = phoenix$self();
        Level level = relay.getLevel();
        if (level == null) return;

        boolean disabled = ((RelayTileDisabledAccessor) (Object) this).phoenix$isDisabled();
        if (level.isClientSide || disabled) {
            ci.cancel();
            return;
        }

        if (level.getGameTime() % 20L != 0L) {
            ci.cancel();
            return;
        }

        BlockPos fromPos = relay.getFromPos();
        if (fromPos != null && level.isLoaded(fromPos)) {
            ISourceTile from = phoenix$resolve(level, fromPos);
            if (from == null) {
                relay.setFromPos(null);
                relay.updateBlock();
                ci.cancel();
                return;
            }

            if (relay.transferSource(from, relay) > 0) {
                relay.updateBlock();
                ParticleUtil.spawnFollowProjectile(level, fromPos, relay.getBlockPos(), relay.getColor());
            }
        }

        BlockPos toPos = relay.getToPos();
        if (toPos != null && level.isLoaded(toPos)) {
            ISourceTile to = phoenix$resolve(level, toPos);
            if (to == null) {
                relay.setToPos(null);
                relay.updateBlock();
                ci.cancel();
                return;
            }

            if (relay.transferSource(relay, to) > 0) {
                ParticleUtil.spawnFollowProjectile(level, relay.getBlockPos(), toPos, relay.getColor());
            }
        }

        ci.cancel();
    }
}
