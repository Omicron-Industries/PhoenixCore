# Creating New Ponder Scenes for PhoenixCore

This document outlines the process for creating new Ponder scenes for PhoenixCore's custom multiblocks. We leverage Java to define the scene structures and logic, which are then automatically converted into KubeJS scripts and installed into the game. This allows developers to work primarily in Java while still benefiting from PonderJS.

## Overview of the Process

1.  **Define the Multiblock Structure in Java**: Represent your multiblock's `FactoryBlockPattern` as a 3D character array (`char[][][]`) within a dedicated Java method.
2.  **Map Characters to Blocks**: Create a mapping from the characters in your 3D array to actual Minecraft block IDs.
3.  **Generate the KubeJS Script**: Use the `PonderSceneGenerator` utility to translate your Java-defined structure and steps into a KubeJS `.js` file.
4.  **Register the Scene**: Add a call to your new scene generation method in `PhoenixPonderSceneDefinitions.java`.
5.  **Build and Install**: Run the `generatePonderScripts` Gradle task to create the KubeJS files, which are then automatically installed into your KubeJS folder by `PonderScriptInstaller` when the game loads.

## Step-by-Step Guide

### 1. Locate `PhoenixPonderSceneDefinitions.java`

All Ponder scene generation logic resides in `src/main/java/net/phoenix/core/integration/ponder/PhoenixPonderSceneDefinitions.java`. Open this file.

### 2. Create a New Scene Generation Method

For each multiblock, create a new private static method within `PhoenixPonderSceneDefinitions.java`. This method will contain the logic for generating that specific multiblock's Ponder scene.

**Example (from `Refined Multiblock Source Tank`):**

```java
private static void generateRefinedMultiblockSourceTankScene(Path outputDir) throws IOException {
    String fileName = "refined_multiblock_source_tank_scene.js";
    PonderSceneGenerator generator = new PonderSceneGenerator(
            PhoenixCore.MOD_ID,
            "refined_multiblock_source_tank", // Unique scene ID
            "Refined Multiblock Source Tank",  // Display title
            "tfg:gregtech_multiblocks/blank_64" // Structure NBT (can be a generic blank for custom structures)
    );

    String casingBlock = getBlockId(PhoenixBlocks.SOURCE_FIBER_MACHINE_CASING);
    String controllerBlock = getBlockId(PhoenixMachines.REFINED_MULTIBLOCK_SOURCE_TANK.get());

    // Define the pattern as a 3D char array [y][z][x]
    // This directly corresponds to your FactoryBlockPattern.
    // 'C' for casing, 'S' for controller, '#' for air (or any other custom characters)
    char[][][] patternChars = {
        // Y = 0 (bottom layer)
        {
            {'C', 'C', 'C'}, // Z = 0 (back)
            {'C', 'C', 'C'}, // Z = 1 (middle)
            {'C', 'C', 'C'}  // Z = 2 (front)
        },
        // Y = 1 (middle layer)
        {
            {'C', 'C', 'C'}, // Z = 0 (back)
            {'C', '#', 'C'}, // Z = 1 (middle) - # is air
            {'C', 'C', 'C'}  // Z = 2 (front)
        },
        // Y = 2 (top layer)
        {
            {'C', 'C', 'C'}, // Z = 0 (back)
            {'C', 'S', 'C'}, // Z = 1 (middle) - S is controller
            {'C', 'C', 'C'}  // Z = 2 (front)
        }
    };

    // Map characters to their corresponding block IDs
    Map<Character, String> blockMapping = new HashMap<>();
    blockMapping.put('C', casingBlock);
    blockMapping.put('S', controllerBlock);
    // '#' is handled as air, no need to map

    int controllerX = -1, controllerY = -1, controllerZ = -1;

    // Iterate through the 3D array and add blocks to the generator
    for (int y = 0; y < patternChars.length; y++) {
        for (int z = 0; z < patternChars[y].length; z++) {
            for (int x = 0; x < patternChars[y][z].length; x++) {
                char blockChar = patternChars[y][z][x];
                if (blockChar == 'S') { // Identify controller position
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

    // Add Ponder scene steps (text, idle, etc.)
    if (controllerX != -1) {
        generator.addText("The Refined Multiblock Source Tank stores large amounts of Source.", controllerX, controllerY, controllerZ, "gold")
                 .addIdle(60);
    }

    // Write the generated script to a file
    Path scriptFile = outputDir.resolve(fileName);
    generator.writeScript(scriptFile);
    generatedFileNames.add(fileName); // Add to list for manifest
}
```

