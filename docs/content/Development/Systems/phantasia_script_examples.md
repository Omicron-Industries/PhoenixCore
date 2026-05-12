# Phantasia Script Examples: Multiblocks, Text, and Timing

This document provides concrete Java code examples for defining Phantasia Script scenes, demonstrating how to integrate multiblocks, manage text display, and control timing within your game.

---

## 1. Alchemical Imbuer Phantasia Script Example

This example demonstrates a basic Phantasia script for the `PhoenixMachines.ALCHEMICAL_IMBUER`, guiding the player through its structure and function.

### Defining the Alchemical Imbuer Phantasia Script (Java)

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
                .hidePos(new BlockPos(1, 0, 0)) // Hide input to focus on controller
            
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

## 2. Rotary Hearth Furnace Phantasia Script Example (Bells & Whistles)

This example showcases a more complex Phantasia script for a hypothetical `PhoenixMachines.ROTARY_HEARTH_FURNACE`, demonstrating a wider range of `PhantasiaScript` features like showing specific layers, individual blocks, hiding blocks, and building a detailed narrative flow.

### Defining the Rotary Hearth Furnace Phantasia Script (Java)

**File:** `src/main/java/net/phoenix/core/integration/phantasia/RotaryHearthFurnacePhantasiaScript.java` (or similar)

```java
package net.phoenix.core.integration.phantasia;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import net.minecraft.core.BlockPos;
import net.phoenix.core.common.machine.PhoenixMachines; // Assuming PhoenixMachines is where ROTARY_HEARTH_FURNACE is defined

public class RotaryHearthFurnacePhantasiaScript {

    /**
     * Registers the PhantasiaScript for the Rotary Hearth Furnace.
     * This method should be called during client-side initialization (e.g., FMLClientSetupEvent).
     */
    public static void registerRotaryHearthFurnaceScript() {
        // Reference to your Rotary Hearth Furnace MultiblockMachineDefinition
        MultiblockMachineDefinition furnaceDefinition = PhoenixMachines.ROTARY_HEARTH_FURNACE;

        // Build the PhantasiaScript using the fluent builder
        PhantasiaScript script = PhantasiaScript.builder()
            // --- Step 0: Introduction ---
            .step(0, "Behold, the Rotary Hearth Furnace: a marvel of metallurgical engineering.")
                .showAll() // Show the entire structure initially
            
            // --- Step 1: Foundation ---
            .step(60, "Its sturdy foundation supports immense heat and weight.")
                .showLayer(0) // Show only the bottom layer (Y=0)
            
            // --- Step 2: Main Structure Walls ---
            .step(120, "The reinforced walls contain the intense temperatures required for smelting.")
                .showLayers(1, 2) // Show the middle two layers (Y=1 and Y=2)
                .hidePos(new BlockPos(1, 1, 1), new BlockPos(1, 2, 1)) // Hide central blocks to imply hollow structure
            
            // --- Step 3: Top Layer / Roof ---
            .step(180, "The top layer often houses exhaust vents and maintenance access points.")
                .showLayer(3) // Show only the top layer (Y=3)
            
            // --- Step 4: Controller Block ---
            .step(240, "The central controller manages the entire smelting process, monitoring conditions.")
                .showPos(new BlockPos(1, 0, 1)) // Highlight the controller, assumed to be at the center of the bottom layer
                .hidePos(new BlockPos(1, 1, 1), new BlockPos(1, 2, 1)) // Ensure central blocks are still hidden if they were
            
            // --- Step 5: Input Hatch ---
            .step(300, "Raw materials are fed into the furnace through the input hatch.")
                .showPos(new BlockPos(1, 1, 0)) // Highlight an input hatch, assumed to be front-middle
                .hidePos(new BlockPos(1, 0, 1)) // Hide controller to focus on input
            
            // --- Step 6: Rotary Hearth Mechanism ---
            .step(360, "At its heart, the rotary hearth slowly turns, ensuring even heating and material agitation.")
                .showPos(new BlockPos(1, 1, 1)) // Highlight the central hearth block
                .hidePos(new BlockPos(1, 1, 0)) // Hide input to focus on hearth
            
            // --- Step 7: Heating Elements ---
            .step(420, "Powerful heating elements line the chamber, bringing the internal temperature to extreme levels.")
                // Highlight blocks surrounding the hearth on the middle layer
                .showPos(
                    new BlockPos(0, 1, 1), new BlockPos(2, 1, 1), // Left/Right of hearth
                    new BlockPos(1, 1, 0), new BlockPos(1, 1, 2)  // Front/Back of hearth
                )
                .hidePos(new BlockPos(1, 1, 1)) // Hide hearth to highlight elements
            
            // --- Step 8: Process Start ---
            .step(480, "The furnace begins its cycle: materials are introduced and slowly melt.")
                .showAll() // Show the full machine again
                .hidePos(new BlockPos(1, 1, 0)) // Hide input hatch, implying it's closed or materials are inside
            
            // --- Step 9: Internal Process Visualization ---
            .step(540, "Internal mechanisms agitate the molten material, facilitating reactions.")
                // Show only the core processing area, hiding outer shell
                .showPos(
                    new BlockPos(1, 1, 1), // Hearth
                    new BlockPos(0, 1, 1), new BlockPos(2, 1, 1),
                    new BlockPos(1, 1, 0), new BlockPos(1, 1, 2),
                    new BlockPos(1, 2, 1) // Part of the upper chamber
                )
            // Note: The previous attempt to use a predicate with hidePos here was incorrect.
            // showPos already defines what is visible. If you need to hide specific blocks
            // from the *currently visible set*, you must list them explicitly.
            
            // --- Step 10: Output Hatch ---
            .step(600, "Finally, the refined product is extracted from the output hatch.")
                .showPos(new BlockPos(1, 2, 0)) // Highlight an output hatch, assumed to be front-top
                .hidePos(new BlockPos(1, 1, 1)) // Hide hearth to focus on output
            
            // --- Step 11: Conclusion ---
            .step(660, "A testament to industrial might, ready for continuous operation.")
                .showAll() // Show the full machine again
            .build();

        // Register the script with the Phantasia system
        PhantasiaScripts.register(furnaceDefinition, script);
    }
}
```

