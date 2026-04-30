#version 150

uniform sampler2D Sampler0;
uniform vec2 ScreenSize;
uniform vec2 BlackHolePos;
uniform float BlackHoleRadius;
uniform float DistortionStrength;
uniform float GameTime;

in vec2 texCoord0;
out vec4 fragColor;

void main() {
    vec2 uv = texCoord0;
    float aspect = ScreenSize.x / ScreenSize.y;
    vec2 delta = uv - BlackHolePos;
    delta.x *= aspect;

    float dist = length(delta);
    float rs = max(0.0005, BlackHoleRadius);

    // Outside influence radius: show the scene
    float influence = rs * 6.0;
    if (dist >= influence) {
        fragColor = texture(Sampler0, uv);
        return;
    }

    // Event horizon: pure black
    if (dist <= rs) {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }

    // Smooth falloff
    float falloff = smoothstep(influence, rs, dist);

    // Lensing distortion
    vec2 dir = normalize(delta);
    float denom = max(0.0025, dist - rs);
    float distortion = (rs * rs) / denom;
    distortion *= (0.25 + 0.75 * falloff) * DistortionStrength;

    vec2 warp = dir * distortion;
    warp.x /= aspect;

    vec2 warpedUV = uv - warp;
    warpedUV = clamp(warpedUV, vec2(0.001), vec2(0.999));

    fragColor = texture(Sampler0, warpedUV);
}
