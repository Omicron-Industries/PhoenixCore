#version 150

in vec4 Position;
out vec2 texCoord;

uniform mat4 ProjMat;

void main() {
    // Standard Minecraft Post-Chain positioning
    vec4 outPos = ProjMat * vec4(Position.xy, 0.0, 1.0);
    gl_Position = vec4(outPos.xy, 0.2, 1.0);

    // Map the corners of the screen to 0.0 - 1.0 UV coordinates
    texCoord = Position.xy / vec2(outPos.w);
}