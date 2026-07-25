package net.phoenix.core;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.event.MaterialEvent;
import com.gregtechceu.gtceu.api.data.chemical.material.event.PostMaterialEvent;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.condition.RecipeConditionType;
import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.MapIngredientTypeManager;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import com.gregtechceu.gtceu.api.sound.SoundEntry;
import com.gregtechceu.gtceu.common.data.GTCreativeModeTabs;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.phoenix.core.api.PhoenixSounds;
import net.phoenix.core.api.recipe.lookup.MapShieldIngredient;
import net.phoenix.core.client.PhoenixClient;
import net.phoenix.core.client.keybind.PhoenixKeybinds;
import net.phoenix.core.client.particle.PhoenixParticles;
import net.phoenix.core.common.block.PhoenixBlocks;
import net.phoenix.core.common.data.PhoenixRecipeTypes;
import net.phoenix.core.common.data.item.PhoenixItems;
import net.phoenix.core.common.data.materials.*;
import net.phoenix.core.common.data.recipeConditions.FluidInHatchCondition;
import net.phoenix.core.common.data.worldgen.CrystalRoseIndicatorGenerator;
import net.phoenix.core.common.machine.*;
import net.phoenix.core.common.machine.multiblock.Shield;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.conflux.ConfluxRegistry;
import net.phoenix.core.conflux.multiblock.ConfluxMultiblockRegistry;
import net.phoenix.core.conflux.network.ConfluxNetwork;
import net.phoenix.core.conflux.producer.ConfluxProducerMachines;
import net.phoenix.core.conflux.research.AxiomResearchCondition;
import net.phoenix.core.conflux.research.PlayerResearchCapability;
import net.phoenix.core.conflux.research.ResearchTreeRegistry;
import net.phoenix.core.datagen.PhoenixDatagen;
import net.phoenix.core.integration.ars_nouveau.api.recipe.lookup.MapSourceIngredient;
import net.phoenix.core.integration.ars_nouveau.client.gui.SourceHatchMenu;
import net.phoenix.core.integration.ars_nouveau.common.data.recipe.custom.SourceIngredient;
import net.phoenix.core.integration.ars_nouveau.common.data.recipeConditons.SoulCondition;
import net.phoenix.core.integration.ars_nouveau.common.event.SourceHatchJarTransferTick;
import net.phoenix.core.integration.matter_manipulater.common.data.item.ManipulaterItems;
import net.phoenix.core.integration.phoenix_tesla_network.common.machine.PhoenixTeslaMachines;
import net.phoenix.core.integration.recipe_helper.RecipeBuilderMenu;
import net.phoenix.core.network.PhoenixNetwork;

import com.tterrag.registrate.util.entry.RegistryEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

@SuppressWarnings("all")
@Mod(PhoenixCore.MOD_ID)
public class PhoenixCore {

    public static final String MOD_ID = "phoenixcore";
    public static final Logger LOGGER = LogManager.getLogger();
    public static GTRegistrate PHOENIX_REGISTRATE = GTRegistrate.create(MOD_ID);

    public static RegistryEntry<CreativeModeTab> PHOENIX_CREATIVE_TAB = REGISTRATE
            .defaultCreativeTab(PhoenixCore.MOD_ID,
                    builder -> builder
                            .displayItems(new GTCreativeModeTabs.RegistrateDisplayItemsGenerator(PhoenixCore.MOD_ID,
                                    REGISTRATE))
                            .title(REGISTRATE.addLang("itemGroup", PhoenixCore.id("creative_tab"),
                                    "PhoenixCore (CoreMod)"))
                            .icon(() -> PhoenixMachines.HIGH_YIELD_PHOTON_EMISSION_REGULATOR.asStack())
                            .build())
            .register();

    public PhoenixCore() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        PhoenixConfigs.init();
        CrystalRoseIndicatorGenerator.register();

        PHOENIX_REGISTRATE.registerEventListeners(modEventBus);

        modEventBus.addListener(this::onRegisterBlocksAndItems);
        modEventBus.addListener(this::commonSetup);

        PhoenixParticles.init(modEventBus);
        if (FMLLoader.getDist().isClient()) {
            modEventBus.addListener(PhoenixKeybinds::register);
            PhoenixClient.init(modEventBus);
        }

