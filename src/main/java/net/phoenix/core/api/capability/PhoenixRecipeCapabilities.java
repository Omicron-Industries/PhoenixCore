package net.phoenix.core.api.capability;

import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.phoenix.core.integration.ars_nouveau.api.capability.SourceRecipeCapability;

@SuppressWarnings("all")
public class PhoenixRecipeCapabilities {

    public static final ShieldRecipeCapability SHIELDTYPES = ShieldRecipeCapability.CAP;

    public static void init() {
        GTRegistries.RECIPE_CAPABILITIES.register(SHIELDTYPES.name, SHIELDTYPES);
        GTRegistries.RECIPE_CAPABILITIES.register(SourceRecipeCapability.CAP.name, SourceRecipeCapability.CAP);
    }
}
