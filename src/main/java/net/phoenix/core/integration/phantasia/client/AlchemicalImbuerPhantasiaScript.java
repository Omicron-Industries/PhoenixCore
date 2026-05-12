package net.phoenix.core.integration.phantasia.client;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;

import net.minecraft.core.BlockPos;
import net.phoenix.core.common.machine.PhoenixMachines;
import net.phoenix.core.integration.phantasia.PhantasiaScript;
import net.phoenix.core.integration.phantasia.PhantasiaScripts;

public class AlchemicalImbuerPhantasiaScript {

    public static void registerAlchemicalImbuerScript() {
        MultiblockMachineDefinition imbuerDefinition = PhoenixMachines.ALCHEMICAL_IMBUER;

        PhantasiaScript script = PhantasiaScript.builder()
                .step(0, "The Alchemical Imbuer: a device for transmuting materials.")
                .showAll()
                .step(60, "Half cause funny stuff.")
                .showLayers(0, 2)
                .step(120, "The central core is where the alchemical reaction takes place.")
                .showPos(new BlockPos(1, 1, 1))
                .step(180, "Input components are placed into the front-facing input hatch.")
                .showPos(new BlockPos(1, 0, 0))
                .hidePos(new BlockPos(1, 1, 1))
                .step(240, "The controller block manages the imbuing process.")
                .showPos(new BlockPos(1, 0, 1))
                .hidePos(new BlockPos(1, 0, 0))
                .step(300, "Once activated, the Imbuer processes the materials...")
                .showAll()
                .hidePos(new BlockPos(1, 0, 0), new BlockPos(1, 0, 1))
                .step(360, "And outputs the imbued product from the output hatch.")
                .showPos(new BlockPos(1, 2, 0))
                .hidePos(new BlockPos(1, 1, 1))
                .step(420, "Ready for your next alchemical endeavor!")
                .showAll()

                // === ADDED: COMMON MISTAKES ===
                // These show up as warning icons when the "Mistakes" button is toggled
                .mistake(new BlockPos(1, 0, 0), "Must be an Input Hatch, not a Bus!", 0xFFFF5252)
                .mistake(new BlockPos(1, 2, 0), "Ensure this is an Output Hatch!", 0xFFFF5252)
                .mistake("Don't forget: Input Hatch can be placed anywhere on the bottom frame!")
                .mistake("Warning: Maintenance Hatch can not be shared.")
                .mistake("Tip: Use Alchemical Glass for better efficiency.")
                // === ADDED: HEATMAP TIERS ===
                // these show up in the heatmap selection screen
                .tier("Core Components", 0xFF64B5F6, new BlockPos(1, 1, 1), new BlockPos(1, 0, 1))
                .tier("Input/Output", 0xFF81C784, new BlockPos(1, 0, 0), new BlockPos(1, 2, 0))

                .build();

        PhantasiaScripts.register(imbuerDefinition, script);
    }
}
