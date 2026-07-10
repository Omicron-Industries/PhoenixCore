#version 150

uniform sampler2D DiffuseSampler;
uniform float Time;
uniform vec2 ScreenSize;
uniform float HeatSources[16];  // packed vec2s: [x0,y0, x1,y1, ...]
uniform float HeatStrength[8];  // intensity per node [0,1]
uniform int   HeatCount;
uniform vec2  CursorUV;         // cursor in screen UV

in vec2 texCoord;
out vec4 fragColor;

// ── Noise ─────────────────────────────────────────────────────────────────────
float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}
float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash(i), hash(i + vec2(1,0)), f.x),
        mix(hash(i + vec2(0,1)), hash(i + vec2(1,1)), f.x),
        f.y
    );
}

// ── Distortion from a single heat source ──────────────────────────────────────
vec2 heatDisplace(vec2 uv, vec2 src, float str, float t) {
    vec2 delta = uv - src;
    float dist = length(delta);
    if (dist < 0.001) return vec2(0.0);

    // Inverse square falloff — strongest right on the node, fades fast
    float falloff = str / (dist * dist * 180.0 + 1.0);

    // Spiral thermal column: rise + twist
    float angle  = atan(delta.y, delta.x);
    float rise   = sin(t * 3.8 + dist * 22.0) * 0.012 * falloff;
    float twist  = cos(t * 2.1 + dist * 15.0) * 0.007 * falloff;

    // Additional Perlin shimmer
    float shimmer = noise(uv * 18.0 + vec2(0, -t * 0.7)) * 0.006 * falloff;

    return vec2(
        twist * cos(angle) - rise * sin(angle),
        rise  * cos(angle) + twist * sin(angle) + shimmer
    );
}

void main() {
    vec2 uv = texCoord;

    // ── Per-node heat distortion ───────────────────────────────────────────────
    for (int i = 0; i < HeatCount && i < 8; i++) {
        uv += heatDisplace(uv, vec2(HeatSources[i*2], HeatSources[i*2+1]), HeatStrength[i], Time);
    }

    // ── Cursor heat shimmer ───────────────────────────────────────────────────
    uv += heatDisplace(uv, CursorUV, 0.35, Time);

    // ── Global heat haze (scrolling noise) ────────────────────────────────────
    float globalPulse = 0.5 + 0.5 * sin(Time * 1.3);
    uv.x += (noise(vec2(uv.y * 9.0, Time * 0.5)) - 0.5) * 0.0012 * globalPulse;
    uv.y -= noise(vec2(uv.x * 7.0, Time * 0.35)) * 0.0008 * globalPulse;

    vec4 col = texture(DiffuseSampler, uv);

    // ── Bloom approximation: sample neighbors ──────────────────────────────────
    vec2 px = 1.0 / ScreenSize;
    vec4 bloom = vec4(0.0);
    float bloomW = 0.0;
    for (int x = -2; x <= 2; x++) {
        for (int y = -2; y <= 2; y++) {
            float w = exp(-float(x*x + y*y) * 0.45);
            bloom += texture(DiffuseSampler, uv + vec2(x, y) * px * 2.5) * w;
            bloomW += w;
        }
    }
    bloom /= bloomW;

    // Additive ember bloom: only bright warm-colored samples contribute
    float warmLum = bloom.r * 0.6 + bloom.g * 0.3;
    float bloomMask = smoothstep(0.25, 0.7, warmLum);
    col.r = min(1.0, col.r + bloom.r * bloomMask * 0.35 * globalPulse);
    col.g = min(1.0, col.g + bloom.g * bloomMask * 0.15 * globalPulse);

    // ── Thermal tinting ───────────────────────────────────────────────────────
    float lum = dot(col.rgb, vec3(0.2126, 0.7152, 0.0722));
    col.r = min(1.0, col.r + lum * 0.08 * globalPulse);
    col.g *= 0.95;
    col.b *= 0.82;

    // ── Vignette ──────────────────────────────────────────────────────────────
    vec2 v = texCoord - 0.5;
    col.rgb *= 1.0 - dot(v, v) * 0.7;

    fragColor = col;
}
