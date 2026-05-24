package net.phoenix.core.integration.vocal_resonance.ingredient;

import com.gregtechceu.gtceu.api.recipe.content.IContentSerializer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SoundIngredient(
                              String soundName,
                              int minLoudness,
                              float targetCentroid,
                              int requiredBPM,
                              boolean exactMatch,   // If true, name must match. If false, only frequency/BPM matters.
                              float tolerance       // 0.0 to 1.0. Lower = harder to match (needs exact frequency).
) {

    public static final SoundIngredient EMPTY = new SoundIngredient("", 1, 0.0f, 0, false, 0.2f);

    public static final Codec<SoundIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("sound").forGetter(SoundIngredient::soundName),
            Codec.INT.optionalFieldOf("minLoudness", 1).forGetter(SoundIngredient::minLoudness),
            Codec.FLOAT.optionalFieldOf("centroid", 0.0f).forGetter(SoundIngredient::targetCentroid),
            Codec.INT.optionalFieldOf("bpm", 0).forGetter(SoundIngredient::requiredBPM),
            Codec.BOOL.optionalFieldOf("exact", false).forGetter(SoundIngredient::exactMatch),
            Codec.FLOAT.optionalFieldOf("tolerance", 0.2f).forGetter(SoundIngredient::tolerance))
            .apply(instance, SoundIngredient::new));

    // Overloaded constructors for convenience
    public SoundIngredient(String soundName) {
        this(soundName, 1, 0.0f, 0, true, 0.2f);
    }

    public SoundIngredient(String soundName, int minLoudness) {
        this(soundName, minLoudness, 0.0f, 0, true, 0.2f);
    }

    public SoundIngredient copy() {
        return new SoundIngredient(soundName, minLoudness, targetCentroid, requiredBPM, exactMatch, tolerance);
    }

    public static final class Serializer implements IContentSerializer<SoundIngredient> {

        public static final Serializer INSTANCE = new Serializer();

        @Override
        public SoundIngredient of(Object o) {
            if (o instanceof String str) return new SoundIngredient(str);
            if (o instanceof SoundIngredient sound) return sound;
            return null;
        }

        @Override
        public SoundIngredient defaultValue() {
            return EMPTY;
        }

        @Override
        public Class<SoundIngredient> contentClass() {
            return SoundIngredient.class;
        }

        @Override
        public Codec<SoundIngredient> codec() {
            return CODEC;
        }
    }
}
