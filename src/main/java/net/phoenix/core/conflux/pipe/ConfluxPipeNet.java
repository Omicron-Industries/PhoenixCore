package net.phoenix.core.conflux.pipe;

import com.gregtechceu.gtceu.api.pipenet.PipeNet;
import net.minecraft.nbt.CompoundTag;

/**
 * Connected-component network for Conflux data pipes.
 * Node data ({@link ConfluxPipeData}) is stateless, so serialisation is a no-op.
 */
public class ConfluxPipeNet extends PipeNet<ConfluxPipeData> {

    public ConfluxPipeNet(LevelConfluxPipeNet world) {
        super(world);
    }

    @Override
    protected void writeNodeData(ConfluxPipeData data, CompoundTag tag) {
        // stateless — nothing to write
    }

    @Override
    protected ConfluxPipeData readNodeData(CompoundTag tag) {
        return ConfluxPipeData.INSTANCE;
    }
}
