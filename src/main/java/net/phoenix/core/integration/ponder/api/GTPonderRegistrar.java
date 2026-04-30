package net.phoenix.core.integration.ponder.api;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.ponder.PonderBuilder;
import net.phoenix.core.integration.ponder.PonderRegistrationManager;
import net.phoenix.core.integration.ponder.multiblocks.GTPonderMultiblocks;
import net.phoenix.core.integration.ponder.multiblocks.GTPonderProcesses;

import java.util.Map;
import java.util.function.Consumer;

public class GTPonderRegistrar {

    public static void registerGTPonderScene(
                                             PonderBuilder builder,
                                             String target,
                                             String sceneId,
                                             String title,
                                             Map<String, Object> options,
                                             Consumer<GTPonderContext> callback) {
        Map<String, Object> sceneOptions = GTPonderAPI.gtPonderOptions(options);

        ResourceLocation itemId = GTPonderAPI.gtPonderResourceLocation(target);
        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null) {
            PhoenixCore.LOGGER.error("GTPonder: Unknown item: " + itemId);
            return;
        }

        ResourceLocation structureId = GTPonderAPI.gtPonderResourceLocation(
                sceneOptions.getOrDefault("structure", GTPonderAPI.GTPONDER_DEFAULT_STRUCTURE_ID));

        builder.forItems(Ingredient.of(item)).scene(
                sceneId,
                title,
                structureId.toString(),
                (sceneBuilder, util) -> {
                    // 1. WRAP the SceneBuilder into your ExtendedSceneBuilder
                    ExtendedSceneBuilder extendedScene = new ExtendedSceneBuilder(sceneBuilder);

                    // 2. Configure the scene using the wrapped version
                    int basePlateSize = (int) GTPonderAPI.gtPonderNumber(
                            sceneOptions.get("basePlateSize"),
                            GTPonderAPI.gtPonderNumber(sceneOptions.get("basePlate"),
                                    GTPonderAPI.GTPONDER_DEFAULT_BASE_PLATE_SIZE));

                    extendedScene.configureBasePlate(
                            (int) GTPonderAPI.gtPonderNumber(sceneOptions.get("basePlateX"), 0),
                            (int) GTPonderAPI.gtPonderNumber(sceneOptions.get("basePlateZ"), 0),
                            basePlateSize);

                    if (sceneOptions.get("scale") != null) {
                        extendedScene
                                .scaleSceneView((float) GTPonderAPI.gtPonderNumber(sceneOptions.get("scale"), 1.0));
                    }

                    if (sceneOptions.get("offsetY") != null) {
                        extendedScene
                                .setSceneOffsetY((float) GTPonderAPI.gtPonderNumber(sceneOptions.get("offsetY"), 0.0));
                    }

                    if (!Boolean.FALSE.equals(sceneOptions.get("showBasePlate"))) {
                        extendedScene.showBasePlate();
                    }

                    if (sceneOptions.get("initialIdle") != null) {
                        extendedScene.idle((int) GTPonderAPI.gtPonderNumber(sceneOptions.get("initialIdle"), 3));
                    }

                    // 3. EXECUTE the callback with the Extended Scene
                    // This is where renderMultiblock() is called!
                    callback.accept(new GTPonderContext(extendedScene, util, sceneOptions));

                    // 4. FINAL VISIBILITY CHECK
                    // If no blocks are visible, ensure the structure section is shown
                    if (!Boolean.FALSE.equals(sceneOptions.get("showSection"))) {
                        extendedScene.world().showSection(util.select().layersFrom(0), Direction.UP);
                    }

                    if (!Boolean.FALSE.equals(sceneOptions.get("markAsFinished"))) {
                        extendedScene.markAsFinished();
                    }
                });
    }

    // This method will be called to register all GTPonder scenes
    public static void registerAllGTPonderScenes() {
        PonderRegistrationManager.register(event -> {
            // registerAllMultiblockScenes iterates every GT MultiblockMachineDefinition and
            // calls addGeneratedMultiblockScene() for each one — this is what populates the
            // Ponder index. The old register() stub is intentionally empty and must NOT be
            // called here.
            GTPonderMultiblocks.registerAllMultiblockScenes(event);
            GTPonderProcesses.register(event);
        });
    }
}
