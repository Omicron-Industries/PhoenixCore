package net.phoenix.core.common.data;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.common.data.GTSoundEntries;
import com.gregtechceu.gtceu.common.mui.GTGuiTextures;

import net.minecraft.client.resources.language.I18n;
import net.phoenix.core.api.gui.PhoenixGuiTextures;
import net.phoenix.core.integration.ars_nouveau.api.capability.SourceRecipeCapability;

import brachy.modularui.drawable.progress.ProgressDrawable;
import brachy.modularui.widgets.ProgressWidget;

import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.*;

public class PhoenixRecipeTypes {

    public static GTRecipeType HONEY_CHAMBER_RECIPES;
    public static GTRecipeType PLEASE;
    public static GTRecipeType TESLA_TOWER;
    public static GTRecipeType JUKEBLOCK;
    public static GTRecipeType HIGH_PERFORMANCE_BREEDER_REACTOR_RECIPES;
    public static GTRecipeType ADVANCED_PRESSURIZED_FISSION_REACTOR_RECIPES;
    public static GTRecipeType PRESSURIZED_FISSION_REACTOR_RECIPES;
    public static GTRecipeType HEAT_EXCHANGER_RECIPES;
    public static GTRecipeType SIMULATED_COLONY_RECIPES;
    public static GTRecipeType COMB_DECANTING_RECIPES;
    public static GTRecipeType SWARM_NURTURING_RECIPES;
    public static GTRecipeType APIS_PROGENITOR_RECIPES;
    public static GTRecipeType MELLIFERIOUS_MATRIX_RECIPES;
    public static GTRecipeType SOURCE_IMBUEMENT_RECIPES, SOURCE_REACTOR_RECIPES, BIO_ENGINE_RECIPES;
    public static GTRecipeType SOURCE_EXTRACTION_RECIPES;
    public static GTRecipeType HIGH_PRESSURE_ARC_FURNACE;
    public static GTRecipeType PHOENIXWARE_FUSION_MK1;
    public static GTRecipeType DIMENSIONAL_ANCHORING_RECIPES, AETHERIAL_FABIRCATION_RECIPES;

