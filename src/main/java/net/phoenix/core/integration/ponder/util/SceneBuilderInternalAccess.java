package net.phoenix.core.integration.ponder.util;

import net.createmod.ponder.api.scene.SpecialInstructions;
import net.createmod.ponder.api.scene.WorldInstructions;
import net.createmod.ponder.foundation.PonderScene;

// NOT a mixin — plain interface for casting in normal code
public interface SceneBuilderInternalAccess {

    PonderScene ponderjs$getPonderScene();

    void ponderjs$setWorldInstructions(WorldInstructions worldInstructions);

    void ponderjs$setSpecialInstructions(SpecialInstructions specialInstructions);
}
