package net.phoenix.core.common.worldgen;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SurfaceRules;

import java.util.ArrayList;
import java.util.List;

public final class PhoenixSurfaceRules {

    private PhoenixSurfaceRules() {}

    public static LayeredBuilder layered() {
        return new LayeredBuilder();
    }

    public static class LayeredBuilder {

        private BlockState topBlock = null;
        private int topDepth = 1;
        private BlockState underBlock = null;
        private int underDepth = 3;
        private BlockState baseBlock = null;
        private final List<ConditionalLayer> extras = new ArrayList<>();

        public LayeredBuilder top(BlockState block) {
            topBlock = block;
            return this;
        }

        public LayeredBuilder top(BlockState block, int depth) {
            topBlock = block;
            topDepth = depth;
            return this;
        }

        public LayeredBuilder under(BlockState block) {
            underBlock = block;
            return this;
        }

        public LayeredBuilder under(BlockState block, int depth) {
            underBlock = block;
            underDepth = depth;
            return this;
        }

        public LayeredBuilder base(BlockState block) {
            baseBlock = block;
            return this;
        }

        public LayeredBuilder belowY(int y, BlockState block) {
            extras.add(new ConditionalLayer(y, block));
            return this;
        }

        public SurfaceRules.RuleSource build() {
            List<SurfaceRules.RuleSource> rules = new ArrayList<>();

            if (topBlock != null) {
                SurfaceRules.RuleSource topRule = SurfaceRules.state(topBlock);
                for (int i = 0; i < topDepth; i++) {
                    rules.add(SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, topRule));
                }
            }

            if (underBlock != null) {
                SurfaceRules.RuleSource underRule = SurfaceRules.state(underBlock);
                for (int i = 0; i < underDepth; i++) {
                    rules.add(SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, underRule));
                }
            }

            extras.stream()
                    .sorted((a, b) -> Integer.compare(b.belowY(), a.belowY()))
                    .forEach(layer -> rules.add(
                            SurfaceRules.ifTrue(
                                    SurfaceRules.yBlockCheck(
                                            net.minecraft.world.level.levelgen.VerticalAnchor.absolute(layer.belowY()),
                                            0),
                                    SurfaceRules.state(layer.block()))));

            if (baseBlock != null) {
                rules.add(SurfaceRules.state(baseBlock));
            }

            return SurfaceRules.sequence(rules.toArray(new SurfaceRules.RuleSource[0]));
        }
    }

    private record ConditionalLayer(int belowY, BlockState block) {}
}
