#version 150

uniform sampler2D DiffuseSampler;
uniform float Time;
uniform vec2 ScreenSize;

in vec2 texCoord;
out vec4 fragColor;

float hash(float n)  { return fract(sin(n) * 43758.5453); }
float hash2(vec2 p)  { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5); }

// ── Signed distance for a regular polygon ─────────────────────────────────────
float sdPolygon(vec2 p, float r, int n) {
    float angle = 6.28318 / float(n);
    float a = atan(p.y, p.x) + 3.14159;
    float sector = floor(a / angle);
    float t = mod(a, angle) - angle * 0.5;
    return cos(t) * length(p) - r;
}

void main() {
    vec2 uv = texCoord;

    // ── Stepped time — Sealed easing is stepped/glitchy ───────────────────────
    float stepTime = floor(Time * 7.0) / 7.0;
    float subStep  = fract(Time * 7.0); // 0→1 between steps

    // ── Horizontal strip tearing ──────────────────────────────────────────────
    float strip    = floor(uv.y * 280.0);
    float gH       = hash2(vec2(strip, stepTime));
    float tearProb = 0.96 + 0.03 * sin(Time * 0.3); // rarer during quiet phase
    vec2 dispUV    = uv;
    float aberr    = 0.0;
    if (gH > tearProb) {
        float shift = (hash2(vec2(strip * 3.0, stepTime)) - 0.5) * 0.022;
        dispUV.x    = fract(dispUV.x + shift);
        aberr       = abs(shift) * 2.2;
    }

    // ── Block corruption: random tiles replaced with solid noise ─────────────
    vec2 tile    = floor(uv * vec2(40.0, 25.0));
    float tileH  = hash2(vec2(tile.x + stepTime * 7.1, tile.y * 3.3));
    bool corrupt = tileH > 0.985;

    if (corrupt) {
        // Fill this tile with grey noise — like a dead pixel cluster
        float noiseVal = hash2(tile + vec2(stepTime));
        fragColor = vec4(vec3(noiseVal * 0.3), 1.0);
        return;
    }

    // ── Chromatic aberration with per-channel UV ──────────────────────────────
    float edgePush = length(texCoord - 0.5) * aberr * 0.5;
    vec4 col;
    col.r = texture(DiffuseSampler, dispUV + vec2( aberr + edgePush, 0.0)).r;
    col.g = texture(DiffuseSampler, dispUV).g;
    col.b = texture(DiffuseSampler, dispUV - vec2( aberr + edgePush, 0.0)).b;
    col.a = 1.0;

    // ── CRT scanlines ─────────────────────────────────────────────────────────
    float scan = sin(texCoord.y * ScreenSize.y * 3.14159) * 0.5 + 0.5;
    col.rgb   *= 0.88 + 0.12 * scan;

    // ── Cold grey desaturation ────────────────────────────────────────────────
    float lum = dot(col.rgb, vec3(0.2126, 0.7152, 0.0722));
    col.rgb   = mix(col.rgb, vec3(lum), 0.35);

    // ── Sigil-shaped vignette (hexagonal falloff instead of circular) ─────────
    vec2 vp    = texCoord - 0.5;
    float poly = sdPolygon(vp, 0.42, 6);
    float vig  = 1.0 - smoothstep(0.0, 0.12, poly);
    col.rgb   *= vig;

    // ── Occasional full-screen red flash on step boundary ─────────────────────
    if (subStep < 0.04 && hash(stepTime) > 0.94) {
        col.rgb = mix(col.rgb, vec3(0.4, 0.0, 0.0), 0.15);
    }

    fragColor = col;
}
