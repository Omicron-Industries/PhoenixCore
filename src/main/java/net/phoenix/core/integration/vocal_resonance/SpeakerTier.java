package net.phoenix.core.integration.vocal_resonance;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SpeakerTier implements ISpeakerType {

    LV("LV", 4, 1.1f),
    MV("MV", 8, 1.25f),
    HV("HV", 16, 1.5f),
    EV("EV", 24, 1.8f),
    IV("IV", 32, 2.2f),
    LuV("LuV", 48, 2.7f),
    ZPM("ZPM", 64, 3.5f),
    UV("UV", 128, 5.0f);

    private final String name;
    private final int rangeBonus;
    private final float resonanceAmplifier;
}
