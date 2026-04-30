"use strict";

/*
 * Generated GTCEu multiblock Ponder scenes.
 *
 * This script is intentionally data-driven. Instead of hand-writing one scene
 * per machine, it reads GTCEu multiblock definitions at client script load time
 * and builds a pair of scenes for each usable machine:
 *
 * - an overview scene with block reveal, formation, mechanic callouts, and
 *   buses/hatches/parts highlighted
 * - a layer-scan scene that walks the player through each Y layer
 *
 * If you are learning the generator, start from the bottom of the file:
 * addGeneratedMultiblockScene() shows the timeline, while the helper functions
 * above it explain how shape data, camera angles, callouts, and formation are
 * derived.
 */

const $GTRegistries = Java.loadClass(
  "com.gregtechceu.gtceu.api.registry.GTRegistries",
);
const $ArrayList = Java.loadClass("java.util.ArrayList");
const $BlockPos = Java.loadClass("net.minecraft.core.BlockPos");
const $I18n = Java.loadClass("net.minecraft.client.resources.language.I18n");
const $Blocks = Java.loadClass("net.minecraft.world.level.block.Blocks");
const $PonderPalette = Java.loadClass("net.createmod.ponder.api.PonderPalette");
const $ReplaceBlocksInstruction = Java.loadClass(
  "net.createmod.ponder.foundation.instruction.ReplaceBlocksInstruction",
);
const $RotateSceneInstruction = Java.loadClass(
  "net.createmod.ponder.foundation.instruction.RotateSceneInstruction",
);
const $ForgeRegistries = Java.loadClass(
  "net.minecraftforge.registries.ForgeRegistries",
);
const $ResourceLocation = Java.loadClass(
  "net.minecraft.resources.ResourceLocation",
);
const $MetaMachineBlockEntity = Java.loadClass(
  "com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity",
);

const GT_PONDER_SCENE_PREFIX = "tfg:gregtech_multiblocks/";
const GT_PONDER_GENERATOR_VERSION = "layout-v15-exposed-camera";
const GT_PONDER_STRUCTURE_ID = new $ResourceLocation(
  "tfg:gregtech_multiblocks/blank_64",
);
const GT_PONDER_STRUCTURE_SIZE = 64;
const GT_PONDER_STRUCTURE_HEIGHT = 64;
const GT_MULTIBLOCK_DEFINITION_CLASS_NAME =
  "com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition";
const GT_META_MACHINE_BLOCK_CLASS_NAME =
  "com.gregtechceu.gtceu.api.block.MetaMachineBlock";
const GT_CALLOUT_DURATION = 24;
const GT_CONTROLLER_CALLOUT_DURATION = 32;
const GT_PART_CALLOUT_DURATION = GT_CALLOUT_DURATION;
const GT_PART_CALLOUT_IDLE = GT_CALLOUT_DURATION;
const GT_DEFAULT_CAMERA_Y_ROTATION = 145;
const GT_DEFAULT_CAMERA_X_ROTATION = -35;
const GT_INITIAL_CAMERA_Y_ROTATION = 35;
const GT_STRUCTURE_REVEAL_SETTLE = 18;
const GT_STRUCTURE_REVEAL_TARGET_TICKS = 140;
const GT_PART_ROTATION_MINIMUM = 20;
const GT_PART_ROTATION_MIN_SETTLE = 4;
const GT_PART_ROTATION_MAX_SETTLE = 16;
const GT_CAMERA_VIEW_BUCKET_DEGREES = 45;
const GT_FLOW_LINE_DURATION = GT_CALLOUT_DURATION;
const GT_MECHANIC_CALLOUT_DURATION = 46;
const GT_MECHANIC_CALLOUT_IDLE = GT_MECHANIC_CALLOUT_DURATION;
const GT_MECHANIC_FLOW_LINE_DURATION = 34;
const GT_MAX_MECHANIC_STEPS = 7;
const GT_LAYER_SCAN_FINAL_DURATION = 64;
const GT_DYNAMIC_CALLOUT_PALETTES = [
  $PonderPalette.BLUE,
  $PonderPalette.MEDIUM,
  $PonderPalette.SLOW,
  $PonderPalette.FAST,
];
const GT_PART_CALLOUTS = [
  {
    key: "maintenance",
    palette: $PonderPalette.RED,
    label: "Maintenance hatches",
    text: "handle repairs and maintenance problems.",
    match: (path) => path.indexOf("maintenance") !== -1,
  },
  {
    key: "energy_input",
    palette: $PonderPalette.GREEN,
    label: "Energy input hatches",
    text: "feed EU into powered multiblocks.",
    match: (path) =>
      path.indexOf("energy") !== -1 &&
      path.indexOf("hatch") !== -1 &&
      path.indexOf("output") === -1,
  },
  {
    key: "energy_output",
    palette: $PonderPalette.GREEN,
    label: "Dynamo hatches",
    text: "emit EU from generator multiblocks.",
    match: (path) =>
      path.indexOf("dynamo") !== -1 ||
      path.indexOf("energy_output") !== -1 ||
      path.indexOf("laser_output") !== -1,
  },
  {
    key: "item_input",
    palette: $PonderPalette.INPUT,
    label: "Input buses",
    text: "accept item ingredients.",
    match: (path) => path.indexOf("input_bus") !== -1,
  },
  {
    key: "fluid_input",
    palette: $PonderPalette.INPUT,
    label: "Input hatches",
    text: "accept fluid ingredients.",
    match: (path) => path.indexOf("input_hatch") !== -1,
  },
  {
    key: "item_output",
    palette: $PonderPalette.OUTPUT,
    label: "Output buses",
    text: "collect item products.",
    match: (path) => path.indexOf("output_bus") !== -1,
  },
  {
    key: "fluid_output",
    palette: $PonderPalette.OUTPUT,
    label: "Output hatches",
    text: "collect fluid products.",
    match: (path) => path.indexOf("output_hatch") !== -1,
  },
  {
    key: "muffler",
    palette: $PonderPalette.SLOW,
    label: "Muffler hatches",
    text: "route exhaust and pollution-sensitive outputs.",
    match: (path) => path.indexOf("muffler") !== -1,
  },
  {
    key: "laser",
    palette: $PonderPalette.FAST,
    label: "Laser hatches",
    text: "move high-amperage laser power.",
    match: (path) => path.indexOf("laser") !== -1,
  },
  {
    key: "data",
    palette: $PonderPalette.MEDIUM,
    label: "Data and computation hatches",
    text: "connect data, computation, or advanced network links.",
    match: (path) =>
      path.indexOf("data") !== -1 ||
      path.indexOf("computation") !== -1 ||
      path.indexOf("network") !== -1,
  },
];

const GT_GENERIC_MECHANIC_RULES = [
  {
    key: "recipe_flow",
    focus: ["inputs", "outputs", "controller"],
    palette: $PonderPalette.BLUE,
    text: "Inputs move toward the controller, while outputs leave through their matching buses or hatches.",
    animation: "automation",
    when: (shape) =>
      hasAnyFocus(shape, "inputs") && hasAnyFocus(shape, "outputs"),
  },
  {
    key: "power_feed",
    focus: ["part:energy_input", "part:laser"],
    palette: $PonderPalette.GREEN,
    text: "Energy hatches define how EU reaches the multiblock; higher tiers may need multiple amps or laser power.",
    animation: "power",
    when: (shape) => hasAnyFocus(shape, ["part:energy_input", "part:laser"]),
  },
  {
    key: "maintenance_access",
    focus: "part:maintenance",
    palette: $PonderPalette.RED,
    text: "Maintenance access keeps the machine running after problems appear; automated hatches can reduce downtime.",
    animation: "pulse",
    when: (shape) => hasAnyFocus(shape, "part:maintenance"),
  },
  {
    key: "heat_coils",
    focus: "structure:coil",
    palette: $PonderPalette.SLOW,
    text: "Coils are recipe-critical: they set heat tier and often affect overclocking or energy use.",
    animation: "heat",
    when: (shape) => hasAnyFocus(shape, "structure:coil"),
  },
  {
    key: "exhaust",
    focus: "part:muffler",
    palette: $PonderPalette.SLOW,
    text: "Mufflers handle exhaust or pollution outputs, so leave their face unobstructed when the machine requires it.",
    animation: "output",
    when: (shape) => hasAnyFocus(shape, "part:muffler"),
  },
  {
    key: "data_links",
    focus: "part:data",
    palette: $PonderPalette.MEDIUM,
    text: "Data and computation hatches connect research, data sticks, CWU, or networked control to advanced machines.",
    animation: "data",
    when: (shape) => hasAnyFocus(shape, "part:data"),
  },
  {
    key: "parallel_control",
    focus: "machine:parallel",
    palette: $PonderPalette.FAST,
    text: "Parallel control hatches let supported machines run multiple recipes at once from the same structure.",
    animation: "pulse",
    when: (shape) => hasAnyFocus(shape, "machine:parallel"),
  },
  {
    key: "clean_environment",
    focus: ["structure:cleanroom", "structure:filter"],
    palette: $PonderPalette.MEDIUM,
    text: "Cleanroom and filter blocks mark controlled-environment requirements for sensitive recipes.",
    animation: "cooling",
    when: (shape) =>
      hasAnyFocus(shape, ["structure:cleanroom", "structure:filter"]),
  },
  {
    key: "rotor_drive",
    focus: [
      "structure:rotor",
      "structure:turbine",
      "machine:rotor",
      "machine:turbine",
    ],
    palette: $PonderPalette.FAST,
    text: "Rotor and turbine parts are the working core for machines that convert motion, fluids, or gases into power.",
    animation: "pulse",
    when: (shape) =>
      hasAnyFocus(shape, [
        "structure:rotor",
        "structure:turbine",
        "machine:rotor",
        "machine:turbine",
      ]),
  },
  {
    key: "firebox_heat",
    focus: ["structure:firebox", "structure:burner"],
    palette: $PonderPalette.SLOW,
    text: "Firebox and burner blocks mark the combustion section that supplies heat to this structure.",
    animation: "heat",
    when: (shape) =>
      hasAnyFocus(shape, ["structure:firebox", "structure:burner"]),
  },
];

