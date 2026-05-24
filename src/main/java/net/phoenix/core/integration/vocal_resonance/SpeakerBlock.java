package net.phoenix.core.integration.vocal_resonance;

import net.minecraft.world.level.block.Block;

import lombok.Getter;

public class SpeakerBlock extends Block {

    @Getter
    private final ISpeakerType speakerType;

    public SpeakerBlock(Properties properties, ISpeakerType speakerType) {
        super(properties);
        this.speakerType = speakerType;
    }
}
