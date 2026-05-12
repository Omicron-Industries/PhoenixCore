package net.phoenix.core.integration.vocal_resonance;

public interface ISpeakerType {
    String getName();
    int getRangeBonus(); // Blocks added to the radius
    float getResonanceAmplifier(); // Multiplier for "Sonic Energy"
}