const GT_MACHINE_MECHANIC_STEPS = {
  "gtceu:assembly_line": [
    {
      key: "recipe_flow",
      focus: ["part:item_input", "part:data", "part:item_output"],
      palette: $PonderPalette.BLUE,
      text: "Assembly Line item inputs are read along the line, then finished products leave through the output bus.",
      animation: "automation",
    },
    {
      key: "data_links",
      focus: "part:data",
      palette: $PonderPalette.MEDIUM,
      text: "The data hatch supplies research data for advanced components that cannot be assembled from items alone.",
      animation: "data",
    },
  ],
  "gtceu:cleanroom": [
    {
      key: "clean_environment",
      focus: ["structure:filter", "structure:cleanroom"],
      palette: $PonderPalette.MEDIUM,
      text: "Filter casings belong in the ceiling and define the controlled environment inside the Cleanroom.",
      animation: "cooling",
    },
    {
      key: "cleanroom_walls",
      focus: ["structure:glass", "structure:door", "structure:plascrete"],
      palette: $PonderPalette.BLUE,
      text: "Walls, glass, and doors enclose the clean volume while still allowing machine access.",
      animation: "pulse",
    },
  ],
  "gtceu:cracker": [
    {
      key: "heat_coils",
      focus: "structure:coil",
      palette: $PonderPalette.SLOW,
      text: "Every coil tier above Cupronickel lowers the Cracker's energy cost for oil cracking recipes.",
      animation: "heat",
    },
  ],
  "gtceu:distillation_tower": [
    {
      key: "recipe_flow",
      focus: ["part:fluid_input", "part:fluid_output", "part:item_output"],
      palette: $PonderPalette.BLUE,
      text: "The tower takes fluid input near the bottom and separates products into vertical output layers.",
      animation: "vertical_outputs",
    },
  ],
  "gtceu:electric_blast_furnace": [
    {
      key: "heat_coils",
      focus: "structure:coil",
      palette: $PonderPalette.SLOW,
      text: "EBF coils set the available heat; hotter coils unlock hotter recipes and improve overclock behavior.",
      animation: "heat",
    },
    {
      key: "exhaust",
      focus: "part:muffler",
      palette: $PonderPalette.SLOW,
      text: "The muffler handles exhaust from high-temperature processing and should remain exposed.",
      animation: "output",
    },
  ],
  "gtceu:implosion_compressor": [
    {
      key: "recipe_flow",
      focus: ["part:item_input", "part:item_output"],
      palette: $PonderPalette.BLUE,
      text: "Explosive inputs are consumed inside the casing and compressed products leave through the output bus.",
      animation: "automation",
    },
  ],
  "gtceu:large_chemical_reactor": [
    {
      key: "recipe_flow",
      focus: [
        "part:item_input",
        "part:fluid_input",
        "part:item_output",
        "part:fluid_output",
      ],
      palette: $PonderPalette.BLUE,
      text: "The LCR combines item and fluid ingredients in one recipe space, then splits item and fluid products.",
      animation: "automation",
    },
    {
      key: "reactor_core",
      focus: ["structure:coil", "structure:pipe"],
      palette: $PonderPalette.SLOW,
      text: "The pipe casing and single Cupronickel coil are required core blocks, not decorative casing.",
      animation: "heat",
    },
  ],
  "gtceu:mega_blast_furnace": [
    {
      key: "heat_coils",
      focus: ["structure:coil", "structure:firebox"],
      palette: $PonderPalette.SLOW,
      text: "The Rotary Hearth's coils and firebox section form the heat core for large-scale smelting.",
      animation: "heat",
    },
    {
      key: "large_frame",
      focus: ["structure:frame", "structure:intake", "structure:vent"],
      palette: $PonderPalette.BLUE,
      text: "Frames, vents, and intakes make this a staged furnace shape rather than a compact EBF shell.",
      animation: "pulse",
    },
  ],
  "gtceu:high_performance_computation_array": [
    {
      key: "data_links",
      focus: ["structure:hpca", "part:data"],
      palette: $PonderPalette.MEDIUM,
      text: "HPCA components provide CWU/t; bridge components connect that computation to networked machines.",
      animation: "data",
    },
    {
      key: "hpca_cooling",
      focus: ["structure:cooler", "structure:cooling", "structure:heat_sink"],
      palette: $PonderPalette.BLUE,
      text: "Cooling components must cover computation heat, or HPCA components can overheat and become damaged.",
      animation: "cooling",
    },
  ],
};

const GT_MACHINE_MECHANIC_PATTERNS = [
  {
    match: (machinePath) => machinePath.indexOf("fusion_reactor") !== -1,
    steps: [
      {
        key: "power_feed",
        focus: "part:energy_input",
        palette: $PonderPalette.GREEN,
        text: "Fusion reactors charge an internal EU buffer before recipes can ignite.",
        animation: "power",
      },
      {
        key: "heat_coils",
        focus: "structure:coil",
        palette: $PonderPalette.SLOW,
        text: "The coil ring and fusion casing define the reactor tier and startup requirements.",
        animation: "heat",
      },
    ],
  },
  {
    match: (machinePath) =>
      machinePath.indexOf("large_turbine") !== -1 ||
      machinePath.indexOf("turbine") !== -1,
    steps: [
      {
        key: "rotor_drive",
        focus: [
          "structure:rotor",
          "structure:turbine",
          "machine:rotor",
          "machine:turbine",
        ],
        palette: $PonderPalette.FAST,
        text: "The rotor section is the working core; inputs drive it and dynamo hatches export EU.",
        animation: "automation",
      },
    ],
  },
  {
    match: (machinePath) =>
      machinePath.indexOf("miner") !== -1 ||
      machinePath.indexOf("drilling_rig") !== -1,
    steps: [
      {
        key: "recipe_flow",
        focus: ["part:energy_input", "part:item_output", "part:fluid_output"],
        palette: $PonderPalette.BLUE,
        text: "Mining multiblocks need power and output space; products leave through item or fluid outputs.",
        animation: "automation",
      },
    ],
  },
  {
    match: (machinePath) => machinePath.indexOf("boiler") !== -1,
    steps: [
      {
        key: "firebox_heat",
        focus: ["structure:firebox", "structure:burner"],
        palette: $PonderPalette.SLOW,
        text: "Boiler firebox blocks mark where fuel heat is converted into steam production.",
        animation: "heat",
      },
    ],
  },
];

function isInstanceOfClassName(value, className) {
  let valueClass = value.getClass();
  while (valueClass !== null) {
    if (String(valueClass.getName()) === className) {
      return true;
    }
    valueClass = valueClass.getSuperclass();
  }
  return false;
}

function titleFromPath(path) {
  return String(path)
    .split("_")
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");
}

function getTranslatedText(descriptionId, fallback) {
  try {
    let translated = String($I18n.get(descriptionId));
    if (translated !== descriptionId) {
      return translated;
    }
  } catch (error) {
    // Language lookup can fail during early client reloads; fall through to a stable generated title.
  }

  return fallback;
}

function getTranslatedMachineName(definition) {
  let descriptionId = String(definition.getDescriptionId());

  let langValue = definition.getLangValue();
  if (langValue !== null) {
    return getTranslatedText(descriptionId, String(langValue));
  }

  return getTranslatedText(
    descriptionId,
    titleFromPath(definition.getId().getPath()),
  );
}

function getBlockRegistryId(block) {
  try {
    let key = $ForgeRegistries.BLOCKS.getKey(block);
    if (key !== null) {
      return String(key);
    }
  } catch (error) {
    // Registry lookup is best-effort; the description id below is still stable enough for grouping.
  }

  return String(block.getDescriptionId());
}

function getPathFromId(id) {
  let stringId = String(id);
  let namespaceSeparator = stringId.indexOf(":");
  return namespaceSeparator === -1
    ? stringId
    : stringId.slice(namespaceSeparator + 1);
}

function getBlockLabel(block, blockId) {
  let fallback = titleFromPath(getPathFromId(blockId));
  return getTranslatedText(String(block.getDescriptionId()), fallback);
}

