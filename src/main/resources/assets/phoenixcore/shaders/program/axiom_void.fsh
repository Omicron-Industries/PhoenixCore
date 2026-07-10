#version 150

uniform sampler2D DiffuseSampler;
uniform float Time;
uniform vec2 ScreenSize;
uniform vec2 CursorUV;
uniform float NodePositions[16]; // packed vec2s: [x0,y0, x1,y1, ...]
uniform int   NodeCount;

in vec2 texCoord;
out vec4 fragColor;

float hash(float n) { return fract(sin(n) * 43758.5453); }
float hash2(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5); }

// ── Gravitational lensing from a point ────────────────────────────────────────
vec2 gravLens(vec2 uv, vec2 src, float mass) {
    vec2 delta = uv - src;
    float dist = length(delta);
    if (dist < 0.001) return vec2(0.0);
    // Einstein ring equation approximation: deflection ∝ mass/dist, directed inward
    float deflect = mass / (dist * 14.0 + 0.5);
    return -normalize(delta) * deflect * 0.018;
}

void main() {
    vec2 uv = texCoord;

    // ── Node gravitational lensing ────────────────────────────────────────────
    for (int i = 0; i < NodeCount && i < 8; i++) {
        uv += gravLens(uv, vec2(NodePositions[i*2], NodePositions[i*2+1]), 0.4);
    }

    // ── Cursor singularity — strongest near cursor, fades instantly ───────────
    uv += gravLens(uv, CursorUV, 0.9);

    // ── Subtle linear drift (Void is unnervingly constant, never accelerates) ──
    float drift = 0.0006;
    uv.x += sin(uv.y * 8.3 + Time * 0.7) * drift;
    uv.y += cos(uv.x * 10.1 + Time * 0.7) * drift;

    // ── Scanline tear: random horizontal bands shift sideways ─────────────────
    float strip    = floor(uv.y * 260.0);
    float stepTime = floor(Time * 10.0);
    float glitchH  = hash2(vec2(strip, stepTime));
    if (glitchH > 0.965) {
        float shift = (hash2(vec2(strip * 2.0, stepTime)) - 0.5) * 0.016;
        uv.x = fract(uv.x + shift);
    }

    // ── RGB channel desync ────────────────────────────────────────────────────
    // Channels split slightly — worse near screen edges (wrongness is peripheral)
    float edgeDist = length(texCoord - 0.5) * 2.0;
    float splitMag = edgeDist * 0.004 + (glitchH > 0.965 ? 0.006 : 0.0);

    vec4 col;
    col.r = texture(DiffuseSampler, uv + vec2( splitMag, 0.0)).r;
    col.g = texture(DiffuseSampler, uv).g;
    col.b = texture(DiffuseSampler, uv - vec2( splitMag, 0.0)).b;
    col.a = texture(DiffuseSampler, uv).a;

    // ── Cold desaturate + indigo push ─────────────────────────────────────────
    float lum = dot(col.rgb, vec3(0.2126, 0.7152, 0.0722));
    col.rgb = mix(col.rgb, vec3(lum), 0.18);
    col.b   = min(1.0, col.b + 0.05);
    col.r  *= 0.88;
    col.g  *= 0.95;

    // ── Vignette — heavier than Phoenix, claustrophobic ───────────────────────
    vec2 v = texCoord - 0.5;
    float vig = 1.0 - dot(v, v) * 2.0;
    col.rgb *= max(0.0, vig);

    fragColor = col;
}
