package net.phoenix.core.conflux.client.render;

/**
 * Single shared clock that every per-discipline animation samples from.
 *
 * Each Discipline has a distinct {@link Signature} that defines its tempo and
 * easing curve. Shape geometry, shader uniforms, and sound all read from the
 * same clock so they stay choreographed instead of drifting out of phase.
 */
public final class MotionClock {

    private float elapsed = 0f;

    public enum Signature {
        /** Explosive start, rapid exponential settle — everything urgent and volatile. */
        PHOENIX(4.2f) {
            @Override public float ease(float t) { return 1f - (float) Math.exp(-5f * t); }
        },
        /** Slow, perfectly linear, unnervingly constant — nothing accelerates or decelerates. */
        VOID(0.7f) {
            @Override public float ease(float t) { return t; }
        },
        /** Slow sine breathing — alive, rhythmic, slightly excessive. */
        SCULK(1.8f) {
            @Override public float ease(float t) {
                return (float)(0.5 - 0.5 * Math.cos(Math.PI * t));
            }
        },
        /** Stepped / glitchy — periodically lurches instead of flowing. */
        SEALED(2.1f) {
            @Override public float ease(float t) {
                return (float) Math.floor(t * 8f) / 8f;
            }
        },
        DEFAULT(2.0f) {
            @Override public float ease(float t) {
                return (float)(0.5 - 0.5 * Math.cos(Math.PI * t));
            }
        };

        public final float tempo;

        Signature(float tempo) { this.tempo = tempo; }

        /** Easing function mapping progress [0,1] → eased [0,1]. */
        public abstract float ease(float t);

        /** Elapsed time scaled to this signature's tempo. */
        public float scaled(float elapsed) { return elapsed * tempo; }

        /** Continuous sine wave [0,1] at this signature's tempo. */
        public float pulse(float elapsed) {
            return 0.5f + 0.5f * (float) Math.sin(elapsed * tempo);
        }

        /** Faster pulse for glint/sparkle. */
        public float fastPulse(float elapsed) {
            return 0.5f + 0.5f * (float) Math.sin(elapsed * tempo * 3.7f);
        }

        public static Signature forDiscipline(String id) {
            if (id == null) return DEFAULT;
            return switch (id) {
                case "phoenix"  -> PHOENIX;
                case "void"     -> VOID;
                case "sculk"    -> SCULK;
                case "sealed_a", "sealed_b" -> SEALED;
                default         -> DEFAULT;
            };
        }
    }

    public void tick(float deltaSeconds) { elapsed += deltaSeconds; }

    public float getElapsed() { return elapsed; }

    /** Global sine at an arbitrary multiplier — discipline-agnostic. */
    public float globalPulse(float multiplier) {
        return 0.5f + 0.5f * (float) Math.sin(elapsed * multiplier);
    }

    /** Cheap deterministic hash → float [0,1]. No allocation, safe in render loops. */
    public static float hash(long seed) {
        seed ^= seed >> 17;
        seed *= 0xBF58476D1CE4E5B9L;
        seed ^= seed >> 31;
        seed *= 0x94D049BB133111EBL;
        return (float) ((seed & 0x7FFFFFFFL) / (double) 0x7FFFFFFFL);
    }

    /** Color linear interpolation — no allocation. */
    public static int lerpColor(int a, int b, float t) {
        int aa = (a >> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab_ = a & 0xFF;
        int ba = (b >> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb_ = b & 0xFF;
        int ra = aa + (int)((ba - aa) * t);
        int rr = ar + (int)((br - ar) * t);
        int rg = ag + (int)((bg - ag) * t);
        int rb = ab_ + (int)((bb_ - ab_) * t);
        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
    }
}
