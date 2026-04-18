package net.phoenix.core.integration.phoenix_fission.common.data.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.phoenix.core.integration.phoenix_fission.common.data.block.entity.NukePrimedEntity;
import net.phoenix.core.integration.phoenix_fission.api.block.PhoenixFissionEntities;
import net.phoenix.core.configs.PhoenixConfigs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NukeBlock extends Block {

    public NukeBlock(Properties props) {
        super(props);
    }

    private void prime(Level level, BlockPos pos, @Nullable LivingEntity igniter) {
        if (level.isClientSide) return;

        var cfg = PhoenixConfigs.INSTANCE.fission;
        if (!cfg.nukeEnabled) return;

        int r = Math.max(1, Math.min(cfg.nukeCubeRadius, Math.max(1, cfg.nukeCubeRadiusCap)));
        int fuse = Math.max(1, cfg.nukeFuseTicks);

        var ent = new NukePrimedEntity(PhoenixFissionEntities.NUKE_PRIMED.get(), level);
        ent.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        ent.setOwner(igniter);
        ent.setFuse(fuse);
        ent.setRadius(r);

        level.removeBlock(pos, false);
        level.addFreshEntity(ent);
        level.gameEvent(igniter, GameEvent.PRIME_FUSE, pos);
    }

    @Override
    public void neighborChanged(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Block block,
                                @NotNull BlockPos fromPos, boolean isMoving) {
        if (level.hasNeighborSignal(pos)) prime(level, pos, null);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Large scale nuclear explosive.").withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(Component.literal("Handle with caution!").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("phoenixcore.nuke_radius",
                Component.literal(String.valueOf(PhoenixConfigs.INSTANCE.fission.nukeCubeRadius))
                        .withStyle(ChatFormatting.RED)));
    }

    @Override
    public void onCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction face,
                             @Nullable LivingEntity igniter) {
        prime(level, pos, igniter);
    }

    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                          Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE)) {
            prime(level, pos, player);
            if (!player.isCreative()) {
                if (stack.is(Items.FLINT_AND_STEEL)) stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
                else stack.shrink(1);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
