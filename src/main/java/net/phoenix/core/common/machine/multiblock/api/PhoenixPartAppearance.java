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

/**
 * Fluent API for configuring per-part visual appearance on Phoenix multiblocks.
 *
 * <p>GTM's default behaviour makes every formed part mimic the controller's casing texture.
 * This API gives you fine-grained control over which parts keep their own hatch/bus model
 * and which ones should blend into the casing.
 *
 * <h2>Quick-start</h2>
 * <pre>{@code
 * // In your machine builder:
 * .partAppearance(PhoenixPartAppearance.rules()
 *     .ownTextureForAbilities(PartAbility.EXPORT_ITEMS, PartAbility.EXPORT_FLUIDS)
 *     .build(MY_CASING_BLOCK))
 * }</pre>
 *
 * <h2>Rule evaluation</h2>
 * Rules are checked in the order you add them.  The first rule that matches a part+side
 * decides the appearance.  If no rule matches, the part shows the casing texture you pass
 * to {@link Builder#build}.
 *
 * <h2>Tier-aware appearance</h2>
 * <pre>{@code
 * .partAppearance(PhoenixPartAppearance.rules()
 *     // Tier-0: all parts blend into casing (default)
 *     // Tier-1: output hatches reveal themselves
 *     .ownTextureForAbilitiesAtTier(1, PartAbility.EXPORT_FLUIDS)
 *     // Tier-2: input AND output hatches are visible
 *     .ownTextureForAbilitiesAtTier(2,
 *         PartAbility.IMPORT_FLUIDS, PartAbility.EXPORT_FLUIDS)
 *     .build(MY_CASING_BLOCK))
 * }</pre>
 *
 * <h2>Face-relative appearance</h2>
 * <pre>{@code
 * .partAppearance(PhoenixPartAppearance.rules()
 *     // Front face shows hatches; all other faces blend in
 *     .ownTextureOnRelativeFace(RelativeMultiblockFace.FRONT)
 *     .build(MY_CASING_BLOCK))
 * }</pre>
 */
@UtilityClass
public class PhoenixPartAppearance {

    // ── Entry-points ──────────────────────────────────────────────────────────

    /** Start building a rule chain. */
    public static Builder rules() {
        return new Builder();
    }

    /**
     * All parts show the casing texture except those with one of the listed abilities,
     * which show their own hatch model.  Convenience wrapper for the most common pattern.
     */
    public static TriFunction<MultiblockControllerMachine, MultiblockPartMachine, Direction, BlockState>
            casingExceptAbilities(Supplier<? extends Block> casing, PartAbility... abilities) {
        return rules().ownTextureForAbilities(abilities).build(casing);
    }

    // ── Result type ───────────────────────────────────────────────────────────

    /**
     * What a matched part should look like.
     *
     * <ul>
     *   <li>{@link #OWN} — keep the part's own hatch/bus texture.</li>
     *   <li>{@link #ofBlock} — show any block's default state.</li>
     *   <li>{@link #ofState} — show an exact BlockState.</li>
     * </ul>
     */
    @FunctionalInterface
    public interface AppearanceResult {

        /** Show the part's own model (hatch, bus, etc.). */
        AppearanceResult OWN = (ctrl, part, side) -> null;

        static AppearanceResult ofBlock(Supplier<? extends Block> block) {
            return (ctrl, part, side) -> block.get().defaultBlockState();
        }

        static AppearanceResult ofState(Supplier<BlockState> state) {
            return (ctrl, part, side) -> state.get();
        }

        /**
         * Resolves the appearance.  Return {@code null} to show the part's own texture.
         * This is called only when the enclosing rule has already matched.
         */
        @Nullable BlockState apply(MultiblockControllerMachine ctrl, MultiblockPartMachine part, Direction side);
    }

    // ── Rule interface ────────────────────────────────────────────────────────

    /**
     * A single appearance rule.  Return {@link RuleMatch#SKIP} when the rule does not
     * apply — the builder then tries the next rule.
     */
    @FunctionalInterface
    public interface AppearanceRule {
        RuleMatch apply(MultiblockControllerMachine ctrl, MultiblockPartMachine part, Direction side);
    }

