package net.phoenix.core;

import com.gregtechceu.gtceu.api.addon.GTAddon;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.api.addon.events.KJSRecipeKeyEvent;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import com.gregtechceu.gtceu.integration.kjs.recipe.components.ContentJS;

import net.minecraft.data.recipes.FinishedRecipe;
import net.phoenix.core.api.capability.PhoenixRecipeCapabilities;
import net.phoenix.core.common.data.PhoenixCovers;
import net.phoenix.core.common.data.PhoenixFissionMachineRecipes;
import net.phoenix.core.common.data.PhoenixMachineRecipes;
import net.phoenix.core.common.data.PhoenixToolRecipes;
import net.phoenix.core.common.data.materials.PhoenixElements;
import net.phoenix.core.common.data.recipe.generated.*;
import net.phoenix.core.common.machine.multiblock.Shield;
import net.phoenix.core.integration.ars_nouveau.common.data.recipe.SourceHatchRecipes;
import net.phoenix.core.integration.phoenix_tesla_network.common.data.recipe.TeslaHatchRecipes;
import net.phoenix.core.integration.phoenix_tesla_network.common.data.recipe.TeslaMultiAmpHatchRecipes;
import net.phoenix.core.integration.phoenix_tesla_network.common.data.recipe.WirelessChargerRecipes;

import com.mojang.datafixers.util.Pair;

import java.util.function.Consumer;

@SuppressWarnings("unused")
@GTAddon
public class PhoenixGTAddon implements IGTAddon {

    @Override
    public GTRegistrate getRegistrate() {
        return PhoenixCore.PHOENIX_REGISTRATE;
    }

    @Override
    public void initializeAddon() {
        PhoenixCovers.init();
    }

    @Override
    public String addonModId() {
        return PhoenixCore.MOD_ID;
    }

    @Override
    public void registerTagPrefixes() {}

    @Override
    public void addRecipes(Consumer<FinishedRecipe> provider) {
        PhoenixMachineRecipes.init(provider);
        PhoenixFissionMachineRecipes.init(provider);
        PhoenixToolRecipes.init(provider);
        PhoenixBeeRecipeGenerator.loadBeeRecipes(provider);
        CrystalRoseAssemblerGenerator.generateCrystalRoseRecipes(provider);
        TeslaHatchRecipes.init(provider);
        TeslaMultiAmpHatchRecipes.init(provider);
        CustomComponetRecipes.init(provider);
        WirelessChargerRecipes.init(provider);
        SourceHatchRecipes.init(provider);
    }

    @Override
    public void registerElements() {
        IGTAddon.super.registerElements();
        PhoenixElements.init();
    }

    @Override
    public void registerRecipeCapabilities() {
        PhoenixRecipeCapabilities.init();
    }




}
