package net.phoenix.core.integration.vocal_resonance.ingredient;

import com.gregtechceu.gtceu.api.recipe.content.IContentSerializer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Describes the acoustic properties a recipe requires from the world.
 *
 * Fields:
 * - {@code sound} — ResourceLocation string; only checked when {@code exact} is true.
 * - {@code minBass} — minimum bass energy (0.0–10.0). 0 = don't check.
 * - {@code minMid} — minimum mid energy (0.0–10.0). 0 = don't check.
 * - {@code minTreble} — minimum treble energy (0.0–10.0). 0 = don't check.
 * Use this to require "high sounds".
 * - {@code requiredBPM} — target BPM. 0 = don't check.
 * - {@code exact} — if true, {@code sound} must match the playing track name.
 * - {@code tolerance} — fractional tolerance for BPM matching (0.0–1.0).
 * e.g. 0.1 = ±10% of requiredBPM is acceptable.
 *
 * Example JSON for "fast, high-pitched world audio, no specific track required":
 * 
 * <pre>
 * {
 *   "type": "phoenix:sound",
 *   "minTreble": 0.5,
 *   "requiredBPM": 160,
 *   "tolerance": 0.15
 * }
 * </pre>
 */
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

    /** Convenience: match a specific named sound with default tolerance. */
    public SoundIngredient(String soundName) {
        this(soundName, 0f, 0f, 0f, 0, true, 0.2f);
    }

    /** Convenience: match on bass energy only, no specific track. */
    public SoundIngredient(String soundName, float minBass) {
        this(soundName, minBass, 0f, 0f, 0, false, 0.2f);
    }

    public SoundIngredient copy() {
        return new SoundIngredient(soundName, minBass, minMid, minTreble, requiredBPM, exactMatch, tolerance);
    }

    // ── Legacy field accessor for code that used the old minLoudness name ──

    /** @deprecated use {@link #minBass()} */
    @Deprecated
    public float minLoudness() {
        return minBass;
    }

    // ── Serializer ──────────────────────────────────────────────────────────

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
