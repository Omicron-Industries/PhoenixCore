package net.phoenix.core.common.data;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.common.data.GTSoundEntries;

import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;

import net.phoenix.core.api.gui.PhoenixGuiTextures;
import net.phoenix.core.integration.ars_nouveau.api.capability.SourceRecipeCapability;

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
                .setSlotOverlay(false, false, GuiTextures.SOLIDIFIER_OVERLAY)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                .setSound(GTSoundEntries.ARC);

        JUKEBLOCK = register("jukeblock", MULTIBLOCK)
                .setEUIO(IO.IN)
                .setMaxIOSize(3, 25, 1, 0)
                .setSlotOverlay(false, false, GuiTextures.SOLIDIFIER_OVERLAY)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                .setSound(GTSoundEntries.ARC);

        DIMENSIONAL_ANCHORING_RECIPES = register("dimensional_anchoring", MULTIBLOCK)
                .setEUIO(IO.IN)
                .setMaxIOSize(3, 30, 1, 0)
                .setSlotOverlay(false, false, GuiTextures.SOLIDIFIER_OVERLAY)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                .setSound(GTSoundEntries.ARC);
        PHOENIXWARE_FUSION_MK1 = register("phoenixware_fusion_mk1", MULTIBLOCK)
                .setEUIO(IO.IN)
                .setMaxIOSize(0, 0, 2, 1)
                .setSlotOverlay(false, false, GuiTextures.SOLIDIFIER_OVERLAY)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                .setSound(GTSoundEntries.ARC);

        PLEASE = register("please", MULTIBLOCK)
                .setMaxIOSize(4, 1, 8, 4)
                .setSlotOverlay(false, false, GuiTextures.BOX_OVERLAY)
                .setProgressBar(GuiTextures.PROGRESS_BAR_COMPRESS, ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                .setSound(GTSoundEntries.FORGE_HAMMER)
                .setEUIO(IO.IN);
        SWARM_NURTURING_RECIPES = register("swarm_nurturing", MULTIBLOCK)
                .setMaxIOSize(3, 1, 1, 1)
                .setSlotOverlay(false, false, GuiTextures.BOX_OVERLAY)
                .setProgressBar(GuiTextures.PROGRESS_BAR_EXTRACT, ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                .setSound(GTSoundEntries.MIXER)
                .setEUIO(IO.IN);

        TESLA_TOWER = register("tesla_tower", MULTIBLOCK)
                .setMaxIOSize(1, 1, 1, 1)
                .setSlotOverlay(false, false, GuiTextures.BOX_OVERLAY)
                .setProgressBar(GuiTextures.PROGRESS_BAR_EXTRACT, ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                .setEUIO(IO.IN);

        HONEY_CHAMBER_RECIPES = register("honey_chamber", MULTIBLOCK)
                .setMaxIOSize(4, 4, 4, 4)
                .setSlotOverlay(false, false, GuiTextures.BOX_OVERLAY)
                .setProgressBar(GuiTextures.PROGRESS_BAR_COMPRESS, ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                .setSound(GTSoundEntries.BATH)
                .setEUIO(IO.IN);

        SIMULATED_COLONY_RECIPES = register("simulated_colony", MULTIBLOCK)
                .setMaxIOSize(4, 1, 2, 1)
                .setSlotOverlay(false, false, GuiTextures.BOX_OVERLAY)
                .setProgressBar(GuiTextures.PROGRESS_BAR_COMPRESS, ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                .setSound(GTSoundEntries.FORGE_HAMMER)
                .setEUIO(IO.IN);

        COMB_DECANTING_RECIPES = register("comb_decanting", MULTIBLOCK)
                .setMaxIOSize(1, 2, 1, 2)
                .setSlotOverlay(false, false, GuiTextures.BOX_OVERLAY)
                .setProgressBar(GuiTextures.PROGRESS_BAR_MACERATE, ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                .setSound(GTSoundEntries.CENTRIFUGE)
                .setEUIO(IO.IN);
        MELLIFERIOUS_MATRIX_RECIPES = register("melliferous_matrix", MULTIBLOCK)
                .setMaxIOSize(1, 2, 1, 2)
                .setSlotOverlay(false, false, GuiTextures.BOX_OVERLAY)
                .setProgressBar(GuiTextures.PROGRESS_BAR_MACERATE, ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                .setSound(GTSoundEntries.CENTRIFUGE)
                .setEUIO(IO.IN);

        HIGH_PRESSURE_ARC_FURNACE = register("high_pressure_arc_furnace", MULTIBLOCK)
                .setMaxIOSize(2, 2, 2, 2)
                .setSlotOverlay(false, false, GuiTextures.SOLIDIFIER_OVERLAY)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                .setSound(GTSoundEntries.ARC)
                .setEUIO(IO.IN);

        SOURCE_REACTOR_RECIPES = register("source_reactor", MULTIBLOCK)
                .setMaxIOSize(3, 2, 2, 2)
                .setMaxSize(IO.IN, SourceRecipeCapability.CAP, 1)
                .setSlotOverlay(false, false, GuiTextures.SOLIDIFIER_OVERLAY)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                .setSound(GTSoundEntries.REPLICATOR)
                .setEUIO(IO.IN)
                .setUiBuilder((recipe, widgetGroup) -> {
                    var size = widgetGroup.getSize();
                    widgetGroup.setSize(size.width, size.height + 5);
                });
        BIO_ENGINE_RECIPES = register("bio_engine", MULTIBLOCK)
                .setMaxIOSize(1, 1, 0, 1)
                .setMaxSize(IO.IN, SourceRecipeCapability.CAP, 1)
                .setMaxSize(IO.OUT, SourceRecipeCapability.CAP, 1)
                .setSlotOverlay(false, false, GuiTextures.SOLIDIFIER_OVERLAY)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                .setSound(GTSoundEntries.REPLICATOR)
                .setEUIO(IO.OUT)
                .setUiBuilder((recipe, widgetGroup) -> {
                    var size = widgetGroup.getSize();
                    widgetGroup.setSize(size.width, size.height + 5);
                });

        SOURCE_IMBUEMENT_RECIPES = register("source_imbuement", MULTIBLOCK)
                .setMaxIOSize(3, 3, 1, 0)
                .setMaxSize(IO.IN, SourceRecipeCapability.CAP, 1)
                .setSlotOverlay(false, false, GuiTextures.SOLIDIFIER_OVERLAY)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                .setSound(GTSoundEntries.CHEMICAL)
                .setUiBuilder((recipe, widgetGroup) -> {
                    var size = widgetGroup.getSize();
                    widgetGroup.setSize(size.width, size.height + 15);
                })
                .setEUIO(IO.IN);

        SOURCE_EXTRACTION_RECIPES = register("source_extraction", MULTIBLOCK)
                .setMaxIOSize(2, 1, 1, 0)
                .setSlotOverlay(false, false, GuiTextures.SOLIDIFIER_OVERLAY)
                .setMaxSize(IO.OUT, SourceRecipeCapability.CAP, 1)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                .setSound(GTSoundEntries.CHEMICAL)
                .setEUIO(IO.IN)
                .setUiBuilder((recipe, widgetGroup) -> {
                    var size = widgetGroup.getSize();
                    widgetGroup.setSize(size.width, size.height + 15);
                });
        APIS_PROGENITOR_RECIPES = register("apis_progenitor", MULTIBLOCK)
                .setMaxIOSize(3, 1, 1, 1)
                .setSlotOverlay(false, false, GuiTextures.BOX_OVERLAY)
                .setProgressBar(GuiTextures.PROGRESS_BAR_EXTRACT, ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                .setSound(GTSoundEntries.MIXER)
                .setEUIO(IO.IN);

        HIGH_PERFORMANCE_BREEDER_REACTOR_RECIPES = register("high_performance_breeder_reactor", MULTIBLOCK)
                .setMaxIOSize(2, 2, 2, 2)
                .setSlotOverlay(false, false, GuiTextures.BOX_OVERLAY)
                .setProgressBar(PhoenixGuiTextures.PROGRESS_BAR_FISSION,
                        ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                .setSound(GTSoundEntries.CHEMICAL)
                .setEUIO(IO.OUT)
                .addDataInfo(data -> {
                    int cooling = data.getInt("required_cooling");
                    if (cooling > 0) {
                        return LocalizationUtils.format("emi_info.phoenixcore.required_cooling", cooling);
                    }
                    return "";
                });

        PRESSURIZED_FISSION_REACTOR_RECIPES = register("pressurized_fission_reactor", MULTIBLOCK)
                .setMaxIOSize(1, 1, 0, 0)
                .setSlotOverlay(false, false, GuiTextures.BOX_OVERLAY)
                .setProgressBar(PhoenixGuiTextures.PROGRESS_BAR_FISSION,
                        ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                .setSound(GTSoundEntries.CHEMICAL)
                .setEUIO(IO.OUT)
                .addDataInfo(data -> {
                    int cooling = data.getInt("required_cooling");
                    if (cooling > 0) {
                        return LocalizationUtils.format("emi_info.phoenixcore.required_cooling", cooling);
                    }
                    return "";
                });
        ADVANCED_PRESSURIZED_FISSION_REACTOR_RECIPES = register("advanced_pressurized_fission_reactor", MULTIBLOCK)
                .setMaxIOSize(1, 1, 1, 1)
                .setSlotOverlay(false, false, GuiTextures.BOX_OVERLAY)
                .setProgressBar(PhoenixGuiTextures.PROGRESS_BAR_FISSION,
                        ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                .setSound(GTSoundEntries.CHEMICAL)
                .addDataInfo(data -> {
                    int cooling = data.getInt("required_cooling");
                    if (cooling > 0) {
                        return LocalizationUtils.format("emi_info.phoenixcore.required_cooling", cooling);
                    }
                    return "";
                });
        HEAT_EXCHANGER_RECIPES = register("heat_exchanging", MULTIBLOCK)
                .setMaxIOSize(1, 0, 1, 1)
                .setSlotOverlay(false, true, GuiTextures.BOX_OVERLAY)
                .setProgressBar(PhoenixGuiTextures.PROGRESS_BAR_FISSION, ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                .setSound(GTSoundEntries.MIXER)
                .setEUIO(IO.OUT);
    }
}
