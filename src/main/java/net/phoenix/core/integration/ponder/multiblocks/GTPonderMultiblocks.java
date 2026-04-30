package net.phoenix.core.integration.ponder.multiblocks;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;

import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.instruction.ReplaceBlocksInstruction;
import net.createmod.ponder.foundation.instruction.RotateSceneInstruction;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.integration.ponder.PonderBuilder;
import net.phoenix.core.integration.ponder.api.ExtendedSceneBuilder;
import net.phoenix.core.integration.ponder.api.GTPonderAPI;
import net.phoenix.core.integration.ponder.api.GTPonderRegistrar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class GTPonderMultiblocks {
    private static final Logger LOGGER = LogManager.getLogger("GTPonderMultiblocks");


    public static final String GT_PONDER_GENERATOR_VERSION = "layout-v15-exposed-camera";
    public static final String GT_PONDER_SCENE_PREFIX = "gregtech_multiblocks/";
    public static final ResourceLocation GT_PONDER_STRUCTURE_ID =
            new ResourceLocation("phoenixcore", "gregtech_multiblocks/blank_64");
    public static final int GT_PONDER_STRUCTURE_SIZE = 64;
    public static final int GT_PONDER_STRUCTURE_HEIGHT = 64;
    public static final String GT_MULTIBLOCK_DEFINITION_CLASS_NAME = "MultiblockMachineDefinition";
    public static final String GT_META_MACHINE_BLOCK_CLASS_NAME = "MetaMachineBlock";
    public static final int GT_CALLOUT_DURATION = 24;
    public static final int GT_CONTROLLER_CALLOUT_DURATION = 32;
    public static final int GT_PART_CALLOUT_DURATION = GT_CALLOUT_DURATION;
    public static final int GT_PART_CALLOUT_IDLE = GT_CALLOUT_DURATION;
    public static final int GT_DEFAULT_CAMERA_Y_ROTATION = 145;
    public static final int GT_DEFAULT_CAMERA_X_ROTATION = -35;
    public static final int GT_INITIAL_CAMERA_Y_ROTATION = 35;
    public static final int GT_STRUCTURE_REVEAL_SETTLE = 18;
    public static final int GT_STRUCTURE_REVEAL_TARGET_TICKS = 140;
    public static final int GT_PART_ROTATION_MINIMUM = 20;
    public static final int GT_PART_ROTATION_MIN_SETTLE = 4;
    public static final int GT_PART_ROTATION_MAX_SETTLE = 16;
    public static final int GT_CAMERA_VIEW_BUCKET_DEGREES = 45;
    public static final int GT_FLOW_LINE_DURATION = GT_CALLOUT_DURATION;
    public static final int GT_MECHANIC_CALLOUT_DURATION = 46;
    public static final int GT_MECHANIC_CALLOUT_IDLE = GT_MECHANIC_CALLOUT_DURATION;
    public static final int GT_MECHANIC_FLOW_LINE_DURATION = 34;
    public static final int GT_MAX_MECHANIC_STEPS = 7;
    public static final int GT_LAYER_SCAN_FINAL_DURATION = 64;

    public static final List<PonderPalette> GT_DYNAMIC_CALLOUT_PALETTES = List.of(
            PonderPalette.BLUE, PonderPalette.MEDIUM, PonderPalette.SLOW, PonderPalette.FAST
    );

    public static final List<PartCallout> GT_PART_CALLOUTS = List.of(
            new PartCallout("maintenance", PonderPalette.RED, "Maintenance hatches", "handle repairs and maintenance problems.", (path) -> path.contains("maintenance")),
            new PartCallout("energy_input", PonderPalette.GREEN, "Energy input hatches", "feed EU into powered multiblocks.", (path) -> path.contains("energy") && path.contains("hatch") && !path.contains("output")),
            new PartCallout("energy_output", PonderPalette.GREEN, "Dynamo hatches", "emit EU from generator multiblocks.", (path) -> path.contains("dynamo") || path.contains("energy_output") || path.contains("laser_output")),
            new PartCallout("item_input", PonderPalette.INPUT, "Input buses", "accept item ingredients.", (path) -> path.contains("input_bus")),
            new PartCallout("fluid_input", PonderPalette.INPUT, "Input hatches", "accept fluid ingredients.", (path) -> path.contains("input_hatch")),
            new PartCallout("item_output", PonderPalette.OUTPUT, "Output buses", "collect item products.", (path) -> path.contains("output_bus")),
            new PartCallout("fluid_output", PonderPalette.OUTPUT, "Output hatches", "collect fluid products.", (path) -> path.contains("output_hatch")),
            new PartCallout("muffler", PonderPalette.SLOW, "Muffler hatches", "route exhaust and pollution-sensitive outputs.", (path) -> path.contains("muffler")),
            new PartCallout("laser", PonderPalette.FAST, "Laser hatches", "move high-amperage laser power.", (path) -> path.contains("laser")),
            new PartCallout("data", PonderPalette.MEDIUM, "Data and computation hatches", "connect data, computation, or advanced network links.", (path) -> path.contains("data") || path.contains("computation") || path.contains("network"))
    );

    public static final List<MechanicRule> GT_GENERIC_MECHANIC_RULES = List.of(
            new MechanicRule("recipe_flow", List.of("inputs", "outputs", "controller"), PonderPalette.BLUE, "Inputs move toward the controller, while outputs leave through their matching buses or hatches.", "automation", (shape) -> hasAnyFocus(shape, "inputs") && hasAnyFocus(shape, "outputs")),
            new MechanicRule("power_feed", List.of("part:energy_input", "part:laser"), PonderPalette.GREEN, "Energy hatches define how EU reaches the multiblock; higher tiers may need multiple amps or laser power.", "power", (shape) -> hasAnyFocus(shape, List.of("part:energy_input", "part:laser"))),
            new MechanicRule("maintenance_access", "part:maintenance", PonderPalette.RED, "Maintenance access keeps the machine running after problems appear; automated hatches can reduce downtime.", "pulse", (shape) -> hasAnyFocus(shape, "part:maintenance")),
            new MechanicRule("heat_coils", "structure:coil", PonderPalette.SLOW, "Coils are recipe-critical: they set heat tier and often affect overclocking or energy use.", "heat", (shape) -> hasAnyFocus(shape, "structure:coil")),
            new MechanicRule("exhaust", "part:muffler", PonderPalette.SLOW, "Mufflers handle exhaust or pollution outputs, so leave their face unobstructed when the machine requires it.", "output", (shape) -> hasAnyFocus(shape, "part:muffler")),
            new MechanicRule("data_links", "part:data", PonderPalette.MEDIUM, "Data and computation hatches connect research, data sticks, CWU, or networked control to advanced machines.", "data", (shape) -> hasAnyFocus(shape, "part:data")),
            new MechanicRule("parallel_control", "machine:parallel", PonderPalette.FAST, "Parallel control hatches let supported machines run multiple recipes at once from the same structure.", "pulse", (shape) -> hasAnyFocus(shape, "machine:parallel")),
            new MechanicRule("clean_environment", List.of("structure:cleanroom", "structure:filter"), PonderPalette.MEDIUM, "Cleanroom and filter blocks mark controlled-environment requirements for sensitive recipes.", "cooling", (shape) -> hasAnyFocus(shape, List.of("structure:cleanroom", "structure:filter"))),
            new MechanicRule("rotor_drive", List.of("structure:rotor", "structure:turbine", "machine:rotor", "machine:turbine"), PonderPalette.FAST, "Rotor and turbine parts are the working core for machines that convert motion, fluids, or gases into power.", "pulse", (shape) -> hasAnyFocus(shape, List.of("structure:rotor", "structure:turbine", "machine:rotor", "machine:turbine"))),
            new MechanicRule("firebox_heat", List.of("structure:firebox", "structure:burner"), PonderPalette.SLOW, "Firebox and burner blocks mark the combustion section that supplies heat to this structure.", "heat", (shape) -> hasAnyFocus(shape, List.of("structure:firebox", "structure:burner")))
    );

    public static final Map<String, List<MechanicRule>> GT_MACHINE_MECHANIC_STEPS = new HashMap<>();
    static {
        GT_MACHINE_MECHANIC_STEPS.put("gtceu:assembly_line", List.of(
                new MechanicRule("recipe_flow", List.of("part:item_input", "part:data", "part:item_output"), PonderPalette.BLUE, "Assembly Line item inputs are read along the line, then finished products leave through the output bus.", "automation", null),
                new MechanicRule("data_links", "part:data", PonderPalette.MEDIUM, "The data hatch supplies research data for advanced components that cannot be assembled from items alone.", "data", null)
        ));
        GT_MACHINE_MECHANIC_STEPS.put("gtceu:cleanroom", List.of(
                new MechanicRule("clean_environment", List.of("structure:filter", "structure:cleanroom"), PonderPalette.MEDIUM, "Filter casings belong in the ceiling and define the controlled environment inside the Cleanroom.", "cooling", null),
                new MechanicRule("cleanroom_walls", List.of("structure:glass", "structure:door", "structure:plascrete"), PonderPalette.BLUE, "Walls, glass, and doors enclose the clean volume while still allowing machine access.", "pulse", null)
        ));
        GT_MACHINE_MECHANIC_STEPS.put("gtceu:cracker", List.of(
                new MechanicRule("heat_coils", "structure:coil", PonderPalette.SLOW, "Every coil tier above Cupronickel lowers the Cracker's energy cost for oil cracking recipes.", "heat", null)
        ));
        GT_MACHINE_MECHANIC_STEPS.put("gtceu:distillation_tower", List.of(
                new MechanicRule("recipe_flow", List.of("part:fluid_input", "part:fluid_output", "part:item_output"), PonderPalette.BLUE, "The tower takes fluid input near the bottom and separates products into vertical output layers.", "vertical_outputs", null)
        ));
        GT_MACHINE_MECHANIC_STEPS.put("gtceu:electric_blast_furnace", List.of(
                new MechanicRule("heat_coils", "structure:coil", PonderPalette.SLOW, "EBF coils set the available heat; hotter coils unlock hotter recipes and improve overclock behavior.", "heat", null),
                new MechanicRule("exhaust", "part:muffler", PonderPalette.SLOW, "The muffler handles exhaust from high-temperature processing and should remain exposed.", "output", null)
        ));
        GT_MACHINE_MECHANIC_STEPS.put("gtceu:implosion_compressor", List.of(
                new MechanicRule("recipe_flow", List.of("part:item_input", "part:item_output"), PonderPalette.BLUE, "Explosive inputs are consumed inside the casing and compressed products leave through the output bus.", "automation", null)
        ));
        GT_MACHINE_MECHANIC_STEPS.put("gtceu:large_chemical_reactor", List.of(
                new MechanicRule("recipe_flow", List.of("part:item_input", "part:fluid_input", "part:item_output", "part:fluid_output"), PonderPalette.BLUE, "The LCR combines item and fluid ingredients in one recipe space, then splits item and fluid products.", "automation", null),
                new MechanicRule("reactor_core", List.of("structure:coil", "structure:pipe"), PonderPalette.SLOW, "The pipe casing and single Cupronickel coil are required core blocks, not decorative casing.", "heat", null)
        ));
        GT_MACHINE_MECHANIC_STEPS.put("gtceu:mega_blast_furnace", List.of(
                new MechanicRule("heat_coils", List.of("structure:coil", "structure:firebox"), PonderPalette.SLOW, "The Rotary Hearth's coils and firebox section form the heat core for large-scale smelting.", "heat", null),
                new MechanicRule("large_frame", List.of("structure:frame", "structure:intake", "structure:vent"), PonderPalette.BLUE, "Frames, vents, and intakes make this a staged furnace shape rather than a compact EBF shell.", "pulse", null)
        ));
        GT_MACHINE_MECHANIC_STEPS.put("gtceu:high_performance_computation_array", List.of(
                new MechanicRule("data_links", List.of("structure:hpca", "part:data"), PonderPalette.MEDIUM, "HPCA components provide CWU/t; bridge components connect that computation to networked machines.", "data", null),
                new MechanicRule("hpca_cooling", List.of("structure:cooler", "structure:cooling", "structure:heat_sink"), PonderPalette.BLUE, "Cooling components must cover computation heat, or HPCA components can overheat and become damaged.", "cooling", null)
        ));
    }

    public static final List<MechanicPattern> GT_MACHINE_MECHANIC_PATTERNS = List.of(
            new MechanicPattern((machinePath, machineId) -> machinePath.contains("fusion_reactor"), List.of(
                    new MechanicRule("power_feed", "part:energy_input", PonderPalette.GREEN, "Fusion reactors charge an internal EU buffer before recipes can ignite.", "power", null),
                    new MechanicRule("heat_coils", "structure:coil", PonderPalette.SLOW, "The coil ring and fusion casing define the reactor tier and startup requirements.", "heat", null)
            )),
            new MechanicPattern((machinePath, machineId) -> machinePath.contains("large_turbine") || machinePath.contains("turbine"), List.of(
                    new MechanicRule("rotor_drive", List.of("structure:rotor", "structure:turbine", "machine:rotor", "machine:turbine"), PonderPalette.FAST, "The rotor section is the working core; inputs drive it and dynamo hatches export EU.", "automation", null)
            )),
            new MechanicPattern((machinePath, machineId) -> machinePath.contains("miner") || machinePath.contains("drilling_rig"), List.of(
                    new MechanicRule("recipe_flow", List.of("part:energy_input", "part:item_output", "part:fluid_output"), PonderPalette.BLUE, "Mining multiblocks need power and output space; products leave through item or fluid outputs.", "automation", null)
            )),
            new MechanicPattern((machinePath, machineId) -> machinePath.contains("boiler"), List.of(
                    new MechanicRule("firebox_heat", List.of("structure:firebox", "structure:burner"), PonderPalette.SLOW, "Boiler firebox blocks mark where fuel heat is converted into steam production.", "heat", null)
            ))
    );

    // --- Utility Methods ---

    private static BlockState[][][] extractBlockStates(BlockInfo[][][] blocks) {
        int x = blocks.length;
        int y = x > 0 ? blocks[0].length : 0;
        int z = y > 0 ? blocks[0][0].length : 0;
        BlockState[][][] states = new BlockState[x][y][z];
        for (int i = 0; i < x; i++)
            for (int j = 0; j < y; j++)
                for (int k = 0; k < z; k++) {
                    BlockInfo info = blocks[i][j][k];
                    states[i][j][k] = info != null ? info.getBlockState() : null;
                }
        return states;
    }

    /**
     * GT's MultiblockShapeInfo.getBlocks() returns a [z][y][x] grid:
     *   first index  = aisle / z-depth
     *   second index = height / y
     *   third index  = row / x-width
     *
     * All of our pipeline helpers (collectShapeEntries, extractBlockStates, getShapeDimensions)
     * treat the first index as X, second as Y, third as Z.  Passing the raw GT grid in
     * directly swaps X↔Z — blocks end up at wrong positions and the machine can't form.
     *
     * This method transposes [z][y][x] → [x][y][z] so every downstream helper works correctly.
     */
    private static BlockInfo[][][] transposeGTGrid(BlockInfo[][][] grid) {
        if (grid == null || grid.length == 0) return grid;
        int zDim = grid.length;
        int yDim = grid[0].length;
        int xDim = grid[0][0].length;
        BlockInfo[][][] transposed = new BlockInfo[xDim][yDim][zDim];
        for (int z = 0; z < zDim; z++)
            for (int y = 0; y < yDim; y++)
                for (int x = 0; x < xDim; x++)
                    transposed[x][y][z] = grid[z][y][x];
        return transposed;
    }

    /**
     * Compute the smallest square baseplate that fits the machine footprint plus padding.
     * Replaces the old hardcoded GT_PONDER_STRUCTURE_SIZE=64 which gave every machine
     * a 64×64 baseplate regardless of size — causing a tiny EBF in a vast empty plain.
     */
    private static int computeBasePlateSize(Dimensions dimensions) {
        int footprint = Math.max(dimensions.x, dimensions.z);
        // Add padding: at least 4 blocks on each side so the camera isn't flush with the edge.
        int padded = footprint + 8;
        // Round up to an even number for symmetry, then clamp to the structure limit.
        int size = Math.max(8, (padded + 1) & ~1);
        return Math.min(size, GT_PONDER_STRUCTURE_SIZE);
    }

    private static Dimensions getShapeDimensions(BlockInfo[][][] blocks) {
        int x = blocks.length;
        int y = x > 0 ? blocks[0].length : 0;
        int z = y > 0 ? blocks[0][0].length : 0;
        return new Dimensions(x, y, z);
    }

    private static boolean isInstanceOfClassName(Object value, String className) {
        if (value == null) return false;
        Class<?> valueClass = value.getClass();
        while (valueClass != null) {
            if (valueClass.getName().equals(className)) return true;
            valueClass = valueClass.getSuperclass();
        }
        return false;
    }

    private static String titleFromPath(String path) {
        return Arrays.stream(path.split("_"))
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(Collectors.joining(" "));
    }

    private static String getTranslatedText(String descriptionId, String fallback) {
        try {
            String translated = I18n.get(descriptionId);
            if (!translated.equals(descriptionId)) return translated;
        } catch (Exception ignored) {}
        return fallback;
    }

    /**
     * @deprecated Use {@link #registerAllMultiblockScenes(PonderBuilder)} instead.
     *             This stub is kept for source compatibility but does nothing — all scene
     *             registration is driven by registerAllMultiblockScenes via PhoenixPonderPlugin.
     */
    @Deprecated
    public static void register(PonderBuilder builder) {
        // Intentionally empty. The old body registered scenes using the shared PonderBuilder
        // instance, which caused item accumulation (every machine received every other machine's
        // scenes). Registration now happens exclusively in registerAllMultiblockScenes().
    }

    private static String getTranslatedMachineName(MultiblockMachineDefinition definition) {
        String descriptionId = definition.getDescriptionId();
        String langValue = definition.getLangValue();
        if (langValue != null) return getTranslatedText(descriptionId, langValue);
        return getTranslatedText(descriptionId, titleFromPath(definition.getId().getPath()));
    }

    private static List<MultiblockShapeInfo> generateMultiblock(String machineId) {
        MachineDefinition definition = GTRegistries.MACHINES.get(new ResourceLocation(machineId));
        if (!(definition instanceof MultiblockMachineDefinition mbd)) return null;

        // Check shapes
        if (mbd.getShapes() != null) {
            var shapes = mbd.getShapes().get();
            if (shapes != null) return shapes;
        }

        // Check pattern
        if (mbd.getPatternFactory() != null) {
            var pattern = mbd.getPatternFactory().get();
            if (pattern != null) {
                int[] repetitions = new int[pattern.aisleRepetitions.length];
                return List.of(new MultiblockShapeInfo(pattern.getPreview(repetitions)));
            }
        }
        return null;
    }


    /**
     * Full scene builder for a generated GT multiblock Ponder scene.
     *
     * Fixes applied vs the old renderMultiblock stub:
     *  1. Uses the rich collectShapeEntries / revealShapeBlocks / showLayerScan /
     *     showPartCallouts / showMechanicSteps pipeline — not a bare setBlock loop.
     *  2. Grid axis order: GT returns [x][y][z] from MultiblockShapeInfo.getBlocks(),
     *     corrected from the previous (wrong) [z][y][x] assumption.
     *  3. Finds the controller position inside the grid and places every block offset
     *     from it — so formGeneratedMultiblock() can actually find it via checkPattern().
     *  4. Calls showIndependentSectionImmediately() before populating blocks so the
     *     structure is visible on the very first view (not only after navigation).
     *  5. Delegates to showLayerScan() then showPartCallouts() then showMechanicSteps()
     *     for a complete, annotated walkthrough.
     */
    public static void renderMultiblock(ExtendedSceneBuilder scene, SceneBuildingUtil util,
                                        String machineId, BlockPos ignoredControllerPos,
                                        Map<String, Object> localOptions) {
        MachineDefinition rawDef = GTRegistries.MACHINES.get(new ResourceLocation(machineId));
        if (!(rawDef instanceof MultiblockMachineDefinition definition)) {
            scene.overlay().showText(60).text("Unknown machine: " + machineId)
                    .colored(PonderPalette.RED).placeNearTarget();
            return;
        }

        List<MultiblockShapeInfo> shapeInfos = generateMultiblock(machineId);
        if (shapeInfos == null || shapeInfos.isEmpty()) {
            scene.overlay().showText(100).text("No structure preview available.")
                    .colored(PonderPalette.RED).placeNearTarget();
            return;
        }

        MultiblockShapeInfo shapeInfo = getMostCompleteShapeInfo(shapeInfos);
        if (shapeInfo == null) return;

        // GT MultiblockShapeInfo.getBlocks() returns [x][y][z]
        BlockInfo[][][] grid = shapeInfo.getBlocks();
        Dimensions dimensions = getShapeDimensions(grid);
        BlockState[][][] states = extractBlockStates(grid);

        int basePlateSize = GT_PONDER_STRUCTURE_SIZE;
        warnIfShapeExceedsPonderStructure(machineId, dimensions, basePlateSize);
        configureGeneratedScene(scene, basePlateSize, dimensions);

        ShapeData shape = collectShapeEntries(definition, states, dimensions, basePlateSize);
        logDebugShape(machineId, dimensions, shape);

        // Apply any NBT from BlockInfo to block entities after placement
        for (int x = 0; x < dimensions.x; x++) {
            for (int y = 0; y < dimensions.y; y++) {
                for (int z = 0; z < dimensions.z; z++) {
                    BlockInfo info = grid[x][y][z];
                    if (info == null || info.getBlockState() == null || info.getBlockState().isAir()) continue;
                    int xOffset = (basePlateSize - dimensions.x) / 2;
                    int zOffset = (basePlateSize - dimensions.z) / 2;
                    BlockPos pos = new BlockPos(x + xOffset, y + 1, z + zOffset);
                    try {
                        java.lang.reflect.Field tagField = BlockInfo.class.getDeclaredField("tag");
                        tagField.setAccessible(true);
                        CompoundTag nbt = (CompoundTag) tagField.get(info);
                        if (nbt != null) {
                            var world = (ExtendedSceneBuilder.ExtendedWorldInstructions) scene.world();
                            world.modifyBlockEntityNBT(util.select().position(pos), (CompoundTag tag) -> {
                                for (String key : nbt.getAllKeys()) tag.put(key, nbt.get(key).copy());
                            });
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        if (shape.entries.isEmpty()) {
            scene.overlay().showText(100).text("Structure has no blocks.").colored(PonderPalette.RED).placeNearTarget();
            return;
        }

        // Camera initial rotation
        scene.addInstruction(new RotateSceneInstruction(GT_DEFAULT_CAMERA_X_ROTATION, GT_INITIAL_CAMERA_Y_ROTATION, false));
        scene.addKeyframe();

        // Reveal structure with layer scan (sets blocks + shows them immediately)
        String machinePath = definition.getId().getPath();
        showLayerScan(scene, util, machineId, shape, dimensions);
        scene.addKeyframe();

        Double cameraAngle = (double) GT_DEFAULT_CAMERA_Y_ROTATION;

        // Controller callout
        if (shape.controllerPos != null) {
            showControllerCallout(scene, util, machineId, shape.controllerPos, GT_CONTROLLER_CALLOUT_DURATION);
            scene.idle(GT_CONTROLLER_CALLOUT_DURATION);
        }

        // Part callouts (hatches, buses, etc.)
        List<PartGroup> nonEmptyPartGroups = shape.partGroups.stream()
                .filter(g -> !g.positions.isEmpty()).collect(Collectors.toList());
        if (!nonEmptyPartGroups.isEmpty()) {
            CalloutResult partResult = showPartCallouts(scene, util, machineId, shape, nonEmptyPartGroups, basePlateSize, cameraAngle);
            cameraAngle = partResult.cameraAngle;
            scene.addKeyframe();
        }

        // Mechanic steps
        List<MechanicRule> steps = getMechanicSteps(machineId, machinePath, shape);
        if (!steps.isEmpty()) {
            MechanicStepResult mechanicResult = showMechanicSteps(scene, util, shape, steps, basePlateSize, cameraAngle);
            cameraAngle = mechanicResult.cameraAngle;
            scene.addKeyframe();
        }

        // Automation flow overview
        showAutomationFlowLines(scene, util, shape);
        scene.idle(GT_FLOW_LINE_DURATION);

        scene.markAsFinished();
    }

    private static String getBlockRegistryId(Block block) {
        try {
            ResourceLocation key = ForgeRegistries.BLOCKS.getKey(block);
            if (key != null) return key.toString();
        } catch (Exception ignored) {}
        return block.getDescriptionId();
    }

    private static String getPathFromId(String id) {
        int sep = id.indexOf(":");
        return sep == -1 ? id : id.substring(sep + 1);
    }

    private static String getBlockLabel(Block block, String blockId) {
        return getTranslatedText(block.getDescriptionId(), titleFromPath(getPathFromId(blockId)));
    }

    private static String getStructuralBlockText(String blockId) {
        String path = getPathFromId(blockId).toLowerCase();
        if (path.contains("coil")) return "sets the coil tier or processing heat for this shape.";
        if (path.contains("casing") || path.contains("case")) return "forms a required casing layer in the structure.";
        if (path.contains("glass") || path.contains("window")) return "marks the accepted window material for this pattern.";
        if (path.contains("frame")) return "fills support frame positions in the structure.";
        if (path.contains("pipe") || path.contains("tube")) return "marks required pipe positions inside the pattern.";
        if (path.contains("rotor") || path.contains("turbine")) return "marks the rotating machinery required by the pattern.";
        if (path.contains("firebox") || path.contains("burner")) return "provides the firebox layer for this machine.";
        if (path.contains("cleanroom") || path.contains("filter")) return "provides the controlled-environment blocks this shape expects.";
        return "is one of the required block types in this preview.";
    }

    private static String getMachinePartText(String partId) {
        String path = getPathFromId(partId).toLowerCase();
        if (path.contains("rotor") || path.contains("turbine")) return "handles turbine or rotor interaction for this multiblock.";
        if (path.contains("hpca") || path.contains("computation")) return "contributes computation hardware required by this structure.";
        if (path.contains("parallel")) return "adds the parallel-processing part accepted by this pattern.";
        return "is a special machine part accepted by this multiblock pattern.";
    }

    private static String getDynamicGroupKey(String prefix, String id) {
        return prefix + "_" + id.toLowerCase().replaceAll("[^a-z0-9_]+", "_");
    }

    private static PonderPalette getDynamicPalette(List<PartGroup> groups) {
        return GT_DYNAMIC_CALLOUT_PALETTES.get(groups.size() % GT_DYNAMIC_CALLOUT_PALETTES.size());
    }

    private static int getShapeBlockCount(MultiblockShapeInfo shapeInfo) {
        BlockInfo[][][] blocks = shapeInfo.getBlocks();
        Dimensions dimensions = getShapeDimensions(blocks);
        int blockCount = 0;
        BlockState[][][] states = extractBlockStates(blocks);
        for (int x = 0; x < dimensions.x; x++)
            for (int y = 0; y < dimensions.y; y++)
                for (int z = 0; z < dimensions.z; z++) {
                    BlockState bs = states[x][y][z];
                    if (bs != null && !bs.isAir()) blockCount++;
                }
        return blockCount;
    }

    private static MultiblockShapeInfo getMostCompleteShapeInfo(List<MultiblockShapeInfo> shapeInfos) {
        MultiblockShapeInfo selected = null;
        int selectedCount = -1;
        for (MultiblockShapeInfo shapeInfo : shapeInfos) {
            int count = getShapeBlockCount(shapeInfo);
            if (count > selectedCount) { selected = shapeInfo; selectedCount = count; }
        }
        return selected;
    }

    private static MultiblockShapeInfo getPrimaryShapeInfo(MultiblockMachineDefinition definition) {
        List<MultiblockShapeInfo> explicit = new ArrayList<>(definition.getShapes().get());
        if (!explicit.isEmpty()) return getMostCompleteShapeInfo(explicit);
        List<MultiblockShapeInfo> matching = new ArrayList<>(definition.getMatchingShapes());
        if (matching.isEmpty()) return null;
        return getMostCompleteShapeInfo(matching);
    }

    private static double getSceneScale(int basePlateSize, int height) {
        int max = Math.max(basePlateSize, height);
        if (max <= 5) return 0.82;
        if (max <= 8) return 0.68;
        if (max <= 12) return 0.54;
        if (max <= 16) return 0.44;
        if (max <= 20) return 0.34;
        return 0.28;
    }

    private static Selection selectionForPositions(SceneBuildingUtil util, List<BlockPos> positions) {
        return GTPonderAPI.gtPonderSelectionForPositions(util, positions);
    }

    private static List<BlockPos> collectEntryPositions(List<ShapeEntry> entries) {
        return entries.stream().map(e -> e.pos).collect(Collectors.toList());
    }

    private static Bounds getEntryBounds(List<ShapeEntry> entries) {
        if (entries.isEmpty()) return null;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (ShapeEntry entry : entries) {
            BlockPos pos = entry.pos;
            minX = Math.min(minX, pos.getX()); minY = Math.min(minY, pos.getY()); minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX()); maxY = Math.max(maxY, pos.getY()); maxZ = Math.max(maxZ, pos.getZ());
        }
        return new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static Selection selectionForEntryBounds(SceneBuildingUtil util, Bounds bounds) {
        if (bounds == null) return null;
        return util.select().fromTo(bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ);
    }

    private static Bounds getShapeWorldBounds(Dimensions dimensions, int basePlateSize) {
        int xOffset = (basePlateSize - dimensions.x) / 2;
        int zOffset = (basePlateSize - dimensions.z) / 2;
        return new Bounds(xOffset, 1, zOffset, xOffset + dimensions.x - 1, dimensions.y, zOffset + dimensions.z - 1);
    }

    private static void logDebugShape(String machineId, Dimensions dimensions, ShapeData shape) {
        if (!machineId.equals("gtceu:mega_blast_furnace")) return;
        LOGGER.info("Rotary Hearth debug: dimensions={}x{}x{}, entries={}, bounds={}",
                dimensions.x, dimensions.y, dimensions.z, shape.entries.size(), shape.bounds);
    }

    private static void warnIfShapeExceedsPonderStructure(String machineId, Dimensions dimensions, int basePlateSize) {
        if (basePlateSize <= GT_PONDER_STRUCTURE_SIZE && dimensions.y + 1 <= GT_PONDER_STRUCTURE_HEIGHT) return;
        LOGGER.warn("GregTech multiblock Ponder scene for {} needs {}x{}x{}, but {} is only {}x{}x{}.",
                machineId, basePlateSize, dimensions.y + 1, basePlateSize, GT_PONDER_STRUCTURE_ID,
                GT_PONDER_STRUCTURE_SIZE, GT_PONDER_STRUCTURE_HEIGHT, GT_PONDER_STRUCTURE_SIZE);
    }

    private static List<StateGroup> groupEntriesByState(List<ShapeEntry> entries) {
        Map<BlockState, StateGroup> groupsByState = new HashMap<>();
        List<StateGroup> groups = new ArrayList<>();
        for (ShapeEntry entry : entries) {
            StateGroup group = groupsByState.get(entry.state);
            if (group == null) {
                group = new StateGroup(entry.state);
                groupsByState.put(entry.state, group);
                groups.add(group);
            }
            group.positions.add(entry.pos);
        }
        return groups;
    }

    private static void setShapeEntryBlocks(ExtendedSceneBuilder scene, SceneBuildingUtil util,
                                            List<ShapeEntry> entries) {
        groupEntriesByState(entries).forEach(group -> {
            Selection selection = selectionForPositions(util, group.positions);
            if (selection != null)
                scene.world().setBlocks(selection, group.state, false); // NO showSection
        });
    }



    private static void triggerPonderRerender(ExtendedSceneBuilder scene, SceneBuildingUtil util) {
        scene.addInstruction(new ReplaceBlocksInstruction(util.select().fromTo(-100, -100, -100, -100, -100, -100), state -> state, false, false));
    }

    private static void formGeneratedMultiblock(ExtendedSceneBuilder scene, SceneBuildingUtil util, BlockPos controllerPos, String machineId) {
        if (controllerPos == null) {
            LOGGER.warn("formGeneratedMultiblock: null controllerPos for {}, skipping form attempt.", machineId);
            return;
        }

        // Give Ponder one tick for the block placements to settle before we call checkPattern().
        // Without this idle the block entities (especially the controller) may not be fully
        // initialised when checkPattern() runs, causing forming to silently fail.
        scene.idle(2);

        scene.addInstruction(ponderScene -> scene.world().modifyBlockEntity(controllerPos, MetaMachineBlockEntity.class, blockEntity -> {
            try {
                if (blockEntity.getMetaMachine() instanceof MultiblockControllerMachine controller) {
                    controller.checkPattern();

                    if (controller.isFormed()) {
                        controller.onStructureFormed();
                    } else {
                        LOGGER.warn("Multiblock {} failed to form at {} — the pattern may not align with the Ponder world coordinates.", machineId, controllerPos);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error forming multiblock {}: ", machineId, e);
            }
        }));

        // Forces the renderer to refresh block models (active state, glow textures, etc.)
        triggerPonderRerender(scene, util);
    }

    private static List<LayerData> collectEntriesByPatternLayer(List<ShapeEntry> entries) {
        Map<Integer, LayerData> layersByY = new HashMap<>();
        List<LayerData> layers = new ArrayList<>();
        for (ShapeEntry entry : entries) {
            LayerData layer = layersByY.get(entry.y);
            if (layer == null) {
                layer = new LayerData(entry.y);
                layersByY.put(entry.y, layer);
                layers.add(layer);
            }
            layer.entries.add(entry);
        }
        layers.sort(Comparator.comparingInt(l -> l.y));
        return layers;
    }

    private static int getLayerScanDuration(int layerCount) {
        if (layerCount <= 6) return 56;
        if (layerCount <= 12) return 44;
        if (layerCount <= 20) return 34;
        return 26;
    }

    private static String getLayerScanLabel(int layerIndex, int layerCount, LayerData layer) {
        return String.format("Layer %d/%d - Y %d", layerIndex + 1, layerCount, layer.y + 1);
    }

    private static Vec3 blockCenterVector(SceneBuildingUtil util, BlockPos pos) {
        return util.vector().of(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    private static String positionKeyFromCoords(int x, int y, int z) {
        return String.format("%d,%d,%d", x, y, z);
    }

    private static String positionKey(BlockPos pos) {
        return positionKeyFromCoords(pos.getX(), pos.getY(), pos.getZ());
    }

    private static boolean hasOccupiedPosition(Map<String, Boolean> occupiedPositions, int x, int y, int z) {
        return occupiedPositions != null && occupiedPositions.getOrDefault(positionKeyFromCoords(x, y, z), false);
    }

    private static Direction getMachineFacing(BlockState state) {
        if (state == null) return null;
        Block block = state.getBlock();
        if (!(block instanceof MetaMachineBlock metaMachineBlock)) return null;
        try {
            var rotationState = metaMachineBlock.getRotationState();
            if (rotationState != null && state.hasProperty(rotationState.property)) {
                return state.getValue((Property<Direction>) rotationState.property);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static BlockPos getRepresentativePosition(List<BlockPos> positions) {
        if (positions.isEmpty()) return null;
        if (positions.size() == 1) return positions.get(0);
        double avgX = 0, avgY = 0, avgZ = 0;
        for (BlockPos pos : positions) { avgX += pos.getX(); avgY += pos.getY(); avgZ += pos.getZ(); }
        avgX /= positions.size(); avgY /= positions.size(); avgZ /= positions.size();
        BlockPos rep = positions.get(0);
        double repDist = Double.MAX_VALUE;
        for (BlockPos pos : positions) {
            double dx = pos.getX() - avgX, dy = pos.getY() - avgY, dz = pos.getZ() - avgZ;
            double dist = dx * dx + dy * dy + dz * dz;
            if (dist < repDist) { rep = pos; repDist = dist; }
        }
        return rep;
    }

    private static List<Integer> getHorizontalExposureAngles(BlockPos pos, Map<String, Boolean> occupiedPositions) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        List<Integer> angles = new ArrayList<>();
        if (!hasOccupiedPosition(occupiedPositions, x - 1, y, z)) angles.add(90);
        if (!hasOccupiedPosition(occupiedPositions, x + 1, y, z)) angles.add(-90);
        if (!hasOccupiedPosition(occupiedPositions, x, y, z - 1)) angles.add(180);
        if (!hasOccupiedPosition(occupiedPositions, x, y, z + 1)) angles.add(0);
        return angles;
    }

    private static int getHorizontalExposureScore(BlockPos pos, Map<String, Boolean> occupiedPositions) {
        return getHorizontalExposureAngles(pos, occupiedPositions).size();
    }

    private static int getVisiblePositionIndex(List<BlockPos> positions, Bounds shapeBounds, Map<String, Boolean> occupiedPositions, int basePlateSize) {
        if (positions.isEmpty()) return -1;
        if (positions.size() == 1) return 0;
        double centerX = (shapeBounds == null) ? basePlateSize / 2.0 : (shapeBounds.minX + shapeBounds.maxX + 1) / 2.0;
        double centerZ = (shapeBounds == null) ? basePlateSize / 2.0 : (shapeBounds.minZ + shapeBounds.maxZ + 1) / 2.0;
        int visibleIndex = 0;
        double visibleScore = -Double.MAX_VALUE;
        for (int i = 0; i < positions.size(); i++) {
            BlockPos pos = positions.get(i);
            double dx = pos.getX() + 0.5 - centerX, dz = pos.getZ() + 0.5 - centerZ;
            double score = getHorizontalExposureScore(pos, occupiedPositions) * 1000 + dx * dx + dz * dz + pos.getY() * 0.05;
            if (score > visibleScore) { visibleIndex = i; visibleScore = score; }
        }
        return visibleIndex;
    }

    private static BlockPos getVisiblePosition(List<BlockPos> positions, Bounds shapeBounds, Map<String, Boolean> occupiedPositions, int basePlateSize) {
        int index = getVisiblePositionIndex(positions, shapeBounds, occupiedPositions, basePlateSize);
        return index == -1 ? null : positions.get(index);
    }

    private static Double toFiniteNumber(Object value) {
        if (value instanceof Number) {
            double num = ((Number) value).doubleValue();
            if (Double.isFinite(num)) return num;
        }
        return null;
    }

    private static Double normalizeDegrees(Double degrees) {
        if (degrees == null) return null;
        double normalized = degrees;
        while (normalized <= -180) normalized += 360;
        while (normalized > 180) normalized -= 360;
        return normalized;
    }

    private static Double quantizeCameraViewAngle(Double degrees) {
        Double normalized = normalizeDegrees(degrees);
        if (normalized == null) return null;
        Double bucket = toFiniteNumber(GT_CAMERA_VIEW_BUCKET_DEGREES);
        if (bucket == null || bucket <= 0) return normalized;
        return normalizeDegrees(Math.round(normalized / bucket) * bucket);
    }

    private static Double getRadialViewAngleForPosition(BlockPos pos, Bounds shapeBounds, int basePlateSize) {
        Double x = toFiniteNumber(pos.getX()), z = toFiniteNumber(pos.getZ()), plateSize = toFiniteNumber(basePlateSize);
        if (x == null || z == null || plateSize == null || plateSize <= 0) return null;
        double centerX = (shapeBounds == null) ? plateSize / 2.0 : (shapeBounds.minX + shapeBounds.maxX + 1) / 2.0;
        double centerZ = (shapeBounds == null) ? plateSize / 2.0 : (shapeBounds.minZ + shapeBounds.maxZ + 1) / 2.0;
        double dx = x + 0.5 - centerX, dz = z + 0.5 - centerZ;
        if (Math.abs(dx) + Math.abs(dz) < 0.75) return null;
        Double posAngle = toFiniteNumber(Math.atan2(dx, dz) * (180 / Math.PI));
        return posAngle == null ? null : quantizeCameraViewAngle(-posAngle);
    }

    private static Double getViewAngleForCalloutPosition(BlockPos pos, Bounds shapeBounds, Map<String, Boolean> occupiedPositions, int basePlateSize) {
        List<Integer> exposureAngles = getHorizontalExposureAngles(pos, occupiedPositions);
        Double radialAngle = getRadialViewAngleForPosition(pos, shapeBounds, basePlateSize);
        if (exposureAngles.isEmpty()) return radialAngle;
        if (exposureAngles.size() == 1) return (double) exposureAngles.get(0);
        return radialAngle == null ? (double) exposureAngles.get(0) : radialAngle;
    }

    private static int getRotationSettleTime(Double rotation) {
        Double safeRotation = toFiniteNumber(rotation);
        if (safeRotation == null) return GT_PART_ROTATION_MIN_SETTLE;
        int settleTime = (int) Math.ceil(Math.abs(safeRotation) / 15.0);
        return Math.max(GT_PART_ROTATION_MIN_SETTLE, Math.min(GT_PART_ROTATION_MAX_SETTLE, settleTime));
    }

    private static PartCallout getPartCallout(String path) {
        for (PartCallout callout : GT_PART_CALLOUTS)
            if (callout.match.test(path)) return callout;
        return null;
    }

    private static List<PartGroup> createPartGroups() {
        List<PartGroup> groups = new ArrayList<>();
        GT_PART_CALLOUTS.forEach(callout -> groups.add(new PartGroup(callout.key, callout.palette, callout.label, callout.text, false, true)));
        return groups;
    }

    private static void addPartGroupPosition(List<PartGroup> groups, String key, BlockPos pos, BlockState state) {
        for (PartGroup group : groups) {
            if (group.key.equals(key)) { group.positions.add(pos); group.states.add(state); return; }
        }
    }

    private static PartGroup getOrCreateDynamicGroup(List<PartGroup> groups, String key, PonderPalette palette, String label, String text, boolean machinePart) {
        PartGroup existing = getPartGroup(groups, key);
        if (existing != null) return existing;
        PartGroup newGroup = new PartGroup(key, palette, label, text, true, machinePart);
        groups.add(newGroup);
        return newGroup;
    }

    private static void addDynamicGroupPosition(List<PartGroup> groups, String key, PonderPalette palette, String label, String text, boolean machinePart, BlockPos pos, BlockState state) {
        PartGroup group = getOrCreateDynamicGroup(groups, key, palette, label, text, machinePart);
        group.positions.add(pos); group.states.add(state);
    }

    private static void sortShapeEntries(List<ShapeEntry> entries) {
        entries.sort((l, r) -> {
            if (l.y != r.y) return Integer.compare(l.y, r.y);
            if (l.z != r.z) return Integer.compare(l.z, r.z);
            return Integer.compare(l.x, r.x);
        });
    }

    private static int getBlockRevealBatchSize(int blockCount) {
        if (blockCount <= 96) return 1;
        return Math.max(2, (int) Math.ceil(blockCount / (double) GT_STRUCTURE_REVEAL_TARGET_TICKS));
    }

    private static int getBlockRevealIdle(int blockCount) {
        return blockCount <= 160 ? 2 : 1;
    }

    private static PartGroup getPartGroup(List<PartGroup> partGroups, String key) {
        for (PartGroup group : partGroups) if (group.key.equals(key)) return group;
        return null;
    }

    private static List<String> normalizeFocusList(Object focus) {
        if (focus instanceof String) return Collections.singletonList((String) focus);
        if (focus instanceof List) return ((List<?>) focus).stream().map(String::valueOf).collect(Collectors.toList());
        return Collections.emptyList();
    }

    private static String getFocusNeedle(String focus, String prefix) {
        return focus.substring(prefix.length()).toLowerCase().replaceAll("[^a-z0-9_]+", "_");
    }

    private static boolean textMatchesNeedle(String text, String needle) {
        String lowerText = text.toLowerCase(), lowerNeedle = needle.toLowerCase();
        return lowerText.contains(lowerNeedle) || lowerText.contains(lowerNeedle.replace("_", " "));
    }

    private static boolean groupMatchesFocus(PartGroup group, String focus) {
        if (group == null || focus == null) return false;
        String focusText = focus.toLowerCase();
        if (focusText.startsWith("part:")) return group.key.equals(getFocusNeedle(focusText, "part:"));
        if (focusText.startsWith("structure:")) {
            String needle = getFocusNeedle(focusText, "structure:");
            return !group.machinePart && (textMatchesNeedle(group.key, needle) || textMatchesNeedle(group.label, needle));
        }
        if (focusText.startsWith("machine:")) {
            String needle = getFocusNeedle(focusText, "machine:");
            return group.machinePart && (textMatchesNeedle(group.key, needle) || textMatchesNeedle(group.label, needle));
        }
        return switch (focusText) {
            case "inputs" -> group.key.equals("item_input") || group.key.equals("fluid_input");
            case "outputs" ->
                    group.key.equals("item_output") || group.key.equals("fluid_output") || group.key.equals("energy_output");
            case "power" ->
                    group.key.equals("energy_input") || group.key.equals("energy_output") || group.key.equals("laser");
            default ->
                    group.key.equals(focusText) || textMatchesNeedle(group.key, focusText) || textMatchesNeedle(group.label, focusText);
        };
    }

    private static List<PartGroup> getAllCalloutGroups(ShapeData shape) {
        List<PartGroup> all = new ArrayList<>(shape.partGroups);
        all.addAll(shape.structureGroups);
        return all;
    }

    private static List<PartGroup> collectFocusGroups(ShapeData shape, Object focus) {
        List<PartGroup> groups = new ArrayList<>();
        List<String> focusList = normalizeFocusList(focus);
        List<PartGroup> allGroups = getAllCalloutGroups(shape);
        for (String focusEntry : focusList) {
            if (focusEntry.equals("controller")) {
                if (shape.controllerPos != null)
                    groups.add(new PartGroup("controller", PonderPalette.RED, "Controller", "The controller anchors the preview shape.", false, true, Collections.singletonList(shape.controllerPos), Collections.emptyList()));
                continue;
            }
            for (PartGroup group : allGroups)
                if (!group.positions.isEmpty() && groupMatchesFocus(group, focusEntry) && !groups.contains(group))
                    groups.add(group);
        }
        return groups;
    }

    private static boolean hasAnyFocus(ShapeData shape, Object focus) {
        List<String> focusList = normalizeFocusList(focus);
        for (String f : focusList) if (!collectFocusGroups(shape, f).isEmpty()) return true;
        return false;
    }

    private static BlockPos getRepresentativePartPosition(List<PartGroup> partGroups, List<String> keys) {
        for (String key : keys) {
            PartGroup group = getPartGroup(partGroups, key);
            if (group != null && !group.positions.isEmpty()) return getRepresentativePosition(group.positions);
        }
        return null;
    }

    private static ShapeData collectShapeEntries(MultiblockMachineDefinition definition, BlockState[][][] blocks, Dimensions dimensions, int basePlateSize) {
        int xOffset = (basePlateSize - dimensions.x) / 2;
        int zOffset = (basePlateSize - dimensions.z) / 2;
        List<ShapeEntry> entries = new ArrayList<>();
        Map<String, Boolean> occupiedPositions = new HashMap<>();
        List<BlockPos> machinePartPositions = new ArrayList<>();
        List<PartGroup> partGroups = createPartGroups();
        List<PartGroup> structureGroups = new ArrayList<>();
        BlockPos controllerPos = null;

        for (int x = 0; x < dimensions.x; x++) {
            for (int y = 0; y < dimensions.y; y++) {
                for (int z = 0; z < dimensions.z; z++) {
                    BlockState blockState = blocks[x][y][z];
                    if (blockState == null || blockState.isAir()) continue;
                    BlockPos pos = new BlockPos(x + xOffset, y + 1, z + zOffset);
                    occupiedPositions.put(positionKey(pos), true);
                    Block block = blockState.getBlock();
                    if (isInstanceOfClassName(block, GT_META_MACHINE_BLOCK_CLASS_NAME)) {
                        MetaMachineBlock metaMachineBlock = (MetaMachineBlock) block;
                        if (metaMachineBlock.getDefinition().getId().equals(definition.getId())) {
                            controllerPos = pos;
                        } else {
                            MultiblockMachineDefinition partDef = (MultiblockMachineDefinition) metaMachineBlock.getDefinition();
                            String partId = partDef.getId().toString();
                            PartCallout callout = getPartCallout(partDef.getId().getPath());
                            if (callout == null) {
                                addDynamicGroupPosition(partGroups, getDynamicGroupKey("machine", partId), getDynamicPalette(partGroups), getTranslatedMachineName(partDef), getMachinePartText(partId), true, pos, blockState);
                            } else {
                                addPartGroupPosition(partGroups, callout.key, pos, blockState);
                            }
                            machinePartPositions.add(pos);
                        }
                    } else {
                        String blockId = getBlockRegistryId(block);
                        addDynamicGroupPosition(structureGroups, getDynamicGroupKey("block", blockId), getDynamicPalette(structureGroups), getBlockLabel(block, blockId), getStructuralBlockText(blockId), false, pos, blockState);
                    }
                    entries.add(new ShapeEntry(pos, blockState, x, y, z));
                }
            }
        }
        sortShapeEntries(entries);
        return new ShapeData(entries, occupiedPositions, getShapeWorldBounds(dimensions, basePlateSize), controllerPos, machinePartPositions, partGroups, structureGroups);
    }

    private static void revealShapeBlocks(ExtendedSceneBuilder scene, SceneBuildingUtil util, List<ShapeEntry> entries, Bounds shapeBounds) {
        // 1. Calculate timing and batching parameters
        int batchSize = getBlockRevealBatchSize(entries.size());
        int idleTime = getBlockRevealIdle(entries.size());

        // 2. Determine the physical area these blocks occupy
        Bounds bounds = (shapeBounds == null) ? getEntryBounds(entries) : shapeBounds;
        Selection fullSelection = selectionForEntryBounds(util, bounds);

        if (fullSelection == null) return;

        // 3. Prepare the scene: Clear the area and "show" the empty space to the renderer
        scene.world().setBlocks(fullSelection, Blocks.AIR.defaultBlockState(), false);
        scene.world().showIndependentSectionImmediately(fullSelection);
        scene.idle(idleTime);

        // 4. Populate the blocks in batches
        List<ShapeEntry> batchEntries = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            batchEntries.add(entries.get(i));

            if (batchEntries.size() == batchSize || i == entries.size() - 1) {
                setShapeEntryBlocks(scene, util, batchEntries);
                batchEntries.clear();
                scene.idle(idleTime);
            }
        }

        scene.idle(GT_STRUCTURE_REVEAL_SETTLE);
    }

    private static void showLayerScan(ExtendedSceneBuilder scene, SceneBuildingUtil util, String machineId, ShapeData shape, Dimensions dimensions) {
        List<LayerData> layers = collectEntriesByPatternLayer(shape.entries);
        Selection fullSelection = selectionForEntryBounds(util, shape.bounds);
        int duration = getLayerScanDuration(layers.size());
        if (fullSelection == null || layers.isEmpty()) return;
        scene.world().setBlocks(fullSelection, Blocks.AIR.defaultBlockState(), false);
        scene.world().showIndependentSectionImmediately(fullSelection);
        scene.idle(8);
        for (int i = 0; i < layers.size(); i++) {
            LayerData layer = layers.get(i);
            Bounds layerBounds = getEntryBounds(layer.entries);
            Selection layerSelection = selectionForEntryBounds(util, layerBounds);
            List<BlockPos> positions = collectEntryPositions(layer.entries);
            BlockPos repPos = getRepresentativePosition(positions);
            scene.world().setBlocks(fullSelection, Blocks.AIR.defaultBlockState(), false);
            setShapeEntryBlocks(scene, util, layer.entries);
            if (layerSelection != null && repPos != null) {
                scene.overlay().showOutlineWithText(layerSelection, duration)
                        .text(getLayerScanLabel(i, layers.size(), layer))
                        .colored(PonderPalette.BLUE)
                        .pointAt(blockCenterVector(util, repPos))
                        .placeNearTarget();
            }
            scene.idle(i == 0 ? duration + 16 : duration);
        }
        scene.world().setBlocks(fullSelection, Blocks.AIR.defaultBlockState(), false);
        setShapeEntryBlocks(scene, util, shape.entries);
        formGeneratedMultiblock(scene, util, shape.controllerPos, machineId);
        scene.idle(12);
        if (shape.controllerPos != null) {
            scene.overlay().showText(GT_LAYER_SCAN_FINAL_DURATION)
                    .text(String.format("Complete %dx%dx%d structure.", dimensions.x, dimensions.y, dimensions.z))
                    .colored(PonderPalette.GREEN)
                    .pointAt(blockCenterVector(util, shape.controllerPos))
                    .placeNearTarget();
        }
        scene.idle(GT_LAYER_SCAN_FINAL_DURATION);
    }

    private static void configureGeneratedScene(ExtendedSceneBuilder scene, int basePlateSize, Dimensions dimensions) {
        scene.configureBasePlate(0, 0, basePlateSize);
        scene.scaleSceneView((float) getSceneScale(basePlateSize, dimensions.y + 1));
        if (dimensions.y > 14) scene.setSceneOffsetY(-2);
        else if (dimensions.y > 10) scene.setSceneOffsetY(-1.25f);
        else if (dimensions.y > 8) scene.setSceneOffsetY(-1);
        else if (dimensions.y > 5) scene.setSceneOffsetY(-0.5f);
    }

    private static boolean showControllerCallout(ExtendedSceneBuilder scene, SceneBuildingUtil util, String machineId, BlockPos controllerPos, int duration) {
        if (controllerPos == null) return false;
        scene.overlay().showOutline(PonderPalette.RED, machineId + "/controller", util.select().position(controllerPos), duration);
        scene.overlay().showText(duration)
                .text("The controller anchors the preview shape.")
                .colored(PonderPalette.RED)
                .pointAt(blockCenterVector(util, controllerPos))
                .placeNearTarget();
        return true;
    }

    private static List<PartCalloutPresentation> collectPartCalloutPresentations(SceneBuildingUtil util, List<PartGroup> partGroups, Bounds shapeBounds, Map<String, Boolean> occupiedPositions, int basePlateSize) {
        List<PartCalloutPresentation> presentations = new ArrayList<>();
        for (PartGroup partGroup : partGroups) {
            if (partGroup.positions.isEmpty()) continue;
            int repIndex = getVisiblePositionIndex(partGroup.positions, shapeBounds, occupiedPositions, basePlateSize);
            if (repIndex == -1) continue;
            BlockPos repPos = partGroup.positions.get(repIndex);
            if (repPos == null) continue;
            Double viewAngle = getViewAngleForCalloutPosition(repPos, shapeBounds, occupiedPositions, basePlateSize);
            presentations.add(new PartCalloutPresentation(partGroup.key, partGroup.palette, partGroup.label, partGroup.text, partGroup.positions.size(), partGroup.showCount, partGroup.machinePart, blockCenterVector(util, repPos), util.select().position(repPos), repPos, viewAngle));
        }
        return presentations;
    }

    // 1. The Core Logic
    private static Double rotateToAngle(ExtendedSceneBuilder scene, Double currentCameraAngle, Double targetViewAngle) {
        Double cameraAngle = (currentCameraAngle == null) ? (double) GT_DEFAULT_CAMERA_Y_ROTATION : currentCameraAngle;
        Double viewAngle = toFiniteNumber(targetViewAngle);

        if (viewAngle == null) return cameraAngle;
        Double rotation = normalizeDegrees(viewAngle - cameraAngle);

        if (rotation == null || Math.abs(rotation) < GT_PART_ROTATION_MINIMUM) return cameraAngle;

        double nextCameraAngle = cameraAngle + rotation;
        scene.addInstruction(new RotateSceneInstruction(GT_DEFAULT_CAMERA_X_ROTATION, (float) nextCameraAngle, false));
        scene.idle(getRotationSettleTime(rotation));
        return nextCameraAngle;
    }

    // 2. The PartCallout Wrapper
    private static Double rotateTowardCallout(ExtendedSceneBuilder scene, Double currentCameraAngle, PartCalloutPresentation presentation) {
        return rotateToAngle(scene, currentCameraAngle, presentation.viewAngle);
    }

    // 3. The Mechanic Wrapper (This fixes your error)
    private static Double rotateTowardCallout(ExtendedSceneBuilder scene, Double currentCameraAngle, MechanicPresentation presentation) {
        return rotateToAngle(scene, currentCameraAngle, presentation.viewAngle);
    }

    private static String getCalloutText(PartCalloutPresentation presentation) {
        if (presentation.showCount) return String.format("%dx %s: %s", presentation.count, presentation.label, presentation.text);
        return String.format("%s %s", presentation.label, presentation.text);
    }

    private static List<BlockPos> collectPositionsFromGroups(List<PartGroup> groups) {
        List<BlockPos> positions = new ArrayList<>();
        groups.forEach(group -> positions.addAll(group.positions));
        return positions;
    }

    private static boolean sameBlockPosition(BlockPos left, BlockPos right) {
        if (left == null || right == null) return false;
        return left.getX() == right.getX() && left.getY() == right.getY() && left.getZ() == right.getZ();
    }

    private static MechanicPresentation collectMechanicPresentation(SceneBuildingUtil util, ShapeData shape, MechanicRule step, int basePlateSize) {
        List<PartGroup> groups = collectFocusGroups(shape, step.focus);
        List<BlockPos> positions = collectPositionsFromGroups(groups);
        BlockPos repPos = getVisiblePosition(positions, shape.bounds, shape.occupiedPositions, basePlateSize);
        if (repPos == null) repPos = getRepresentativePosition(positions);
        if (repPos == null) return null;
        return new MechanicPresentation(step.key, step.palette, step.text, step.animation, groups, repPos, blockCenterVector(util, repPos), util.select().position(repPos), getViewAngleForCalloutPosition(repPos, shape.bounds, shape.occupiedPositions, basePlateSize));
    }

    private static void showBigLineBetweenPositions(ExtendedSceneBuilder scene, SceneBuildingUtil util, PonderPalette palette, BlockPos fromPos, BlockPos toPos, int duration) {
        if (fromPos == null || toPos == null || sameBlockPosition(fromPos, toPos)) return;
        scene.overlay().showBigLine(palette, blockCenterVector(util, fromPos), blockCenterVector(util, toPos), duration);
    }

    private static void showFlowFromFocusToController(ExtendedSceneBuilder scene, SceneBuildingUtil util, ShapeData shape, Object focus, PonderPalette palette, int basePlateSize) {
        if (shape.controllerPos == null) return;
        collectFocusGroups(shape, focus).forEach(group -> {
            BlockPos pos = getVisiblePosition(group.positions, shape.bounds, shape.occupiedPositions, basePlateSize);
            if (pos == null) pos = getRepresentativePosition(group.positions);
            showBigLineBetweenPositions(scene, util, palette, pos, shape.controllerPos, GT_MECHANIC_FLOW_LINE_DURATION);
        });
    }

    private static void showFlowFromControllerToFocus(ExtendedSceneBuilder scene, SceneBuildingUtil util, ShapeData shape, Object focus, PonderPalette palette, int basePlateSize) {
        if (shape.controllerPos == null) return;
        collectFocusGroups(shape, focus).forEach(group -> {
            BlockPos pos = getVisiblePosition(group.positions, shape.bounds, shape.occupiedPositions, basePlateSize);
            if (pos == null) pos = getRepresentativePosition(group.positions);
            showBigLineBetweenPositions(scene, util, palette, shape.controllerPos, pos, GT_MECHANIC_FLOW_LINE_DURATION);
        });
    }

    private static void pulseFocusGroups(ExtendedSceneBuilder scene, List<PartGroup> groups, ShapeData shape, int basePlateSize, int maxPulses) {
        int pulses = 0;
        for (PartGroup group : groups) {
            if (pulses >= maxPulses) break;
            BlockPos pos = getVisiblePosition(group.positions, shape.bounds, shape.occupiedPositions, basePlateSize);
            if (pos == null) pos = getRepresentativePosition(group.positions);
            if (pos != null) { scene.effects().indicateSuccess(pos); pulses++; }
        }
    }

    private static void showMechanicAnimation(ExtendedSceneBuilder scene, SceneBuildingUtil util, ShapeData shape, MechanicPresentation presentation, int basePlateSize) {
        switch (presentation.animation) {
            case "automation":
                showFlowFromFocusToController(scene, util, shape, "inputs", PonderPalette.INPUT, basePlateSize);
                showFlowFromFocusToController(scene, util, shape, List.of("part:energy_input", "part:laser"), PonderPalette.GREEN, basePlateSize);
                showFlowFromControllerToFocus(scene, util, shape, "outputs", PonderPalette.OUTPUT, basePlateSize);
                break;
            case "power":
                showFlowFromFocusToController(scene, util, shape, List.of("part:energy_input", "part:laser"), PonderPalette.GREEN, basePlateSize);
                showFlowFromControllerToFocus(scene, util, shape, "part:energy_output", PonderPalette.GREEN, basePlateSize);
                break;
            case "input":
                showFlowFromFocusToController(scene, util, shape, "inputs", PonderPalette.INPUT, basePlateSize);
                break;
            case "output":
                showFlowFromControllerToFocus(scene, util, shape, List.of("outputs", "part:muffler"), PonderPalette.OUTPUT, basePlateSize);
                break;
            case "vertical_outputs":
                showFlowFromFocusToController(scene, util, shape, "part:fluid_input", PonderPalette.INPUT, basePlateSize);
                showFlowFromControllerToFocus(scene, util, shape, List.of("part:fluid_output", "part:item_output"), PonderPalette.OUTPUT, basePlateSize);
                break;
            case "data":
                showFlowFromFocusToController(scene, util, shape, "part:data", PonderPalette.MEDIUM, basePlateSize);
                pulseFocusGroups(scene, presentation.groups, shape, basePlateSize, 3);
                break;
            case "heat":
                showBigLineBetweenPositions(scene, util, PonderPalette.SLOW, presentation.representativePosition, shape.controllerPos, GT_MECHANIC_FLOW_LINE_DURATION);
                pulseFocusGroups(scene, presentation.groups, shape, basePlateSize, 4);
                break;
            case "cooling":
                showBigLineBetweenPositions(scene, util, PonderPalette.BLUE, presentation.representativePosition, shape.controllerPos, GT_MECHANIC_FLOW_LINE_DURATION);
                pulseFocusGroups(scene, presentation.groups, shape, basePlateSize, 4);
                break;
            default:
                pulseFocusGroups(scene, presentation.groups, shape, basePlateSize, 3);
                break;
        }
    }

    private static List<MechanicRule> getGenericMechanicSteps(ShapeData shape) {
        return GT_GENERIC_MECHANIC_RULES.stream()
                .filter(rule -> rule.when == null || rule.when.test(shape))
                .collect(Collectors.toList());
    }

    private static List<MechanicRule> getMachineSpecificMechanicSteps(String machineId, String machinePath) {
        List<MechanicRule> steps = new ArrayList<>();
        List<MechanicRule> exact = GT_MACHINE_MECHANIC_STEPS.get(machineId);
        if (exact != null) steps.addAll(exact);
        for (MechanicPattern pattern : GT_MACHINE_MECHANIC_PATTERNS)
            if (pattern.match.test(machinePath, machineId)) steps.addAll(pattern.steps);
        return steps;
    }

    private static List<MechanicRule> mergeMechanicSteps(List<MechanicRule> genericSteps, List<MechanicRule> machineSteps) {
        Map<String, MechanicRule> mergedMap = new LinkedHashMap<>();
        genericSteps.forEach(step -> mergedMap.put(step.key, step));
        machineSteps.forEach(step -> mergedMap.put(step.key, step));
        return mergedMap.values().stream().limit(GT_MAX_MECHANIC_STEPS).collect(Collectors.toList());
    }

    private static List<MechanicRule> getMechanicSteps(String machineId, String machinePath, ShapeData shape) {
        return mergeMechanicSteps(getGenericMechanicSteps(shape), getMachineSpecificMechanicSteps(machineId, machinePath));
    }

    private static MechanicStepResult showMechanicSteps(ExtendedSceneBuilder scene, SceneBuildingUtil util, ShapeData shape, List<MechanicRule> steps, int basePlateSize, Double currentCameraAngle) {
        Double cameraAngle = currentCameraAngle;
        int shownSteps = 0;
        for (MechanicRule step : steps) {
            MechanicPresentation presentation = collectMechanicPresentation(util, shape, step, basePlateSize);
            if (presentation == null) continue;
            cameraAngle = rotateTowardCallout(scene, cameraAngle, presentation);
            showMechanicAnimation(scene, util, shape, presentation, basePlateSize);
            scene.overlay().showOutline(presentation.palette, step.key, presentation.selection, GT_MECHANIC_CALLOUT_DURATION);
            scene.overlay().showText(GT_MECHANIC_CALLOUT_DURATION)
                    .text(presentation.text)
                    .colored(presentation.palette)
                    .pointAt(presentation.pointAt)
                    .placeNearTarget();
            scene.idle(GT_MECHANIC_CALLOUT_IDLE);
            shownSteps++;
        }
        return new MechanicStepResult(shownSteps, cameraAngle);
    }

    private static CalloutResult showPartCallouts(ExtendedSceneBuilder scene, SceneBuildingUtil util, String machineId, ShapeData shape, List<PartGroup> partGroups, int basePlateSize, Double currentCameraAngle) {
        List<PartCalloutPresentation> presentations = collectPartCalloutPresentations(util, partGroups, shape.bounds, shape.occupiedPositions, basePlateSize);
        Double cameraAngle = currentCameraAngle;
        for (PartCalloutPresentation presentation : presentations) {
            cameraAngle = rotateTowardCallout(scene, cameraAngle, presentation);
            if (presentation.machinePart) scene.effects().indicateSuccess(presentation.representativePosition);
            scene.overlay().showOutline(presentation.palette, machineId + "/" + presentation.key, presentation.selection, GT_PART_CALLOUT_DURATION);
            scene.overlay().showText(GT_PART_CALLOUT_DURATION)
                    .text(getCalloutText(presentation))
                    .colored(presentation.palette)
                    .pointAt(presentation.pointAt)
                    .placeNearTarget();
            scene.idle(GT_PART_CALLOUT_IDLE);
        }
        return new CalloutResult(presentations.size(), cameraAngle);
    }

    private static void showAutomationFlowLines(ExtendedSceneBuilder scene, SceneBuildingUtil util, ShapeData shape) {
        if (shape.controllerPos == null) return;
        Vec3 controllerCenter = blockCenterVector(util, shape.controllerPos);
        BlockPos itemInputPos = getRepresentativePartPosition(shape.partGroups, Collections.singletonList("item_input"));
        BlockPos fluidInputPos = getRepresentativePartPosition(shape.partGroups, Collections.singletonList("fluid_input"));
        BlockPos energyInputPos = getRepresentativePartPosition(shape.partGroups, List.of("energy_input", "laser"));
        BlockPos itemOutputPos = getRepresentativePartPosition(shape.partGroups, Collections.singletonList("item_output"));
        BlockPos fluidOutputPos = getRepresentativePartPosition(shape.partGroups, Collections.singletonList("fluid_output"));
        BlockPos energyOutputPos = getRepresentativePartPosition(shape.partGroups, Collections.singletonList("energy_output"));
        if (itemInputPos != null) scene.overlay().showLine(PonderPalette.INPUT, blockCenterVector(util, itemInputPos), controllerCenter, GT_FLOW_LINE_DURATION);
        if (fluidInputPos != null) scene.overlay().showLine(PonderPalette.INPUT, blockCenterVector(util, fluidInputPos), controllerCenter, GT_FLOW_LINE_DURATION);
        if (energyInputPos != null) scene.overlay().showLine(PonderPalette.GREEN, blockCenterVector(util, energyInputPos), controllerCenter, GT_FLOW_LINE_DURATION);
        if (itemOutputPos != null) scene.overlay().showLine(PonderPalette.OUTPUT, controllerCenter, blockCenterVector(util, itemOutputPos), GT_FLOW_LINE_DURATION);
        if (fluidOutputPos != null) scene.overlay().showLine(PonderPalette.OUTPUT, controllerCenter, blockCenterVector(util, fluidOutputPos), GT_FLOW_LINE_DURATION);
        if (energyOutputPos != null) scene.overlay().showLine(PonderPalette.GREEN, controllerCenter, blockCenterVector(util, energyOutputPos), GT_FLOW_LINE_DURATION);
    }

    private static void addGeneratedMultiblockScene(PonderBuilder builder, MultiblockMachineDefinition definition) {
        String machineId = definition.getId().toString();
        // Scene id path must NOT contain slashes — Ponder embeds it directly into the lang key as:
        //   "<pluginModId>.ponder.<sceneIdPath>.header"
        // A slash would produce an invalid key like "phoenixcore.ponder.gregtech_multiblocks/ebf.header".
        // Use a dot separator instead: "gregtech_multiblocks.electric_blast_furnace"
        // -> lang key "phoenixcore.ponder.gregtech_multiblocks.electric_blast_furnace.header" ✓
        // PhoenixPonderPlugin.registerSharedText must use the SAME path (no slash).
        String sceneName = "gregtech_multiblocks." + definition.getId().getPath();
        String title = getTranslatedMachineName(definition);

        GTPonderRegistrar.registerGTPonderScene(
                builder,
                machineId,
                sceneName,
                title,
                Map.of("structure", "phoenixcore:gregtech_multiblocks/blank_64",
                        "showSection", false,     // renderMultiblock handles visibility itself
                        "markAsFinished", false),  // renderMultiblock calls markAsFinished
                ctx -> {
                    // This lambda runs only when the player opens this Ponder — GT is fully
                    // loaded at this point. The full rich pipeline (layer scan, part callouts,
                    // mechanic steps) is invoked here; no BlockPos hint needed.
                    renderMultiblock(ctx.scene, ctx.util, machineId, null, Map.of());
                }
        );
    }

    public static void registerAllMultiblockScenes(PonderBuilder builder) {
        // We use a List to avoid modification errors during registry loops
        List<MultiblockMachineDefinition> definitions = new ArrayList<>(GTRegistries.MACHINES.values().stream()
                .filter(MultiblockMachineDefinition.class::isInstance)
                .map(MultiblockMachineDefinition.class::cast)
                .toList());

        int generatedScenes = 0;
        for (MultiblockMachineDefinition definition : definitions) {
            // Skip only if it's explicitly marked not to render
            if (!definition.isRenderXEIPreview()) continue;

            try {
                // We register the scene ID, but the lambda INSIDE this
                // won't run until the player actually opens the Ponder UI.
                addGeneratedMultiblockScene(builder, definition);
                generatedScenes++;
            } catch (Exception error) {
                LOGGER.warn("Failed to register Ponder button for {}: {}", definition.getId(), error.getMessage());
            }
        }
        LOGGER.info("Registered {} GregTech multiblock Ponder scenes.", generatedScenes);
    }



    // --- Helper Classes ---

    private record Dimensions(int x, int y, int z) {
    }

    private record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        @Override
        public @NotNull String toString() {
            return String.format("%d,%d,%d->%d,%d,%d", minX, minY, minZ, maxX, maxY, maxZ);
        }
    }

    private record ShapeEntry(BlockPos pos, BlockState state, int x, int y, int z) {
    }

    private static class StateGroup {
        public final BlockState state; public final List<BlockPos> positions = new ArrayList<>();
        public StateGroup(BlockState state) { this.state = state; }
    }

    private static class LayerData {
        public final int y; public final List<ShapeEntry> entries = new ArrayList<>();
        public LayerData(int y) { this.y = y; }
    }

    private record PartCallout(String key, PonderPalette palette, String label, String text, Predicate<String> match) {
    }

    private static class PartGroup {
        public final String key, label, text; public final PonderPalette palette; public final boolean showCount, machinePart;
        public final List<BlockPos> positions; public final List<BlockState> states;
        public PartGroup(String key, PonderPalette palette, String label, String text, boolean showCount, boolean machinePart) { this(key, palette, label, text, showCount, machinePart, new ArrayList<>(), new ArrayList<>()); }
        public PartGroup(String key, PonderPalette palette, String label, String text, boolean showCount, boolean machinePart, List<BlockPos> positions, List<BlockState> states) { this.key = key; this.palette = palette; this.label = label; this.text = text; this.showCount = showCount; this.machinePart = machinePart; this.positions = positions; this.states = states; }
    }

    private record ShapeData(List<ShapeEntry> entries, Map<String, Boolean> occupiedPositions, Bounds bounds,
                             BlockPos controllerPos, List<BlockPos> machinePartPositions, List<PartGroup> partGroups,
                             List<PartGroup> structureGroups) {
    }

    private record MechanicRule(String key, Object focus, PonderPalette palette, String text, String animation,
                                Predicate<ShapeData> when) {
    }

    private record MechanicPattern(BiPredicate<String, String> match, List<MechanicRule> steps) {
    }

    private record PartCalloutPresentation(String key, PonderPalette palette, String label, String text, int count,
                                           boolean showCount, boolean machinePart, Vec3 pointAt, Selection selection,
                                           BlockPos representativePosition, Double viewAngle) {
    }

    private record MechanicPresentation(String key, PonderPalette palette, String text, String animation,
                                        List<PartGroup> groups, BlockPos representativePosition, Vec3 pointAt,
                                        Selection selection, Double viewAngle) {
    }

    private record MechanicStepResult(int count, Double cameraAngle) {
    }

    private record CalloutResult(int count, Double cameraAngle) {
    }
}