---

## Integration into Client Setup

To ensure these scripts are loaded and available in-game, you need to call their respective `register...Script()` methods during your mod's client setup. You also need to add each machine to the `PhantasiaSceneSelectionScreen.PHANTASIA_SCENES` list so they appear in the in-game UI.

**File:** `src/main/java/net/phoenix/core/client/PhoenixClient.java` (or similar client setup class)

```java
package net.phoenix.core.client;

import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;
import com.gregtechceu.gtceu.common.data.machines.GCYMMachines;

import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.client.particle.PhoenixParticles;
import net.phoenix.core.client.renderer.machine.*;
import net.phoenix.core.common.block.PhoenixBlocks;
import net.phoenix.core.common.machine.PhoenixMachines;
import net.phoenix.core.integration.ars_nouveau.client.gui.SourceHatchScreen;
import net.phoenix.core.integration.phantasia.client.AlchemicalImbuerPhantasiaScript;
import net.phoenix.core.integration.phantasia.client.RotaryHearthFurnacePhantasiaScript; // NEW IMPORT
import net.phoenix.core.integration.phantasia.client.PhantasiaSceneSelectionScreen;
import net.phoenix.core.integration.phoenix_fission.api.block.PhoenixFissionEntities;
import net.phoenix.core.integration.phoenix_fission.client.NukePrimedRenderer;
import net.phoenix.core.integration.phoenix_tesla_network.client.particles.TeslaSparkParticle;
import net.phoenix.core.integration.phoenix_tesla_network.client.renderer.machine.TeslaTowerRenderer;
import net.phoenix.core.integration.phoenix_tesla_network.common.machine.PhoenixTeslaMachines;

import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class PhoenixClient {

    private PhoenixClient() {}

    public static void init(IEventBus modBus) {
        MinecraftForge.EVENT_BUS.register(PhoenixShaders.class);
        // GTCEu Dynamic Renders
        DynamicRenderManager.register(PhoenixCore.id("eye_of_harmony"), EyeOfHarmonyRender.TYPE);
        DynamicRenderManager.register(PhoenixCore.id("artificial_star"), ArtificialStarRender.TYPE);
        DynamicRenderManager.register(PhoenixCore.id("plasma_arc_furnace"), PlasmaArcFurnaceRender.TYPE);
        DynamicRenderManager.register(PhoenixCore.id("custom_fluid"), CustomFluidRender.TYPE);
        DynamicRenderManager.register(PhoenixCore.id("helical_fusion"), HelicalFusionRenderer.TYPE);
        DynamicRenderManager.register(PhoenixCore.id("honey_chamber"), HoneyChamberDynamicRender.TYPE);
        DynamicRenderManager.register(PhoenixCore.id("tesla_tower"), TeslaTowerRenderer.TYPE);
        DynamicRenderManager.register(PhoenixCore.id("engine_gearbox"), EngineGearboxRenderer.TYPE);
    }

    // --- PARTICLE FACTORY REGISTRATION ---
    @SubscribeEvent
    public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(PhoenixParticles.TESLA_SPARK.get(), TeslaSparkProvider::new);
    }

    public static class TeslaSparkProvider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        public TeslaSparkProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            TeslaSparkParticle particle = new TeslaSparkParticle(level, x, y, z);
            if (this.sprites != null) {
                particle.pickSprite(this.sprites);
            }
            return particle;
        }
    }

    // --- MODEL & SETUP LOGIC ---
    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(EyeOfHarmonyRender.SPACE_SHELL_MODEL_RL);
        event.register(EyeOfHarmonyRender.STAR_MODEL_RL);
        EyeOfHarmonyRender.ORBIT_OBJECTS_RL.forEach(event::register);
        event.register(ArtificialStarRender.ARTIFICIAL_STAR_MODEL_RL);
        event.register(PlasmaArcFurnaceRender.RINGS_MODEL_RL);
        event.register(PlasmaArcFurnaceRender.SPHERE_MODEL_RL);
    }

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(PhoenixCore.SOURCE_HATCH_MENU.get(), SourceHatchScreen::new);
            ItemBlockRenderTypes.setRenderLayer(PhoenixBlocks.COIL_TRUE_HEAT_STABLE.get(), RenderType.cutoutMipped());
            EntityRenderers.register(PhoenixFissionEntities.NUKE_PRIMED.get(), NukePrimedRenderer::new);
            PonderIndex.addPlugin(new PhoenixPonderPlugin());

            // Add the Alchemical Imbuer to the list of machines with Phantasia scenes
            PhantasiaSceneSelectionScreen.PHANTASIA_SCENES.add(PhoenixMachines.ALCHEMICAL_IMBUER);
            // Register the actual Phantasia script for the Alchemical Imbuer
            AlchemicalImbuerPhantasiaScript.registerAlchemicalImbuerScript();

            // Add the Rotary Hearth Furnace to the list of machines with Phantasia scenes
            PhantasiaSceneSelectionScreen.PHANTASIA_SCENES.add(PhoenixMachines.ROTARY_HEARTH_FURNACE);
            // Register the actual Phantasia script for the Rotary Hearth Furnace
            RotaryHearthFurnacePhantasiaScript.registerRotaryHearthFurnaceScript();

            // ... other Phantasia scene additions ...
            PhantasiaSceneSelectionScreen.PHANTASIA_SCENES.add(GCYMMachines.MEGA_BLAST_FURNACE);
            PhantasiaSceneSelectionScreen.PHANTASIA_SCENES.add(PhoenixTeslaMachines.TESLA_TOWER);
        });
    }
}
