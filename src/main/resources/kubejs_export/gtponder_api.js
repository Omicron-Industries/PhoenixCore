"use strict";

/*
 * GTPonder simplified PonderJS helper API.
 *
 * This file is for pack and addon authors who want to write approachable
 * hand-authored Ponder scenes without copying the larger generated multiblock
 * scene machinery. It deliberately wraps only common operations: positions,
 * block placement, text, outlines, item cues, flow lines, and simple reveals.
 *
 * Example:
 *
 * Ponder.registry((event) => {
 *   GTPonder.scene(event, "gtceu:lv_mixer", "my_mod:mixer/example", "Mixer Basics", (p) => {
 *     let mixer = p.pos(3, 1, 3);
 *     p.block(mixer, "gtceu:lv_mixer");
 *     p.text("The mixer combines item and fluid ingredients.", mixer, {
 *       palette: "input",
 *     });
 *     p.idle(50);
 *   });
 * });
 */

const $GTPonderBlockPos = Java.loadClass("net.minecraft.core.BlockPos");
const $GTPonderDirection = Java.loadClass("net.minecraft.core.Direction");
const $GTPonderBlocks = Java.loadClass("net.minecraft.world.level.block.Blocks");
const $GTPonderItemStack = Java.loadClass("net.minecraft.world.item.ItemStack");
const $GTPonderPonderPalette = Java.loadClass(
  "net.createmod.ponder.api.PonderPalette",
);
const $GTPonderPointing = Java.loadClass("net.createmod.catnip.math.Pointing");
const $GTPonderForgeRegistries = Java.loadClass(
  "net.minecraftforge.registries.ForgeRegistries",
);
const $GTPonderResourceLocation = Java.loadClass(
  "net.minecraft.resources.ResourceLocation",
);

const GTPONDER_API_VERSION = "0.1.0";
const GTPONDER_DEFAULT_STRUCTURE_ID = new $GTPonderResourceLocation(
  "tfg:gregtech_multiblocks/blank_64",
);
const GTPONDER_DEFAULT_BASE_PLATE_SIZE = 7;
const GTPONDER_DEFAULT_TEXT_DURATION = 56;
const GTPONDER_DEFAULT_CUE_DURATION = 36;
const GTPONDER_DEFAULT_FLOW_DURATION = 34;
const GTPONDER_DEFAULT_REVEAL_IDLE = 2;

const GTPONDER_PALETTES = {
  white: $GTPonderPonderPalette.WHITE,
  black: $GTPonderPonderPalette.BLACK,
  red: $GTPonderPonderPalette.RED,
  green: $GTPonderPonderPalette.GREEN,
  blue: $GTPonderPonderPalette.BLUE,
  slow: $GTPonderPonderPalette.SLOW,
  medium: $GTPonderPonderPalette.MEDIUM,
  fast: $GTPonderPonderPalette.FAST,
  input: $GTPonderPonderPalette.INPUT,
  output: $GTPonderPonderPalette.OUTPUT,
};

const GTPONDER_DIRECTIONS = {
  up: $GTPonderDirection.UP,
  down: $GTPonderDirection.DOWN,
  north: $GTPonderDirection.NORTH,
  south: $GTPonderDirection.SOUTH,
  west: $GTPonderDirection.WEST,
  east: $GTPonderDirection.EAST,
};

const GTPONDER_POINTING = {
  up: $GTPonderPointing.UP,
  down: $GTPonderPointing.DOWN,
  left: $GTPonderPointing.LEFT,
  right: $GTPonderPointing.RIGHT,
};

function gtPonderOptions(options) {
  return options === null || options === undefined ? {} : options;
}

function gtPonderNumber(value, fallback) {
  let numberValue = Number(value);
  return isNaN(numberValue) ? fallback : numberValue;
}

function gtPonderResourceLocation(id) {
  if (id instanceof $GTPonderResourceLocation) {
    return id;
  }

  return new $GTPonderResourceLocation(String(id));
}

function gtPonderPos(x, y, z) {
  if (Array.isArray(x)) {
    return new $GTPonderBlockPos(x[0], x[1], x[2]);
  }

  if (x !== null && x !== undefined && typeof x.getX === "function") {
    return x;
  }

  return new $GTPonderBlockPos(x, y, z);
}

function gtPonderBlockState(id) {
  let block = $GTPonderForgeRegistries.BLOCKS.getValue(
    gtPonderResourceLocation(id),
  );
  if (block === null) {
    console.warn(`Unknown block in GTPonder scene: ${id}`);
    return $GTPonderBlocks.BARRIER.defaultBlockState();
  }

  return block.defaultBlockState();
}

function gtPonderItemStack(id) {
  if (id instanceof $GTPonderItemStack) {
    return id;
  }

  let item = $GTPonderForgeRegistries.ITEMS.getValue(
    gtPonderResourceLocation(id),
  );
  if (item === null) {
    console.warn(`Unknown item in GTPonder scene: ${id}`);
    item = $GTPonderForgeRegistries.ITEMS.getValue(
      gtPonderResourceLocation("minecraft:barrier"),
    );
  }

  return new $GTPonderItemStack(item);
}

