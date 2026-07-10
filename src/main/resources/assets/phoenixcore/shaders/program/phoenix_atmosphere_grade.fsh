#version 150

uniform sampler2D InSampler;

uniform vec2  OutSize;
uniform float Saturation;       // 0 = greyscale, 1 = full colour
uniform float TintStrength;     // 0 = no tint, 1 = full tint
uniform vec3  TintColor;        // RGB of the tint
uniform float VignetteStrength; // 0 = none, 1 = heavy dark edges
uniform float Brightness;       // 1 = neutral

in  vec2 texCoord;
out vec4 fragColor;

// NTSC luminance weights for perceptually correct greyscale
const vec3 LUMA = vec3(0.299, 0.587, 0.114);

void main() {
    vec4 col = texture(InSampler, texCoord);

    // ── Saturation ────────────────────────────────────────────────────────────
    float luma = dot(col.rgb, LUMA);
    col.rgb = mix(vec3(luma), col.rgb, Saturation);

    // ── Colour tint ───────────────────────────────────────────────────────────
    // Additive blend toward the tint colour — doesn't darken the scene
    col.rgb = mix(col.rgb, col.rgb + TintColor * luma, TintStrength);

    // ── Brightness ────────────────────────────────────────────────────────────
    col.rgb *= Brightness;

    // ── Vignette ──────────────────────────────────────────────────────────────
    // Smooth dark oval toward screen edges
    vec2 centered = texCoord - 0.5;
    float vDist   = length(centered * vec2(1.0, 1.35)); // slight vertical bias
    float vignette = 1.0 - smoothstep(0.35, 0.80, vDist) * VignetteStrength;
    col.rgb *= vignette;

    fragColor = vec4(col.rgb, col.a);
}
