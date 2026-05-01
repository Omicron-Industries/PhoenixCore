package net.phoenix.core.integration.ponder.multiblocks;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.phoenix.core.integration.ponder.PonderBuilder;
import net.phoenix.core.integration.ponder.api.GTPonderContext;
import net.phoenix.core.integration.ponder.api.GTPonderRegistrar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GTPonderProcesses {


    public static final ResourceLocation PROCESS_PONDER_STRUCTURE_ID = new ResourceLocation("phoenixcore",
            "blank_48");

    public static final int PROCESS_BASE_PLATE_SIZE = 9;
    public static final int PROCESS_TEXT_DURATION = 68;
    public static final int PROCESS_STEP_IDLE = 16;
    public static final int PROCESS_ITEM_CUE_DURATION = 36;
    public static final int PROCESS_FLOW_LINE_DURATION = 34;

    // Collect all scene titles so PhoenixPonderPlugin.registerSharedText can expose them
    // Key = scene name path (e.g. "processes/glue_early_routes"), Value = title string
    public static final java.util.LinkedHashMap<String, String> SCENE_TITLES = new java.util.LinkedHashMap<>();

    public static void register(PonderBuilder builder) {
        registerGlueEarlyRoutesScene(builder);
        registerGlueResinRoutesScene(builder);
    }

    public static void registerManualTest(PonderBuilder builder) {
        // We use the underlying 'event' if possible, or just the builder
        builder.forItems(Ingredient.of(Items.GOLD_BLOCK))
                .scene("test_raw_api", "Raw API Test", "phoenixcore:blank_48", (scene, util) -> {

                    // --- 1. THE SETUP (MUST BE IN THIS ORDER) ---
                    // This defines the 9x9 grid inside the 48x48 area
                    scene.configureBasePlate(0, 0, 9);

                    // This anchors the camera and tells Ponder to DRAW the grid
                    scene.showBasePlate();

                    // Wait for the renderer to catch up
                    scene.idle(10);

                    // --- 2. THE PLACEMENT ---
                    // Use 4, 1, 4 to be in the center of the 9x9 grid
                    BlockPos center = new BlockPos(4, 1, 4);
                    scene.world().setBlock(center, Blocks.GOLD_BLOCK.defaultBlockState(), false);

                    // --- 3. THE REVEAL ---
                    scene.world().showSection(util.select().position(center), Direction.DOWN);

                    scene.idle(20);
                    scene.overlay().showText(60).text("Baseplate should be visible now!").placeNearTarget();
                });
    }

    private static void registerScene(PonderBuilder builder, String target,
                                      String sceneIdPath, String title,
                                      java.util.function.Consumer<GTPonderContext> callback) {
        // Track the title for registerSharedText
        SCENE_TITLES.put(sceneIdPath, title);
        GTPonderRegistrar.registerGTPonderScene(
                builder, target, sceneIdPath, title,
                Map.of("structure", PROCESS_PONDER_STRUCTURE_ID.toString()),
                callback);
    }

    private static void configureProcessScene(GTPonderContext ctx) {
        ctx.scene.configureBasePlate(0, 0, PROCESS_BASE_PLATE_SIZE);
        ctx.scene.scaleSceneView(0.75f);
        ctx.scene.setSceneOffsetY(-0.6f);
        ctx.scene.showBasePlate();
        ctx.scene.idle(6);
    }

    private static void registerGlueEarlyRoutesScene(PonderBuilder builder) {
        registerScene(builder, "tfc:glue", "processes/glue_early_routes", "Glue: Early Routes", ctx -> {
            BlockPos barrelPos = new BlockPos(3, 1, 3);
            BlockPos mixerPos = new BlockPos(5, 1, 3);
            BlockPos gluePos = new BlockPos(7, 1, 3);

            configureProcessScene(ctx);
            ctx.block(barrelPos, "tfc:wood/barrel/oak", Map.of("direction", Direction.DOWN));
            ctx.idle(PROCESS_STEP_IDLE);

            ctx.outline("glue/barrel/start", barrelPos, Map.of("palette", PonderPalette.MEDIUM, "duration", 72));
            ctx.text("Glue starts as simple chemistry: limewater plus bone meal.", barrelPos,
                    Map.of("palette", PonderPalette.MEDIUM, "duration", 72));
            ctx.idle(72);

            ctx.outline("glue/barrel/limewater", barrelPos, Map.of("palette", PonderPalette.INPUT, "duration", 48));
            ctx.itemCue("minecraft:water_bucket", barrelPos,
                    Map.of("rightClick", true, "pointing", Pointing.DOWN, "xOffset", -0.25));
            ctx.itemCue("tfc:powder/flux", barrelPos,
                    Map.of("rightClick", true, "pointing", Pointing.LEFT, "xOffset", 0.25));
            ctx.text("In a barrel, 500 mB water plus flux or lime becomes limewater.", barrelPos,
                    Map.of("palette", PonderPalette.INPUT, "duration", 48));
            ctx.idle(72);

            ctx.itemCue("minecraft:bone_meal", barrelPos, Map.of("rightClick", true, "pointing", Pointing.RIGHT));
            ctx.text("Seal bone meal in limewater to get solid TFC glue.", barrelPos,
                    Map.of("palette", PonderPalette.OUTPUT, "duration", 48));
            ctx.idle(72);

            ctx.itemCue("tfc:glue", gluePos, Map.of("pointing", Pointing.DOWN));
            ctx.itemEntity("tfc:glue", gluePos, Map.of());
            ctx.line(barrelPos, gluePos, Map.of("palette", PonderPalette.OUTPUT));
            ctx.scene.effects().indicateSuccess(gluePos);
            ctx.idle(PROCESS_FLOW_LINE_DURATION + PROCESS_STEP_IDLE);

            ctx.scene.rotateCameraY(28);
            ctx.idle(12);
            ctx.block(mixerPos, "gtceu:lv_mixer", Map.of("direction", Direction.DOWN));
            ctx.line(barrelPos, mixerPos, Map.of("palette", PonderPalette.INPUT));
            ctx.line(mixerPos, gluePos, Map.of("palette", PonderPalette.OUTPUT));
            ctx.text("Once LV is available, the Mixer does the same bone meal + limewater route as liquid glue.",
                    mixerPos, Map.of("palette", PonderPalette.BLUE, "duration", 76));
            ctx.itemCue("minecraft:bone_meal", mixerPos, Map.of("pointing", Pointing.LEFT));
            ctx.idle(82);
        });
    }

    private static void registerGlueResinRoutesScene(PonderBuilder builder) {
        registerScene(builder, "tfc:glue", "processes/glue_resin_routes", "Glue: Resin Routes", ctx -> {
            BlockPos treeTapPos = new BlockPos(2, 2, 2);
            BlockPos vatPos = new BlockPos(4, 1, 2);
            BlockPos centrifugePos = new BlockPos(6, 1, 2);
            BlockPos solidifierPos = new BlockPos(7, 1, 4);
            BlockPos gluePos = new BlockPos(5, 1, 5);

            configureProcessScene(ctx);
            ctx.scene.rotateCameraY(-24);
            ctx.idle(12);

            List<Object> treeBlocks = makeSpruceTapTreeBlocks(0, 1);
            ctx.blocks(treeBlocks, Map.of("direction", Direction.DOWN));
            ctx.outline("glue/resin/tree_tap", treeTapPos, Map.of("palette", PonderPalette.GREEN, "duration", 58));
            ctx.text("Rosin trees in cold, wet areas can be tapped for conifer pitch.", treeTapPos,
                    Map.of("palette", PonderPalette.GREEN, "duration", 70));
            ctx.itemCue("tfg:conifer_pitch_bucket", treeTapPos, Map.of("pointing", Pointing.RIGHT));
            ctx.idle(78);

            ctx.block(vatPos, "firmalife:vat", Map.of("direction", Direction.DOWN));
            ctx.line(treeTapPos, vatPos, Map.of("palette", PonderPalette.GREEN));
            ctx.itemCue("tfc:powder/wood_ash", vatPos, Map.of("rightClick", true, "pointing", Pointing.LEFT));
            ctx.itemCue("tfc:powder/charcoal", vatPos, Map.of("rightClick", true, "pointing", Pointing.RIGHT));
            ctx.text("Heat pitch with wood ash for sticky resin, or charcoal for conifer rosin.", vatPos,
                    Map.of("palette", PonderPalette.MEDIUM, "duration", 76));
            ctx.idle(84);

            ctx.block(centrifugePos, "gtceu:lv_centrifuge", Map.of("direction", Direction.DOWN));
            ctx.line(vatPos, centrifugePos, Map.of("palette", PonderPalette.INPUT));
            ctx.itemCue("gtceu:sticky_resin", centrifugePos, Map.of("pointing", Pointing.LEFT, "xOffset", -0.25));
            ctx.itemCue("tfg:conifer_rosin", centrifugePos, Map.of("pointing", Pointing.RIGHT, "xOffset", 0.25));
            ctx.text("Centrifuge sticky resin or conifer rosin to turn the tree route into liquid glue.", centrifugePos,
                    Map.of("palette", PonderPalette.BLUE, "duration", 76));
            ctx.idle(84);

            ctx.block(solidifierPos, "gtceu:lv_fluid_solidifier", Map.of("direction", Direction.DOWN));
            ctx.line(centrifugePos, solidifierPos, Map.of("palette", PonderPalette.OUTPUT));
            ctx.itemCue("gtceu:ball_casting_mold", solidifierPos,
                    Map.of("rightClick", true, "pointing", Pointing.DOWN));
            ctx.text("A Fluid Solidifier with a ball mold converts 50 mB liquid glue back into TFC glue.",
                    solidifierPos, Map.of("palette", PonderPalette.OUTPUT, "duration", 76));
            ctx.idle(84);

            ctx.itemCue("tfc:glue", gluePos, Map.of("pointing", Pointing.DOWN));
            ctx.itemEntity("tfc:glue", gluePos, Map.of());
            ctx.line(solidifierPos, gluePos, Map.of("palette", PonderPalette.OUTPUT));
            ctx.scene.effects().indicateSuccess(gluePos);
            ctx.idle(PROCESS_FLOW_LINE_DURATION + 20);

            ctx.idle(8);
        });
    }

    private static List<Object> makeSpruceTapTreeBlocks(int originX, int originZ) {
        List<Object> blocks = new ArrayList<>();
        for (int y = 1; y <= 4; y++)
            blocks.add(Map.of("pos", new BlockPos(originX + 1, y, originZ + 1), "id", "tfc:wood/log/spruce"));
        for (int x = originX; x <= originX + 2; x++)
            for (int z = originZ; z <= originZ + 2; z++) {
                blocks.add(Map.of("pos", new BlockPos(x, 4, z), "id", "tfc:wood/leaves/spruce"));
                blocks.add(Map.of("pos", new BlockPos(x, 5, z), "id", "tfc:wood/leaves/spruce"));
            }
        blocks.add(Map.of("pos", new BlockPos(originX + 2, 2, originZ + 1), "id", "afc:tree_tap"));
        return blocks;
    }
}