function getStructuralBlockText(blockId) {
  let path = getPathFromId(blockId).toLowerCase();

  if (path.indexOf("coil") !== -1) {
    return "sets the coil tier or processing heat for this shape.";
  }
  if (path.indexOf("casing") !== -1 || path.indexOf("case") !== -1) {
    return "forms a required casing layer in the structure.";
  }
  if (path.indexOf("glass") !== -1 || path.indexOf("window") !== -1) {
    return "marks the accepted window material for this pattern.";
  }
  if (path.indexOf("frame") !== -1) {
    return "fills support frame positions in the structure.";
  }
  if (path.indexOf("pipe") !== -1 || path.indexOf("tube") !== -1) {
    return "marks required pipe positions inside the pattern.";
  }
  if (path.indexOf("rotor") !== -1 || path.indexOf("turbine") !== -1) {
    return "marks the rotating machinery required by the pattern.";
  }
  if (path.indexOf("firebox") !== -1 || path.indexOf("burner") !== -1) {
    return "provides the firebox layer for this machine.";
  }
  if (path.indexOf("cleanroom") !== -1 || path.indexOf("filter") !== -1) {
    return "provides the controlled-environment blocks this shape expects.";
  }

  return "is one of the required block types in this preview.";
}

function getMachinePartText(partId) {
  let path = getPathFromId(partId).toLowerCase();

  if (path.indexOf("rotor") !== -1 || path.indexOf("turbine") !== -1) {
    return "handles turbine or rotor interaction for this multiblock.";
  }
  if (path.indexOf("hpca") !== -1 || path.indexOf("computation") !== -1) {
    return "contributes computation hardware required by this structure.";
  }
  if (path.indexOf("parallel") !== -1) {
    return "adds the parallel-processing part accepted by this pattern.";
  }

  return "is a special machine part accepted by this multiblock pattern.";
}

function getDynamicGroupKey(prefix, id) {
  return `${prefix}_${String(id)
    .toLowerCase()
    .replace(/[^a-z0-9_]+/g, "_")}`;
}

function getDynamicPalette(groups) {
  return GT_DYNAMIC_CALLOUT_PALETTES[
    groups.length % GT_DYNAMIC_CALLOUT_PALETTES.length
  ];
}

function getShapeBlockCount(shapeInfo) {
  let blocks = shapeInfo.getBlocks();
  let dimensions = getShapeDimensions(blocks);
  let blockCount = 0;

  for (let x = 0; x < dimensions.x; x++) {
    for (let y = 0; y < dimensions.y; y++) {
      for (let z = 0; z < dimensions.z; z++) {
        let blockInfo = blocks[x][y][z];
        if (blockInfo === null) {
          continue;
        }

        let state = blockInfo.getBlockState();
        if (!state.isAir()) {
          blockCount++;
        }
      }
    }
  }

  return blockCount;
}

function getMostCompleteShapeInfo(shapeInfos) {
  let selectedShape = null;
  let selectedBlockCount = -1;

  for (let i = 0; i < shapeInfos.size(); i++) {
    let shapeInfo = shapeInfos.get(i);
    let blockCount = getShapeBlockCount(shapeInfo);
    if (blockCount > selectedBlockCount) {
      selectedShape = shapeInfo;
      selectedBlockCount = blockCount;
    }
  }

  return selectedShape;
}

function getPrimaryShapeInfo(definition) {
  let explicitShapes = new $ArrayList(definition.getShapes().get());
  if (!explicitShapes.isEmpty()) {
    return getMostCompleteShapeInfo(explicitShapes);
  }

  let matchingShapes = new $ArrayList(definition.getMatchingShapes());
  if (matchingShapes.isEmpty()) {
    return null;
  }

  return getMostCompleteShapeInfo(matchingShapes);
}

function getShapeDimensions(blocks) {
  let x = blocks.length;
  let y = x > 0 ? blocks[0].length : 0;
  let z = y > 0 ? blocks[0][0].length : 0;

  return { x: x, y: y, z: z };
}

function getSceneScale(basePlateSize, height) {
  let maxDimension = Math.max(basePlateSize, height);

  if (maxDimension <= 5) {
    return 0.82;
  }
  if (maxDimension <= 8) {
    return 0.68;
  }
  if (maxDimension <= 12) {
    return 0.54;
  }
  if (maxDimension <= 16) {
    return 0.44;
  }
  if (maxDimension <= 20) {
    return 0.34;
  }
  return 0.28;
}

function selectionForPositions(util, positions) {
  let selection = null;

  positions.forEach((pos) => {
    let next = util.select.position(pos);
    selection = selection === null ? next : selection.add(next);
  });

  return selection;
}

function collectEntryPositions(entries) {
  let positions = [];

  entries.forEach((entry) => {
    positions.push(entry.pos);
  });

  return positions;
}

function getEntryBounds(entries) {
  if (entries.length === 0) {
    return null;
  }

  let bounds = {
    minX: Number.MAX_VALUE,
    minY: Number.MAX_VALUE,
    minZ: Number.MAX_VALUE,
    maxX: -Number.MAX_VALUE,
    maxY: -Number.MAX_VALUE,
    maxZ: -Number.MAX_VALUE,
  };

  entries.forEach((entry) => {
    let pos = entry.pos;
    bounds.minX = Math.min(bounds.minX, pos.getX());
    bounds.minY = Math.min(bounds.minY, pos.getY());
    bounds.minZ = Math.min(bounds.minZ, pos.getZ());
    bounds.maxX = Math.max(bounds.maxX, pos.getX());
    bounds.maxY = Math.max(bounds.maxY, pos.getY());
    bounds.maxZ = Math.max(bounds.maxZ, pos.getZ());
  });

  return bounds;
}

function selectionForEntryBounds(util, bounds) {
  if (bounds === null) {
    return null;
  }

  return util.select.fromTo(
    bounds.minX,
    bounds.minY,
    bounds.minZ,
    bounds.maxX,
    bounds.maxY,
    bounds.maxZ,
  );
}

function getShapeWorldBounds(dimensions, basePlateSize) {
  let xOffset = Math.floor((basePlateSize - dimensions.x) / 2);
  let zOffset = Math.floor((basePlateSize - dimensions.z) / 2);

  return {
    minX: xOffset,
    minY: 1,
    minZ: zOffset,
    maxX: xOffset + dimensions.x - 1,
    maxY: dimensions.y,
    maxZ: zOffset + dimensions.z - 1,
  };
}

function logDebugShape(machineId, dimensions, shape) {
  if (machineId !== "gtceu:mega_blast_furnace") {
    return;
  }

  console.info(
    `Rotary Hearth debug: dimensions=${dimensions.x}x${dimensions.y}x${dimensions.z}, entries=${shape.entries.length}, ` +
      `bounds=${shape.bounds.minX},${shape.bounds.minY},${shape.bounds.minZ}->${shape.bounds.maxX},${shape.bounds.maxY},${shape.bounds.maxZ}`,
  );
}

function warnIfShapeExceedsPonderStructure(
  machineId,
  dimensions,
  basePlateSize
) {
  if (
    basePlateSize <= GT_PONDER_STRUCTURE_SIZE &&
    dimensions.y + 1 <= GT_PONDER_STRUCTURE_HEIGHT
  ) {
    return;
  }

  console.warn(
    `GregTech multiblock Ponder scene for ${machineId} needs ${basePlateSize}x${dimensions.y + 1}x` +
      `${basePlateSize}, but ${GT_PONDER_STRUCTURE_ID} is only ${GT_PONDER_STRUCTURE_SIZE}x` +
      `${GT_PONDER_STRUCTURE_HEIGHT}x${GT_PONDER_STRUCTURE_SIZE}.`,
  );
}

function groupEntriesByState(entries) {
  let groupsByState = {};
  let groups = [];

  entries.forEach((entry) => {
    let key = String(entry.state);
    let group = groupsByState[key];
    if (group === undefined) {
      group = { state: entry.state, positions: [] };
      groupsByState[key] = group;
      groups.push(group);
    }

    group.positions.push(entry.pos);
  });

  return groups;
}

function setShapeEntryBlocks(scene, util, entries) {
  groupEntriesByState(entries).forEach((group) => {
    let selection = selectionForPositions(util, group.positions);
    if (selection !== null) {
      scene.world.setBlocks(selection, group.state, false);
    }
  });
}

function triggerPonderRerender(scene, util) {
  /*
   * GTCEu changes render state when a multiblock forms. Ponder does not always
   * notice that a block entity changed, so this harmless no-op replacement asks
   * Ponder to refresh scene rendering after formGeneratedMultiblock runs.
   */
  scene.addInstruction(
    new $ReplaceBlocksInstruction(
      util.select.fromTo(-100, -100, -100, -100, -100, -100),
      (state) => state,
      false,
      false,
    ),
  );
}

