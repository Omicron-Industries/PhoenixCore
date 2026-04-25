package net.phoenix.core.datagen.models;

import com.gregtechceu.gtceu.api.block.property.GTBlockStateProperties;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.phoenix_fission.api.block.IFissionBlanketType;
import net.phoenix.core.integration.phoenix_fission.api.block.IFissionCoolerType;
import net.phoenix.core.integration.phoenix_fission.api.block.IFissionFuelRodType;
import net.phoenix.core.integration.phoenix_fission.api.block.IFissionModeratorType;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;

public class PhoenixFissionMachineModels {

    // These are the Grayscale parts that WILL be colored (tintindex 0)
    private static final ResourceLocation COOLER_BASE = PhoenixCore.id("block/fission/cooler_base");
    private static final ResourceLocation BLANKET_BASE = PhoenixCore.id("block/fission/blanket_base");
    private static final ResourceLocation FUEL_ROD_BASE = PhoenixCore.id("block/fission/fuel_rod_base");
    private static final ResourceLocation MODERATOR_BASE = PhoenixCore.id("block/fission/moderator_base");

    private static ResourceLocation tinted2LayerParent() {
        // Ensure this parent model uses the -0.01 / 16.01 trick for the tinted layer
        return PhoenixCore.id("block/cube_2_layer_all_tinted");
    }

    // These are the Metal Frames that will NOT be colored
    private static ResourceLocation coolerMask() {
        return PhoenixCore.id("block/fission/masks/cooler_mask");
    }

    private static ResourceLocation coolerMaskOn() {
        return PhoenixCore.id("block/fission/masks/cooler_mask_active");
    }

    private static ResourceLocation rodMask() {
        return PhoenixCore.id("block/fission/masks/fuel_rod_mask");
    }

    private static ResourceLocation rodMaskOn() {
        return PhoenixCore.id("block/fission/masks/fuel_rod_mask_active");
    }

    private static ResourceLocation blanketMask() {
        return PhoenixCore.id("block/fission/masks/blanket_mask");
    }

    private static ResourceLocation blanketMaskOn() {
        return PhoenixCore.id("block/fission/masks/blanket_mask_active");
    }

    private static ResourceLocation modMask() {
        return PhoenixCore.id("block/fission/masks/moderator_mask");
    }

    public static <
            T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> createActiveCoolerModel(IFissionCoolerType type) {
        return (ctx, prov) -> {
            var inactive = prov.models().withExistingParent(ctx.getName(), tinted2LayerParent())
                    .texture("bot_all", COOLER_BASE) // TINTED CENTER
                    .texture("top_all", coolerMask()); // FIXED FRAME

            var active = prov.models().withExistingParent(ctx.getName() + "_active", tinted2LayerParent())
                    .texture("bot_all", COOLER_BASE)
                    .texture("top_all", coolerMaskOn());

            prov.getVariantBuilder(ctx.getEntry())
                    .partialState().with(GTBlockStateProperties.ACTIVE, false).modelForState().modelFile(inactive)
                    .addModel()
                    .partialState().with(GTBlockStateProperties.ACTIVE, true).modelForState().modelFile(active)
                    .addModel();
        };
    }

    public static <
            T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> createBlanketRodModel(IFissionBlanketType type) {
        return (ctx, prov) -> {
            var inactive = prov.models().withExistingParent(ctx.getName(), tinted2LayerParent())
                    .texture("bot_all", BLANKET_BASE)
                    .texture("top_all", blanketMask());

            var active = prov.models().withExistingParent(ctx.getName() + "_active", tinted2LayerParent())
                    .texture("bot_all", BLANKET_BASE)
                    .texture("top_all", blanketMaskOn());

            prov.getVariantBuilder(ctx.getEntry())
                    .partialState().with(GTBlockStateProperties.ACTIVE, false).modelForState().modelFile(inactive)
                    .addModel()
                    .partialState().with(GTBlockStateProperties.ACTIVE, true).modelForState().modelFile(active)
                    .addModel();
        };
    }

    public static <
            T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> createFuelRodModel(IFissionFuelRodType type) {
        return (ctx, prov) -> {
            var inactive = prov.models().withExistingParent(ctx.getName(), tinted2LayerParent())
                    .texture("bot_all", FUEL_ROD_BASE)
                    .texture("top_all", rodMask());

            var active = prov.models().withExistingParent(ctx.getName() + "_active", tinted2LayerParent())
                    .texture("bot_all", FUEL_ROD_BASE)
                    .texture("top_all", rodMaskOn());

            prov.getVariantBuilder(ctx.getEntry())
                    .partialState().with(GTBlockStateProperties.ACTIVE, false).modelForState().modelFile(inactive)
                    .addModel()
                    .partialState().with(GTBlockStateProperties.ACTIVE, true).modelForState().modelFile(active)
                    .addModel();
        };
    }

    public static <
            T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> createFissionModeratorModel(IFissionModeratorType type) {
        return (ctx, prov) -> {
            var model = prov.models().withExistingParent(ctx.getName(), tinted2LayerParent())
                    .texture("bot_all", MODERATOR_BASE)
                    .texture("top_all", modMask());
            prov.simpleBlock(ctx.getEntry(), model);
        };
    }
}
