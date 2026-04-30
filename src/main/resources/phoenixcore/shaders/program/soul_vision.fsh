#version 150

// This is a simplified test shader.
// It ignores all inputs and should make the screen solid magenta.

out vec4 fragColor;

void main() {
    // Output solid magenta (Red=1.0, Green=0.0, Blue=1.0)
    fragColor = vec4(1.0, 0.0, 1.0, 1.0);
}
