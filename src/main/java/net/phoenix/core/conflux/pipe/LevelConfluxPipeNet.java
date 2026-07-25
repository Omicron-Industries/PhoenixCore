package net.phoenix.core.conflux.pipe;

import com.gregtechceu.gtceu.api.pipenet.LevelPipeNet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

public class LevelConfluxPipeNet extends LevelPipeNet<ConfluxPipeData, ConfluxPipeNet> {

    private static final String DATA_ID = "phoenix_conflux_pipe_net";

    public static LevelConfluxPipeNet getOrCreate(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                tag -> new LevelConfluxPipeNet(level, tag),
                () -> new LevelConfluxPipeNet(level),
                DATA_ID
        );
    }

    public LevelConfluxPipeNet(ServerLevel level) {
        super(level);
    }

    public LevelConfluxPipeNet(ServerLevel level, CompoundTag tag) {
        super(level, tag);
    }

    @Override
    protected ConfluxPipeNet createNetInstance() {
        return new ConfluxPipeNet(this);
    }
}
