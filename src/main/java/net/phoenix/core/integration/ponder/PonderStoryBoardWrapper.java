package net.phoenix.core.integration.ponder;

import net.createmod.ponder.api.scene.PonderStoryBoard;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.phoenix.core.integration.ponder.api.ExtendedPonderStoryBoard;
import net.phoenix.core.integration.ponder.api.ExtendedSceneBuilder;
import net.phoenix.core.integration.ponder.api.SceneBuildingUtilDelegate;
import net.phoenix.core.integration.ponder.util.PonderErrorHelper;

public class PonderStoryBoardWrapper implements PonderStoryBoard {

    private final ExtendedPonderStoryBoard storyBoard;

    protected PonderStoryBoardWrapper(ExtendedPonderStoryBoard storyBoard) {
        this.storyBoard = storyBoard;
    }

    @Override
    public void program(SceneBuilder builder, SceneBuildingUtil util) {
        try {
            // Wrap the builder directly — don't extract the scene and make a new builder
            ExtendedSceneBuilder extended = new ExtendedSceneBuilder(builder);
            storyBoard.program(extended, new SceneBuildingUtilDelegate(util));
        } catch (Exception e) {
            PonderErrorHelper.yeet(e);
        }
    }
}
