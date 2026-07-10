#version 150

uniform vec2  OutSize;
uniform mat4  InvViewMat;
uniform mat4  InvProjMat;
uniform vec3  PrimaryColor;
uniform vec3  SecondaryColor;
uniform float Density;
uniform float Scale;
uniform float Seed;
uniform float Time;

in  vec2 texCoord;
out vec4 fragColor;

// ── Noise primitives ──────────────────────────────────────────────────────────

float hash(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yxz + 33.33);
    return fract((p.x + p.y) * p.z);
}

// Smooth 3-D value noise
float noise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f); // Hermite smoothstep

    return mix(
        mix(mix(hash(i),            hash(i + vec3(1,0,0)), f.x),
            mix(hash(i + vec3(0,1,0)), hash(i + vec3(1,1,0)), f.x), f.y),
        mix(mix(hash(i + vec3(0,0,1)), hash(i + vec3(1,0,1)), f.x),
            mix(hash(i + vec3(0,1,1)), hash(i + vec3(1,1,1)), f.x), f.y),
        f.z);
}

// Fractal Brownian Motion — 6 octaves
float fbm(vec3 p) {
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 6; i++) {
        v += a * noise(p);
        p  = p * 2.01 + vec3(7.3, 5.9, 11.1); // avoid axis-aligned repetition
        a *= 0.5;
    }
    return v;
}

// Domain-warped fBm: the noise is warped by another layer of noise,
// producing the filament/void structure characteristic of real nebulae.
float nebulaDensity(vec3 dir) {
    vec3 p = dir * Scale + Seed;

    // First warp pass — big structural deformation
    vec3 q = vec3(fbm(p),
                  fbm(p + vec3(5.2, 1.3, 2.7)),
                  fbm(p + vec3(1.7, 9.2, 3.6)));

    // Second warp pass — fine detail on top
    vec3 r = vec3(fbm(p + 1.5 * q + vec3(1.7, 9.2, 0.0)),
                  fbm(p + 1.5 * q + vec3(8.3, 2.8, 3.1)),
                  fbm(p + 1.5 * q + vec3(5.1, 4.7, 9.0)));

    return fbm(p + 1.5 * r);
}

// ── Main ──────────────────────────────────────────────────────────────────────

void main() {
    // Reconstruct the view-space ray direction for this pixel
    vec2 ndc = texCoord * 2.0 - 1.0;
    vec4 viewDir = InvProjMat * vec4(ndc, 1.0, 1.0);
    viewDir /= viewDir.w;
    vec3 rayDir = normalize((InvViewMat * vec4(viewDir.xyz, 0.0)).xyz);

    // Slow drift over time for a living-nebula feel
    float drift = Time * 0.0003;
    float density = nebulaDensity(rayDir + drift);

    // Remap: ignore below-midpoint noise (creates the void gaps between filaments)
    float clamped = smoothstep(0.35, 0.75, density);
    float bright  = pow(clamped, 2.0); // punch highlights

    // Two-colour mixing: secondary at thin edges, primary at dense cores
    vec3 col = mix(SecondaryColor * 0.6, PrimaryColor * 1.4, clamped);

    // Star-like bright specks where the noise peaks very high
    float sparkle = pow(max(density - 0.7, 0.0) * 3.33, 4.0);
    col += sparkle * vec3(1.0, 0.9, 0.8) * 3.0;

    float alpha = bright * Density;

    fragColor = vec4(col, clamp(alpha, 0.0, 1.0));
}
