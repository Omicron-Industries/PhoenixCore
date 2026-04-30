"use strict";

/*
 * Optional TFG process Ponder scenes.
 *
 * These scenes are hand-authored examples that complement the generated
 * multiblock scenes. They demonstrate ordinary PonderJS timeline authoring:
 * placing blocks, showing text, drawing flow lines, and pointing at item cues.
 *
 * The addon installer only copies this file when the `tfg` mod is loaded,
 * because several scenes reference TFG-specific items.
 */

const $ProcessBlockPos = Java.loadClass("net.minecraft.core.BlockPos");
const $ProcessDirection = Java.loadClass("net.minecraft.core.Direction");
const $ProcessBlocks = Java.loadClass("net.minecraft.world.level.block.Blocks");
const $ProcessItemStack = Java.loadClass("net.minecraft.world.item.ItemStack");
const $ProcessPonderPalette = Java.loadClass("net.createmod.ponder.api.PonderPalette");
const $ProcessPointing = Java.loadClass("net.createmod.catnip.math.Pointing");
const $ProcessForgeRegistries = Java.loadClass("net.minecraftforge.registries.ForgeRegistries");
const $ProcessResourceLocation = Java.loadClass("net.minecraft.resources.ResourceLocation");

const PROCESS_PONDER_STRUCTURE_ID = new $ProcessResourceLocation("tfg:gregtech_multiblocks/blank_64");
const PROCESS_BASE_PLATE_SIZE = 9;
const PROCESS_TEXT_DURATION = 68;
const PROCESS_STEP_IDLE = 16;
const PROCESS_ITEM_CUE_DURATION = 36;
const PROCESS_FLOW_LINE_DURATION = 34;

function processPos(x, y, z) {
    return new $ProcessBlockPos(x, y, z);
}

function processResourceLocation(id) {
    return new $ProcessResourceLocation(id);
}

function processBlockState(id) {
    let block = $ProcessForgeRegistries.BLOCKS.getValue(processResourceLocation(id));
    if (block === null) {
        console.warn(`Unknown block in process Ponder scene: ${id}`);
        return $ProcessBlocks.BARRIER.defaultBlockState();
    }

    return block.defaultBlockState();
}

function processItemStack(id) {
    let item = $ProcessForgeRegistries.ITEMS.getValue(processResourceLocation(id));
    if (item === null) {
        console.warn(`Unknown item in process Ponder scene: ${id}`);
        item = $ProcessForgeRegistries.ITEMS.getValue(processResourceLocation("minecraft:barrier"));
    }

    return new $ProcessItemStack(item);
}

