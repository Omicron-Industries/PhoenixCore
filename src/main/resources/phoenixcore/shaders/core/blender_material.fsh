#version 150

uniform sampler2D Sampler0; // Albedo
uniform sampler2D Sampler1; // Normal Map (_n)
uniform sampler2D Sampler2; // Specular Map (_s)

in vec2 texCoord0;
in vec4 vertexColor;
in vec3 vertexNormal;

out vec4 fragColor;

void main() {
    vec4 albedo = texture(Sampler0, texCoord0) * vertexColor;
    if (albedo.a < 0.1) discard;

    // 1. NORMAL MAPPING
    // Sample the _n texture and convert from 0..1 to -1..1
    vec3 nMap = texture(Sampler1, texCoord0).rgb * 2.0 - 1.0;
    // Blend the map with the 3D model's vertex normal
    vec3 normal = normalize(vertexNormal + nMap);

    // 2. SPECULAR & METALLIC
    vec4 specData = texture(Sampler2, texCoord0);
    float metallic = specData.r;  // Red channel = How much like metal
    float roughness = specData.g; // Green channel = How blurry reflections are

    // 3. LIGHTING (Directional Light from 'The Star')
    vec3 lightDir = normalize(vec3(0.5, 1.0, 0.5));
    float diff = max(dot(normal, lightDir), 0.3); // Ambient base of 0.3

    // 4. SPECULAR HIGHLIGHT (Blinn-Phong)
    vec3 viewDir = vec3(0.0, 0.0, 1.0);
    vec3 halfDir = normalize(lightDir + viewDir);
    float specPower = pow(max(dot(normal, halfDir), 0.0), (1.0 - roughness) * 128.0);
    vec3 specular = vec3(specPower) * metallic;

    // 5. COMBINE
    vec3 finalRGB = (albedo.rgb * diff) + specular;

    fragColor = vec4(finalRGB, albedo.a);
}