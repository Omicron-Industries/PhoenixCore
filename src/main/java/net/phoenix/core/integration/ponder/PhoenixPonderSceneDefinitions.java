package net.phoenix.core.integration.ponder;

import com.gregtechceu.gtceu.api.block.IMachineBlock;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTBlocks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.block.PhoenixBlocks;
import net.phoenix.core.common.machine.PhoenixBeeMachines;
import net.phoenix.core.common.machine.PhoenixMachines;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class PhoenixPonderSceneDefinitions {

    // This is the directory where generated scripts will be placed within resources
    public static final String GENERATED_SCRIPT_SUBPATH = "client_scripts/generated/";
    private static final String GENERATED_SCRIPT_PATH = "kubejs_export/" + GENERATED_SCRIPT_SUBPATH;
    private static final String MANIFEST_FILE_NAME = "generated_ponder_manifest.txt";

    private static final List<String> generatedFileNames = new ArrayList<>();

    // Replace your existing main method with this
    public static void generateAllScenes(Path outputDir) {
        try {
            // Ensure the directory exists and clean old files
            java.nio.file.Files.createDirectories(outputDir);

            // Your existing generation logic
            generateRefinedMultiblockSourceTankScene(outputDir);
            generateHoneyCrystallizationChamberScene(outputDir);
            generateAlchemicalImbuerScene(outputDir);
            generateSourceReactorScene(outputDir);

            // Write the manifest so the Installer knows what to copy
            java.nio.file.Path manifestPath = outputDir.resolve("generated_ponder_manifest.txt");
            java.nio.file.Files.write(manifestPath, generatedFileNames);

        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Helper methods to get block IDs
    private static String getBlockId(Supplier<? extends Block> blockSupplier) {
        return ForgeRegistries.BLOCKS.getKey(blockSupplier.get()).toString();
    }

    // For GTM Machine Blocks
    public static String getBlockId(IMachineBlock machineBlock) {
        return BuiltInRegistries.BLOCK.getKey((Block) machineBlock).toString();
    }

    public static String getBlockId(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof BlockItem blockItem) {
            return BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).toString();
        }
        // Fallback for items that aren't blocks (like a wrench or screwdriver)
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    // For raw Minecraft Blocks
    public static String getBlockId(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    private static String getFrameBlockId(TagPrefix tagPrefix, Material material) {
        return ForgeRegistries.BLOCKS.getKey(ChemicalHelper.getBlock(tagPrefix, material)).toString();
    }

    private static String getBlockId(String modId, String blockName) {
        return new ResourceLocation(modId, blockName).toString();
    }

    private static void generateRefinedMultiblockSourceTankScene(Path outputDir) throws IOException {
        String fileName = "refined_multiblock_source_tank_scene.js";
        PonderSceneGenerator generator = new PonderSceneGenerator(
                PhoenixCore.MOD_ID,
                "refined_multiblock_source_tank",
                "Refined Multiblock Source Tank",
                "tfg:gregtech_multiblocks/blank_64");

        String casingBlock = getBlockId(PhoenixBlocks.SOURCE_FIBER_MACHINE_CASING);
        String controllerBlock = getBlockId(PhoenixMachines.REFINED_MULTIBLOCK_SOURCE_TANK.asStack());

        // Define the pattern as a 3D char array [y][z][x]
        char[][][] patternChars = {
                // Y = 0 (bottom layer)
                {
                        { 'C', 'C', 'C' }, // Z = 0 (back)
                        { 'C', 'C', 'C' }, // Z = 1 (middle)
                        { 'C', 'C', 'C' }  // Z = 2 (front)
                },
                // Y = 1 (middle layer)
                {
                        { 'C', 'C', 'C' }, // Z = 0 (back)
                        { 'C', '#', 'C' }, // Z = 1 (middle) - # is air
                        { 'C', 'C', 'C' }  // Z = 2 (front)
                },
                // Y = 2 (top layer)
                {
                        { 'C', 'C', 'C' }, // Z = 0 (back)
                        { 'C', 'S', 'C' }, // Z = 1 (middle) - S is controller
                        { 'C', 'C', 'C' }  // Z = 2 (front)
                }
        };

        Map<Character, String> blockMapping = new HashMap<>();
        blockMapping.put('C', casingBlock);
        blockMapping.put('S', controllerBlock);
        // '#' is handled as air, no need to map

        int controllerX = -1, controllerY = -1, controllerZ = -1;

        for (int y = 0; y < patternChars.length; y++) {
            for (int z = 0; z < patternChars[y].length; z++) {
                for (int x = 0; x < patternChars[y][z].length; x++) {
                    char blockChar = patternChars[y][z][x];
                    if (blockChar == 'S') {
                        controllerX = x;
                        controllerY = y;
                        controllerZ = z;
                    }
                    if (blockChar != '#' && blockChar != ' ') { // Don't add air blocks
                        generator.addBlock(x, y, z, blockMapping.getOrDefault(blockChar, casingBlock));
                    }
                }
            }
        }

        if (controllerX != -1) {
            generator
                    .addText("The Refined Multiblock Source Tank stores large amounts of Source.", controllerX,
                            controllerY, controllerZ, "gold")
                    .addIdle(60);
        }

        Path scriptFile = outputDir.resolve(fileName);
        generator.writeScript(scriptFile);
        generatedFileNames.add(fileName);
    }

    private static void generateHoneyCrystallizationChamberScene(Path outputDir) throws IOException {
        String fileName = "honey_crystallization_chamber_scene.js";
        PonderSceneGenerator generator = new PonderSceneGenerator(
                PhoenixCore.MOD_ID,
                "honey_crystallization_chamber",
                "Honey Crystallization Chamber",
                "tfg:gregtech_multiblocks/blank_64");

        String steelFrame = getBlockId("gtceu", "steel_frame");
        String bronzeBricks = getBlockId(GTBlocks.CASING_BRONZE_BRICKS);
        String laminatedGlass = getBlockId(GTBlocks.CASING_LAMINATED_GLASS);
        String steelSolid = getBlockId(GTBlocks.CASING_STEEL_SOLID);
        String stainlessClean = getBlockId(GTBlocks.CASING_STAINLESS_CLEAN);
        String controllerBlock = getBlockId(PhoenixBeeMachines.HONEY_CRYSTALLIZATION_CHAMBER.get());

        // Define the pattern as a 3D char array [y][z][x]
        // Dimensions: 11 (X) x 9 (Y) x 11 (Z)
        char[][][] patternChars = {
                // Y = 0 (bottom layer)
                {
                        "BBBBBBBBBBB".toCharArray(), // Z=0
                        "BBBBBBBBBBB".toCharArray(), // Z=1
                        "BBBBBBBBBBB".toCharArray(), // Z=2
                        "BBBBBBBBBBB".toCharArray(), // Z=3
                        "BBBBBBBBBBB".toCharArray(), // Z=4
                        "BBBBBCBBBBB".toCharArray(), // Z=5
                        "BBBBBCBBBBB".toCharArray(), // Z=6
                        "BBBBBBBBBBB".toCharArray(), // Z=7
                        "BBBBBBBBBBB".toCharArray(), // Z=8
                        "BBBBBBBBBBB".toCharArray(), // Z=9
                        "BBBBBBBBBBB".toCharArray()  // Z=10
                },
                // Y = 1
                {
                        "BDDDDDDDDDB".toCharArray(),
                        "BBBBBBBBBBB".toCharArray(),
                        "BBBBBBBBBBB".toCharArray(),
                        "BBBBBBBBBBB".toCharArray(),
                        "BBBBBCBBBBB".toCharArray(),
                        "BBBBEEEBBBB".toCharArray(),
                        "BBBBEEEBBBB".toCharArray(),
                        "BBBBBCBBBBB".toCharArray(),
                        "BBBBBBBBBBB".toCharArray(),
                        "BBBBBBBBBBB".toCharArray(),
                        "BBBBBBBBBBB".toCharArray()
                },
                // Y = 2
                {
                        "BDDDDDDDDDB".toCharArray(),
                        "BBFBBBBBFBB".toCharArray(),
                        "BBFBBBBBFBB".toCharArray(),
                        "BBFBBCBBFBB".toCharArray(),
                        "BBFGGGGGFBB".toCharArray(),
                        "BBGGAAAGGBB".toCharArray(),
                        "BBGGAAAGGBB".toCharArray(),
                        "BBBGGGGGBBB".toCharArray(),
                        "BBBBBCBBBBB".toCharArray(),
                        "BBBBBBBBBBB".toCharArray(),
                        "BBBBBBBBBBB".toCharArray()
                },
                // Y = 3
                {
                        "BDDDDDDDDDB".toCharArray(),
                        "BBBBBBBBBBB".toCharArray(),
                        "BBBBBBBBBBB".toCharArray(),
                        "BBBCBCBCBBB".toCharArray(),
                        "BBGGGGGGGBB".toCharArray(),
                        "BBGAAAAAGBB".toCharArray(),
                        "BBGAAAAAGBB".toCharArray(),
                        "BBGGGGGGGBB".toCharArray(),
                        "BBBBBCBBBBB".toCharArray(),
                        "BBBBBBBBBBB".toCharArray(),
                        "BBBBBBBBBBB".toCharArray()
                },
                // Y = 4
                {
                        "BDDDDDDDDDB".toCharArray(),
                        "BBBBBCBBBBB".toCharArray(),
                        "BBBBBCBBBBB".toCharArray(),
                        "BBBBCCCBBBB".toCharArray(),
                        "BBGGAAAGGBB".toCharArray(),
                        "BEAAAAAAAEB".toCharArray(),
                        "BEAAAAAAAEB".toCharArray(),
                        "BBGGGAGGGBB".toCharArray(),
                        "BBBBBCBBBBB".toCharArray(),
                        "BBBBBBBBBBB".toCharArray(),
                        "BBBBBBBBBBB".toCharArray()
                },
                // Y = 5 (Controller layer)
                {
                        "BDDDDDDDDDB".toCharArray(),
                        "BBBBCDCBBBB".toCharArray(),
                        "BBBBCDCBBBB".toCharArray(),
                        "BBCCCCCCCBB".toCharArray(),
                        "BCGAACAAGCB".toCharArray(),
                        "CEAAACAAAEC".toCharArray(),
                        "CEAAACAAAEC".toCharArray(),
                        "BCGGACAGGCB".toCharArray(),
                        "BBCCCCCCCBB".toCharArray(),
                        "BBBBBBBBBBB".toCharArray(),
                        "BBBBBBBBBBB".toCharArray()
                },
                // Y = 6
                {
                        "BDDDDDDDDDB".toCharArray(),
                        "BBBBBCBBBBB".toCharArray(),
                        "BBBBBCBBBBB".toCharArray(),
                        "BBBBCCCBBBB".toCharArray(),
                        "BBGGAAAGGBB".toCharArray(),
                        "BEAAAAAAAEB".toCharArray(),
                        "BEAAAAAAAEB".toCharArray(),
                        "BBGGGAGGGBB".toCharArray(),
                        "BBBBBCBBBBB".toCharArray(),
                        "BBBBBBBBBBB".toCharArray(),
                        "BBBBBBBBBBB".toCharArray()
                },
                // Y = 7
                {
                        "BDDDDDDDDDB".toCharArray(),
                        "BBBBBBBBBBB".toCharArray(),
                        "BBBBBBBBBBB".toCharArray(),
                        "BBBCBCBCBBB".toCharArray(),
                        "BBGGGGGGGBB".toCharArray(),
                        "BBGAAAAAGBB".toCharArray(),
                        "BBGAAAAAGBB".toCharArray(),
                        "BBGGGGGGGBB".toCharArray(),
                        "BBBBBCBBBBB".toCharArray(),
                        "BBBBBBBBBBB".toCharArray(),
                        "BBBBBBBBBBB".toCharArray()
                },
                // Y = 8 (top layer)
                {
                        "BDDDDDDDDDB".toCharArray(),
                        "BBFBBBBBFBB".toCharArray(),
                        "BBFBBBBBFBB".toCharArray(),
                        "BBFBBCBBFBB".toCharArray(),
                        "BBFGGGGGFBB".toCharArray(),
                        "BBGGAAAGGBB".toCharArray(),
                        "BBGGAAAGGBB".toCharArray(),
                        "BBBGGGGGBBB".toCharArray(),
                        "BBBBBCBBBBB".toCharArray(),
                        "BBBBBBBBBBB".toCharArray(),
                        "BBBBBBBBBBB".toCharArray()
                }
        };

        Map<Character, String> blockMapping = new HashMap<>();
        blockMapping.put('C', steelFrame);
        blockMapping.put('D', bronzeBricks);
        blockMapping.put('E', laminatedGlass);
        blockMapping.put('F', steelSolid);
        blockMapping.put('G', stainlessClean);
        blockMapping.put('H', controllerBlock);
        // 'A' and 'B' are handled below

        int controllerX = -1, controllerY = -1, controllerZ = -1;

        for (int y = 0; y < patternChars.length; y++) {
            for (int z = 0; z < patternChars[y].length; z++) {
                for (int x = 0; x < patternChars[y][z].length; x++) {
                    char blockChar = patternChars[y][z][x];
                    if (blockChar == 'H') { // Controller
                        controllerX = x;
                        controllerY = y;
                        controllerZ = z;
                        generator.addBlock(x, y, z, controllerBlock);
                    } else if (blockChar == 'A' || blockChar == ' ') { // Air
                        // Do nothing
                    } else if (blockChar == 'B') { // Any block, use stainlessClean as default casing
                        generator.addBlock(x, y, z, stainlessClean);
                    } else {
                        generator.addBlock(x, y, z, blockMapping.get(blockChar));
                    }
                }
            }
        }

        if (controllerX != -1) {
            generator
                    .addText("The Honey Crystallization Chamber processes honey into crystallized honey.", controllerX,
                            controllerY, controllerZ, "gold")
                    .addIdle(60);
        }

        Path scriptFile = outputDir.resolve(fileName);
        generator.writeScript(scriptFile);
        generatedFileNames.add(fileName);
    }

    private static void generateAlchemicalImbuerScene(Path outputDir) throws IOException {
        String fileName = "alchemical_imbuer_scene.js";
        PonderSceneGenerator generator = new PonderSceneGenerator(
                PhoenixCore.MOD_ID,
                "alchemical_imbuer",
                "Alchemical Imbuer",
                "tfg:gregtech_multiblocks/blank_64");

        String sourceFiberCasing = getBlockId(PhoenixBlocks.SOURCE_FIBER_MACHINE_CASING);
        String sourcestone = "ars_nouveau:sourcestone";
        String magebloomBlock = "ars_nouveau:magebloom_block";
        String temperedGlass = getBlockId(GTBlocks.CASING_TEMPERED_GLASS);
        String voidPrism = "ars_nouveau:void_prism";
        String sourceGemBlock = "ars_nouveau:source_gem_block";
        String arcaneCore = "ars_nouveau:arcane_core";
        String vitalicSourcelink = "ars_nouveau:vitalic_sourcelink";
        String controllerBlock = getBlockId(PhoenixMachines.ALCHEMICAL_IMBUER.get());

        // Pattern from PhoenixMachines.java
        // .aisle("BBCCCBB", "BBBBBBB", "BBBBBBB", "BBBBBBB", "BBBBBBB", "BBBBBBB", "BBCCCBB")
        // .aisle("BCDDDCB", "BBCCCBB", "BBBBBBB", "BBBBBBB", "BBBBBBB", "BBCCCBB", "BCDDDCB")
        // .aisle("CDDDDDC", "BCEEECB", "BBFFFBB", "BBFFFBB", "BBFFFBB", "BCDDDCB", "CDGGGDC")
        // .aisle("CDDDDDC", "BCEEECB", "BBFEFBB", "BBFHFBB", "BBFEFBB", "BCDIDCB", "CDGJGDC")
        // .aisle("CDDDDDC", "BCEEECB", "BBFFFBB", "BBFFFBB", "BBFFFBB", "BCDDDCB", "CDGGGDC")
        // .aisle("BCDDDCB", "BBCCCBB", "BBBBBBB", "BBBBBBB", "BBBBBBB", "BBCCCBB", "BCDDDCB")
        // .aisle("BBCKCBB", "BBBBBBB", "BBBBBBB", "BBBBBBB", "BBBBBBB", "BBBBBBB", "BBCCCBB")
        char[][][] patternChars = {
                // Y = 0
                {
                        "BBCCCBB".toCharArray(),
                        "BCDDDCB".toCharArray(),
                        "CDDDDDC".toCharArray(),
                        "CDDDDDC".toCharArray(),
                        "CDDDDDC".toCharArray(),
                        "BCDDDCB".toCharArray(),
                        "BBCKCBB".toCharArray()
                },
                // Y = 1
                {
                        "BBBBBBB".toCharArray(),
                        "BBCCCBB".toCharArray(),
                        "BCEEECB".toCharArray(),
                        "BCEEECB".toCharArray(),
                        "BCEEECB".toCharArray(),
                        "BBCCCBB".toCharArray(),
                        "BBBBBBB".toCharArray()
                },
                // Y = 2
                {
                        "BBBBBBB".toCharArray(),
                        "BBBBBBB".toCharArray(),
                        "BBFFFBB".toCharArray(),
                        "BBFEFBB".toCharArray(),
                        "BBFFFBB".toCharArray(),
                        "BBBBBBB".toCharArray(),
                        "BBBBBBB".toCharArray()
                },
                // Y = 3
                {
                        "BBBBBBB".toCharArray(),
                        "BBBBBBB".toCharArray(),
                        "BBFFFBB".toCharArray(),
                        "BBFHFBB".toCharArray(),
                        "BBFFFBB".toCharArray(),
                        "BBBBBBB".toCharArray(),
                        "BBBBBBB".toCharArray()
                },
                // Y = 4
                {
                        "BBBBBBB".toCharArray(),
                        "BBBBBBB".toCharArray(),
                        "BBFFFBB".toCharArray(),
                        "BBFEFBB".toCharArray(),
                        "BBFFFBB".toCharArray(),
                        "BBBBBBB".toCharArray(),
                        "BBBBBBB".toCharArray()
                },
                // Y = 5
                {
                        "BBBBBBB".toCharArray(),
                        "BBBBBBB".toCharArray(),
                        "BBBBBBB".toCharArray(),
                        "BCDIDCB".toCharArray(),
                        "BBBBBBB".toCharArray(),
                        "BBCCCBB".toCharArray(),
                        "BBBBBBB".toCharArray()
                },
                // Y = 6
                {
                        "BBCCCBB".toCharArray(),
                        "BCDDDCB".toCharArray(),
                        "CDGGGDC".toCharArray(),
                        "CDGJGDC".toCharArray(),
                        "CDGGGDC".toCharArray(),
                        "BCDDDCB".toCharArray(),
                        "BBCCCBB".toCharArray()
                }
        };

        Map<Character, String> blockMapping = new HashMap<>();
        blockMapping.put('C', sourceFiberCasing);
        blockMapping.put('D', sourcestone);
        blockMapping.put('E', magebloomBlock);
        blockMapping.put('F', temperedGlass);
        blockMapping.put('G', voidPrism);
        blockMapping.put('H', sourceGemBlock);
        blockMapping.put('I', arcaneCore);
        blockMapping.put('J', vitalicSourcelink);
        blockMapping.put('K', controllerBlock);
        // 'B' is any, use sourceFiberCasing as default

        int controllerX = -1, controllerY = -1, controllerZ = -1;

        for (int y = 0; y < patternChars.length; y++) {
            for (int z = 0; z < patternChars[y].length; z++) {
                for (int x = 0; x < patternChars[y][z].length; x++) {
                    char blockChar = patternChars[y][z][x];
                    if (blockChar == 'K') {
                        controllerX = x;
                        controllerY = y;
                        controllerZ = z;
                        generator.addBlock(x, y, z, controllerBlock);
                    } else if (blockChar != 'B' && blockChar != ' ' && blockChar != '#') {
                        generator.addBlock(x, y, z, blockMapping.get(blockChar));
                    } else if (blockChar == 'B') {
                        generator.addBlock(x, y, z, sourceFiberCasing);
                    }
                }
            }
        }

        if (controllerX != -1) {
            generator
                    .addText("The Alchemical Imbuer is used for Source imbuement and extraction.", controllerX,
                            controllerY, controllerZ, "gold")
                    .addIdle(60);
        }

        Path scriptFile = outputDir.resolve(fileName);
        generator.writeScript(scriptFile);
        generatedFileNames.add(fileName);
    }

    private static void generateSourceReactorScene(Path outputDir) throws IOException {
        String fileName = "source_reactor_scene.js";
        PonderSceneGenerator generator = new PonderSceneGenerator(
                PhoenixCore.MOD_ID,
                "source_reactor",
                "Source Reactor",
                "tfg:gregtech_multiblocks/blank_64");

        String sourceFiberCasing = getBlockId(PhoenixBlocks.SOURCE_FIBER_MACHINE_CASING);
        String ptfePipe = getBlockId(GTBlocks.CASING_POLYTETRAFLUOROETHYLENE_PIPE);
        String controllerBlock = getBlockId(PhoenixMachines.SOURCE_REACTOR.get());

        // Pattern from PhoenixMachines.java
        // .aisle("XXX", "XXX", "XXX")
        // .aisle("XXX", "XPX", "XXX")
        // .aisle("XXX", "XSX", "XXX")
        char[][][] patternChars = {
                // Y = 0 (bottom)
                {
                        "XXX".toCharArray(),
                        "XXX".toCharArray(),
                        "XXX".toCharArray()
                },
                // Y = 1 (middle)
                {
                        "XXX".toCharArray(),
                        "XPX".toCharArray(),
                        "XSX".toCharArray()
                },
                // Y = 2 (top)
                {
                        "XXX".toCharArray(),
                        "XXX".toCharArray(),
                        "XXX".toCharArray()
                }
        };

        int controllerX = -1, controllerY = -1, controllerZ = -1;

        for (int y = 0; y < patternChars.length; y++) {
            for (int z = 0; z < patternChars[y].length; z++) {
                for (int x = 0; x < patternChars[y][z].length; x++) {
                    char blockChar = patternChars[y][z][x];
                    if (blockChar == 'S') {
                        controllerX = x;
                        controllerY = y;
                        controllerZ = z;
                        generator.addBlock(x, y, z, controllerBlock);
                    } else if (blockChar == 'X') {
                        generator.addBlock(x, y, z, sourceFiberCasing);
                    } else if (blockChar == 'P') {
                        generator.addBlock(x, y, z, ptfePipe);
                    }
                }
            }
        }

        if (controllerX != -1) {
            generator
                    .addText("The Source Reactor is a specialized multiblock for Source-related processes.",
                            controllerX, controllerY, controllerZ, "gold")
                    .addIdle(60);
        }

        Path scriptFile = outputDir.resolve(fileName);
        generator.writeScript(scriptFile);
        generatedFileNames.add(fileName);
    }
}
