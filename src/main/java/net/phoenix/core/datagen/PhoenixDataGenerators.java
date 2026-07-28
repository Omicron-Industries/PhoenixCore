package net.phoenix.core.datagen;

import com.gregtechceu.gtceu.api.registry.registrate.SoundEntryBuilder;
import com.gregtechceu.gtceu.common.data.GTMaterialBlocks;

import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.data.materials.PhoenixMaterialFlags;

import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.loot.RegistrateLootTableProvider;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class PhoenixDataGenerators {

    static {

        REGISTRATE.addDataGenerator(ProviderType.LOOT,
                prov -> prov.addLootAction(RegistrateLootTableProvider.LootType.BLOCK, blockLoot -> {
                    for (var entry : GTMaterialBlocks.MATERIAL_BLOCKS.row(PhoenixMaterialFlags.crystal_rose)
                            .values()) {
                        if (!entry.getId().getNamespace().equals(PhoenixCore.MOD_ID)) continue;
                        blockLoot.dropSelf(entry.get());
                    }
                }));
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        PackOutput packOutput = event.getGenerator().getPackOutput();

        if (event.includeClient()) {

            net.phoenix.core.client.renderer.machine.multiblock.PhoenixDynamicRenderHelpers.registerAll();

            event.getGenerator().addProvider(
                    true,
                    new SoundEntryBuilder.SoundEntryProvider(packOutput, PhoenixCore.MOD_ID));

        }
    }
}
