package net.phoenix.core.common.worldgen;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SurfaceRules;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for assembling {@link SurfaceRules.RuleSource} sequences.
 * <p>
 * Call {@link #layered()} to start, chain the builder methods, then call
 * {@link LayeredBuilder#build()} and pass the result to your biome's noise settings.
 * <p>
 * Nothing here is registered automatically — it's purely a helper for writing biomes.
 *
 * <pre>{@code
 * SurfaceRules.RuleSource rules = PhoenixSurfaceRules.layered()
 *     .top(Blocks.GRASS_BLOCK.defaultBlockState())
 *     .under(Blocks.DIRT.defaultBlockState(), 3)
 *     .base(Blocks.STONE.defaultBlockState())
 *     .belowY(0, Blocks.DEEPSLATE.defaultBlockState())
 *     .build();
 * }</pre>
 */
public final class PhoenixSurfaceRules {

    private PhoenixSurfaceRules() {}

    public static LayeredBuilder layered() {
        return new LayeredBuilder();
    }

    public static class LayeredBuilder {
        private BlockState topBlock   = null;
        private int        topDepth   = 1;
        private BlockState underBlock = null;
        private int        underDepth = 3;
        private BlockState baseBlock  = null;
        private final List<ConditionalLayer> extras = new ArrayList<>();

        public LayeredBuilder top(BlockState block)              { topBlock = block;                    return this; }
        public LayeredBuilder top(BlockState block, int depth)   { topBlock = block; topDepth = depth;  return this; }
        public LayeredBuilder under(BlockState block)            { underBlock = block;                  return this; }
        public LayeredBuilder under(BlockState block, int depth) { underBlock = block; underDepth = depth; return this; }
        public LayeredBuilder base(BlockState block)             { baseBlock = block;                   return this; }

        /**
         * Adds a layer that replaces the base block below the given Y level.
         * Multiple calls are evaluated in order (first match wins).
         */
        public LayeredBuilder belowY(int y, BlockState block) {
            extras.add(new ConditionalLayer(y, block));
            return this;
        }

        public SurfaceRules.RuleSource build() {
            List<SurfaceRules.RuleSource> rules = new ArrayList<>();

            // Top surface block(s)
            if (topBlock != null) {
                SurfaceRules.RuleSource topRule = SurfaceRules.state(topBlock);
                for (int i = 0; i < topDepth; i++) {
                    rules.add(SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, topRule));
                }
            }

            // Under-surface block(s) — UNDER_FLOOR covers the next N blocks below the surface
            if (underBlock != null) {
                SurfaceRules.RuleSource underRule = SurfaceRules.state(underBlock);
                for (int i = 0; i < underDepth; i++) {
                    rules.add(SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, underRule));
                }
            }

            // Y-conditional deep layers (e.g. deepslate below y=0)
            // Sort descending so higher-Y overrides come first (first match wins in sequence)
            extras.stream()
                    .sorted((a, b) -> Integer.compare(b.belowY(), a.belowY()))
                    .forEach(layer -> rules.add(
                            SurfaceRules.ifTrue(
                                    SurfaceRules.yBlockCheck(
                                            net.minecraft.world.level.levelgen.VerticalAnchor.absolute(layer.belowY()), 0),
                                    SurfaceRules.state(layer.block()))));

            // Fallback base block — always matches
            if (baseBlock != null) {
                rules.add(SurfaceRules.state(baseBlock));
            }

            return SurfaceRules.sequence(rules.toArray(new SurfaceRules.RuleSource[0]));
        }
    }

    private record ConditionalLayer(int belowY, BlockState block) {}
}
