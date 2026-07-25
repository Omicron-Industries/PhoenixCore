package net.phoenix.core.integration.vocal_resonance.ingredient;

import com.gregtechceu.gtceu.api.recipe.content.IContentSerializer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SoundIngredient(
                              String soundName,
                              float minBass,
                              float minMid,
                              float minTreble,
                              int requiredBPM,
                              boolean exactMatch,
                              float tolerance) {

    public static final SoundIngredient EMPTY = new SoundIngredient("", 0f, 0f, 0f, 0, false, 0.2f);

    public static final Codec<SoundIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("sound", "").forGetter(SoundIngredient::soundName),
            Codec.FLOAT.optionalFieldOf("minBass", 0f).forGetter(SoundIngredient::minBass),
            Codec.FLOAT.optionalFieldOf("minMid", 0f).forGetter(SoundIngredient::minMid),
            Codec.FLOAT.optionalFieldOf("minTreble", 0f).forGetter(SoundIngredient::minTreble),
            Codec.INT.optionalFieldOf("bpm", 0).forGetter(SoundIngredient::requiredBPM),
            Codec.BOOL.optionalFieldOf("exact", false).forGetter(SoundIngredient::exactMatch),
            Codec.FLOAT.optionalFieldOf("tolerance", 0.2f).forGetter(SoundIngredient::tolerance))
            .apply(instance, SoundIngredient::new));

    public SoundIngredient(String soundName) {
        this(soundName, 0f, 0f, 0f, 0, true, 0.2f);
    }

    public SoundIngredient(String soundName, float minBass) {
        this(soundName, minBass, 0f, 0f, 0, false, 0.2f);
    }

    public SoundIngredient copy() {
        return new SoundIngredient(soundName, minBass, minMid, minTreble, requiredBPM, exactMatch, tolerance);
    }

    @Deprecated
    public float minLoudness() {
        return minBass;
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
