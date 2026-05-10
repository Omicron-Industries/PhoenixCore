package net.phoenix.core.integration.kubejs;

import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.phoenix.core.PhoenixCore;
import net.phoenix.core.client.renderer.machine.multiblock.PhoenixDynamicRenderHelpers;
import net.phoenix.core.common.block.PhoenixBlocks;
import net.phoenix.core.common.data.PhoenixRecipeTypes;
import net.phoenix.core.common.data.item.PhoenixItems;
import net.phoenix.core.common.data.materials.PhoenixElements;
import net.phoenix.core.common.data.materials.PhoenixMaterials;
import net.phoenix.core.common.data.materials.PhoenixOres;
import net.phoenix.core.common.machine.PhoenixMachines;
import net.phoenix.core.common.machine.multiblock.*;
import net.phoenix.core.common.machine.multiblock.unique.BlazingCleanroom;
import net.phoenix.core.common.machine.multiblock.unique.CreativeEnergyMultiMachine;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.kubejs.recipe.PhoenixRecipeSchema;
import net.phoenix.core.integration.phantasia.PhantasiaKubeJS;

import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.recipe.schema.RegisterRecipeSchemasEvent;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.ClassFilter;

public class PhoenixKubeJSPlugin extends KubeJSPlugin {

    @Override
    public void initStartup() {
        super.initStartup();
    }

    @Override
    public void init() {
        super.init();
    }

    @Override
    public void registerClasses(ScriptType type, ClassFilter filter) {
        super.registerClasses(type, filter);
        filter.allow("net.phoenix.core");
    }

    @Override
    public void registerRecipeSchemas(RegisterRecipeSchemasEvent event) {
        for (var entry : GTRegistries.RECIPE_TYPES.entries()) {
            event.register(entry.getKey(), PhoenixRecipeSchema.SCHEMA);
        }
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        super.registerBindings(event);
        event.add("ShieldType", Shield.ShieldTypes.class);
        event.add("PhoenixMaterials", PhoenixMaterials.class);
        event.add("PhoenixOres", PhoenixOres.class);
        event.add("PhoenixConfigs", PhoenixConfigs.class);
        event.add("BlazingCleanroom", BlazingCleanroom.class);
        event.add("PhoenixElements", PhoenixElements.class);
        event.add("PhoenixBlocks", PhoenixBlocks.class);
        event.add("PhoenixMachines", PhoenixMachines.class);
        event.add("PhoenixResearchMachines", PhoenixMachines.class);
        event.add("CreativeEnergyMultiMachine", CreativeEnergyMultiMachine.class);
        event.add("PhoenixItems", PhoenixItems.class);
        event.add("PhoenixRecipeTypes", PhoenixRecipeTypes.class);
        event.add("PhoenixDynamicRenderHelpers", PhoenixDynamicRenderHelpers.class);
        event.add("PhoenixCore", PhoenixCore.class);

        // Phantasia — only register for client-side startup scripts
        // PhantasiaKubeJS.register() is safe to call from startup scripts;
        // it resolves block registry lookups which are available after startup.
        if (event.getType() == ScriptType.STARTUP) {
            event.add("Phantasia", PhantasiaKubeJS.class);
        }
    }
}
