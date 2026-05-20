package net.phoenix.core.integration.phoenix_fission.common.data.multiblock.fission;

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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.PhoenixAPI;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.phoenix_fission.api.block.IFissionBlanketType;
import net.phoenix.core.integration.phoenix_fission.api.block.IFissionFuelRodType;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@MethodsReturnNonnullByDefault
public class BreederWorkableElectricMultiblockMachine extends DynamicFissionReactorMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            BreederWorkableElectricMultiblockMachine.class,
            DynamicFissionReactorMachine.MANAGED_FIELD_HOLDER);

    private transient List<IFissionBlanketType> activeBlankets = new ArrayList<>();

    @Nullable
    @Getter
    private transient IFissionBlanketType primaryBlanket = null;

    @Persisted
    private long blanketCycleCount = 0;

    @Persisted
    private int blanketCycleTicks = 0;

    public BreederWorkableElectricMultiblockMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        resolveBlanketsFromPersisted();
        selectPrimaryBlanket();
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();

        Object obj = getMultiblockState().getMatchContext().get("BlanketTypes");
        if (obj instanceof List<?> list) {
            this.activeBlankets = (List<IFissionBlanketType>) list;
        } else {
            this.activeBlankets = new ArrayList<>();
        }

        selectPrimaryBlanket();
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        this.activeBlankets.clear();
        this.primaryBlanket = null;
    }

    @Override
    protected @Nullable IFissionFuelRodType getFuelRodForConsumption() {
        if (activeFuelRods == null || activeFuelRods.isEmpty()) return null;
        return activeFuelRods.stream()
                .max(Comparator.comparingInt(IFissionFuelRodType::getTier))
                .orElse(null);
    }

    /**
     * Updated Breeder gate to ensure blankets and fuel are both checked.
     */
    @Override
    protected boolean shouldRunReactor() {
        if (!isFormed()) return false;
        if (activeFuelRods == null || activeFuelRods.isEmpty()) return false;
        if (isScramActive()) return false;

        // The reactor runs as long as it has driver fuel available!
        return hasFuelAvailableForNextTick();
    }

    /**
     * The logic in handleReactorLogic already respects the 'running' boolean.
     * If shouldRunReactor() is false, this won't produce heat or consume items.
     */
    @Override
    protected void handleReactorLogic(boolean running) {
        if (running) {
            lastParallels = Math.max(1, computeParallels());
        }

        // Only calls super (heat/fuel) and processBreeding if running is true
        super.handleReactorLogic(running);

        if (running && isFormed() && primaryBlanket != null) {
            processBreeding(PhoenixConfigs.INSTANCE.fission, lastParallels);
        }
    }

    private void selectPrimaryBlanket() {
        this.primaryBlanket = activeBlankets.stream()
                .max(Comparator.comparingInt(IFissionBlanketType::getTier))
                .orElse(null);
    }

    private void resolveBlanketsFromPersisted() {
        if (this.persistedBlanketIDs == null || this.persistedBlanketIDs.isEmpty()) return;

        this.activeBlankets = new ArrayList<>();
        for (String id : this.persistedBlanketIDs) {
            IFissionBlanketType t = PhoenixAPI.FISSION_BLANKETS.keySet().stream()
                    .filter(b -> b.getName().equals(id))
                    .findFirst()
                    .orElse(null);
            if (t != null) this.activeBlankets.add(t);
        }
    }

    private void processBreeding(PhoenixConfigs.FissionConfigs cfg, int parallels) {
        if (activeBlankets == null || activeBlankets.isEmpty()) return;

        IFissionBlanketType primary = primaryBlanket != null ? primaryBlanket : activeBlankets.get(0);
        IFissionFuelRodType activeFuel = getFuelRodForConsumption();

        // TIER CHECK: If there's no fuel, or the fuel tier is lower than the primary blanket tier, skip breeding
        int fuelTier = activeFuel != null ? activeFuel.getTier() : 0;
        if (fuelTier < primary.getTier()) {
            return; // Breeding is inert, but reactor keeps running
        }

        int duration = Math.max(1, primary.getDurationTicks());

        // GATING CHECK FOR NON-ADDITIVE USAGE: Verify item is present before progressing the cycle
        if (!cfg.blanketUsageAdditive) {
            double burnMul = getBurnMultiplier();
            int p = Math.max(1, parallels);
            int basePerCycle = Math.max(0, primary.getAmountPerCycle());
            int amount = (int) Math.ceil(basePerCycle * p * burnMul);

            if (amount > 0 && !canConsumeResource(primary.getInputKey(), amount)) {
                return; // Missing breeding inputs, pause progression
            }
        }

        blanketCycleTicks++;

        if (blanketCycleTicks < duration) return;

        blanketCycleTicks = 0;
        blanketCycleCount++;

        double burnMul = getBurnMultiplier();
        int p = Math.max(1, parallels);

        int spectrumBias = getReactorSpectrumBias();
        Random rng = makeBlanketRng();

        if (!cfg.blanketUsageAdditive) {
            int basePerCycle = Math.max(0, primary.getAmountPerCycle());
            int amount = (int) Math.ceil(basePerCycle * p * burnMul);
            if (amount <= 0) return;

            if (!tryConsumeResource(primary.getInputKey(), amount)) return;

            var dist = buildAdjustedDistribution(primary, spectrumBias);
            var outputs = sampleOutputs(dist, amount, rng);
            outputBatch(outputs);
            return;
        }

        for (var blanket : activeBlankets) {
            // TIER CHECK FOR ADDITIVE INDIVIDUAL BLANKETS:
            if (fuelTier < blanket.getTier()) continue;

            int basePerCycle = Math.max(0, blanket.getAmountPerCycle());
            int amount = (int) Math.ceil(basePerCycle * p * burnMul);
            if (amount <= 0) continue;

            if (!tryConsumeResource(blanket.getInputKey(), amount)) continue;

            var dist = buildAdjustedDistribution(blanket, spectrumBias);
            var outputs = sampleOutputs(dist, amount, rng);
            outputBatch(outputs);
        }
    }

    private String keyToPrettyName(String key) {
        ResourceLocation rl = ResourceLocation.tryParse(key);
        if (rl != null) {
            Item it = ForgeRegistries.ITEMS.getValue(rl);
            if (it != null && it != net.minecraft.world.item.Items.AIR) {
                return new ItemStack(it, 1).getHoverName().getString();
            }
            var fl = ForgeRegistries.FLUIDS.getValue(rl);
            if (fl != null && fl != Fluids.EMPTY) {
                return Component.translatable(fl.getFluidType().getDescriptionId()).getString();
            }
        }
        return key;
    }

    private boolean canConsumeResource(String key, int amount) {
        if (amount <= 0) return true;

        ItemStack is = resolveKeyToItem(key, amount);
        if (!is.isEmpty()) return canConsumeItem(is);

        return false;
    }

    private boolean canConsumeFluid(FluidStack fs) {
        if (fs.isEmpty()) return true;
        GTRecipe dummy = GTRecipeBuilder.ofRaw()
                .inputFluids(fs)
                .buildRawRecipe();
        return RecipeHelper.matchRecipe(this, dummy).isSuccess();
    }

    private boolean canConsumeItem(ItemStack stack) {
        if (stack.isEmpty()) return true;
        GTRecipe dummy = GTRecipeBuilder.ofRaw()
                .inputItems(stack)
                .buildRawRecipe();
        return RecipeHelper.matchRecipe(this, dummy).isSuccess();
    }

    public static ModifierFunction recipeModifier(MetaMachine machine, GTRecipe recipe) {
        if (!(machine instanceof BreederWorkableElectricMultiblockMachine m)) {
            return com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier
                    .nullWrongType(BreederWorkableElectricMultiblockMachine.class, machine);
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

    // 1. Use the Parent's helper (ensure it's not returning AIR)

    @Nullable
    protected ItemStack resolveKeyToItem(String key, int amount) {
        if (amount <= 0) return ItemStack.EMPTY;

        ResourceLocation rl = ResourceLocation.tryParse(key);
        if (rl == null) return ItemStack.EMPTY;

        // Strictly Forge Registry - bypasses GregTech Material logic
        Item item = ForgeRegistries.ITEMS.getValue(rl);

        // Forge returns AIR if not found.
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            return ItemStack.EMPTY;
        }

        return new ItemStack(item, amount);
    }

    private void tryOutputResource(String key, int amount) {
        if (amount <= 0) return;

        ItemStack is = resolveKeyToItem(key, amount);
        if (!is.isEmpty()) {
            GTRecipe dummy = GTRecipeBuilder.ofRaw()
                    .outputItems(is)
                    .buildRawRecipe();

            // Push to GT Output Buses
            var result = RecipeHelper.handleRecipeIO(this, dummy, IO.OUT, getRecipeLogic().getChanceCaches());

            // If buses are full or IO fails, eject at controller pos
            if (!result.isSuccess()) {
                net.minecraft.world.Containers.dropItemStack(
                        getLevel(),
                        getPos().getX(), getPos().getY(), getPos().getZ(),
                        is);
            }
        }
    }

    // 3. Fix the "roulettePick" and "sampleOutputs"
    // Ensure these call the local tryOutputResource so they benefit from the fix
    private void outputBatch(Map<String, Integer> outputs) {
        for (var e : outputs.entrySet()) {
            tryOutputResource(e.getKey(), e.getValue());
        }
    }

    private boolean tryConsumeResource(String key, int amount) {
        if (amount <= 0) return true;

        ItemStack is = resolveKeyToItem(key, amount);
        if (!is.isEmpty()) return tryConsumeItem(is);

        return false;
    }

    private boolean tryConsumeFluid(FluidStack fs) {
        if (fs.isEmpty()) return true;

        GTRecipe dummy = GTRecipeBuilder.ofRaw()
                .inputFluids(fs)
                .buildRawRecipe();

        if (!RecipeHelper.matchRecipe(this, dummy).isSuccess()) return false;

        return RecipeHelper.handleRecipeIO(
                this,
                dummy,
                IO.IN,
                getRecipeLogic().getChanceCaches()).isSuccess();
    }

    private boolean tryConsumeItem(ItemStack stack) {
        if (stack.isEmpty()) return true;

        GTRecipe dummy = GTRecipeBuilder.ofRaw()
                .inputItems(stack)
                .buildRawRecipe();

        if (!RecipeHelper.matchRecipe(this, dummy).isSuccess()) return false;

        return RecipeHelper.handleRecipeIO(
                this,
                dummy,
                IO.IN,
                getRecipeLogic().getChanceCaches()).isSuccess();
    }

    private record WeightedKey(String key, double weight, int instability) {}

    private int getReactorSpectrumBias() {
        int bias = 0;

        IFissionFuelRodType rod = getFuelRodForConsumption();
        if (rod != null) {
            try {
                bias += rod.getNeutronBias();
            } catch (Throwable ignored) {}
        }

        if (activeModerators != null && !activeModerators.isEmpty()) {
            for (var m : activeModerators) {
                try {} catch (Throwable ignored) {}
            }
        }

        return Math.max(-100, Math.min(100, bias));
    }

    private List<WeightedKey> buildAdjustedDistribution(IFissionBlanketType blanket, int spectrumBias) {
        // Normalize bias to a -1.0 to 1.0 range
        double normalizedBias = spectrumBias / 100.0;

        List<WeightedKey> out = new ArrayList<>();
        for (var bo : blanket.getOutputs()) {
            if (bo == null) continue;
            int baseWeight = Math.max(0, bo.weight());
            if (baseWeight <= 0) continue;

            // Linear scaling: Bias shifts weight based on instability.
            // If bias is positive and instability is high, weight increases.
            double factor = 1.0 + (normalizedBias * bo.instability() * 0.5);
            double adjustedWeight = baseWeight * Math.max(0.1, factor); // Clamp to 10% minimum weight

            out.add(new WeightedKey(bo.key(), adjustedWeight, bo.instability()));
        }
        return out;
    }

    private Random makeBlanketRng() {
        long seed = 0x9E3779B97F4A7C15L;
        if (getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            seed ^= serverLevel.getSeed();
        }

        seed ^= getPos().asLong() * 0xD1B54A32D192ED03L;
        seed ^= blanketCycleCount * 0x94D049BB133111EBL;
        return new Random(seed);
    }

    private Map<String, Integer> sampleOutputs(List<WeightedKey> dist, int amount, Random rng) {
        Map<String, Integer> result = new HashMap<>();
        if (amount <= 0 || dist.isEmpty()) return result;

        double totalWeight = dist.stream().mapToDouble(WeightedKey::weight).sum();
        if (totalWeight <= 0.0) return result;

        // Use a frequency-based approach for large batches (Parallels)
        for (int i = 0; i < amount; i++) {
            double selector = rng.nextDouble() * totalWeight;
            double runningSum = 0.0;
            boolean picked = false;

            for (WeightedKey entry : dist) {
                runningSum += entry.weight();
                if (selector <= runningSum) {
                    result.merge(entry.key(), 1, Integer::sum);
                    picked = true;
                    break;
                }
            }

            // Fallback to avoid "lost" items due to precision errors
            if (!picked) {
                result.merge(dist.get(rng.nextInt(dist.size())).key(), 1, Integer::sum);
            }
        }
        return result;
    }

    private String roulettePick(List<WeightedKey> dist, double total, Random rng) {
        double r = rng.nextDouble() * total;
        double cum = 0.0;
        for (var w : dist) {
            cum += w.weight();
            if (r <= cum) return w.key();
        }
        return dist.get(dist.size() - 1).key();
    }

    @Override
    public void addDisplayText(@NotNull List<Component> textList) {
        super.addDisplayText(textList);

        if (isFormed() && primaryBlanket != null) {
            IFissionFuelRodType activeFuel = getFuelRodForConsumption();
            int fuelTier = activeFuel != null ? activeFuel.getTier() : 0;

            if (fuelTier < primaryBlanket.getRequiredFuelTier()) {
                textList.add(Component
                        .literal("WARNING: Breeding Offline - Requires Tier " + primaryBlanket.getRequiredFuelTier() +
                                "+ Driver Fuel")
                        .withStyle(net.minecraft.ChatFormatting.GOLD));
            } else {
                textList.add(Component.literal("Breeding Active (Using Tier " + fuelTier + " Fuel)")
                        .withStyle(net.minecraft.ChatFormatting.GREEN));
            }
        }
    }
}
