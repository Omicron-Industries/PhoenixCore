package net.phoenix.core.common.machine.multiblock.api;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

@UtilityClass
public class PhoenixPartAppearance {

    public static Builder rules() {
        return new Builder();
    }

    public static TriFunction<MultiblockControllerMachine, MultiblockPartMachine, Direction, BlockState> casingExceptAbilities(Supplier<? extends Block> casing,
                                                                                                                               PartAbility... abilities) {
        return rules().ownTextureForAbilities(abilities).build(casing);
    }

    @FunctionalInterface
    public interface AppearanceResult {

        AppearanceResult OWN = (ctrl, part, side) -> null;

        static AppearanceResult ofBlock(Supplier<? extends Block> block) {
            return (ctrl, part, side) -> block.get().defaultBlockState();
        }

        static AppearanceResult ofState(Supplier<BlockState> state) {
            return (ctrl, part, side) -> state.get();
        }

        @Nullable
        BlockState apply(MultiblockControllerMachine ctrl, MultiblockPartMachine part, Direction side);
    }

    @FunctionalInterface
    public interface AppearanceRule {

        RuleMatch apply(MultiblockControllerMachine ctrl, MultiblockPartMachine part, Direction side);
    }

    public sealed interface RuleMatch permits RuleMatch.Skip, RuleMatch.Matched {

        RuleMatch SKIP = new Skip();

        static RuleMatch matched(@Nullable BlockState state) {
            return new Matched(state);
        }

        record Skip() implements RuleMatch {}

        record Matched(@Nullable BlockState state) implements RuleMatch {}
    }

    public static final class Builder {

        private final List<AppearanceRule> rules = new ArrayList<>();

        public Builder forAbilities(AppearanceResult result, PartAbility... abilities) {
            return when((ctrl, part, side) -> hasAbility(part, abilities) ?
                    RuleMatch.matched(result.apply(ctrl, part, side)) : RuleMatch.SKIP);
        }

        public Builder ownTextureForAbilities(PartAbility... abilities) {
            return forAbilities(AppearanceResult.OWN, abilities);
        }

        public Builder ownTextureForAbilitiesAtTier(int minTier, PartAbility... abilities) {
            return when((ctrl, part, side) -> {
                if (!isAtLeastTier(ctrl, minTier)) return RuleMatch.SKIP;
                if (!hasAbility(part, abilities)) return RuleMatch.SKIP;
                return RuleMatch.matched(null);
            });
        }

        public Builder forClass(Class<?> partClass, AppearanceResult result) {
            return when((ctrl, part, side) -> partClass.isInstance(part) ?
                    RuleMatch.matched(result.apply(ctrl, part, side)) : RuleMatch.SKIP);
        }

        public Builder ownTextureForClass(Class<?> partClass) {
            return forClass(partClass, AppearanceResult.OWN);
        }

        public Builder forWorldFace(Direction face, AppearanceResult result) {
            return when((ctrl, part, side) -> {
                if (part == null) return RuleMatch.SKIP;
                return dominantFace(ctrl, part) == face ? RuleMatch.matched(result.apply(ctrl, part, side)) :
                        RuleMatch.SKIP;
            });
        }

        public Builder ownTextureOnWorldFace(Direction face) {
            return forWorldFace(face, AppearanceResult.OWN);
        }

        public Builder forRelativeFace(RelativeMultiblockFace face, AppearanceResult result) {
            return when((ctrl, part, side) -> {
                if (part == null) return RuleMatch.SKIP;
                Direction worldFace = face.toWorldDirection(ctrl.getFrontFacing());
                return dominantFace(ctrl, part) == worldFace ? RuleMatch.matched(result.apply(ctrl, part, side)) :
                        RuleMatch.SKIP;
            });
        }

        public Builder ownTextureOnRelativeFace(RelativeMultiblockFace face) {
            return forRelativeFace(face, AppearanceResult.OWN);
        }

        public Builder forRelativeY(int yOffset, AppearanceResult result) {
            return when((ctrl, part, side) -> {
                if (part == null) return RuleMatch.SKIP;
                int target = ctrl.self().getBlockPos().getY() + yOffset;
                return part.getBlockPos().getY() == target ? RuleMatch.matched(result.apply(ctrl, part, side)) :
                        RuleMatch.SKIP;
            });
        }

        public Builder forTierAtLeast(int minTier, AppearanceResult result) {
            return when((ctrl, part, side) -> {
                if (!isAtLeastTier(ctrl, minTier)) return RuleMatch.SKIP;
                return RuleMatch.matched(result.apply(ctrl, part, side));
            });
        }

        public Builder when(AppearanceRule rule) {
            rules.add(rule);
            return this;
        }

        public TriFunction<MultiblockControllerMachine, MultiblockPartMachine, Direction, BlockState> build(
                                                                                                            Supplier<? extends Block> defaultCasing) {
            List<AppearanceRule> frozen = List.copyOf(rules);
            return (ctrl, part, side) -> {
                for (AppearanceRule rule : frozen) {
                    RuleMatch match = rule.apply(ctrl, part, side);
                    if (match instanceof RuleMatch.Matched m) return m.state();
                }
                return defaultCasing.get().defaultBlockState();
            };
        }
    }

    public enum RelativeMultiblockFace {

        FRONT,
        BACK,
        LEFT,
        RIGHT,
        TOP,
        BOTTOM;

        public Direction toWorldDirection(Direction controllerFacing) {
            return switch (this) {
                case FRONT -> controllerFacing;
                case BACK -> controllerFacing.getOpposite();
                case RIGHT -> controllerFacing.getClockWise();
                case LEFT -> controllerFacing.getCounterClockWise();
                case TOP -> Direction.UP;
                case BOTTOM -> Direction.DOWN;
            };
        }
    }

    private static boolean hasAbility(MultiblockPartMachine part, PartAbility[] abilities) {
        if (part == null) return false;

        var block = part.getLevel().getBlockState(part.getBlockPos()).getBlock();
        return Arrays.stream(abilities).anyMatch(a -> a.getAllBlocks().contains(block));
    }

    private static boolean isAtLeastTier(MultiblockControllerMachine ctrl, int minTier) {
        return ctrl instanceof TierAwareMultiblockMachine tiered && tiered.isAtLeastTier(minTier);
    }

    private static Direction dominantFace(MultiblockControllerMachine ctrl, MetaMachine part) {
        BlockPos c = ctrl.self().getBlockPos();
        BlockPos p = part.getBlockPos();
        int dx = p.getX() - c.getX();
        int dy = p.getY() - c.getY();
        int dz = p.getZ() - c.getZ();
        int ax = Math.abs(dx), ay = Math.abs(dy), az = Math.abs(dz);
        if (ay >= ax && ay >= az) return dy >= 0 ? Direction.UP : Direction.DOWN;
        if (ax >= az) return dx >= 0 ? Direction.EAST : Direction.WEST;
        return dz >= 0 ? Direction.SOUTH : Direction.NORTH;
    }
}
