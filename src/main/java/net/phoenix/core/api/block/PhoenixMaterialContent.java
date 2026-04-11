package net.phoenix.core.api.block;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerBlock;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.data.materials.PhoenixMaterialFlags;

import com.tterrag.registrate.util.entry.BlockEntry;

import java.util.HashMap;
import java.util.Map;

public class PhoenixMaterialContent {

    public static final Map<Material, BlockEntry<FlowerBlock>> CRYSTAL_ROSES = new HashMap<>();

    public static void registerMaterialCrystalRoses() {
        System.out.println("DEBUG: CRYSTAL ROSE DATAGEN STARTING");
        var registrate = PhoenixCore.PHOENIX_REGISTRATE;

        for (Material material : GTCEuAPI.materialManager.getRegisteredMaterials()) {
            if (!material.hasFlag(PhoenixMaterialFlags.GENERATE_CRYSTAL_ROSE)) continue;

            String name = material.getName() + "_crystal_rose";

            BlockEntry<FlowerBlock> block = registrate.block(name,
                    props -> new FlowerBlock(() -> MobEffects.GLOWING, 5,
                            props.copy(Blocks.POPPY).noCollission()))
                    .initialProperties(() -> Blocks.POPPY)
                    .addLayer(() -> RenderType::cutout)
                    // This tells Datagen to create [material]_crystal_rose.json blockstate
                    .blockstate((ctx, prov) -> prov.simpleBlock(ctx.get(),
                            prov.models().cross(ctx.getName(), prov.modLoc("block/crystal_rose"))))
                    .item()
                    // This tells Datagen to create [material]_crystal_rose.json item model
                    .model((ctx, prov) -> prov.generated(ctx, prov.modLoc("block/crystal_rose")))
                    .build()
                    .register();

            CRYSTAL_ROSES.put(material, block);
        }
    }
}
