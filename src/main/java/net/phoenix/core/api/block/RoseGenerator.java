package net.phoenix.core.api.block;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;

import net.phoenix.core.common.data.materials.PhoenixMaterialFlags;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RoseGenerator {

    // Adjust this path to your local project structure
    private static final String ASSETS_PATH = "src/main/resources/assets/phoenixcore/";

    public static void generate() {
        System.out.println("Starting Crystal Rose JSON Generation...");

        // Ensure directories exist
        new File(ASSETS_PATH + "blockstates").mkdirs();
        new File(ASSETS_PATH + "models/item").mkdirs();

        for (Material material : GTCEuAPI.materialManager.getRegisteredMaterials()) {
            if (!material.hasFlag(PhoenixMaterialFlags.GENERATE_CRYSTAL_ROSE)) continue;

            String name = material.getName() + "_crystal_rose";

            try {
                // 1. Generate Blockstate
                // Points to the "master" model crystal_rose.json
                String blockstateJson = "{\n" +
                        "  \"variants\": {\n" +
                        "    \"\": { \"model\": \"phoenixcore:block/crystal_rose\" }\n" +
                        "  }\n" +
                        "}";
                write(ASSETS_PATH + "blockstates/" + name + ".json", blockstateJson);

                // 2. Generate Item Model
                // Inherits from the "master" block model to get the cross shape and tinting
                String itemJson = "{\n" +
                        "  \"parent\": \"phoenixcore:block/crystal_rose\"\n" +
                        "}";
                write(ASSETS_PATH + "models/item/" + name + ".json", itemJson);

            } catch (IOException e) {
                System.err.println("Failed to generate JSON for " + name);
                e.printStackTrace();
            }
        }
        System.out.println("Generation Complete! Refresh your resources folder.");
    }

    private static void write(String path, String content) throws IOException {
        Files.writeString(Paths.get(path), content);
    }
}