    public static void init() {
        AETHERIAL_FABIRCATION_RECIPES = register("aetherial_fabrication", MULTIBLOCK)
                .setEUIO(IO.IN)
                .setMaxIOSize(3, 25, 1, 0)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_ARROW))
                .setSound(GTSoundEntries.ARC);

        JUKEBLOCK = register("jukeblock", MULTIBLOCK)
                .setEUIO(IO.IN)
                .setMaxIOSize(3, 25, 1, 0)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_ARROW))
                .setSound(GTSoundEntries.ARC);

        DIMENSIONAL_ANCHORING_RECIPES = register("dimensional_anchoring", MULTIBLOCK)
                .setEUIO(IO.IN)
                .setMaxIOSize(3, 30, 1, 0)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_ARROW))
                .setSound(GTSoundEntries.ARC);

        PHOENIXWARE_FUSION_MK1 = register("phoenixware_fusion_mk1", MULTIBLOCK)
                .setEUIO(IO.IN)
                .setMaxIOSize(0, 0, 2, 1)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_ARROW))
                .setSound(GTSoundEntries.ARC);

        PLEASE = register("please", MULTIBLOCK)
                .setMaxIOSize(4, 1, 8, 4)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_COMPRESS))
                .setSound(GTSoundEntries.FORGE_HAMMER);

        SWARM_NURTURING_RECIPES = register("swarm_nurturing", MULTIBLOCK)
                .setMaxIOSize(3, 1, 1, 1)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_EXTRACT))
                .setSound(GTSoundEntries.MIXER);

        TESLA_TOWER = register("tesla_tower", MULTIBLOCK)
                .setMaxIOSize(1, 1, 1, 1)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_EXTRACT));

        HONEY_CHAMBER_RECIPES = register("honey_chamber", MULTIBLOCK)
                .setMaxIOSize(4, 4, 4, 4)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_COMPRESS))
                .setSound(GTSoundEntries.BATH);

        SIMULATED_COLONY_RECIPES = register("simulated_colony", MULTIBLOCK)
                .setMaxIOSize(4, 1, 2, 1)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_COMPRESS))
                .setSound(GTSoundEntries.FORGE_HAMMER);

        COMB_DECANTING_RECIPES = register("comb_decanting", MULTIBLOCK)
                .setMaxIOSize(1, 2, 1, 2)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_MACERATE))
                .setSound(GTSoundEntries.CENTRIFUGE);

        MELLIFERIOUS_MATRIX_RECIPES = register("melliferous_matrix", MULTIBLOCK)
                .setMaxIOSize(1, 2, 1, 2)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_MACERATE))
                .setSound(GTSoundEntries.CENTRIFUGE);

        HIGH_PRESSURE_ARC_FURNACE = register("high_pressure_arc_furnace", MULTIBLOCK)
                .setMaxIOSize(2, 2, 2, 2)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_ARROW))
                .setSound(GTSoundEntries.ARC);

        SOURCE_REACTOR_RECIPES = register("source_reactor", MULTIBLOCK)
                .setMaxIOSize(3, 2, 2, 2)
                .setMaxSize(IO.IN, SourceRecipeCapability.CAP, 1)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_ARROW))
                .setSound(GTSoundEntries.REPLICATOR);

        BIO_ENGINE_RECIPES = register("bio_engine", MULTIBLOCK)
                .setMaxIOSize(1, 1, 0, 1)
                .setMaxSize(IO.IN, SourceRecipeCapability.CAP, 1)
                .setMaxSize(IO.OUT, SourceRecipeCapability.CAP, 1)
                .setEUIO(IO.OUT)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_ARROW))
                .setSound(GTSoundEntries.REPLICATOR);

        SOURCE_IMBUEMENT_RECIPES = register("source_imbuement", MULTIBLOCK)
                .setMaxIOSize(3, 3, 1, 0)
                .setMaxSize(IO.IN, SourceRecipeCapability.CAP, 1)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_ARROW))
                .setSound(GTSoundEntries.CHEMICAL);

        SOURCE_EXTRACTION_RECIPES = register("source_extraction", MULTIBLOCK)
                .setMaxIOSize(2, 1, 1, 0)
                .setMaxSize(IO.OUT, SourceRecipeCapability.CAP, 1)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_ARROW))
                .setSound(GTSoundEntries.CHEMICAL);

        APIS_PROGENITOR_RECIPES = register("apis_progenitor", MULTIBLOCK)
                .setMaxIOSize(3, 1, 1, 1)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_EXTRACT))
                .setSound(GTSoundEntries.MIXER);

        HIGH_PERFORMANCE_BREEDER_REACTOR_RECIPES = register("high_performance_breeder_reactor", MULTIBLOCK)
                .setMaxIOSize(2, 2, 2, 2)
                .setEUIO(IO.OUT)
                .UI(builder -> builder.setProgressBarSupplier((layout, value, machine) -> new ProgressWidget()
                        .value(value)
                        .name("progressBar")
                        .texture(PhoenixGuiTextures.PROGRESS_BAR_FISSION, ProgressDrawable.Direction.RIGHT)
                        .size(20)))
                .setSound(GTSoundEntries.CHEMICAL);
        HIGH_PERFORMANCE_BREEDER_REACTOR_RECIPES.getDataInfos().add(data -> {
            int cooling = data.getInt("required_cooling");
            return cooling > 0 ? I18n.get("emi_info.phoenixcore.required_cooling", cooling) : "";
        });

        PRESSURIZED_FISSION_REACTOR_RECIPES = register("pressurized_fission_reactor", MULTIBLOCK)
                .setMaxIOSize(1, 1, 0, 0)
                .setEUIO(IO.OUT)
                .UI(builder -> builder.setProgressBarSupplier((layout, value, machine) -> new ProgressWidget()
                        .value(value)
                        .name("progressBar")
                        .texture(PhoenixGuiTextures.PROGRESS_BAR_FISSION, ProgressDrawable.Direction.RIGHT)
                        .size(20)))
                .setSound(GTSoundEntries.CHEMICAL);
        PRESSURIZED_FISSION_REACTOR_RECIPES.getDataInfos().add(data -> {
            int cooling = data.getInt("required_cooling");
            return cooling > 0 ? I18n.get("emi_info.phoenixcore.required_cooling", cooling) : "";
        });

        ADVANCED_PRESSURIZED_FISSION_REACTOR_RECIPES = register("advanced_pressurized_fission_reactor", MULTIBLOCK)
                .setMaxIOSize(1, 1, 1, 1)
                .setEUIO(IO.OUT)
                .UI(builder -> builder.setProgressBarSupplier((layout, value, machine) -> new ProgressWidget()
                        .value(value)
                        .name("progressBar")
                        .texture(PhoenixGuiTextures.PROGRESS_BAR_FISSION, ProgressDrawable.Direction.RIGHT)
                        .size(20)))
                .setSound(GTSoundEntries.CHEMICAL);
        ADVANCED_PRESSURIZED_FISSION_REACTOR_RECIPES.getDataInfos().add(data -> {
            int cooling = data.getInt("required_cooling");
            return cooling > 0 ? I18n.get("emi_info.phoenixcore.required_cooling", cooling) : "";
        });

        HEAT_EXCHANGER_RECIPES = register("heat_exchanging", MULTIBLOCK)
                .setMaxIOSize(1, 0, 1, 1)
                .setEUIO(IO.OUT)
                .UI(builder -> builder.setProgressBarSupplier((layout, value, machine) -> new ProgressWidget()
                        .value(value)
                        .name("progressBar")
                        .texture(PhoenixGuiTextures.PROGRESS_BAR_FISSION, ProgressDrawable.Direction.RIGHT)
                        .size(20)))
                .setSound(GTSoundEntries.MIXER);
    }
}