function gtPonderPalette(value, fallback) {
  if (value === null || value === undefined) {
    return fallback || $GTPonderPonderPalette.WHITE;
  }

  let key = String(value).toLowerCase();
  return GTPONDER_PALETTES[key] || value;
}

function gtPonderDirection(value, fallback) {
  if (value === null || value === undefined) {
    return fallback || $GTPonderDirection.DOWN;
  }

  let key = String(value).toLowerCase();
  return GTPONDER_DIRECTIONS[key] || value;
}

function gtPonderPointing(value, fallback) {
  if (value === null || value === undefined) {
    return fallback || $GTPonderPointing.DOWN;
  }

  let key = String(value).toLowerCase();
  return GTPONDER_POINTING[key] || value;
}

function gtPonderBlockCenter(util, pos) {
  let blockPos = gtPonderPos(pos);
  return util.vector.of(
    blockPos.getX() + 0.5,
    blockPos.getY() + 0.5,
    blockPos.getZ() + 0.5,
  );
}

function gtPonderBlockTop(util, pos, xOffset, zOffset) {
  let blockPos = gtPonderPos(pos);
  return util.vector.of(
    blockPos.getX() + 0.5 + gtPonderNumber(xOffset, 0),
    blockPos.getY() + 1.05,
    blockPos.getZ() + 0.5 + gtPonderNumber(zOffset, 0),
  );
}

function gtPonderSelectionForPositions(util, positions) {
  let selection = null;

  positions.forEach((entry) => {
    let next = util.select.position(gtPonderPos(entry));
    selection = selection === null ? next : selection.add(next);
  });

  return selection;
}

function gtPonderNormalizeBlockEntry(entry) {
  if (Array.isArray(entry)) {
    return { pos: gtPonderPos(entry[0], entry[1], entry[2]), id: entry[3] };
  }

  return {
    pos: gtPonderPos(entry.pos || entry.position),
    id: entry.id || entry.block,
  };
}

function gtPonderContext(scene, util, sceneOptions) {
  let options = gtPonderOptions(sceneOptions);
  let defaultTextDuration =
    options.textDuration || GTPONDER_DEFAULT_TEXT_DURATION;
  let defaultCueDuration = options.cueDuration || GTPONDER_DEFAULT_CUE_DURATION;
  let defaultFlowDuration =
    options.flowDuration || GTPONDER_DEFAULT_FLOW_DURATION;

  function showBlock(pos, id, blockOptions) {
    let localOptions = gtPonderOptions(blockOptions);
    let blockPos = gtPonderPos(pos);
    scene.world.setBlock(blockPos, gtPonderBlockState(id), false);

    if (localOptions.show !== false) {
      scene.world.showSection(
        util.select.position(blockPos),
        gtPonderDirection(localOptions.direction, $GTPonderDirection.DOWN),
      );
    }

    return blockPos;
  }

  function showBlocks(blocks, blockOptions) {
    let localOptions = gtPonderOptions(blockOptions);
    let positions = [];

    blocks.forEach((entry) => {
      let block = gtPonderNormalizeBlockEntry(entry);
      scene.world.setBlock(block.pos, gtPonderBlockState(block.id), false);
      positions.push(block.pos);
    });

    let selection = gtPonderSelectionForPositions(util, positions);
    if (selection !== null && localOptions.show !== false) {
      scene.world.showSection(
        selection,
        gtPonderDirection(localOptions.direction, $GTPonderDirection.DOWN),
      );
    }

    return positions;
  }

  function showText(text, targetPos, textOptions) {
    let localOptions = gtPonderOptions(textOptions);
    let duration = localOptions.duration || defaultTextDuration;
    let overlay = scene.overlay
      .showText(duration)
      .text(text)
      .colored(
        gtPonderPalette(localOptions.palette || localOptions.color, GTPONDER_PALETTES.white),
      );

    if (targetPos !== null && targetPos !== undefined) {
      overlay.pointAt(gtPonderBlockCenter(util, targetPos));
      if (localOptions.placeNearTarget !== false) {
        overlay.placeNearTarget();
      }
    }

    return overlay;
  }

  function showOutline(key, targetPos, outlineOptions) {
    let localOptions = gtPonderOptions(outlineOptions);
    let duration = localOptions.duration || defaultTextDuration;
    let palette = gtPonderPalette(
      localOptions.palette || localOptions.color,
      GTPONDER_PALETTES.blue,
    );

    scene.overlay.showOutline(
      palette,
      key,
      util.select.position(gtPonderPos(targetPos)),
      duration,
    );
  }

  function showLine(fromPos, toPos, lineOptions) {
    let localOptions = gtPonderOptions(lineOptions);
    let duration = localOptions.duration || defaultFlowDuration;
    let palette = gtPonderPalette(
      localOptions.palette || localOptions.color,
      GTPONDER_PALETTES.medium,
    );

    if (localOptions.big === false) {
      scene.overlay.showLine(
        palette,
        gtPonderBlockCenter(util, fromPos),
        gtPonderBlockCenter(util, toPos),
        duration,
      );
      return;
    }

    scene.overlay.showBigLine(
      palette,
      gtPonderBlockCenter(util, fromPos),
      gtPonderBlockCenter(util, toPos),
      duration,
    );
  }

  function showItemCue(itemId, targetPos, cueOptions) {
    let localOptions = gtPonderOptions(cueOptions);
    let cue = scene.overlay.showControls(
      gtPonderBlockTop(util, targetPos, localOptions.xOffset, localOptions.zOffset),
      gtPonderPointing(localOptions.pointing, $GTPonderPointing.DOWN),
      localOptions.duration || defaultCueDuration,
    );

    if (localOptions.rightClick === true) {
      cue.rightClick();
    }

    cue.withItem(gtPonderItemStack(itemId));
  }

  function showItemEntity(itemId, targetPos, entityOptions) {
    let localOptions = gtPonderOptions(entityOptions);
    scene.world.createItemEntity(
      gtPonderBlockTop(util, targetPos, localOptions.xOffset, localOptions.zOffset),
      util.vector.of(
        gtPonderNumber(localOptions.velocityX, 0),
        gtPonderNumber(localOptions.velocityY, 0.02),
        gtPonderNumber(localOptions.velocityZ, 0),
      ),
      gtPonderItemStack(itemId),
    );
  }

  function reveal(blocks, revealOptions) {
    let localOptions = gtPonderOptions(revealOptions);
    let idleTime = localOptions.idle || GTPONDER_DEFAULT_REVEAL_IDLE;
    let direction = gtPonderDirection(localOptions.direction, $GTPonderDirection.DOWN);

    blocks.forEach((entry) => {
      showBlocks([entry], { direction: direction });
      scene.idle(idleTime);
    });
  }

  return {
    scene: scene,
    util: util,
    palette: GTPONDER_PALETTES,
    direction: GTPONDER_DIRECTIONS,
    pointing: GTPONDER_POINTING,
    pos: gtPonderPos,
    center: (pos) => gtPonderBlockCenter(util, pos),
    top: (pos, xOffset, zOffset) =>
      gtPonderBlockTop(util, pos, xOffset, zOffset),
    blockState: gtPonderBlockState,
    itemStack: gtPonderItemStack,
    selection: (positions) => gtPonderSelectionForPositions(util, positions),
    idle: (ticks) => scene.idle(ticks),
    block: showBlock,
    blocks: showBlocks,
    text: showText,
    outline: showOutline,
    highlight: showOutline,
    line: showLine,
    flow: showLine,
    itemCue: showItemCue,
    rightClick: (itemId, targetPos, cueOptions) => {
      let localOptions = gtPonderOptions(cueOptions);
      localOptions.rightClick = true;
      showItemCue(itemId, targetPos, localOptions);
    },
    itemEntity: showItemEntity,
    reveal: reveal,
  };
}