        modEventBus.addGenericListener(RecipeConditionType.class, this::registerConditions);
        modEventBus.addGenericListener(GTRecipeType.class, this::registerRecipeTypes);
        modEventBus.addGenericListener(SoundEntry.class, this::registerSounds);
        modEventBus.addGenericListener(MachineDefinition.class, this::registerMachines);
        modEventBus.addGenericListener(MaterialIconSet.class, this::registerMaterialIconSets);

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::addMaterials);
        modEventBus.addListener(this::modifyMaterials);

        MENUS.register(modEventBus);

        ConfluxRegistry.register(modEventBus);
        modEventBus.addListener(ConfluxRegistry::registerCapabilities);
        ConfluxNetwork.register();
        modEventBus.addListener(PlayerResearchCapability::register);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new SourceHatchJarTransferTick());
        MinecraftForge.EVENT_BUS.addGenericListener(net.minecraft.world.entity.Entity.class,
                PlayerResearchCapability::onAttachCapabilities);
        MinecraftForge.EVENT_BUS.addListener(PlayerResearchCapability::onPlayerClone);
        MinecraftForge.EVENT_BUS.addListener(ResearchTreeRegistry::onAddReloadListeners);
    }

    private void onRegisterBlocksAndItems(net.minecraftforge.registries.RegisterEvent event) {
        PhoenixBlocks.init();
        PhoenixItems.init();
        ManipulaterItems.init();

        if (FMLLoader.getDist().isClient()) {
            PhoenixDatagen.init();
        }
    }

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES,
            MOD_ID);

    public static final RegistryObject<MenuType<SourceHatchMenu>> SOURCE_HATCH_MENU = MENUS.register("source_hatch",
            () -> IForgeMenuType.create((IContainerFactory<SourceHatchMenu>) SourceHatchMenu::fromNetwork));

    public static final RegistryObject<MenuType<RecipeBuilderMenu>> RECIPE_BUILDER_MENU = MENUS.register(
            "recipe_builder",
            () -> IForgeMenuType.create(
                    (windowId, inv, data) -> new RecipeBuilderMenu(windowId, inv)));

    public void registerConditions(GTCEuAPI.RegisterEvent<String, RecipeConditionType<?>> event) {
        FluidInHatchCondition.TYPE = new RecipeConditionType<>(
                FluidInHatchCondition::new,
                FluidInHatchCondition.CODEC);

        event.register(PhoenixCore.id("plasma_temp_condition").toString(), FluidInHatchCondition.TYPE);

        SoulCondition.TYPE = new RecipeConditionType<>(
                SoulCondition::new,
                SoulCondition.CODEC);

        event.register(PhoenixCore.id("soul_resonance").toString(), SoulCondition.TYPE);

        AxiomResearchCondition.TYPE = new RecipeConditionType<>(
                AxiomResearchCondition::new,
                AxiomResearchCondition.CODEC);
        event.register(AxiomResearchCondition.CONDITION_ID, AxiomResearchCondition.TYPE);
    }

    @SubscribeEvent
    public void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            PhoenixNetwork.init();

            MapIngredientTypeManager.registerMapIngredient(Shield.ShieldTypes.class, MapShieldIngredient::from);
            MapIngredientTypeManager.registerMapIngredient(
                    SourceIngredient.class,
                    MapSourceIngredient::convertToMapIngredient);
        });
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {}

    private void addCreative(BuildCreativeModeTabContentsEvent event) {}

    private void registerMaterialIconSets(GTCEuAPI.RegisterEvent<String, MaterialIconSet> event) {
        PhoenixMaterialSet.init();
    }

    private void addMaterials(MaterialEvent event) {
        PhoenixOres.register();
        PhoenixMaterials.register();
        PhoenixProgressionMaterials.register();
        PhoenixPolymerMaterials.register();
        PhoenixBeeMaterials.register();
        PhoenixFissionMaterials.register();
        PhoenixMaterialFlags.init();
    }

    private void modifyMaterials(PostMaterialEvent event) {
        PhoenixMaterials.modifyMaterials();
    }

    private void registerRecipeTypes(GTCEuAPI.RegisterEvent<ResourceLocation, GTRecipeType> event) {
        PhoenixRecipeTypes.init();
    }

    public void registerSounds(GTCEuAPI.RegisterEvent<ResourceLocation, SoundEntry> event) {
        PhoenixSounds.init();
    }

    private void registerMachines(GTCEuAPI.RegisterEvent<ResourceLocation, MachineDefinition> event) {
        PhoenixMachines.init();
        PhoenixBeeMachines.init();
        PhoenixTeslaMachines.init();
        ConfluxProducerMachines.init();
        ConfluxMultiblockRegistry.init();
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    public static Fluid plasma(Material material) {
        return material.getFluid(FluidStorageKeys.PLASMA, 1).getFluid();
    }
}
