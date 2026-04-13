package net.phoenix.core.common.machine.multiblock.fission;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.api.block.IFissionCoolerType;
import net.phoenix.core.api.block.IFissionFuelRodType;
import net.phoenix.core.configs.PhoenixConfigs;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@MethodsReturnNonnullByDefault
public class DynamicFissionReactorMachine extends FissionWorkableElectricMultiblockMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            DynamicFissionReactorMachine.class,
            FissionWorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    @Getter
    @Persisted
    private double currentHeatMirror = 0.0;

    public DynamicFissionReactorMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    protected void handleReactorLogic(boolean running) {
        currentHeatMirror = this.heat;

        super.handleReactorLogic(running);

        currentHeatMirror = this.heat;
    }

    public static ModifierFunction recipeModifier(MetaMachine machine, GTRecipe recipe) {
        if (!(machine instanceof FissionWorkableElectricMultiblockMachine m)) {
            return com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier
                    .nullWrongType(FissionWorkableElectricMultiblockMachine.class, machine);
        }
        if (!m.isFormed()) return ModifierFunction.IDENTITY;

        int parallels = Math.max(1, m.computeParallels());

        int euBoost = m.getModeratorEUBoostClamped();
        int fuelDiscount = m.getModeratorFuelDiscountClamped();

        double eutMultiplier = 1.0 + (euBoost / 100.0);
        double durationMultiplier = Math.max(0.01, 1.0 - (fuelDiscount / 100.0));

        var b = ModifierFunction.builder()
                .eutMultiplier(eutMultiplier)
                .durationMultiplier(durationMultiplier);

        if (parallels > 1) {
            var mult = ContentModifier.multiplier(parallels);
            b.inputModifier(mult)
                    .outputModifier(mult)
                    .parallels(parallels);
        }

        return b.build();
    }

    @Override
    protected @Nullable IFissionFuelRodType getFuelRodForConsumption() {
        if (activeFuelRods == null || activeFuelRods.isEmpty()) return null;
        return activeFuelRods.stream()
                .max(Comparator.comparingInt(IFissionFuelRodType::getTier))
                .orElse(null);
    }

    protected boolean canConsumeCoolantForThisTickMachineDriven() {
        var cfg = PhoenixConfigs.INSTANCE.fission;
        if (!cfg.coolingRequiresCoolant) return true;
        if (activeCoolers.isEmpty()) return true;

        if (!cfg.coolantUsageAdditive) {
            IFissionCoolerType primary = primaryCoolerType;
            if (primary == null) return true;

            int mb = Math.max(0, primary.getCoolantPerTick());
            if (mb <= 0) return true;

            String inId = primary.getInputCoolantFluidId();
            if (inId.isEmpty() || "none".equalsIgnoreCase(inId)) return true;

            String outId = primary.getOutputCoolantFluidId();
            return canConvertCoolant(inId, outId, mb);
        }

        Map<String, Integer> required = new HashMap<>();
        Map<String, String> keyToIn = new HashMap<>();
        Map<String, String> keyToOut = new HashMap<>();

        for (IFissionCoolerType c : activeCoolers) {
            int mb = Math.max(0, c.getCoolantPerTick());
            if (mb <= 0) continue;

            String inId = c.getInputCoolantFluidId();
            if (inId.isEmpty() || "none".equalsIgnoreCase(inId)) continue;

            String outId = c.getOutputCoolantFluidId();
            String key = inId + "->" + outId;

            required.merge(key, mb, Integer::sum);
            keyToIn.putIfAbsent(key, inId);
            keyToOut.putIfAbsent(key, outId);
        }

        for (var e : required.entrySet()) {
            String key = e.getKey();
            String inId = keyToIn.getOrDefault(key, "");
            String outId = keyToOut.getOrDefault(key, "");
            if (!canConvertCoolant(inId, outId, e.getValue())) return false;
        }

        return true;
    }

    protected boolean consumeCoolantForThisTickMachineDriven() {
        var cfg = PhoenixConfigs.INSTANCE.fission;
        if (!cfg.coolingRequiresCoolant) return true;
        if (activeCoolers.isEmpty()) return true;

        if (!cfg.coolantUsageAdditive) {
            IFissionCoolerType primary = primaryCoolerType;
            if (primary == null) return true;

            int mb = Math.max(0, primary.getCoolantPerTick());
            if (mb <= 0) return true;

            String inId = primary.getInputCoolantFluidId();
            if (inId.isEmpty() || "none".equalsIgnoreCase(inId)) return true;

            String outId = primary.getOutputCoolantFluidId();
            return tryConvertCoolant(inId, outId, mb);
        }

        Map<String, Integer> required = new HashMap<>();
        Map<String, String> keyToIn = new HashMap<>();
        Map<String, String> keyToOut = new HashMap<>();

        for (IFissionCoolerType c : activeCoolers) {
            int mb = Math.max(0, c.getCoolantPerTick());
            if (mb <= 0) continue;

            String inId = c.getInputCoolantFluidId();
            if (inId.isEmpty() || "none".equalsIgnoreCase(inId)) continue;

            String outId = c.getOutputCoolantFluidId();
            String key = inId + "->" + outId;

            required.merge(key, mb, Integer::sum);
            keyToIn.putIfAbsent(key, inId);
            keyToOut.putIfAbsent(key, outId);
        }

        for (var e : required.entrySet()) {
            String key = e.getKey();
            String inId = keyToIn.getOrDefault(key, "");
            String outId = keyToOut.getOrDefault(key, "");
            if (!tryConvertCoolant(inId, outId, e.getValue())) return false;
        }

        return true;
    }

    /**
     * Fuel consumption + optional spent/depleted fuel output.
     *
     * NOTE: This class overrides the base tickFuelConsumptionMachineDriven(), so any spent-fuel output logic
     * must live here (otherwise it will never run for machines extending this class, e.g. breeders).
     */
    @Override
    protected void tickFuelConsumptionMachineDriven(int parallels) {
        var cfg = PhoenixConfigs.INSTANCE.fission;

        IFissionFuelRodType fuelType = getFuelRodForConsumption();
        if (fuelType == null) return;

        int rodCount = activeFuelRods.size();
        int duration = Math.max(1, fuelType.getDurationTicks());
        int amountPerCycle = Math.max(0, fuelType.getAmountPerCycle());

        double totalPerCycle = amountPerCycle;

        if (cfg.fuelUsageScalesWithRodCount) {
            totalPerCycle *= rodCount;
        }
        if (cfg.fuelUsageScalesWithParallels) {
            totalPerCycle *= Math.max(1, parallels);
        }

        int discountPct = getModeratorFuelDiscountClamped();
        double mult = 1.0 - (discountPct / 100.0);
        if (mult < 0.0) mult = 0.0;
        totalPerCycle *= mult;

        double perTick = totalPerCycle / duration;
        fuelRemainder += perTick;

        int toConsumeNow = (int) Math.floor(fuelRemainder);
        if (toConsumeNow <= 0) return;

        String inItemId = getFuelItemIdCompat(fuelType);
        if (inItemId.isEmpty()) return;

        if (!canConsumeItemKey(inItemId, toConsumeNow)) {
            return;
        }

        if (!tryConsumeItemKey(inItemId, toConsumeNow)) {
            return;
        }

        fuelRemainder -= toConsumeNow;

        String outItemId = getFuelOutputItemIdCompat(fuelType);
        if (!outItemId.isEmpty() && !"none".equalsIgnoreCase(outItemId) && !outItemId.equalsIgnoreCase(inItemId)) {

            int max = getMaxStackSizeForItemId(outItemId);
            int remaining = toConsumeNow;
            while (remaining > 0) {
                int batch = Math.min(max, remaining);
                tryOutputItemId(outItemId, batch);

                remaining -= batch;
            }
        }
    }

    protected boolean canConsumeItemKey(String itemId, int count) {
        if (count <= 0) return true;

        ResourceLocation rl = ResourceLocation.tryParse(itemId);
        if (rl == null) return false;

        Item item = ForgeRegistries.ITEMS.getValue(rl);
        if (item == null || item == net.minecraft.world.item.Items.AIR) return false;

        ItemStack stack = new ItemStack(item, count);

        GTRecipe dummy = GTRecipeBuilder.ofRaw()
                .inputItems(stack)
                .buildRawRecipe();

        return RecipeHelper.matchRecipe(this, dummy).isSuccess();
    }

    protected boolean tryConsumeItemKey(String itemId, int count) {
        if (count <= 0) return true;

        ResourceLocation rl = ResourceLocation.tryParse(itemId);
        if (rl == null) return false;

        Item item = ForgeRegistries.ITEMS.getValue(rl);
        if (item == null || item == net.minecraft.world.item.Items.AIR) return false;

        ItemStack stack = new ItemStack(item, count);

        GTRecipe dummy = GTRecipeBuilder.ofRaw()
                .inputItems(stack)
                .buildRawRecipe();

        if (!RecipeHelper.matchRecipe(this, dummy).isSuccess()) return false;

        return RecipeHelper.handleRecipeIO(this, dummy, IO.IN, getRecipeLogic().getChanceCaches()).isSuccess();
    }
}
