package net.phoenix.core.integration.ponder.api;

import net.createmod.ponder.api.scene.PositionUtil;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.SelectionUtil;
import net.createmod.ponder.api.scene.VectorUtil;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class SceneBuildingUtilDelegate implements SceneBuildingUtil {

    private final SceneBuildingUtil util;

    public SceneBuildingUtilDelegate(SceneBuildingUtil util) {
        this.util = util;
    }

    @Override
    public SelectionUtil select() {
        return util.select();
    }

    @Override
    public VectorUtil vector() {
        return util.vector();
    }

    @Override
    public PositionUtil grid() {
        return util.grid();
    }

    // Convenience getters kept for backwards compat
    public SelectionUtil getSelect() {
        return util.select();
    }

    public VectorUtil getVector() {
        return util.vector();
    }

    public PositionUtil getGrid() {
        return util.grid();
    }

    public BlockState getDefaultState(Block block) {
        return block.defaultBlockState();
    }
}
