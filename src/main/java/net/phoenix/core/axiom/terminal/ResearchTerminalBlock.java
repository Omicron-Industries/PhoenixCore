package net.phoenix.core.axiom.terminal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.phoenix.core.client.PhoenixClient;

import org.jetbrains.annotations.Nullable;

public class ResearchTerminalBlock extends BaseEntityBlock {

    public ResearchTerminalBlock(Properties props) {
        super(props);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ResearchTerminalBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof ResearchTerminalBlockEntity terminal) {
                // Safely execute client-side code only when on a CLIENT environment
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> PhoenixClient.openResearchTerminalScreen(terminal));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
