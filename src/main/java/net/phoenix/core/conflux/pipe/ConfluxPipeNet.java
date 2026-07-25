package net.phoenix.core.conflux.pipe;

import com.gregtechceu.gtceu.api.pipenet.PipeNet;

import net.minecraft.nbt.CompoundTag;

public class ConfluxPipeNet extends PipeNet<ConfluxPipeData> {

    public ConfluxPipeNet(LevelConfluxPipeNet world) {
        super(world);
    }

    @Override
    protected void writeNodeData(ConfluxPipeData data, CompoundTag tag) {}

    @Override
    protected ConfluxPipeData readNodeData(CompoundTag tag) {
        return ConfluxPipeData.INSTANCE;
    }
}
