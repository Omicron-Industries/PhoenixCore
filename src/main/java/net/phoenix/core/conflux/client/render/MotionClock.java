package net.phoenix.core.conflux.client.render;

public final class MotionClock {

    private float elapsed = 0f;

    public enum Signature {

        PHOENIX(4.2f) {

            @Override
            public float ease(float t) {
                return 1f - (float) Math.exp(-5f * t);
            }
        },

        VOID(0.7f) {

            @Override
            public float ease(float t) {
                return t;
            }
        },

        SCULK(1.8f) {

            @Override
            public float ease(float t) {
                return (float) (0.5 - 0.5 * Math.cos(Math.PI * t));
            }
        },

        SEALED(2.1f) {

            @Override
            public float ease(float t) {
                return (float) Math.floor(t * 8f) / 8f;
            }
        },
        DEFAULT(2.0f) {

            @Override
            public float ease(float t) {
                return (float) (0.5 - 0.5 * Math.cos(Math.PI * t));
            }
        };

        public final float tempo;

        Signature(float tempo) {
            this.tempo = tempo;
        }

        public abstract float ease(float t);

        public float scaled(float elapsed) {
            return elapsed * tempo;
        }

        public float pulse(float elapsed) {
            return 0.5f + 0.5f * (float) Math.sin(elapsed * tempo);
        }

        public float fastPulse(float elapsed) {
            return 0.5f + 0.5f * (float) Math.sin(elapsed * tempo * 3.7f);
        }

        public static Signature forDiscipline(String id) {
            if (id == null) return DEFAULT;
            return switch (id) {
                case "phoenix" -> PHOENIX;
                case "void" -> VOID;
                case "sculk" -> SCULK;
                case "sealed_a", "sealed_b" -> SEALED;
                default -> DEFAULT;
            };
        }
    }

    public void tick(float deltaSeconds) {
        elapsed += deltaSeconds;
    }

    public float getElapsed() {
        return elapsed;
    }

    public float globalPulse(float multiplier) {
        return 0.5f + 0.5f * (float) Math.sin(elapsed * multiplier);
    }

    public static float hash(long seed) {
        seed ^= seed >> 17;
        seed *= 0xBF58476D1CE4E5B9L;
        seed ^= seed >> 31;
        seed *= 0x94D049BB133111EBL;
        return (float) ((seed & 0x7FFFFFFFL) / (double) 0x7FFFFFFFL);
    }

    public static int lerpColor(int a, int b, float t) {
        int aa = (a >> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab_ = a & 0xFF;
        int ba = (b >> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb_ = b & 0xFF;
        int ra = aa + (int) ((ba - aa) * t);
        int rr = ar + (int) ((br - ar) * t);
        int rg = ag + (int) ((bg - ag) * t);
        int rb = ab_ + (int) ((bb_ - ab_) * t);
        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
    }
}
