package net.phoenix.core.api.capability;

import com.gregtechceu.gtceu.api.registry.GTRegistries;
import net.phoenix.core.integration.ars_nouveau.api.capability.SourceRecipeCapability;

@SuppressWarnings("all")
public class PhoenixRecipeCapabilities {

    public static final ShieldRecipeCapability SHIELDTYPES = ShieldRecipeCapability.CAP;

    public static void init() {
        // FIXED FOR 8.0.0: Use the '.id' field (ResourceLocation) instead of the deleted '.name' string
        GTRegistries.RECIPE_CAPABILITIES.register(SHIELDTYPES.id, SHIELDTYPES);
        GTRegistries.RECIPE_CAPABILITIES.register(SourceRecipeCapability.CAP.id, SourceRecipeCapability.CAP);
    }
}