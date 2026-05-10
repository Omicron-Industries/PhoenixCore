# Phantasia Script Scene Example: The Alchemical Imbuer

This document provides a specific example of a Phantasia script scene that utilizes the `PhoenixMachines.ALCHEMICAL_IMBUER` multiblock. This scene demonstrates how to define a PhantasiaScript for the imbuing machine, guiding the player through its structure and function.

---

## Defining the Alchemical Imbuer Phantasia Script (Java)

Phantasia scripts are defined in Java code using the `PhantasiaScript.builder()` fluent API. This script would typically be registered during your mod's client setup phase using `PhantasiaScripts.register()`.

**File:** `src/main/java/net/phoenix/core/integration/phantasia/AlchemicalImbuerPhantasiaScript.java` (or similar)

```java
package net.phoenix.core.integration.phantasia;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import net.minecraft.core.BlockPos;
import net.phoenix.core.common.machine.PhoenixMachines; // Assuming PhoenixMachines is where ALCHEMICAL_IMBUER is defined

public class AlchemicalImbuerPhantasiaScript {

    /**
     * Registers the PhantasiaScript for the Alchemical Imbuer.
     * This method should be called during client-side initialization (e.g., FMLClientSetupEvent).
     */
    public static void registerAlchemicalImbuerScript() {
        // Reference to your Alchemical Imbuer MultiblockMachineDefinition
        MultiblockMachineDefinition imbuerDefinition = PhoenixMachines.ALCHEMICAL_IMBUER;

        // Build the PhantasiaScript using the fluent builder
        PhantasiaScript script = PhantasiaScript.builder()
            // Step 1: Introduction to the machine
            .step(0, "The Alchemical Imbuer: a device for transmuting materials.")
                .showAll() // Show the entire structure initially
            
            // Step 2: Highlight its general structure
            .step(60, "It typically forms a 3x3x3 structure.")
                .showLayers(0, 2) // Show all three layers (assuming 0-indexed Y for a 3-block height)
            
            // Step 3: Focus on the central core
            .step(120, "The central core is where the alchemical reaction takes place.")
                .showPos(new BlockPos(1, 1, 1)) // Assuming (1,1,1) is the center of the middle layer
            
            // Step 4: Highlight the input mechanism
            .step(180, "Input components are placed into the front-facing input hatch.")
                .showPos(new BlockPos(1, 0, 0)) // Assuming (1,0,0) is a front-bottom input position
                .hidePos(new BlockPos(1, 1, 1)) // Hide core to highlight input
            
            // Step 5: Point out the controller
            .step(240, "The controller block manages the imbuing process.")
                .showPos(new BlockPos(1, 0, 1)) // Assuming (1,0,1) is the controller on the bottom layer
                .hidePos(new BlockPos(1, 0, 0)) // Hide input to highlight controller
            
            // Step 6: Illustrate the process (showing all blocks again)
            .step(300, "Once activated, the Imbuer processes the materials...")
                .showAll() // Show all again for the process visualization
                // Optionally hide specific parts to focus on the "process" area if needed
                // .hidePos(new BlockPos(1, 0, 0), new BlockPos(1, 0, 1)) 
            
            // Step 7: Highlight the output mechanism
            .step(360, "And outputs the imbued product from the output hatch.")
                .showPos(new BlockPos(1, 2, 0)) // Assuming (1,2,0) is a front-top output position
                .hidePos(new BlockPos(1, 1, 1)) // Hide core to highlight output
            
            // Step 8: Concluding message
            .step(420, "Ready for your next alchemical endeavor!")
                .showAll() // Show the full machine again
            .build();

        // Register the script with the Phantasia system
        PhantasiaScripts.register(imbuerDefinition, script);
    }
}
```

---

## Integration into Client Setup

To ensure this script is loaded and available in-game, you need to call the `registerAlchemicalImbuerScript()` method during your mod's client setup. You also need to add the `ALCHEMICAL_IMBUER` to the `PhantasiaSceneSelectionScreen.PHANTASIA_SCENES` list so it appears in the in-game UI.

**File:** `src/main/java/net/phoenix/core/client/PhoenixClient.java` (or similar client setup class)

```java
package net.phoenix.core.client;

// ... other imports ...
import net.phoenix.core.common.machine.PhoenixMachines;
import net.phoenix.core.integration.phantasia.AlchemicalImbuerPhantasiaScript;
import net.phoenix.core.integration.phantasia.PhantasiaSceneSelectionScreen;
// ... other imports ...

// ... other class code ...

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // ... existing setup code ...

            // Add the Alchemical Imbuer to the list of machines with Phantasia scenes
            PhantasiaSceneSelectionScreen.PHANTASIA_SCENES.add(PhoenixMachines.ALCHEMICAL_IMBUER);
            
            // Register the actual Phantasia script for the Alchemical Imbuer
            AlchemicalImbuerPhantasiaScript.registerAlchemicalImbuerScript();

            // ... other Phantasia scene additions ...
            PhantasiaSceneSelectionScreen.PHANTASIA_SCENES.add(GCYMMachines.MEGA_BLAST_FURNACE);
            PhantasiaSceneSelectionScreen.PHANTASIA_SCENES.add(PhoenixTeslaMachines.TESLA_TOWER);
        });
    }
// ... rest of the class ...
```

---

## Important Notes:

*   **Local Coordinates:** The `BlockPos` values used in `showPos()` and `hidePos()` are **local coordinates** relative to the multiblock's origin (usually `(0,0,0)`). You must adjust these to match the actual internal structure of your `ALCHEMICAL_IMBUER` multiblock.
*   **`MultiblockMachineDefinition`:** Ensure `PhoenixMachines.ALCHEMICAL_IMBUER` correctly points to the `MultiblockMachineDefinition` instance for your Alchemical Imbuer.
*   **Timing (`tickOffset`):** The `tickOffset` parameter for each `step()` is in game ticks (20 ticks = 1 second). Adjust these values to control the pacing of your scene.
*   **Captions:** The `caption` string is what will be displayed to the player. Use clear and concise language.
*   **`showAll()`, `showLayer()`, `showPos()`, `hidePos()`:** These methods control which blocks of the multiblock are visible during that specific step of the scene. They are cumulative within a step (e.g., `showLayer(0).showPos(new BlockPos(1,1,1))` will show layer 0 *and* the specific block at (1,1,1)). `hidePos()` can be used to exclude specific blocks from what would otherwise be shown.