### 3. Understand the `PonderSceneGenerator` Methods

The `PonderSceneGenerator` class provides simple methods to build your Ponder scene:

*   `PonderSceneGenerator(String modId, String sceneId, String title, String structureId)`: Constructor to initialize the generator.
    *   `modId`: Your mod's ID (e.g., `PhoenixCore.MOD_ID`).
    *   `sceneId`: A unique identifier for this specific scene (e.g., `"refined_multiblock_source_tank"`).
    *   `title`: The display title for the scene in Ponder.
    *   `structureId`: The ID of the NBT structure file to load. For custom multiblocks, `tfg:gregtech_multiblocks/blank_64` is often used as a base, and you then place blocks programmatically.
*   `addBlock(int x, int y, int z, String blockId)`: Places a block at the specified coordinates.
    *   `x, y, z`: Coordinates relative to the scene's origin (usually 0,0,0).
    *   `blockId`: The full resource location of the block (e.g., `"phoenixcore:source_fiber_machine_casing"`).
*   `addText(String text, int x, int y, int z, String palette)`: Displays text in the scene.
    *   `text`: The message to display.
    *   `x, y, z`: Coordinates to anchor the text.
    *   `palette`: A color palette for the text (e.g., `"gold"`, `"blue"`).
*   `addIdle(int ticks)`: Pauses the scene for a specified number of ticks (20 ticks = 1 second).
*   `writeScript(Path outputPath)`: Writes the generated KubeJS script to the specified file path.

### 4. Helper Methods for Block IDs

`PhoenixPonderSceneDefinitions.java` includes helper methods to easily get block IDs:

*   `getBlockId(Supplier<? extends Block> blockSupplier)`: For blocks registered directly in your mod.
*   `getFrameBlockId(TagPrefix tagPrefix, Material material)`: For GregTechCEu frame blocks.
*   `getBlockId(String modId, String blockName)`: For blocks from other mods or when you know the full ID.

### 5. Call Your New Scene Method

In the `main` method of `PhoenixPonderSceneDefinitions.java`, add a call to your new scene generation method:

```java
public static void main(String[] args) {
    // ... existing setup ...

    try {
        // ... existing cleanup ...

        // Call your new scene generation methods here
        generateRefinedMultiblockSourceTankScene(outputDir);
        generateHoneyCrystallizationChamberScene(outputDir);
        generateAlchemicalImbuerScene(outputDir);
        generateSourceReactorScene(outputDir);
        // Add calls for other multiblocks here!

        // ... existing manifest writing ...

    } catch (IOException e) {
        PhoenixCore.LOGGER.error("Failed to generate PonderJS scripts: " + e.getMessage(), e);
    }
}
```

### 6. Generate and Test

1.  **Run the Gradle Task**: In your IDE's Gradle panel, find and run the `generatePonderScripts` task. This will execute your Java code and create the `.js` files.
2.  **Start Minecraft**: Launch your Minecraft client with PhoenixCore installed.
3.  **Check Ponder**: Open the Ponder interface in-game (usually by pressing `W` on an item or block that has a Ponder scene) and verify that your new scene appears and functions as expected.

## Important Considerations

*   **Coordinate System**: Ponder scenes use a local coordinate system. The `addBlock(x, y, z, ...)` method places blocks relative to the scene's origin.
*   **Complexity**: For very large or complex multiblocks, you might need to simplify the visual representation in Ponder or focus on key components and interactions.
*   **Dynamic Elements**: If your multiblock has dynamic elements (e.g., fluid levels, rotating parts), you might need to explore more advanced PonderJS features directly in the generated JavaScript, or extend `PonderSceneGenerator` with more specialized methods.
*   **Error Handling**: Ensure your Java code handles potential `IOException`s during file operations.
*   **Manifest**: The `generated_ponder_manifest.txt` file is crucial for `PonderScriptInstaller` to know which generated scripts to copy. Ensure `generatedFileNames.add(fileName);` is called for each generated script.

By following these steps, you can efficiently create and manage Ponder scenes for all your custom multiblocks within PhoenixCore, leveraging the power of Java for structured content generation.