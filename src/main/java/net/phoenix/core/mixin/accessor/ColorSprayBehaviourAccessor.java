package net.phoenix.core.mixin.accessor;

import com.gregtechceu.gtceu.api.blockentity.IPaintable;
import com.gregtechceu.gtceu.api.pipenet.IPipeNode;
import com.gregtechceu.gtceu.common.item.ColorSprayBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.util.TriPredicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(value = ColorSprayBehaviour.class, remap = false)
public interface ColorSprayBehaviourAccessor {



    @Accessor("paintablePredicate")
    static TriPredicate<IPaintable, IPaintable, Direction> getPaintablePredicate() {
        throw new AssertionError();
    }

    @Accessor("gtPipePredicate")
    static TriPredicate<IPipeNode, IPipeNode, Direction> getGtPipePredicate() {
        throw new AssertionError();
    }

    @Invoker("paintPaintable")
    static boolean callPaintPaintable(IPaintable paintable, DyeColor color) {
        throw new AssertionError();
    }

    @Invoker("recolorBlockNoState")
    static boolean callRecolorBlockNoState(Map<DyeColor, Block> map, DyeColor color, Level world, BlockPos pos, Block defaultBlock) {
        throw new AssertionError();
    }

    @Invoker("tryStripBlockColor")
    boolean callTryStripBlockColor(Level world, BlockPos pos, Block block);

    @Invoker("recolorBlockState")
    static boolean callRecolorBlockState(Level level, BlockPos pos, DyeColor color) {
        throw new AssertionError();
    }
}
