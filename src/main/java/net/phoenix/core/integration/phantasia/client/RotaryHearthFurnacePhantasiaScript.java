package net.phoenix.core.integration.phantasia.client;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.common.block.CoilBlock;
import com.gregtechceu.gtceu.common.data.machines.GCYMMachines;
import com.gregtechceu.gtceu.common.machine.multiblock.part.MaintenanceHatchPartMachine;

import net.minecraft.core.BlockPos;
import net.phoenix.core.integration.phantasia.PhantasiaScript;
import net.phoenix.core.integration.phantasia.PhantasiaScripts;

public class RotaryHearthFurnacePhantasiaScript {

    public static void registerRotaryHearthFurnaceScript() {
        MultiblockMachineDefinition furnaceDefinition = GCYMMachines.MEGA_BLAST_FURNACE;
        PhantasiaScript.Builder builder = PhantasiaScript.builder();

        // --- DYNAMIC HEATMAPS (COILS) ---
        for (CoilBlock.CoilType type : CoilBlock.CoilType.values()) {
            int materialColor = type.getMaterial().getMaterialRGB() | 0xFF000000;
            String displayName = type.getName().substring(0, 1).toUpperCase() + type.getName().substring(1) + " Coils";

            builder.tierState(displayName, materialColor, state -> {
                if (state.getBlock() instanceof CoilBlock cb) {
                    return cb.coilType == type;
                }
                return false;
            });
        }

        // --- ADDITIONAL HEATMAPS ---
        builder.tierState("Maintenance", 0xFF55FF55, state -> state.getBlock() instanceof MetaMachineBlock mmb &&
                mmb.getDefinition().get() instanceof MaintenanceHatchPartMachine);


        // --- SCRIPTED STEPS ---

        // Step 0: Intro - Wide view
        builder.step(0, "Behold, the Rotary Hearth Furnace: a marvel of metallurgical engineering.")
            //    .camera(45, -20) // High angled overview
                .showAll();

        // Step 60: Foundation focus
        builder.step(60, "Its sturdy foundation supports immense heat and weight.")
             //   .camera(45, -5)  // Lower angle to see base
                .showLayer(0);

        // Step 120: Walls focus
        builder.step(120, "The reinforced walls contain the intense temperatures required for smelting.")
             //   .camera(90, -15) // Side view
                .showLayers(1, 2)
                .hidePos(new BlockPos(1, 1, 1), new BlockPos(1, 2, 1));

        // Step 180: Top Layer
        builder.step(180, "The top layer often houses exhaust vents and maintenance access points.")
             //   .camera(45, -45) // Bird's eye view
                .showLayer(3);

        // Step 240: Controller focus
        builder.step(240, "The central controller manages the entire smelting process, monitoring conditions.")
             //   .camera(0, 0)    // Face-on view of the controller
                .showPos(new BlockPos(1, 0, 1))
                .hidePos(new BlockPos(1, 1, 1), new BlockPos(1, 2, 1));

        // Step 300: Input Hatch
        builder.step(300, "Raw materials are fed into the furnace through the input hatch.")
             //   .camera(-90, -10) // Side view of input
                .showPos(new BlockPos(1, 1, 0))
                .hidePos(new BlockPos(1, 0, 1));

        // Step 360: Interior Hearth
        builder.step(360, "At its heart, the rotary hearth slowly turns, ensuring even heating.")
             //   .camera(135, -35) // Look down into the core
                .showPos(new BlockPos(1, 1, 1))
                .hidePos(new BlockPos(1, 1, 0));

        // Step 420: Heating Elements
        builder.step(420, "Powerful heating elements line the chamber, bringing the internal temperature to extreme levels.")
              //  .camera(180, -20) // Rear view
                .showPos(
                        new BlockPos(0, 1, 1), new BlockPos(2, 1, 1),
                        new BlockPos(1, 1, 0), new BlockPos(1, 1, 2))
                .hidePos(new BlockPos(1, 1, 1));

        // Step 480: Process Start - Machine Glows
        builder.step(480, "The furnace begins its cycle: materials are introduced and slowly melt.")
           //     .camera(45, -20) // Reset to standard overview
                .showAll()
                .hidePos(new BlockPos(1, 1, 0))
                .setWorking(true);

        // Step 540: Processing
        builder.step(540, "Internal mechanisms agitate the molten material, facilitating reactions.")
              //  .camera(45, -45) // Look in from top
                .showPos(
                        new BlockPos(1, 1, 1),
                        new BlockPos(0, 1, 1), new BlockPos(2, 1, 1),
                        new BlockPos(1, 1, 0), new BlockPos(1, 1, 2),
                        new BlockPos(1, 2, 1))
                .setWorking(true);

        // Step 560: PHYSICAL UPGRADE - Camera swing to show coil change
        builder.step(560, "Upgrading to Kanthal Coils for higher efficiency.")
              //  .camera(-135, -30) // Swing around to show coils clearly
                .coil(1)           // Swap block models to Kanthal
                .showAll()
                .setWorking(true);

        // Step 600: Output focus
        builder.step(600, "Finally, the refined product is extracted from the output hatch.")
              //  .camera(90, -10)  // Focus on output side
                .showPos(new BlockPos(1, 2, 0))
                .hidePos(new BlockPos(1, 1, 1))
                .setWorking(false);

        // Step 660: Conclusion
        builder.step(660, "A testament to industrial might, ready for continuous operation.")
             //   .camera(225, -20) // Cinematic slow orbit finish
                .showAll();

        builder.step(720, "").showAll().setWorking(false);

        PhantasiaScripts.register(furnaceDefinition, builder.build());
    }
}