package net.phoenix.core.integration.phantasia.client;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import net.phoenix.core.integration.phantasia.PhantasiaScript;
import net.phoenix.core.integration.phantasia.PhantasiaScripts;
import net.phoenix.core.integration.phoenix_fission.common.PhoenixFissionMachines;

public class HeatExchangerPhantasiaScript {

    public static void register() {
        MultiblockMachineDefinition definition = PhoenixFissionMachines.HEAT_EXCHANGER;
        PhantasiaScript.Builder builder = PhantasiaScript.builder();

        // --- STEP 0: Smallest Version ---
        builder.step(0, "The Heat Exchanger: A modular thermal management system.")
                .shape(0) // Physically set to the smallest size
                .showAll();

        // --- STEP 1: Internal Components ---
        builder.step(60, "Internal gearboxes and frames provide the exchange surface.")
                .showIdContains("frame")
                .showIdContains("gearbox");

        // --- STEP 2: Physical Expansion ---
        // This triggers the screen to reload the pattern and update the UI button text
        builder.step(120, "The structure is scalable; adding modules increases capacity.")
                .shape(10) // Physically expand to a medium size (Index 10)
                .showAll();

        // --- STEP 3: Smart Part Detection ---
        builder.step(180, "I/O Hatches are integrated into the side walls of the expanded unit.")
                .showParts();

        // --- STEP 4: Maximum Expansion ---
        builder.step(240, "At maximum length, it can facilitate massive heat transfer.")
                .shape(19) // Physically expand to the largest size (e.g., 20 blocks / Index 19)
                .showAll();

        // --- STEP 5: Working State ---
        builder.step(300, "System Engaged: Cooling cycle active.")
                .showAll()
                .working(true);

        PhantasiaScripts.register(definition, builder.build());
    }
}