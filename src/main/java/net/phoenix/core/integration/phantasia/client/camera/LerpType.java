package net.phoenix.core.integration.phantasia.client.camera;

public enum LerpType {

    SNAP,
    LINEAR,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT,
    SMOOTHSTEP,

    // New Sine Easing Types
    SINE_IN,
    SINE_OUT,
    SINE_IN_OUT;

    public static LerpType fromString(String s) {
        if (s == null) return SNAP; //
        return switch (s.toUpperCase(java.util.Locale.ROOT)) { //
            case "LINEAR" -> LINEAR; //
            case "EASE_IN" -> EASE_IN;
            case "EASE_OUT" -> EASE_OUT; //
            case "EASE_IN_OUT" -> EASE_IN_OUT; //
            case "SMOOTHSTEP" -> SMOOTHSTEP;
            case "SINE_IN" -> SINE_IN;
            case "SINE_OUT" -> SINE_OUT;
            case "SINE_IN_OUT" -> SINE_IN_OUT;
            default -> SNAP; //
        };
    }
}