function formGeneratedMultiblock(scene, util, controllerPos, machineId) {
  /*
   * This mirrors the important part of in-world GTCEu multiblock formation.
   * The blocks are already placed by the scene timeline; this helper reaches
   * into the controller block entity, checks the pattern, and calls
   * onStructureFormed() so formed overlays/models can appear in Ponder.
   */
  if (controllerPos === null) {
    return;
  }

  scene.world.modifyBlockEntity(
    controllerPos,
    $MetaMachineBlockEntity,
    (blockEntity) => {
      try {
        let metaMachine = blockEntity.getMetaMachine();
        if (metaMachine === null) {
          console.warn(`GregTech multiblock Ponder scene for ${machineId} had no meta machine at controller.`);
          return;
        }

        let matched = metaMachine.checkPattern();
        if (!matched) {
          let pattern = metaMachine.getPattern();
          matched =
            pattern !== null &&
            pattern.checkPatternAt(metaMachine.getMultiblockState(), true);
        }

        if (matched) {
          metaMachine.onStructureFormed();
          console.info(
            `GregTech multiblock Ponder scene for ${machineId} formed=${metaMachine.isFormed()}.`,
          );
        } else {
          console.warn(`GregTech multiblock Ponder scene for ${machineId} did not match its pattern in Ponder.`);
        }
      } catch (error) {
        console.warn(`Failed to form GregTech multiblock in Ponder for ${machineId}: ${error}`);
      }
    }
  );

  triggerPonderRerender(scene, util);
}

function collectEntriesByPatternLayer(entries) {
  let layersByY = {};
  let layers = [];

  entries.forEach((entry) => {
    let layer = layersByY[entry.y];
    if (layer === undefined) {
      layer = { y: entry.y, entries: [] };
      layersByY[entry.y] = layer;
      layers.push(layer);
    }

    layer.entries.push(entry);
  });

  layers.sort((left, right) => left.y - right.y);
  return layers;
}

function getLayerScanDuration(layerCount) {
  if (layerCount <= 6) {
    return 56;
  }
  if (layerCount <= 12) {
    return 44;
  }
  if (layerCount <= 20) {
    return 34;
  }

  return 26;
}

function getLayerScanLabel(layerIndex, layerCount, layer) {
  return `Layer ${layerIndex + 1}/${layerCount} - Y ${layer.y + 1}`;
}

