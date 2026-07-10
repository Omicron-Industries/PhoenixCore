#version 150

uniform sampler2D DiffuseSampler;
uniform float Time;
uniform vec2 ScreenSize;
uniform float RippleOrigins[8]; // packed vec2s: [x0,y0, x1,y1, ...]
uniform vec4  RippleAge;        // packed: x=age0, y=age1, z=age2, w=age3 — 0=just unlocked, 1=faded
uniform int   RippleCount;
uniform float NodePositions[16]; // packed vec2s

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5); }

// ── Worley/cellular noise for mycelial texture ─────────────────────────────────
float worley(vec2 p) {
    vec2 i = floor(p);
    float minDist = 1e9;
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            vec2 cell = i + vec2(x, y);
            vec2 pt   = cell + vec2(hash(cell), hash(cell + vec2(31.0, 17.0)));
            minDist   = min(minDist, length(p - pt));
        }
    }
    return minDist;
}

// ── Expanding ripple UV distortion from an origin ────────────────────────────
vec2 rippleDisplace(vec2 uv, vec2 origin, float age) {
    vec2 delta = uv - origin;
    float dist  = length(delta);
    float ringR = age * 0.55;            // ring expands outward
    float ringW = 0.045 * (1.0 - age);  // ring thins as it fades
    float power = exp(-pow((dist - ringR) / max(ringW, 0.001), 2.0));
    float fade  = 1.0 - age;
    return -normalize(delta + 0.001) * power * fade * 0.014;
}

void main() {
    vec2 uv = texCoord;

    // ── Ripple distortion from unlock events ──────────────────────────────────
    for (int i = 0; i < RippleCount && i < 4; i++) {
        uv += rippleDisplace(uv, vec2(RippleOrigins[i*2], RippleOrigins[i*2+1]), RippleAge[i]);
    }

    // ── Slow organic sway using worley noise ──────────────────────────────────
    float breathe = 0.5 + 0.5 * sin(Time * 1.8);
    float sway = (worley(uv * 6.0 + vec2(0, -Time * 0.22)) - 0.5) * 0.0018 * breathe;
    uv += vec2(sway, sway * 0.6);

    // ── Sample ────────────────────────────────────────────────────────────────
    vec4 col = texture(DiffuseSampler, uv);
    vec2 px  = 1.0 / ScreenSize;

    // ── Bioluminescent bloom: teal bloom from bright pixels ───────────────────
    // Wide soft bloom pass
    vec4 bloom = vec4(0.0);
    float bloomW = 0.0;
    for (int x = -3; x <= 3; x++) {
        for (int y = -3; y <= 3; y++) {
            float w = exp(-float(x*x + y*y) * 0.28);
            bloom += texture(DiffuseSampler, uv + vec2(x, y) * px * 3.0) * w;
            bloomW += w;
        }
    }
    bloom /= bloomW;

    // Only cyan-green-ish samples bloom (bioluminescent color selection)
    float bioLum = bloom.g * 0.65 + bloom.b * 0.35;
    float bloomMask = smoothstep(0.2, 0.65, bioLum);
    float pulse = breathe * breathe; // sharper pulse

    col.g = min(1.0, col.g + bloom.g * bloomMask * 0.28 * pulse);
    col.b = min(1.0, col.b + bloom.b * bloomMask * 0.14 * pulse);
    col.r *= 0.93;

    // ── Mycelial vein overlay: faint worley pattern tinted teal ───────────────
    float vein = smoothstep(0.28, 0.35, 1.0 - worley(texCoord * 14.0 + vec2(0, -Time * 0.08)));
    col.g = min(1.0, col.g + vein * 0.04 * pulse);

    // ── Heartbeat brightness oscillation ─────────────────────────────────────
    float hb = pow(breathe, 3.0) * 0.06 - 0.03; // -0.03 to +0.03
    col.rgb = clamp(col.rgb + hb, 0.0, 1.0);

    // ── Soft vignette ─────────────────────────────────────────────────────────
    vec2 v = texCoord - 0.5;
    col.rgb *= 1.0 - dot(v, v) * 1.1;

    fragColor = col;
}