function processBlockCenter(util, pos) {
    return util.vector.of(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
}

function processBlockTop(util, pos, xOffset, zOffset) {
    return util.vector.of(pos.getX() + 0.5 + (xOffset || 0), pos.getY() + 1.15, pos.getZ() + 0.5 + (zOffset || 0));
}

function selectionForProcessPositions(util, positions) {
    let selection = null;

    positions.forEach((pos) => {
        let positionSelection = util.select.position(pos);
        selection = selection === null ? positionSelection : selection.add(positionSelection);
    });

    return selection;
}

function showProcessBlocks(scene, util, blocks, direction) {
    let positions = [];

    blocks.forEach((block) => {
        scene.world.setBlock(block.pos, processBlockState(block.id), false);
        positions.push(block.pos);
    });

    let selection = selectionForProcessPositions(util, positions);
    if (selection !== null) {
        scene.world.showSection(selection, direction);
    }
}

function showProcessBlock(scene, util, pos, id, direction) {
    showProcessBlocks(scene, util, [{ pos: pos, id: id }], direction);
}

function showProcessText(scene, util, text, palette, pos, duration) {
    scene.overlay
        .showText(duration || PROCESS_TEXT_DURATION)
        .text(text)
        .colored(palette)
        .pointAt(processBlockCenter(util, pos))
        .placeNearTarget();
}

function showProcessOutline(scene, util, key, palette, pos, duration) {
    scene.overlay.showOutline(palette, key, util.select.position(pos), duration || PROCESS_TEXT_DURATION);
}

function showProcessItemCue(scene, util, itemId, pos, pointing, xOffset, zOffset) {
    scene.overlay
        .showControls(
            processBlockTop(util, pos, xOffset, zOffset),
            pointing || $ProcessPointing.DOWN,
            PROCESS_ITEM_CUE_DURATION,
        )
        .withItem(processItemStack(itemId));
}

function showProcessRightClickCue(scene, util, itemId, pos, pointing, xOffset, zOffset) {
    scene.overlay
        .showControls(
            processBlockTop(util, pos, xOffset, zOffset),
            pointing || $ProcessPointing.DOWN,
            PROCESS_ITEM_CUE_DURATION,
        )
        .rightClick()
        .withItem(processItemStack(itemId));
}

function showProcessItemEntity(scene, util, itemId, pos) {
    scene.world.createItemEntity(
        processBlockTop(util, pos, 0, 0),
        util.vector.of(0, 0.02, 0),
        processItemStack(itemId),
    );
}

function showProcessFlow(scene, util, palette, fromPos, toPos) {
    scene.overlay.showBigLine(
        palette,
        processBlockCenter(util, fromPos),
        processBlockCenter(util, toPos),
        PROCESS_FLOW_LINE_DURATION,
    );
}

function makeSpruceTapTreeBlocks(originX, originZ) {
    let blocks = [];

    for (let y = 1; y <= 4; y++) {
        blocks.push({ pos: processPos(originX + 1, y, originZ + 1), id: "tfc:wood/log/spruce" });
    }

    for (let x = originX; x <= originX + 2; x++) {
        for (let z = originZ; z <= originZ + 2; z++) {
            blocks.push({ pos: processPos(x, 4, z), id: "tfc:wood/leaves/spruce" });
            blocks.push({ pos: processPos(x, 5, z), id: "tfc:wood/leaves/spruce" });
        }
    }

    blocks.push({ pos: processPos(originX + 2, 2, originZ + 1), id: "afc:tree_tap" });
    return blocks;
}

function configureProcessScene(scene) {
    scene.configureBasePlate(0, 0, PROCESS_BASE_PLATE_SIZE);
    scene.scaleSceneView(0.75);
    scene.setSceneOffsetY(-0.6);
    scene.showBasePlate();
    scene.idle(6);
}

function registerGlueEarlyRoutesScene(event) {
    event
        .create("tfc:glue")
        .scene("tfg:processes/glue_early_routes", "Glue: Early Routes", PROCESS_PONDER_STRUCTURE_ID, (scene, util) => {
            let barrelPos = processPos(3, 1, 3);
            let mixerPos = processPos(5, 1, 3);
            let gluePos = processPos(7, 1, 3);

            configureProcessScene(scene);
            showProcessBlock(scene, util, barrelPos, "tfc:wood/barrel/oak", $ProcessDirection.DOWN);
            scene.idle(PROCESS_STEP_IDLE);
            showProcessOutline(scene, util, "glue/barrel/start", $ProcessPonderPalette.MEDIUM, barrelPos, 72);
            showProcessText(
                scene,
                util,
                "Glue starts as simple chemistry: limewater plus bone meal.",
                $ProcessPonderPalette.MEDIUM,
                barrelPos,
                72,
            );
            scene.idle(72);

            showProcessOutline(scene, util, "glue/barrel/limewater", $ProcessPonderPalette.INPUT, barrelPos, 48);
            showProcessRightClickCue(scene, util, "minecraft:water_bucket", barrelPos, $ProcessPointing.DOWN, -0.25, 0);
            showProcessRightClickCue(scene, util, "tfc:powder/flux", barrelPos, $ProcessPointing.LEFT, 0.25, 0);
            showProcessText(
                scene,
                util,
                "In a barrel, 500 mB water plus flux or lime becomes limewater.",
                $ProcessPonderPalette.INPUT,
                barrelPos,
                64,
            );
            scene.idle(72);

            showProcessRightClickCue(scene, util, "minecraft:bone_meal", barrelPos, $ProcessPointing.RIGHT);
            showProcessText(
                scene,
                util,
                "Seal bone meal in limewater to get solid TFC glue.",
                $ProcessPonderPalette.OUTPUT,
                barrelPos,
                64,
            );
            scene.idle(72);

            showProcessItemCue(scene, util, "tfc:glue", gluePos, $ProcessPointing.DOWN);
            showProcessItemEntity(scene, util, "tfc:glue", gluePos);
            showProcessFlow(scene, util, $ProcessPonderPalette.OUTPUT, barrelPos, gluePos);
            scene.effects.indicateSuccess(gluePos);
            scene.idle(PROCESS_FLOW_LINE_DURATION + PROCESS_STEP_IDLE);

            scene.rotateCameraY(28);
            scene.idle(12);
            showProcessBlock(scene, util, mixerPos, "gtceu:lv_mixer", $ProcessDirection.DOWN);
            showProcessFlow(scene, util, $ProcessPonderPalette.INPUT, barrelPos, mixerPos);
            showProcessFlow(scene, util, $ProcessPonderPalette.OUTPUT, mixerPos, gluePos);
            showProcessText(
                scene,
                util,
                "Once LV is available, the Mixer does the same bone meal + limewater route as liquid glue.",
                $ProcessPonderPalette.BLUE,
                mixerPos,
                76,
            );
            showProcessItemCue(scene, util, "minecraft:bone_meal", mixerPos, $ProcessPointing.LEFT);
            scene.idle(82);

            scene.markAsFinished();
        });
}

function registerGlueResinRoutesScene(event) {
    event
        .create("tfc:glue")
        .scene("tfg:processes/glue_resin_routes", "Glue: Resin Routes", PROCESS_PONDER_STRUCTURE_ID, (scene, util) => {
            let treeTapPos = processPos(2, 2, 2);
            let vatPos = processPos(4, 1, 2);
            let centrifugePos = processPos(6, 1, 2);
            let solidifierPos = processPos(7, 1, 4);
            let gluePos = processPos(5, 1, 5);

            configureProcessScene(scene);
            scene.rotateCameraY(-24);
            scene.idle(12);

            showProcessBlocks(scene, util, makeSpruceTapTreeBlocks(0, 1), $ProcessDirection.DOWN);
            showProcessOutline(scene, util, "glue/resin/tree_tap", $ProcessPonderPalette.GREEN, treeTapPos, 58);
            showProcessText(
                scene,
                util,
                "Rosin trees in cold, wet areas can be tapped for conifer pitch.",
                $ProcessPonderPalette.GREEN,
                treeTapPos,
                70,
            );
            showProcessItemCue(scene, util, "tfg:conifer_pitch_bucket", treeTapPos, $ProcessPointing.RIGHT);
            scene.idle(78);

            showProcessBlock(scene, util, vatPos, "firmalife:vat", $ProcessDirection.DOWN);
            showProcessFlow(scene, util, $ProcessPonderPalette.GREEN, treeTapPos, vatPos);
            showProcessRightClickCue(scene, util, "tfc:powder/wood_ash", vatPos, $ProcessPointing.LEFT);
            showProcessRightClickCue(scene, util, "tfc:powder/charcoal", vatPos, $ProcessPointing.RIGHT);
            showProcessText(
                scene,
                util,
                "Heat pitch with wood ash for sticky resin, or charcoal for conifer rosin.",
                $ProcessPonderPalette.MEDIUM,
                vatPos,
                76,
            );
            scene.idle(84);

            showProcessBlock(scene, util, centrifugePos, "gtceu:lv_centrifuge", $ProcessDirection.DOWN);
            showProcessFlow(scene, util, $ProcessPonderPalette.INPUT, vatPos, centrifugePos);
            showProcessItemCue(scene, util, "gtceu:sticky_resin", centrifugePos, $ProcessPointing.LEFT, -0.25, 0);
            showProcessItemCue(scene, util, "tfg:conifer_rosin", centrifugePos, $ProcessPointing.RIGHT, 0.25, 0);
            showProcessText(
                scene,
                util,
                "Centrifuge sticky resin or conifer rosin to turn the tree route into liquid glue.",
                $ProcessPonderPalette.BLUE,
                centrifugePos,
                76,
            );
            scene.idle(84);

            showProcessBlock(scene, util, solidifierPos, "gtceu:lv_fluid_solidifier", $ProcessDirection.DOWN);
            showProcessFlow(scene, util, $ProcessPonderPalette.OUTPUT, centrifugePos, solidifierPos);
            showProcessRightClickCue(scene, util, "gtceu:ball_casting_mold", solidifierPos, $ProcessPointing.DOWN);
            showProcessText(
                scene,
                util,
                "A Fluid Solidifier with a ball mold converts 50 mB liquid glue back into TFC glue.",
                $ProcessPonderPalette.OUTPUT,
                solidifierPos,
                76,
            );
            scene.idle(84);

            showProcessItemCue(scene, util, "tfc:glue", gluePos, $ProcessPointing.DOWN);
            showProcessItemEntity(scene, util, "tfc:glue", gluePos);
            showProcessFlow(scene, util, $ProcessPonderPalette.OUTPUT, solidifierPos, gluePos);
            scene.effects.indicateSuccess(gluePos);
            scene.idle(PROCESS_FLOW_LINE_DURATION + 20);

            scene.idle(8);
            scene.markAsFinished();
        });
}

Ponder.registry((event) => {
    registerGlueEarlyRoutesScene(event);
    registerGlueResinRoutesScene(event);
});