function gtPonderScene(event, target, sceneId, title, options, callback) {
  let sceneOptions = options;
  let sceneCallback = callback;

  if (typeof options === "function") {
    sceneOptions = {};
    sceneCallback = options;
  }

  sceneOptions = gtPonderOptions(sceneOptions);

  event.create(target).scene(
    sceneId,
    title,
    sceneOptions.structure || GTPONDER_DEFAULT_STRUCTURE_ID,
    (scene, util) => {
      let basePlateSize =
        sceneOptions.basePlateSize ||
        sceneOptions.basePlate ||
        GTPONDER_DEFAULT_BASE_PLATE_SIZE;
      scene.configureBasePlate(
        sceneOptions.basePlateX || 0,
        sceneOptions.basePlateZ || 0,
        basePlateSize,
      );

      if (sceneOptions.scale !== null && sceneOptions.scale !== undefined) {
        scene.scaleSceneView(sceneOptions.scale);
      }
      if (sceneOptions.offsetY !== null && sceneOptions.offsetY !== undefined) {
        scene.setSceneOffsetY(sceneOptions.offsetY);
      }
      if (sceneOptions.showBasePlate !== false) {
        scene.showBasePlate();
      }
      if (sceneOptions.initialIdle !== false) {
        scene.idle(sceneOptions.initialIdle || 3);
      }

      sceneCallback(gtPonderContext(scene, util, sceneOptions));

      if (sceneOptions.markAsFinished !== false) {
        scene.markAsFinished();
      }
    },
  );
}

global.GTPonder = {
  version: GTPONDER_API_VERSION,
  defaultStructureId: GTPONDER_DEFAULT_STRUCTURE_ID,
  palette: GTPONDER_PALETTES,
  direction: GTPONDER_DIRECTIONS,
  pointing: GTPONDER_POINTING,
  pos: gtPonderPos,
  resourceLocation: gtPonderResourceLocation,
  blockState: gtPonderBlockState,
  itemStack: gtPonderItemStack,
  scene: gtPonderScene,
  context: gtPonderContext,
};

console.info(`Loaded GTPonder helper API ${GTPONDER_API_VERSION}.`);
