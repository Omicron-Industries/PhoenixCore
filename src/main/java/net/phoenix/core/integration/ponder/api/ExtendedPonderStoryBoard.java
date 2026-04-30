package net.phoenix.core.integration.ponder.api;

@FunctionalInterface
public interface ExtendedPonderStoryBoard {

    void program(ExtendedSceneBuilder scene, SceneBuildingUtilDelegate util);
}
