package net.phoenix.core.integration.ars_nouveau.common.data.recipe.custom;

import com.gregtechceu.gtceu.api.recipe.content.IContentSerializer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;

public class SourceIngredient {

    public static final SourceIngredient EMPTY = new SourceIngredient(0);

    public static final Codec<SourceIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("source").forGetter(SourceIngredient::getSource)).apply(instance, SourceIngredient::new));

    @Getter
    private int source;

    public SourceIngredient(int source) {
        this.source = source;
    }

    public SourceIngredient copy() {
        return new SourceIngredient(source);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SourceIngredient other)) return false;
        return this.source == other.source;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(source);
    }

    @Override
    public String toString() {
        return "SourceIngredient{source=" + source + "}";
    }

    public static final class Serializer implements IContentSerializer<SourceIngredient> {

        public static final SourceIngredient.Serializer INSTANCE = new SourceIngredient.Serializer();

        @Override
        public SourceIngredient of(Object o) {
            if (o instanceof Integer integer) {
                return new SourceIngredient(integer);
            } else if (o instanceof SourceIngredient SourceIngredient) {
                return SourceIngredient;
            }
            return null;
        }

        @Override
        public SourceIngredient defaultValue() {
            return EMPTY;
        }

        @Override
        public Class<SourceIngredient> contentClass() {
            return SourceIngredient.class;
        }

        @Override
        public Codec<SourceIngredient> codec() {
            return CODEC;
        }
    }
}