    /**
     * Sealed result returned by an {@link AppearanceRule}.
     *
     * <ul>
     *   <li>{@link #SKIP} — this rule does not match; continue to next rule.</li>
     *   <li>{@link #matched(BlockState)} — use the given state (null = own texture).</li>
     * </ul>
     */
    public sealed interface RuleMatch permits RuleMatch.Skip, RuleMatch.Matched {

        /** Singleton: rule did not match. */
        RuleMatch SKIP = new Skip();

        /** The rule matched and the part should use {@code state} ({@code null} = own texture). */
        static RuleMatch matched(@Nullable BlockState state) {
            return new Matched(state);
        }

        record Skip() implements RuleMatch {}

        record Matched(@Nullable BlockState state) implements RuleMatch {}
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static final class Builder {

        private final List<AppearanceRule> rules = new ArrayList<>();

        // ── Ability-based ─────────────────────────────────────────────────────

        /**
         * Parts with any of the listed abilities use {@code result} when the structure is formed.
         */
        public Builder forAbilities(AppearanceResult result, PartAbility... abilities) {
            return when((ctrl, part, side) ->
                    hasAbility(part, abilities) ? RuleMatch.matched(result.apply(ctrl, part, side)) : RuleMatch.SKIP);
        }

        /** Parts with any of the listed abilities show their own texture. */
        public Builder ownTextureForAbilities(PartAbility... abilities) {
            return forAbilities(AppearanceResult.OWN, abilities);
        }

        /**
         * Parts with any of the listed abilities show their own texture, but ONLY when the
         * controller is at {@code minTier} or higher (requires {@link TierAwareMultiblockMachine}).
         */
        public Builder ownTextureForAbilitiesAtTier(int minTier, PartAbility... abilities) {
            return when((ctrl, part, side) -> {
                if (!isAtLeastTier(ctrl, minTier)) return RuleMatch.SKIP;
                if (!hasAbility(part, abilities)) return RuleMatch.SKIP;
                return RuleMatch.matched(null);
            });
        }

        // ── Class-based ───────────────────────────────────────────────────────

        /** Parts that are instances of {@code partClass} use {@code result}. */
        public Builder forClass(Class<?> partClass, AppearanceResult result) {
            return when((ctrl, part, side) ->
                    partClass.isInstance(part) ? RuleMatch.matched(result.apply(ctrl, part, side)) : RuleMatch.SKIP);
        }

        /** Parts that are instances of {@code partClass} show their own texture. */
        public Builder ownTextureForClass(Class<?> partClass) {
            return forClass(partClass, AppearanceResult.OWN);
        }

        // ── Face-based ────────────────────────────────────────────────────────

        /**
         * Parts whose dominant displacement from the controller is in the given world
         * {@link Direction} use {@code result}.
         *
         * <p>Use {@link #forRelativeFace} instead if you want to specify faces relative
         * to the controller's own facing direction (front/back/left/right).
         */
        public Builder forWorldFace(Direction face, AppearanceResult result) {
            return when((ctrl, part, side) -> {
                if (part == null) return RuleMatch.SKIP;
                return dominantFace(ctrl, part) == face
                        ? RuleMatch.matched(result.apply(ctrl, part, side)) : RuleMatch.SKIP;
            });
        }

        /** Parts on the given world face show their own texture. */
        public Builder ownTextureOnWorldFace(Direction face) {
            return forWorldFace(face, AppearanceResult.OWN);
        }

        /**
         * Parts on the given logical face of the multiblock (relative to the controller's
         * front-facing direction) use {@code result}.
         *
         * <pre>{@code
         * // Output face of machine shows hatches; rest blends in
         * .forRelativeFace(RelativeMultiblockFace.BACK, AppearanceResult.OWN)
         * }</pre>
         */
        public Builder forRelativeFace(RelativeMultiblockFace face, AppearanceResult result) {
            return when((ctrl, part, side) -> {
                if (part == null) return RuleMatch.SKIP;
                Direction worldFace = face.toWorldDirection(ctrl.getFrontFacing());
                return dominantFace(ctrl, part) == worldFace
                        ? RuleMatch.matched(result.apply(ctrl, part, side)) : RuleMatch.SKIP;
            });
        }

        /** Parts on the given logical face show their own texture. */
        public Builder ownTextureOnRelativeFace(RelativeMultiblockFace face) {
            return forRelativeFace(face, AppearanceResult.OWN);
        }

        // ── Y-level ───────────────────────────────────────────────────────────

        /**
         * Parts at {@code yOffset} blocks above (or below, for negative values) the
         * controller use {@code result}.  Handy for tall multiblocks (e.g. EBF-style
         * firebox rings at the base).
         */
        public Builder forRelativeY(int yOffset, AppearanceResult result) {
            return when((ctrl, part, side) -> {
                if (part == null) return RuleMatch.SKIP;
                int target = ctrl.self().getBlockPos().getY() + yOffset;
                return part.getBlockPos().getY() == target
                        ? RuleMatch.matched(result.apply(ctrl, part, side)) : RuleMatch.SKIP;
            });
        }

        // ── Tier-based ────────────────────────────────────────────────────────

        /**
         * Applies {@code result} to ALL parts when the controller is at least {@code minTier}.
         * Requires a {@link TierAwareMultiblockMachine} controller; silently skips otherwise.
         */
        public Builder forTierAtLeast(int minTier, AppearanceResult result) {
            return when((ctrl, part, side) -> {
                if (!isAtLeastTier(ctrl, minTier)) return RuleMatch.SKIP;
                return RuleMatch.matched(result.apply(ctrl, part, side));
            });
        }

        // ── Escape hatch ──────────────────────────────────────────────────────

        /** Add a fully custom rule. */
        public Builder when(AppearanceRule rule) {
            rules.add(rule);
            return this;
        }

        // ── Terminal ──────────────────────────────────────────────────────────

        /**
         * Builds the {@code TriFunction} to pass to {@code .partAppearance(...)} in your
         * machine builder.  Parts that match no rule receive {@code defaultCasing}'s default state.
         *
         * <pre>{@code
         * .partAppearance(PhoenixPartAppearance.rules()
         *     .ownTextureForAbilities(PartAbility.EXPORT_ITEMS)
         *     .build(MY_CASING))
         * }</pre>
         */
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

    // ── Relative face enum ────────────────────────────────────────────────────

    /**
     * Logical face of a multiblock relative to the controller's own facing direction.
     *
     * <p>{@link #FRONT} is the face the controller looks toward.
     * {@link #LEFT} and {@link #RIGHT} follow the right-hand rule with respect to FRONT.
     */
    public enum RelativeMultiblockFace {
        FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM;

        /**
         * Converts this logical face to the world {@link Direction} it maps to given
         * the controller's front-facing direction.
         */
        public Direction toWorldDirection(Direction controllerFacing) {
            return switch (this) {
                case FRONT  -> controllerFacing;
                case BACK   -> controllerFacing.getOpposite();
                case RIGHT  -> controllerFacing.getClockWise();
                case LEFT   -> controllerFacing.getCounterClockWise();
                case TOP    -> Direction.UP;
                case BOTTOM -> Direction.DOWN;
            };
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private static boolean hasAbility(MultiblockPartMachine part, PartAbility[] abilities) {
        // If there is no part, it definitely doesn't have the ability
        if (part == null) return false;

        var block = part.getLevel().getBlockState(part.getBlockPos()).getBlock();
        return Arrays.stream(abilities).anyMatch(a -> a.getAllBlocks().contains(block));
    }

    private static boolean isAtLeastTier(MultiblockControllerMachine ctrl, int minTier) {
        return ctrl instanceof TierAwareMultiblockMachine tiered && tiered.isAtLeastTier(minTier);
    }

    /**
     * Returns the world {@link Direction} that best describes where {@code part} sits
     * relative to {@code ctrl}.  Used to determine which structural face of the multiblock
     * the part belongs to.
     */
    private static Direction dominantFace(MultiblockControllerMachine ctrl, MetaMachine part) {
        BlockPos c = ctrl.self().getBlockPos();
        BlockPos p = part.getBlockPos();
        int dx = p.getX() - c.getX();
        int dy = p.getY() - c.getY();
        int dz = p.getZ() - c.getZ();
        int ax = Math.abs(dx), ay = Math.abs(dy), az = Math.abs(dz);
        if (ay >= ax && ay >= az) return dy >= 0 ? Direction.UP : Direction.DOWN;
        if (ax >= az)             return dx >= 0 ? Direction.EAST : Direction.WEST;
        return                           dz >= 0 ? Direction.SOUTH : Direction.NORTH;
    }
}
