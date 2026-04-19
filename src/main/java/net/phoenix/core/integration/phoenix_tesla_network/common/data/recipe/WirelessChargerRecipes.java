package net.phoenix.core.integration.phoenix_tesla_network.common.data.recipe;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.data.recipe.VanillaRecipeHelper;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.integration.phoenix_tesla_network.common.machine.PhoenixTeslaMachines;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static net.phoenix.core.common.data.recipe.generated.CustomComponetRecipes.*;
import static net.phoenix.core.integration.phoenix_tesla_network.common.data.recipe.TeslaHatchRecipes.*;

public class WirelessChargerRecipes {

    private static final int[] VA = GTValues.VA;

    public static void init(@NotNull Consumer<FinishedRecipe> provider) {
        run(provider);
    }

    public static void run(@NotNull Consumer<FinishedRecipe> provider) {
        for (int tier = GTValues.LV; tier <= GTValues.OpV; tier++) {
            if (PhoenixTeslaMachines.TESLA_WIRELESS_CHARGER[tier] == null) continue;

            if (tier <= EV) {
                processWirelessCharger(provider, tier);
            } else {
                processWirelessChargerWithQuantumChest(provider, tier);
            }
        }
    }

    private static void processWirelessCharger(@NotNull Consumer<FinishedRecipe> provider, int tier) {
        if (GTMachines.SUPER_CHEST[tier] == null || GTMachines.CHARGER_4[tier] == null) return;

        ItemStack stabilizer = getTeslaStabilizerForTier(tier);
        if (stabilizer.isEmpty()) return;

        VanillaRecipeHelper.addShapedRecipe(provider, true,
                "wireless_turbo_charger_" + GTValues.VN[tier].toLowerCase(),
                PhoenixTeslaMachines.TESLA_WIRELESS_CHARGER[tier].asStack(),
                "WCW", "WTW", "RXR",
                'C', GTMachines.SUPER_CHEST[tier].asStack(),
                'T', GTMachines.CHARGER_4[tier].asStack(),
                'R', getOctalWireForTier(tier),
                'X', stabilizer,
                'W', getQuadCableForTier(tier));
    }

    private static void processWirelessChargerWithQuantumChest(@NotNull Consumer<FinishedRecipe> provider, int tier) {
        if (tier >= GTMachines.QUANTUM_CHEST.length || GTMachines.QUANTUM_CHEST[tier] == null ||
                tier >= GTMachines.CHARGER_4.length || GTMachines.CHARGER_4[tier] == null)
            return;

        ItemStack stabilizer = getTeslaStabilizerForTier(tier);
        if (stabilizer.isEmpty()) return;

        VanillaRecipeHelper.addShapedRecipe(provider, true,
                "wireless_turbo_charger_" + GTValues.VN[tier].toLowerCase(),
                PhoenixTeslaMachines.TESLA_WIRELESS_CHARGER[tier].asStack(),
                "WCW", "WTW", "RXR",
                'C', GTMachines.QUANTUM_CHEST[tier].asStack(),
                'T', GTMachines.CHARGER_4[tier].asStack(),
                'R', getOctalWireForTier(tier),
                'X', stabilizer,
                'W', getQuadCableForTier(tier));
    }
}
