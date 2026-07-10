#version 150

uniform sampler2D InSampler;

uniform vec2  OutSize;
uniform vec2  BlackHoleScreenPos;   // screen UV [0,1]
uniform float EventHorizonRadius;   // in screen-height units
uniform float LensingStrength;      // bending coefficient
uniform float AccretionDiskBrightness;
uniform float AspectRatio;          // width / height
uniform float Time;

in  vec2 texCoord;
out vec4 fragColor;

// ── Helpers ───────────────────────────────────────────────────────────────────

float smoothEdge(float inner, float outer, float x) {
    return 1.0 - smoothstep(inner, outer, x);
}

// Simple hash — used to break up the accretion disk uniformity
float hash(float n) {
    return fract(sin(n) * 43758.5453123);
}

// ── Main ──────────────────────────────────────────────────────────────────────

void main() {
    vec2 uv = texCoord;

    // Correct for screen aspect ratio so the black hole is circular
    vec2 toHole = BlackHoleScreenPos - uv;
    toHole.x *= AspectRatio;
    float dist = length(toHole);

    // ── Event horizon: pure gravitational void ────────────────────────────────
    if (dist < EventHorizonRadius) {
        // Soften the hard edge very slightly (anti-alias)
        float edge = smoothstep(EventHorizonRadius * 0.95, EventHorizonRadius, dist);
        fragColor = vec4(0.0, 0.0, 0.0, 1.0 - edge * 0.01);
        return;
    }

    // ── Gravitational lensing ─────────────────────────────────────────────────
    // Classic 1/r² bending toward the hole, scaled by LensingStrength
    float deflection = LensingStrength / (dist * dist);
    vec2 deflDir = normalize(toHole);
    // Undo aspect correction for UV sampling
    deflDir.x /= AspectRatio;
    vec2 lensedUV = clamp(uv + deflDir * deflection, 0.001, 0.999);
    vec4 sky = texture(InSampler, lensedUV);

    // ── Gravitational redshift ────────────────────────────────────────────────
    // Light climbing out of a gravity well loses energy (gets redder)
    float gravDepth = EventHorizonRadius / max(dist, 0.0001);
    sky.r = min(sky.r * (1.0 + gravDepth * 0.4), 1.0);
    sky.g = sky.g * max(1.0 - gravDepth * 0.1, 0.0);
    sky.b = sky.b * max(1.0 - gravDepth * 0.2, 0.0);

    // ── Photon sphere ─────────────────────────────────────────────────────────
    // Ring of captured photons at ~1.5× Schwarzschild radius; very bright
    float photonR     = EventHorizonRadius * 1.5;
    float photonWidth = EventHorizonRadius * 0.18;
    float photon = exp(-pow((dist - photonR) / photonWidth, 2.0) * 12.0);
    sky.rgb += photon * 2.5;

    // ── Accretion disk ────────────────────────────────────────────────────────
    // Flat ring in the "equatorial plane" (fake 2D).
    // Doppler shift: approaching side (bottom of screen) is hot-white;
    //               receding side is dim red-orange.
    float diskInner = EventHorizonRadius * 1.15;
    float diskOuter = EventHorizonRadius * 4.0;

    if (dist > diskInner && dist < diskOuter) {
        float t = (dist - diskInner) / (diskOuter - diskInner);

        // Angle around the hole — drives Doppler effect
        float angle = atan(toHole.y, toHole.x / AspectRatio);

        // Animate the disk slowly
        float animAngle = angle + Time * 0.4;

        // Doppler brightening on the approaching limb
        float doppler  = 0.5 + 0.5 * cos(animAngle);
        // Thin clumpy structure along the disk
        float structure = 0.6 + 0.4 * sin(animAngle * 3.0 + hash(floor(dist * 120.0)) * 6.28);

        // Radial fade: bright at inner edge, fades outward
        float radialFade = pow(1.0 - t, 1.8) * smoothstep(0.0, 0.05, t);

        // Colour: hot white/yellow at inner edge → deep orange at outer
        vec3 innerCol = vec3(1.00, 0.92, 0.70) * 5.0;
        vec3 outerCol = vec3(0.90, 0.25, 0.03) * 1.2;
        vec3 diskCol  = mix(innerCol, outerCol, t);

        diskCol *= radialFade * (0.4 + 0.6 * doppler) * structure * AccretionDiskBrightness;
        sky.rgb += diskCol;
    }

    fragColor = vec4(sky.rgb, 1.0);
}
