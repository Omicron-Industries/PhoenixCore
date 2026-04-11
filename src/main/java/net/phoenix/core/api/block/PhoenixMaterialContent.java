package net.phoenix.core.api.block;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerBlock;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.data.materials.PhoenixMaterialFlags;
import net.phoenix.core.common.registry.PhoenixRegistration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class PhoenixMaterialContent {

    public static final Map<Material, BlockEntry<FlowerBlock>> CRYSTAL_ROSES = new HashMap<>();

    // Updated to point to the 'generated' folder structure
    private static final String GENERATED_PATH = "C:/Users/conno/Desktop/PFT Core/PhoenixCore/src/generated/resources/assets/phoenixcore/";

    public static void registerMaterialCrystalRoses() {
        var registrate = PhoenixCore.PHOENIX_REGISTRATE;

        for (Material material : GTCEuAPI.materialManager.getRegisteredMaterials()) {
            if (!material.hasFlag(PhoenixMaterialFlags.GENERATE_CRYSTAL_ROSE)) continue;

            String name = material.getName() + "_crystal_rose";

            // Active writing to src/generated/resources
            generateJsonFiles(name);

            BlockEntry<FlowerBlock> block = registrate.block(name,
                            props -> new FlowerBlock(() -> MobEffects.GLOWING, 5,
                                    props.copy(Blocks.POPPY).noCollission()))
                    .initialProperties(() -> Blocks.POPPY)
                    .addLayer(() -> RenderType::cutout)
                    .blockstate((ctx, prov) -> prov.simpleBlock(ctx.get(),
                            prov.models().cross(ctx.getName(), prov.modLoc("block/crystal_rose"))))
                    .item()
                    .model((ctx, prov) -> prov.generated(ctx, prov.modLoc("item/crystal_rose")))
                    .build()
                    .register();

            CRYSTAL_ROSES.put(material, block);
        }
    }

    private static void generateJsonFiles(String name) {
        try {
            Path bsPath = Paths.get(GENERATED_PATH + "blockstates/" + name + ".json");
            Path itemPath = Paths.get(GENERATED_PATH + "models/item/" + name + ".json");

            // Ensure directories exist
            Files.createDirectories(bsPath.getParent());
            Files.createDirectories(itemPath.getParent());

            // Write files
            String bsContent = "{\"variants\":{\"\":{\"model\":\"phoenixcore:block/crystal_rose\"}}}";
            Files.writeString(bsPath, bsContent);

            String itemContent = "{\"parent\":\"phoenixcore:block/crystal_rose\"}";
            Files.writeString(itemPath, itemContent);

        } catch (IOException e) {
            System.err.println("Failed to write to generated folder for: " + name);
            e.printStackTrace();
        }
    }
}