#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec2 UV2;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec4 ColorModulator;

out vec2 texCoord0;
out vec4 vertexColor;
out vec3 vertexNormal;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    texCoord0 = UV0;
    vertexColor = Color * ColorModulator;
    vertexNormal = normalize(mat3(ModelViewMat) * Normal);
}