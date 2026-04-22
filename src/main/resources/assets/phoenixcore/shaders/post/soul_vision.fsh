#version 150

uniform sampler2D DiffuseSampler;
uniform float Saturation;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));

    vec3 finalColor;
    if (Saturation > 1.0) {
        // Vibrant Purple logic: Boost saturation and tint
        vec3 purpleTint = vec3(1.2, 0.8, 1.5);
        finalColor = color.rgb * purpleTint * (Saturation * 0.8);
    } else {
        // Standard Grayscale logic
        finalColor = mix(vec3(gray), color.rgb, Saturation);
    }

    fragColor = vec4(finalColor, color.a);
}
