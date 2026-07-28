package net.phoenix.core.growth.tendril;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;

public record TendrilShape(ResourceLocation id, List<BlockPos> steps) {

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public static TendrilShape generated(ResourceLocation id, int length, IntFunction<BlockPos> stepFn) {
        List<BlockPos> steps = new ArrayList<>(length);
        for (int i = 1; i <= length; i++) {
            steps.add(stepFn.apply(i));
        }
        return new TendrilShape(id, List.copyOf(steps));
    }

    public static final class Builder {

        private final ResourceLocation id;
        private final List<BlockPos> steps = new ArrayList<>();
        private BlockPos cursor = BlockPos.ZERO;

        private Builder(ResourceLocation id) {
            this.id = id;
        }

        public Builder move(int dx, int dy, int dz) {
            cursor = cursor.offset(dx, dy, dz);
            steps.add(cursor);
            return this;
        }

        public Builder up(int blocks) {
            return move(0, blocks, 0);
        }

        public Builder drift(int dx, int dz) {
            return move(dx, 1, dz);
        }

        public Builder repeat(int times, Consumer<Builder> step) {
            for (int i = 0; i < times; i++) {
                step.accept(this);
            }
            return this;
        }

        public TendrilShape build() {
            return new TendrilShape(id, List.copyOf(steps));
        }
    }
}