function blockCenterVector(util, pos) {
  return util.vector.of(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
}

function positionKeyFromCoords(x, y, z) {
  return `${x},${y},${z}`;
}

function positionKey(pos) {
  return positionKeyFromCoords(pos.getX(), pos.getY(), pos.getZ());
}

function hasOccupiedPosition(occupiedPositions, x, y, z) {
  if (occupiedPositions === null || occupiedPositions === undefined) {
    return false;
  }

  return occupiedPositions[positionKeyFromCoords(x, y, z)] === true;
}

function getDirectionName(direction) {
  return String(direction).toLowerCase();
}

function getMachineFacing(state) {
  if (state === null || state === undefined) {
    return null;
  }

  let block = state.getBlock();
  if (!isInstanceOfClassName(block, GT_META_MACHINE_BLOCK_CLASS_NAME)) {
    return null;
  }

  try {
    let rotationState = block.getRotationState();
    if (rotationState !== null && state.hasProperty(rotationState.property)) {
      return state.getValue(rotationState.property);
    }
  } catch (error) {
    // Some machine states use fixed/default facing properties; center outlines are still valid fallback.
  }

  return null;
}

function getRepresentativePosition(positions) {
  if (positions.length === 0) {
    return null;
  }

  let averageX = 0;
  let averageY = 0;
  let averageZ = 0;

  positions.forEach((pos) => {
    averageX += pos.getX();
    averageY += pos.getY();
    averageZ += pos.getZ();
  });

  averageX /= positions.length;
  averageY /= positions.length;
  averageZ /= positions.length;

  let representative = positions[0];
  let representativeDistance = Number.MAX_VALUE;

  positions.forEach((pos) => {
    let xDistance = pos.getX() - averageX;
    let yDistance = pos.getY() - averageY;
    let zDistance = pos.getZ() - averageZ;
    let distance =
      xDistance * xDistance + yDistance * yDistance + zDistance * zDistance;

    if (distance < representativeDistance) {
      representative = pos;
      representativeDistance = distance;
    }
  });

  return representative;
}

function getExposedPositionIndex(positions, basePlateSize) {
  if (positions.length === 0) {
    return -1;
  }
  if (positions.length === 1) {
    return 0;
  }

  let plateCenter = (basePlateSize - 1) / 2;
  let exposedIndex = 0;
  let exposedScore = -Number.MAX_VALUE;

  positions.forEach((pos, index) => {
    let xDistance = pos.getX() - plateCenter;
    let zDistance = pos.getZ() - plateCenter;
    let horizontalScore = xDistance * xDistance + zDistance * zDistance;
    let score = horizontalScore + pos.getY() * 0.05;

    if (score > exposedScore) {
      exposedIndex = index;
      exposedScore = score;
    }
  });

  return exposedIndex;
}

function getExposedPosition(positions, basePlateSize) {
  let index = getExposedPositionIndex(positions, basePlateSize);
  return index === -1 ? null : positions[index];
}

function getHorizontalExposureAngles(pos, occupiedPositions) {
  let x = pos.getX();
  let y = pos.getY();
  let z = pos.getZ();
  let angles = [];

  if (!hasOccupiedPosition(occupiedPositions, x - 1, y, z)) {
    angles.push(90);
  }
  if (!hasOccupiedPosition(occupiedPositions, x + 1, y, z)) {
    angles.push(-90);
  }
  if (!hasOccupiedPosition(occupiedPositions, x, y, z - 1)) {
    angles.push(180);
  }
  if (!hasOccupiedPosition(occupiedPositions, x, y, z + 1)) {
    angles.push(0);
  }

  return angles;
}

function getHorizontalExposureScore(pos, occupiedPositions) {
  return getHorizontalExposureAngles(pos, occupiedPositions).length;
}

function getVisiblePositionIndex(
  positions,
  shapeBounds,
  occupiedPositions,
  basePlateSize
) {
  if (positions.length === 0) {
    return -1;
  }
  if (positions.length === 1) {
    return 0;
  }

  let centerX =
    shapeBounds === null || shapeBounds === undefined
      ? basePlateSize / 2
      : (shapeBounds.minX + shapeBounds.maxX + 1) / 2;
  let centerZ =
    shapeBounds === null || shapeBounds === undefined
      ? basePlateSize / 2
      : (shapeBounds.minZ + shapeBounds.maxZ + 1) / 2;
  let visibleIndex = 0;
  let visibleScore = -Number.MAX_VALUE;

  positions.forEach((pos, index) => {
    let xDistance = pos.getX() + 0.5 - centerX;
    let zDistance = pos.getZ() + 0.5 - centerZ;
    let horizontalScore = xDistance * xDistance + zDistance * zDistance;
    let exposureScore = getHorizontalExposureScore(pos, occupiedPositions);
    let score = exposureScore * 1000 + horizontalScore + pos.getY() * 0.05;

    if (score > visibleScore) {
      visibleIndex = index;
      visibleScore = score;
    }
  });

  return visibleIndex;
}

function getVisiblePosition(
  positions,
  shapeBounds,
  occupiedPositions,
  basePlateSize
) {
  let index = getVisiblePositionIndex(
    positions,
    shapeBounds,
    occupiedPositions,
    basePlateSize,
  );
  return index === -1 ? null : positions[index];
}

function toFiniteNumber(value) {
  let numberValue = Number(value);
  if (
    isNaN(numberValue) ||
    numberValue === Infinity ||
    numberValue === -Infinity
  ) {
    return null;
  }

  return numberValue;
}

function normalizeDegrees(degrees) {
  let normalized = toFiniteNumber(degrees);
  if (normalized === null) {
    return null;
  }

  while (normalized <= -180) {
    normalized += 360;
  }

  while (normalized > 180) {
    normalized -= 360;
  }

  return normalized;
}

function quantizeCameraViewAngle(degrees) {
  let normalized = normalizeDegrees(degrees);
  if (normalized === null) {
    return null;
  }

  let bucket = toFiniteNumber(GT_CAMERA_VIEW_BUCKET_DEGREES);
  if (bucket === null || bucket <= 0) {
    return normalized;
  }

  return normalizeDegrees(Math.round(normalized / bucket) * bucket);
}

function getRadialViewAngleForPosition(pos, shapeBounds, basePlateSize) {
  let x = toFiniteNumber(pos.getX());
  let z = toFiniteNumber(pos.getZ());
  let plateSize = toFiniteNumber(basePlateSize);
  if (x === null || z === null || plateSize === null || plateSize <= 0) {
    return null;
  }

  let centerX =
    shapeBounds === null || shapeBounds === undefined
      ? plateSize / 2
      : (shapeBounds.minX + shapeBounds.maxX + 1) / 2;
  let centerZ =
    shapeBounds === null || shapeBounds === undefined
      ? plateSize / 2
      : (shapeBounds.minZ + shapeBounds.maxZ + 1) / 2;
  let xDistance = x + 0.5 - centerX;
  let zDistance = z + 0.5 - centerZ;

  if (Math.abs(xDistance) + Math.abs(zDistance) < 0.75) {
    return null;
  }

  let positionAngle = toFiniteNumber(
    Math.atan2(xDistance, zDistance) * (180 / Math.PI),
  );
  return positionAngle === null
    ? null
    : quantizeCameraViewAngle(-positionAngle);
}

function getViewAngleForPosition(pos, basePlateSize) {
  return getRadialViewAngleForPosition(pos, null, basePlateSize);
}

function getViewAngleForCalloutPosition(
  pos,
  shapeBounds,
  occupiedPositions,
  basePlateSize
) {
  let exposureAngles = getHorizontalExposureAngles(pos, occupiedPositions);
  let radialAngle = getRadialViewAngleForPosition(
    pos,
    shapeBounds,
    basePlateSize,
  );

  if (exposureAngles.length === 0) {
    return radialAngle;
  }

  if (exposureAngles.length === 1) {
    return exposureAngles[0];
  }

  return radialAngle === null ? exposureAngles[0] : radialAngle;
}

function getViewAngleForFacing(facing) {
  if (facing === null) {
    return null;
  }

  switch (getDirectionName(facing)) {
    case "north":
      return 180;
    case "south":
      return 0;
    case "west":
      return 90;
    case "east":
      return -90;
    default:
      return null;
  }
}

function getRotationSettleTime(rotation) {
  let safeRotation = toFiniteNumber(rotation);
  if (safeRotation === null) {
    return GT_PART_ROTATION_MIN_SETTLE;
  }

  let settleTime = Math.ceil(Math.abs(safeRotation) / 15);

  if (settleTime < GT_PART_ROTATION_MIN_SETTLE) {
    return GT_PART_ROTATION_MIN_SETTLE;
  }

  if (settleTime > GT_PART_ROTATION_MAX_SETTLE) {
    return GT_PART_ROTATION_MAX_SETTLE;
  }

  return settleTime;
}

function getPartCallout(path) {
  for (let i = 0; i < GT_PART_CALLOUTS.length; i++) {
    let callout = GT_PART_CALLOUTS[i];
    if (callout.match(path)) {
      return callout;
    }
  }

  return null;
}

function createPartGroups() {
  let groups = [];

  GT_PART_CALLOUTS.forEach((callout) => {
    groups.push({
      key: callout.key,
      palette: callout.palette,
      label: callout.label,
      text: callout.text,
      showCount: false,
      machinePart: true,
      positions: [],
      states: [],
    });
  });

  return groups;
}

function addPartGroupPosition(groups, key, pos, state) {
  for (let i = 0; i < groups.length; i++) {
    if (groups[i].key === key) {
      groups[i].positions.push(pos);
      groups[i].states.push(state);
      return;
    }
  }
}

function getOrCreateDynamicGroup(
  groups,
  key,
  palette,
  label,
  text,
  machinePart
) {
  let existingGroup = getPartGroup(groups, key);
  if (existingGroup !== null) {
    return existingGroup;
  }

  let group = {
    key: key,
    palette: palette,
    label: label,
    text: text,
    showCount: true,
    machinePart: machinePart,
    positions: [],
    states: [],
  };

  groups.push(group);
  return group;
}

function addDynamicGroupPosition(
  groups,
  key,
  palette,
  label,
  text,
  machinePart,
  pos,
  state
) {
  let group = getOrCreateDynamicGroup(
    groups,
    key,
    palette,
    label,
    text,
    machinePart,
  );
  group.positions.push(pos);
  group.states.push(state);
}

function sortShapeEntries(entries) {
  entries.sort((left, right) => {
    if (left.y !== right.y) {
      return left.y - right.y;
    }
    if (left.z !== right.z) {
      return left.z - right.z;
    }
    return left.x - right.x;
  });
}

function getBlockRevealBatchSize(blockCount) {
  if (blockCount <= 96) {
    return 1;
  }

  return Math.max(2, Math.ceil(blockCount / GT_STRUCTURE_REVEAL_TARGET_TICKS));
}

function getBlockRevealIdle(blockCount) {
  return blockCount <= 160 ? 2 : 1;
}

function getPartGroup(partGroups, key) {
  for (let i = 0; i < partGroups.length; i++) {
    if (partGroups[i].key === key) {
      return partGroups[i];
    }
  }

  return null;
}

function normalizeFocusList(focus) {
  return Array.isArray(focus) ? focus : [focus];
}

function getFocusNeedle(focus, prefix) {
  return focus
    .slice(prefix.length)
    .toLowerCase()
    .replace(/[^a-z0-9_]+/g, "_");
}

function textMatchesNeedle(text, needle) {
  let lowerText = String(text).toLowerCase();
  let lowerNeedle = String(needle).toLowerCase();

  return (
    lowerText.indexOf(lowerNeedle) !== -1 ||
    lowerText.indexOf(lowerNeedle.replace(/_/g, " ")) !== -1
  );
}

function groupMatchesFocus(group, focus) {
  if (
    group === null ||
    group === undefined ||
    focus === null ||
    focus === undefined
  ) {
    return false;
  }

  let focusText = String(focus).toLowerCase();

  if (focusText.indexOf("part:") === 0) {
    return group.key === getFocusNeedle(focusText, "part:");
  }

  if (focusText.indexOf("structure:") === 0) {
    let needle = getFocusNeedle(focusText, "structure:");
    return (
      !group.machinePart &&
      (textMatchesNeedle(group.key, needle) ||
        textMatchesNeedle(group.label, needle))
    );
  }

  if (focusText.indexOf("machine:") === 0) {
    let machineNeedle = getFocusNeedle(focusText, "machine:");
    return (
      group.machinePart &&
      (textMatchesNeedle(group.key, machineNeedle) ||
        textMatchesNeedle(group.label, machineNeedle))
    );
  }

  switch (focusText) {
    case "inputs":
      return group.key === "item_input" || group.key === "fluid_input";
    case "outputs":
      return (
        group.key === "item_output" ||
        group.key === "fluid_output" ||
        group.key === "energy_output"
      );
    case "power":
      return (
        group.key === "energy_input" ||
        group.key === "energy_output" ||
        group.key === "laser"
      );
    default:
      return (
        group.key === focusText ||
        textMatchesNeedle(group.key, focusText) ||
        textMatchesNeedle(group.label, focusText)
      );
  }
}

function getAllCalloutGroups(shape) {
  return shape.partGroups.concat(shape.structureGroups);
}

function collectFocusGroups(shape, focus) {
  let groups = [];
  let focusList = normalizeFocusList(focus);
  let allGroups = getAllCalloutGroups(shape);

  focusList.forEach((focusEntry) => {
    if (focusEntry === "controller") {
      if (shape.controllerPos !== null) {
        groups.push({
          key: "controller",
          label: "Controller",
          machinePart: true,
          positions: [shape.controllerPos],
          states: [],
        });
      }
      return;
    }

    allGroups.forEach((group) => {
      if (
        group.positions.length > 0 &&
        groupMatchesFocus(group, focusEntry) &&
        groups.indexOf(group) === -1
      ) {
        groups.push(group);
      }
    });
  });

  return groups;
}

function hasAnyFocus(shape, focus) {
  let focusList = normalizeFocusList(focus);

  for (let i = 0; i < focusList.length; i++) {
    if (collectFocusGroups(shape, focusList[i]).length > 0) {
      return true;
    }
  }

  return false;
}

function getRepresentativePartPosition(partGroups, keys) {
  for (let i = 0; i < keys.length; i++) {
    let group = getPartGroup(partGroups, keys[i]);
    if (group !== null && group.positions.length > 0) {
      return getRepresentativePosition(group.positions);
    }
  }

  return null;
}

function collectShapeEntries(definition, blocks, dimensions, basePlateSize) {
  /*
   * Convert GTCEu preview shape coordinates into Ponder world coordinates.
   *
   * GTCEu shape arrays are indexed from their own local origin. Ponder scenes
   * need actual BlockPos values inside the backing NBT. The x/z offsets center
   * the machine on the configured base plate, and y + 1 leaves y=0 available
   * for the visible base plate.
   *
   * While walking the shape, this function also builds the semantic groups used
   * later by tooltips and mechanic animations.
   */
  let xOffset = Math.floor((basePlateSize - dimensions.x) / 2);
  let zOffset = Math.floor((basePlateSize - dimensions.z) / 2);
  let entries = [];
  let occupiedPositions = {};
  let machinePartPositions = [];
  let partGroups = createPartGroups();
  let structureGroups = [];
  let controllerPos = null;

  for (let x = 0; x < dimensions.x; x++) {
    for (let y = 0; y < dimensions.y; y++) {
      for (let z = 0; z < dimensions.z; z++) {
        let blockInfo = blocks[x][y][z];
        if (blockInfo === null) {
          continue;
        }

        let state = blockInfo.getBlockState();
        if (state.isAir()) {
          continue;
        }

        let pos = new $BlockPos(x + xOffset, y + 1, z + zOffset);
        occupiedPositions[positionKey(pos)] = true;
        let block = state.getBlock();

        if (isInstanceOfClassName(block, GT_META_MACHINE_BLOCK_CLASS_NAME)) {
          if (block.getDefinition().getId().equals(definition.getId())) {
            controllerPos = pos;
          } else {
            let partDefinition = block.getDefinition();
            let partId = String(partDefinition.getId());
            let partIdPath = String(partDefinition.getId().getPath());
            let callout = getPartCallout(partIdPath);
            if (callout === null) {
              addDynamicGroupPosition(
                partGroups,
                getDynamicGroupKey("machine", partId),
                getDynamicPalette(partGroups),
                getTranslatedMachineName(partDefinition),
                getMachinePartText(partId),
                true,
                pos,
                state,
              );
            } else {
              addPartGroupPosition(partGroups, callout.key, pos, state);
            }
            machinePartPositions.push(pos);
          }
        } else {
          let blockId = getBlockRegistryId(block);
          addDynamicGroupPosition(
            structureGroups,
            getDynamicGroupKey("block", blockId),
            getDynamicPalette(structureGroups),
            getBlockLabel(block, blockId),
            getStructuralBlockText(blockId),
            false,
            pos,
            state,
          );
        }

        entries.push({ pos: pos, state: state, x: x, y: y, z: z });
      }
    }
  }

  sortShapeEntries(entries);

  return {
    entries: entries,
    occupiedPositions: occupiedPositions,
    bounds: getShapeWorldBounds(dimensions, basePlateSize),
    controllerPos: controllerPos,
    machinePartPositions: machinePartPositions,
    partGroups: partGroups,
    structureGroups: structureGroups,
  };
}

function revealShapeBlocks(scene, util, entries, shapeBounds) {
  /*
   * Ponder can show a full structure instantly, but GT multiblocks are easier
   * to learn when the preview assembles in order. The entries are already
   * sorted bottom-to-top, so batched placement produces a readable build-up
   * without spending hundreds of ticks on very large machines.
   */
  let batchSize = getBlockRevealBatchSize(entries.length);
  let idleTime = getBlockRevealIdle(entries.length);
  let bounds =
    shapeBounds === null || shapeBounds === undefined
      ? getEntryBounds(entries)
      : shapeBounds;
  let fullSelection = selectionForEntryBounds(util, bounds);
  let batchEntries = [];

  if (fullSelection === null) {
    return;
  }

  scene.world.setBlocks(fullSelection, $Blocks.AIR.defaultBlockState(), false);
  scene.world.showIndependentSectionImmediately(fullSelection);
  scene.idle(idleTime);

  entries.forEach((entry, index) => {
    batchEntries.push(entry);

    if (batchEntries.length === batchSize || index === entries.length - 1) {
      setShapeEntryBlocks(scene, util, batchEntries);
      batchEntries = [];
      scene.idle(idleTime);
    }
  });

  scene.idle(GT_STRUCTURE_REVEAL_SETTLE);
}

function showLayerScan(scene, util, machineId, shape, dimensions) {
  /*
   * Ponder scenes are not a true interactive layer viewer, so this generated
   * companion scene approximates one by showing only one Y layer at a time.
   * It is especially useful for tall, hollow, or asymmetric machines.
   */
  let layers = collectEntriesByPatternLayer(shape.entries);
  let fullSelection = selectionForEntryBounds(util, shape.bounds);
  let duration = getLayerScanDuration(layers.length);

  if (fullSelection === null || layers.length === 0) {
    return;
  }

  scene.world.setBlocks(fullSelection, $Blocks.AIR.defaultBlockState(), false);
  scene.world.showIndependentSectionImmediately(fullSelection);
  scene.idle(8);

  layers.forEach((layer, index) => {
    let layerBounds = getEntryBounds(layer.entries);
    let layerSelection = selectionForEntryBounds(util, layerBounds);
    let positions = collectEntryPositions(layer.entries);
    let representativePosition = getRepresentativePosition(positions);

    scene.world.setBlocks(fullSelection, $Blocks.AIR.defaultBlockState(), false);
    setShapeEntryBlocks(scene, util, layer.entries);

    if (layerSelection !== null && representativePosition !== null) {
      scene.overlay
        .showOutlineWithText(layerSelection, duration)
        .text(getLayerScanLabel(index, layers.length, layer))
        .colored($PonderPalette.BLUE)
        .pointAt(blockCenterVector(util, representativePosition))
        .placeNearTarget();
    }

    scene.idle(index === 0 ? duration + 16 : duration);
  });

  scene.world.setBlocks(fullSelection, $Blocks.AIR.defaultBlockState(), false);
  setShapeEntryBlocks(scene, util, shape.entries);
  formGeneratedMultiblock(scene, util, shape.controllerPos, machineId);
  scene.idle(12);

  if (shape.controllerPos !== null) {
    scene.overlay
      .showText(GT_LAYER_SCAN_FINAL_DURATION)
      .text(`Complete ${dimensions.x}x${dimensions.y}x${dimensions.z} structure.`)
      .colored($PonderPalette.GREEN)
      .pointAt(blockCenterVector(util, shape.controllerPos))
      .placeNearTarget();
  }

  scene.idle(GT_LAYER_SCAN_FINAL_DURATION);
}

function configureGeneratedScene(scene, basePlateSize, dimensions) {
  scene.configureBasePlate(0, 0, basePlateSize);
  scene.scaleSceneView(getSceneScale(basePlateSize, dimensions.y + 1));
  if (dimensions.y > 14) {
    scene.setSceneOffsetY(-2);
  } else if (dimensions.y > 10) {
    scene.setSceneOffsetY(-1.25);
  } else if (dimensions.y > 8) {
    scene.setSceneOffsetY(-1);
  } else if (dimensions.y > 5) {
    scene.setSceneOffsetY(-0.5);
  }
}

function showControllerCallout(
  scene,
  util,
  machineId,
  controllerPos,
  duration
) {
  if (controllerPos === null) {
    return false;
  }

  scene.overlay.showOutline(
    $PonderPalette.RED,
    `${machineId}/controller`,
    util.select.position(controllerPos),
    duration,
  );
  scene.overlay
    .showText(duration)
    .text("The controller anchors the preview shape.")
    .colored($PonderPalette.RED)
    .pointAt(blockCenterVector(util, controllerPos))
    .placeNearTarget();

  return true;
}

function getSceneCalloutGroups(shape) {
  return getAllCalloutGroups(shape);
}

function collectPartCalloutPresentations(
  util,
  partGroups,
  shapeBounds,
  occupiedPositions,
  basePlateSize
) {
  /*
   * A presentation is the final data needed for one tooltip: what to outline,
   * where the text points, which color to use, and which camera angle should
   * show the target clearly.
   */
  let presentations = [];

  partGroups.forEach((partGroup) => {
    if (partGroup.positions.length === 0) {
      return;
    }

    let representativeIndex = getVisiblePositionIndex(
      partGroup.positions,
      shapeBounds,
      occupiedPositions,
      basePlateSize,
    );
    if (representativeIndex === -1) {
      return;
    }

    let representativePosition = partGroup.positions[representativeIndex];
    if (representativePosition === null) {
      return;
    }
    let viewAngle = getViewAngleForCalloutPosition(
      representativePosition,
      shapeBounds,
      occupiedPositions,
      basePlateSize,
    );

    presentations.push({
      key: partGroup.key,
      palette: partGroup.palette,
      label: partGroup.label,
      text: partGroup.text,
      count: partGroup.positions.length,
      showCount: partGroup.showCount,
      machinePart: partGroup.machinePart,
      pointAt: blockCenterVector(util, representativePosition),
      selection: util.select.position(representativePosition),
      representativePosition: representativePosition,
      viewAngle: viewAngle,
    });
  });

  return presentations;
}

function rotateTowardCallout(scene, currentCameraAngle, presentation) {
  /*
   * Camera instructions are absolute targets here, not blind relative nudges.
   * We still compute the shortest equivalent path from the current angle so
   * crossing -180/180 does not make the camera spin the long way around.
   */
  let cameraAngle = toFiniteNumber(currentCameraAngle);
  if (cameraAngle === null) {
    cameraAngle = GT_DEFAULT_CAMERA_Y_ROTATION;
  }

  let viewAngle = toFiniteNumber(presentation.viewAngle);
  if (viewAngle === null) {
    return cameraAngle;
  }

  let rotation = normalizeDegrees(viewAngle - cameraAngle);
  if (rotation === null || Math.abs(rotation) < GT_PART_ROTATION_MINIMUM) {
    return cameraAngle;
  }

  let nextCameraAngle = cameraAngle + rotation;

  scene.addInstruction(
    new $RotateSceneInstruction(
      GT_DEFAULT_CAMERA_X_ROTATION,
      nextCameraAngle,
      false,
    ),
  );
  scene.idle(getRotationSettleTime(rotation));

  return nextCameraAngle;
}

function getCalloutText(presentation) {
  if (presentation.showCount) {
    return `${presentation.count}x ${presentation.label}: ${presentation.text}`;
  }

  return `${presentation.label} ${presentation.text}`;
}

function collectPositionsFromGroups(groups) {
  let positions = [];

  groups.forEach((group) => {
    group.positions.forEach((pos) => {
      positions.push(pos);
    });
  });

  return positions;
}

function sameBlockPosition(left, right) {
  if (left === null || right === null) {
    return false;
  }

  return (
    left.getX() === right.getX() &&
    left.getY() === right.getY() &&
    left.getZ() === right.getZ()
  );
}

function getRepresentativeFocusPosition(shape, focus, basePlateSize) {
  let groups = collectFocusGroups(shape, focus);
  let positions = collectPositionsFromGroups(groups);
  let exposedPosition = getVisiblePosition(
    positions,
    shape.bounds,
    shape.occupiedPositions,
    basePlateSize,
  );

  return exposedPosition === null
    ? getRepresentativePosition(positions)
    : exposedPosition;
}

function collectMechanicPresentation(util, shape, step, basePlateSize) {
  let groups = collectFocusGroups(shape, step.focus);
  let positions = collectPositionsFromGroups(groups);
  let representativePosition = getVisiblePosition(
    positions,
    shape.bounds,
    shape.occupiedPositions,
    basePlateSize,
  );

  if (representativePosition === null) {
    representativePosition = getRepresentativePosition(positions);
  }

  if (representativePosition === null) {
    return null;
  }

  return {
    key: step.key,
    palette: step.palette,
    text: step.text,
    animation: step.animation,
    groups: groups,
    representativePosition: representativePosition,
    pointAt: blockCenterVector(util, representativePosition),
    selection: util.select.position(representativePosition),
    viewAngle: getViewAngleForCalloutPosition(
      representativePosition,
      shape.bounds,
      shape.occupiedPositions,
      basePlateSize,
    ),
  };
}

function showBigLineBetweenPositions(
  scene,
  util,
  palette,
  fromPos,
  toPos,
  duration
) {
  if (fromPos === null || toPos === null || sameBlockPosition(fromPos, toPos)) {
    return;
  }

  scene.overlay.showBigLine(
    palette,
    blockCenterVector(util, fromPos),
    blockCenterVector(util, toPos),
    duration,
  );
}

function showFlowFromFocusToController(
  scene,
  util,
  shape,
  focus,
  palette,
  basePlateSize
) {
  if (shape.controllerPos === null) {
    return;
  }

  collectFocusGroups(shape, focus).forEach((group) => {
    let pos = getVisiblePosition(
      group.positions,
      shape.bounds,
      shape.occupiedPositions,
      basePlateSize,
    );
    if (pos === null) {
      pos = getRepresentativePosition(group.positions);
    }
    showBigLineBetweenPositions(
      scene,
      util,
      palette,
      pos,
      shape.controllerPos,
      GT_MECHANIC_FLOW_LINE_DURATION,
    );
  });
}

function showFlowFromControllerToFocus(
  scene,
  util,
  shape,
  focus,
  palette,
  basePlateSize
) {
  if (shape.controllerPos === null) {
    return;
  }

  collectFocusGroups(shape, focus).forEach((group) => {
    let pos = getVisiblePosition(
      group.positions,
      shape.bounds,
      shape.occupiedPositions,
      basePlateSize,
    );
    if (pos === null) {
      pos = getRepresentativePosition(group.positions);
    }
    showBigLineBetweenPositions(
      scene,
      util,
      palette,
      shape.controllerPos,
      pos,
      GT_MECHANIC_FLOW_LINE_DURATION,
    );
  });
}

function pulseFocusGroups(scene, groups, shape, basePlateSize, maxPulses) {
  let pulses = 0;

  groups.forEach((group) => {
    if (pulses >= maxPulses) {
      return;
    }

    let pos = getVisiblePosition(
      group.positions,
      shape.bounds,
      shape.occupiedPositions,
      basePlateSize,
    );
    if (pos === null) {
      pos = getRepresentativePosition(group.positions);
    }

    if (pos !== null) {
      scene.effects.indicateSuccess(pos);
      pulses++;
    }
  });
}

function showMechanicAnimation(
  scene,
  util,
  shape,
  presentation,
  basePlateSize
) {
  switch (presentation.animation) {
    case "automation":
      showFlowFromFocusToController(
        scene,
        util,
        shape,
        "inputs",
        $PonderPalette.INPUT,
        basePlateSize,
      );
      showFlowFromFocusToController(
        scene,
        util,
        shape,
        ["part:energy_input", "part:laser"],
        $PonderPalette.GREEN,
        basePlateSize,
      );
      showFlowFromControllerToFocus(
        scene,
        util,
        shape,
        "outputs",
        $PonderPalette.OUTPUT,
        basePlateSize,
      );
      break;
    case "power":
      showFlowFromFocusToController(
        scene,
        util,
        shape,
        ["part:energy_input", "part:laser"],
        $PonderPalette.GREEN,
        basePlateSize,
      );
      showFlowFromControllerToFocus(
        scene,
        util,
        shape,
        "part:energy_output",
        $PonderPalette.GREEN,
        basePlateSize,
      );
      break;
    case "input":
      showFlowFromFocusToController(
        scene,
        util,
        shape,
        "inputs",
        $PonderPalette.INPUT,
        basePlateSize,
      );
      break;
    case "output":
      showFlowFromControllerToFocus(
        scene,
        util,
        shape,
        ["outputs", "part:muffler"],
        $PonderPalette.OUTPUT,
        basePlateSize,
      );
      break;
    case "vertical_outputs":
      showFlowFromFocusToController(
        scene,
        util,
        shape,
        "part:fluid_input",
        $PonderPalette.INPUT,
        basePlateSize,
      );
      showFlowFromControllerToFocus(
        scene,
        util,
        shape,
        ["part:fluid_output", "part:item_output"],
        $PonderPalette.OUTPUT,
        basePlateSize,
      );
      break;
    case "data":
      showFlowFromFocusToController(
        scene,
        util,
        shape,
        "part:data",
        $PonderPalette.MEDIUM,
        basePlateSize,
      );
      pulseFocusGroups(scene, presentation.groups, shape, basePlateSize, 3);
      break;
    case "heat":
      showBigLineBetweenPositions(
        scene,
        util,
        $PonderPalette.SLOW,
        presentation.representativePosition,
        shape.controllerPos,
        GT_MECHANIC_FLOW_LINE_DURATION,
      );
      pulseFocusGroups(scene, presentation.groups, shape, basePlateSize, 4);
      break;
    case "cooling":
      showBigLineBetweenPositions(
        scene,
        util,
        $PonderPalette.BLUE,
        presentation.representativePosition,
        shape.controllerPos,
        GT_MECHANIC_FLOW_LINE_DURATION,
      );
      pulseFocusGroups(scene, presentation.groups, shape, basePlateSize, 4);
      break;
    default:
      pulseFocusGroups(scene, presentation.groups, shape, basePlateSize, 3);
      break;
  }
}

function getGenericMechanicSteps(shape) {
  let steps = [];

  GT_GENERIC_MECHANIC_RULES.forEach((rule) => {
    if (rule.when(shape)) {
      steps.push(rule);
    }
  });

  return steps;
}

function getMachineSpecificMechanicSteps(machineId, machinePath) {
  let steps = [];
  let exactSteps =
    GT_MACHINE_MECHANIC_STEPS[machineId] ||
    GT_MACHINE_MECHANIC_STEPS[machinePath];

  if (exactSteps !== undefined) {
    steps = steps.concat(exactSteps);
  }

  GT_MACHINE_MECHANIC_PATTERNS.forEach((pattern) => {
    if (pattern.match(machinePath, machineId)) {
      steps = steps.concat(pattern.steps);
    }
  });

  return steps;
}

function mergeMechanicSteps(genericSteps, machineSteps) {
  let merged = [];
  let indexesByKey = {};

  function addOrReplace(step) {
    let key = step.key;
    if (indexesByKey[key] !== undefined) {
      merged[indexesByKey[key]] = step;
      return;
    }

    indexesByKey[key] = merged.length;
    merged.push(step);
  }

  genericSteps.forEach(addOrReplace);
  machineSteps.forEach(addOrReplace);

  return merged.slice(0, GT_MAX_MECHANIC_STEPS);
}

function getMechanicSteps(machineId, machinePath, shape) {
  return mergeMechanicSteps(
    getGenericMechanicSteps(shape),
    getMachineSpecificMechanicSteps(machineId, machinePath),
  );
}

function showMechanicSteps(
  scene,
  util,
  shape,
  steps,
  basePlateSize,
  currentCameraAngle
) {
  /*
   * Mechanic steps are the "why this block matters" layer. They run after the
   * structure is visible, use the same focus/group system as part callouts, and
   * can add animations such as flow lines or pulses.
   */
  let cameraAngle = currentCameraAngle;
  let shownSteps = 0;

  steps.forEach((step) => {
    let presentation = collectMechanicPresentation(
      util,
      shape,
      step,
      basePlateSize,
    );
    if (presentation === null) {
      return;
    }

    cameraAngle = rotateTowardCallout(scene, cameraAngle, presentation);
    showMechanicAnimation(scene, util, shape, presentation, basePlateSize);
    scene.overlay
      .showOutlineWithText(presentation.selection, GT_MECHANIC_CALLOUT_DURATION)
      .text(presentation.text)
      .colored(presentation.palette)
      .pointAt(presentation.pointAt)
      .placeNearTarget();

    scene.idle(GT_MECHANIC_CALLOUT_IDLE);
    shownSteps++;
  });

  return { count: shownSteps, cameraAngle: cameraAngle };
}

function showPartCallouts(
  scene,
  util,
  machineId,
  shape,
  partGroups,
  basePlateSize,
  currentCameraAngle
) {
  let presentations = collectPartCalloutPresentations(
    util,
    partGroups,
    shape.bounds,
    shape.occupiedPositions,
    basePlateSize,
  );
  let cameraAngle = currentCameraAngle;

  presentations.forEach((presentation) => {
    cameraAngle = rotateTowardCallout(scene, cameraAngle, presentation);
    if (presentation.machinePart) {
      scene.effects.indicateSuccess(presentation.representativePosition);
    }
    scene.overlay
      .showOutlineWithText(presentation.selection, GT_PART_CALLOUT_DURATION)
      .text(getCalloutText(presentation))
      .colored(presentation.palette)
      .pointAt(presentation.pointAt)
      .placeNearTarget();

    scene.idle(GT_PART_CALLOUT_IDLE);
  });

  return { count: presentations.length, cameraAngle: cameraAngle };
}

function showAutomationFlowLines(scene, util, shape) {
  if (shape.controllerPos === null) {
    return;
  }

  let controllerCenter = blockCenterVector(util, shape.controllerPos);
  let itemInputPos = getRepresentativePartPosition(shape.partGroups, [
    "item_input",
  ]);
  let fluidInputPos = getRepresentativePartPosition(shape.partGroups, [
    "fluid_input",
  ]);
  let energyInputPos = getRepresentativePartPosition(shape.partGroups, [
    "energy_input",
    "laser",
  ]);
  let itemOutputPos = getRepresentativePartPosition(shape.partGroups, [
    "item_output",
  ]);
  let fluidOutputPos = getRepresentativePartPosition(shape.partGroups, [
    "fluid_output",
  ]);
  let energyOutputPos = getRepresentativePartPosition(shape.partGroups, [
    "energy_output",
  ]);

  if (itemInputPos !== null) {
    scene.overlay.showLine(
      $PonderPalette.INPUT,
      blockCenterVector(util, itemInputPos),
      controllerCenter,
      GT_FLOW_LINE_DURATION,
    );
  }
  if (fluidInputPos !== null) {
    scene.overlay.showLine(
      $PonderPalette.INPUT,
      blockCenterVector(util, fluidInputPos),
      controllerCenter,
      GT_FLOW_LINE_DURATION,
    );
  }
  if (energyInputPos !== null) {
    scene.overlay.showLine(
      $PonderPalette.GREEN,
      blockCenterVector(util, energyInputPos),
      controllerCenter,
      GT_FLOW_LINE_DURATION,
    );
  }
  if (itemOutputPos !== null) {
    scene.overlay.showLine(
      $PonderPalette.OUTPUT,
      controllerCenter,
      blockCenterVector(util, itemOutputPos),
      GT_FLOW_LINE_DURATION,
    );
  }
  if (fluidOutputPos !== null) {
    scene.overlay.showLine(
      $PonderPalette.OUTPUT,
      controllerCenter,
      blockCenterVector(util, fluidOutputPos),
      GT_FLOW_LINE_DURATION,
    );
  }
  if (energyOutputPos !== null) {
    scene.overlay.showLine(
      $PonderPalette.GREEN,
      controllerCenter,
      blockCenterVector(util, energyOutputPos),
      GT_FLOW_LINE_DURATION,
    );
  }
}

function addGeneratedMultiblockScene(event, definition) {
  /*
   * Main scene factory. Everything above this function prepares reusable data
   * or draws one kind of scene element. This function decides whether a GTCEu
   * definition is usable, then registers the overview and layer-scan scenes.
   */
  let machineId = String(definition.getId());
  let machinePath = String(definition.getId().getPath());
  let machineName = getTranslatedMachineName(definition);
  let shapeInfo = getPrimaryShapeInfo(definition);
  if (shapeInfo === null) {
    return false;
  }

  let blocks = shapeInfo.getBlocks();
  let dimensions = getShapeDimensions(blocks);
  if (dimensions.x === 0 || dimensions.y === 0 || dimensions.z === 0) {
    return false;
  }

  let basePlateSize = Math.max(3, dimensions.x, dimensions.z);
  let shape = collectShapeEntries(
    definition,
    blocks,
    dimensions,
    basePlateSize,
  );
  if (shape.entries.length === 0) {
    return false;
  }
  logDebugShape(machineId, dimensions, shape);
  warnIfShapeExceedsPonderStructure(machineId, dimensions, basePlateSize);

  let sceneBuilder = event.create(machineId);

  sceneBuilder.scene(
    `${GT_PONDER_SCENE_PREFIX}${machinePath}`,
    machineName,
    GT_PONDER_STRUCTURE_ID,
    (scene, util) => {
      configureGeneratedScene(scene, basePlateSize, dimensions);
      scene.showBasePlate();
      scene.idle(3);
      revealShapeBlocks(scene, util, shape.entries, shape.bounds);
      formGeneratedMultiblock(scene, util, shape.controllerPos, machineId);
      scene.idle(14);
      scene.rotateCameraY(GT_INITIAL_CAMERA_Y_ROTATION);
      let cameraAngle = normalizeDegrees(
        GT_DEFAULT_CAMERA_Y_ROTATION + GT_INITIAL_CAMERA_Y_ROTATION,
      );
      scene.idle(20);

      showAutomationFlowLines(scene, util, shape);
      scene.idle(GT_FLOW_LINE_DURATION);

      if (
        showControllerCallout(
          scene,
          util,
          machineId,
          shape.controllerPos,
          GT_CONTROLLER_CALLOUT_DURATION,
        )
      ) {
        scene.idle(GT_CONTROLLER_CALLOUT_DURATION);
      }

      let mechanicResult = showMechanicSteps(
        scene,
        util,
        shape,
        getMechanicSteps(machineId, machinePath, shape),
        basePlateSize,
        cameraAngle,
      );
      cameraAngle = mechanicResult.cameraAngle;

      let calloutResult = showPartCallouts(
        scene,
        util,
        machineId,
        shape,
        getSceneCalloutGroups(shape),
        basePlateSize,
        cameraAngle,
      );
      cameraAngle = calloutResult.cameraAngle;

      if (calloutResult.count === 0) {
        let machinePartSelection = selectionForPositions(
          util,
          shape.machinePartPositions,
        );
        let representativePosition = getRepresentativePosition(
          shape.machinePartPositions,
        );
        if (machinePartSelection !== null && representativePosition !== null) {
          scene.overlay.showOutline(
            $PonderPalette.BLUE,
            `${machineId}/parts`,
            machinePartSelection,
            GT_PART_CALLOUT_DURATION,
          );
          scene.overlay
            .showText(GT_PART_CALLOUT_DURATION)
            .text(
              "Blue highlights show the preview's buses, hatches, and machine parts.",
            )
            .colored($PonderPalette.BLUE)
            .pointAt(blockCenterVector(util, representativePosition))
            .placeNearTarget();
          scene.idle(GT_PART_CALLOUT_IDLE);
        }
      }

      scene.rotateCameraY(GT_INITIAL_CAMERA_Y_ROTATION);
      scene.idle(29);
      scene.markAsFinished();
    },
  );

  sceneBuilder.scene(
    `${GT_PONDER_SCENE_PREFIX}${machinePath}_layers`,
    `${machineName}: Layers`,
    GT_PONDER_STRUCTURE_ID,
    (scene, util) => {
      configureGeneratedScene(scene, basePlateSize, dimensions);
      scene.showBasePlate();
      scene.idle(3);
      scene.rotateCameraY(GT_INITIAL_CAMERA_Y_ROTATION);
      scene.idle(12);
      showLayerScan(scene, util, machineId, shape, dimensions);
      scene.idle(16);
      scene.markAsFinished();
    },
  );

  return true;
}

Ponder.registry((event) => {
  let definitions = $GTRegistries.MACHINES.values().iterator();
  let generatedScenes = 0;

  while (definitions.hasNext()) {
    let definition = definitions.next();
    if (
      !isInstanceOfClassName(definition, GT_MULTIBLOCK_DEFINITION_CLASS_NAME) ||
      !definition.isRenderXEIPreview()
    ) {
      continue;
    }

    try {
      if (addGeneratedMultiblockScene(event, definition)) {
        generatedScenes++;
      }
    } catch (error) {
      console.warn(
        `Failed to generate GregTech multiblock Ponder scene for ${definition.getId()}: ${error}`,
      );
    }
  }

  console.info(
    `Generated ${generatedScenes} GregTech multiblock Ponder scenes (${GT_PONDER_GENERATOR_VERSION}).`,
  );
});
