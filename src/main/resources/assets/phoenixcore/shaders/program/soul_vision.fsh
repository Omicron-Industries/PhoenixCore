#version 150

uniform sampler2D DiffuseSampler;
uniform float Saturation;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));

    // Mix between full color and grayscale based on Saturation (0.0 to 1.0)
    fragColor = vec4(mix(vec3(gray), color.rgb, Saturation), color.a);
}
