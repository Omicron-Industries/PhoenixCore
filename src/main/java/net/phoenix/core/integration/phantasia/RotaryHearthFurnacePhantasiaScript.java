package net.phoenix.core.integration.phantasia;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.common.block.CoilBlock;
import com.gregtechceu.gtceu.common.data.machines.GCYMMachines;
import com.gregtechceu.gtceu.common.machine.multiblock.part.MaintenanceHatchPartMachine;

import net.minecraft.core.BlockPos;

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

        // Step 0-7: Build-up phase (Not working)
        builder.step(0, "Behold, the Rotary Hearth Furnace: a marvel of metallurgical engineering.")
                .showAll();

        builder.step(60, "Its sturdy foundation supports immense heat and weight.")
                .showLayer(0);

        builder.step(120, "The reinforced walls contain the intense temperatures required for smelting.")
                .showLayers(1, 2)
                .hidePos(new BlockPos(1, 1, 1), new BlockPos(1, 2, 1));

        builder.step(180, "The top layer often houses exhaust vents and maintenance access points.")
                .showLayer(3);

        builder.step(240, "The central controller manages the entire smelting process, monitoring conditions.")
                .showPos(new BlockPos(1, 0, 1))
                .hidePos(new BlockPos(1, 1, 1), new BlockPos(1, 2, 1));

        builder.step(300, "Raw materials are fed into the furnace through the input hatch.")
                .showPos(new BlockPos(1, 1, 0))
                .hidePos(new BlockPos(1, 0, 1));

        builder.step(360, "At its heart, the rotary hearth slowly turns, ensuring even heating and material agitation.")
                .showPos(new BlockPos(1, 1, 1))
                .hidePos(new BlockPos(1, 1, 0));

        builder.step(420,
                "Powerful heating elements line the chamber, bringing the internal temperature to extreme levels.")
                .showPos(
                        new BlockPos(0, 1, 1), new BlockPos(2, 1, 1),
                        new BlockPos(1, 1, 0), new BlockPos(1, 1, 2))
                .hidePos(new BlockPos(1, 1, 1));

        // Step 8: Process Start - MACHINE TURNS ON
        builder.step(480, "The furnace begins its cycle: materials are introduced and slowly melt.")
                .showAll()
                .hidePos(new BlockPos(1, 1, 0))
                .setWorking(true); // Machine will glow and play animations

        // Step 9: Internal Process Visualization - KEEP WORKING
        builder.step(540, "Internal mechanisms agitate the molten material, facilitating reactions.")
                .showPos(
                        new BlockPos(1, 1, 1),
                        new BlockPos(0, 1, 1), new BlockPos(2, 1, 1),
                        new BlockPos(1, 1, 0), new BlockPos(1, 1, 2),
                        new BlockPos(1, 2, 1))
                .setWorking(true); // Ensure working state persists

        // Step 10: Output Hatch - PROCESS ENDS
        builder.step(600, "Finally, the refined product is extracted from the output hatch.")
                .showPos(new BlockPos(1, 2, 0))
                .hidePos(new BlockPos(1, 1, 1))
                .setWorking(false); // Return to idle

        // Step 11: Conclusion
        builder.step(660, "A testament to industrial might, ready for continuous operation.")
                .showAll();

        PhantasiaScripts.register(furnaceDefinition, builder.build());
    }
}
