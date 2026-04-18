package net.phoenix.core.integration.phoenix_fission.common.data.multiblock.fission;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IExplosionMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.PhoenixAPI;
import net.phoenix.core.integration.phoenix_fission.api.block.*;
import net.phoenix.core.integration.phoenix_fission.common.data.block.FissionFuelRodBlock;
import net.phoenix.core.integration.phoenix_fission.common.data.block.FissionModeratorBlock;
import net.phoenix.core.integration.phoenix_fission.common.data.block.PhoenixFissionBlocks;
import net.phoenix.core.configs.PhoenixConfigs;

import lombok.Getter;
import net.phoenix.core.integration.phoenix_fission.common.data.multiblock.part.fission.AdvancedFissionScramHatchPart;
import net.phoenix.core.integration.phoenix_fission.common.data.multiblock.part.fission.FissionScramHatchPart;
import net.phoenix.core.integration.phoenix_fission.common.data.multiblock.part.fission.SensorHatchPartMachine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@MethodsReturnNonnullByDefault
public class FissionWorkableElectricMultiblockMachine extends WorkableElectricMultiblockMachine
                                                      implements IExplosionMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            FissionWorkableElectricMultiblockMachine.class,
            WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);
    private int debugTick = 0;

    private void logEvery20(String msg) {
        if ((debugTick++ % 20) == 0) {
            // PhoenixAPI.LOGGER.info("[FISSION][{}] {}", getPos(), msg);
        }
    }

    @Persisted
    protected int meltdownTimerTicks = -1;

    @Persisted
    protected int meltdownTimerMax = 0;
    @Persisted
    protected long continuousBurnTicks = 0;

    protected transient boolean meltdownInProgress = false;

    @Persisted
    @Getter
    protected double heat = 0.0;

    @Persisted
    protected double fuelRemainder = 0.0;

    @Persisted
    @Getter
    protected List<String> persistedCoolerIDs = new ArrayList<>();
    @Persisted
    @Getter
    protected List<String> persistedModeratorIDs = new ArrayList<>();
    @Persisted
    @Getter
    protected List<String> persistedFuelRodIDs = new ArrayList<>();
    @Persisted
    @Getter
    protected List<String> persistedBlanketIDs = new ArrayList<>();
    @Persisted
    @Getter
    protected List<String> persistedStabilityIDs = new ArrayList<>();
    @Persisted
    @Getter
    protected List<String> persistedSensorIDs = new ArrayList<>();

    @Getter
    protected transient List<IFissionCoolerType> activeCoolers = new ArrayList<>();
    @Getter
    protected transient List<IFissionModeratorType> activeModerators = new ArrayList<>();
    @Getter
    protected transient List<IFissionFuelRodType> activeFuelRods = new ArrayList<>();
    @Getter
    protected transient List<IFissionBlanketType> activeBlankets = new ArrayList<>();
    @Getter
    protected transient List<IFissionStabilityHatchType> activeStabilityHatches = new ArrayList<>();
    @Getter
    protected transient List<IFissionSensorHatchType> activeSensorHatches = new ArrayList<>();

    @Nullable
    protected transient IFissionCoolerType primaryCoolerType = null;
    @Nullable
    protected transient IFissionModeratorType primaryModeratorType = null;
    @Nullable
    protected transient IFissionFuelRodType primaryFuelRodType = null;

    @Persisted
    public int lastRequiredCooling = 0;
    @Persisted
    public int lastProvidedCooling = 0;
    @Persisted
    public boolean lastHasCoolant = true;
    @Persisted
    public boolean lastHotReturnVoided = false;
    @Persisted
    public int lastParallels = 1;
    @Persisted
    public long lastGeneratedEUt = 0;
    @Persisted
    public double lastHeatGainedPerTick = 0.0;
    @Persisted
    public double lastHeatRemovedPerTick = 0.0;

    protected final ConditionalSubscriptionHandler reactorTickHandler;

    public FissionWorkableElectricMultiblockMachine(IMachineBlockEntity holder) {
        super(holder);
        this.reactorTickHandler = new ConditionalSubscriptionHandler(this, this::reactorTick, this::isFormed);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    public static PhoenixConfigs.FissionConfigs cfg() {
        return PhoenixConfigs.INSTANCE.fission;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        resolvePersistedComponents();
        recalcPrimaries();
        clampHeat();
        reactorTickHandler.updateSubscription();
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();

        this.activeCoolers = getListFromContext("CoolerTypes");
        this.activeModerators = getListFromContext("ModeratorTypes");
        this.activeFuelRods = getListFromContext("FuelRodTypes");
        this.activeBlankets = getListFromContext("BlanketTypes");
        this.activeStabilityHatches = getListFromContext("StabilityTypes");
        this.activeSensorHatches = getListFromContext("SensorTypes");

        this.persistedCoolerIDs = new ArrayList<>(
                this.activeCoolers.stream().map(IFissionCoolerType::getName).toList());
        this.persistedModeratorIDs = new ArrayList<>(
                this.activeModerators.stream().map(IFissionModeratorType::getName).toList());
        this.persistedFuelRodIDs = new ArrayList<>(
                this.activeFuelRods.stream().map(IFissionFuelRodType::getName).toList());
        this.persistedBlanketIDs = new ArrayList<>(
                this.activeBlankets.stream().map(IFissionBlanketType::getName).toList());
        this.persistedStabilityIDs = new ArrayList<>(
                this.activeStabilityHatches.stream().map(IFissionStabilityHatchType::getName).toList());
        this.persistedSensorIDs = new ArrayList<>(
                this.activeSensorHatches.stream().map(IFissionSensorHatchType::getName).toList());

        recalcPrimaries();

        if (violatesTierRules()) {
            super.onStructureInvalid();

            activeCoolers.clear();
            activeModerators.clear();
            activeFuelRods.clear();
            activeBlankets.clear();
            activeStabilityHatches.clear();
            activeSensorHatches.clear();

            persistedCoolerIDs.clear();
            persistedModeratorIDs.clear();
            persistedFuelRodIDs.clear();
            persistedBlanketIDs.clear();
            persistedStabilityIDs.clear();
            persistedSensorIDs.clear();

            primaryCoolerType = null;
            primaryModeratorType = null;
            primaryFuelRodType = null;

            meltdownTimerTicks = -1;
            meltdownTimerMax = 0;

            lastRequiredCooling = 0;
            lastProvidedCooling = 0;
            lastHasCoolant = true;

            lastParallels = 1;

            lastGeneratedEUt = 0;
            lastHeatGainedPerTick = 0.0;
            lastHeatRemovedPerTick = 0.0;

            reactorTickHandler.updateSubscription();
            markDirty();
            return;
        }

        meltdownTimerTicks = -1;
        meltdownTimerMax = 0;

        lastParallels = Math.max(1, computeParallels());
        applyParallelsToRecipeLogic(lastParallels);

        reactorTickHandler.updateSubscription();
        markDirty();
    }

    @Override
    public void onStructureInvalid() {
        // We REMOVE the doMeltdown() checks here.
        // Breaking a block is now a valid (if messy) way to stop a meltdown.

        super.onStructureInvalid();

        // Reset all internal logic so the machine "forgets" it was about to explode
        meltdownInProgress = false;
        meltdownTimerTicks = -1;
        meltdownTimerMax = 0;

        activeCoolers.clear();
        activeModerators.clear();
        activeFuelRods.clear();
        activeBlankets.clear();
        activeStabilityHatches.clear();
        activeSensorHatches.clear();

        persistedCoolerIDs.clear();
        persistedModeratorIDs.clear();
        persistedFuelRodIDs.clear();
        persistedBlanketIDs.clear();
        persistedStabilityIDs.clear();
        persistedSensorIDs.clear();

        primaryCoolerType = null;
        primaryModeratorType = null;
        primaryFuelRodType = null;

        lastRequiredCooling = 0;
        lastProvidedCooling = 0;
        lastHasCoolant = true;

        lastParallels = 1;

        lastGeneratedEUt = 0;
        lastHeatGainedPerTick = 0.0;
        lastHeatRemovedPerTick = 0.0;

        reactorTickHandler.updateSubscription();
        markDirty();
    }

    @SuppressWarnings("unchecked")
    protected <T> List<T> getListFromContext(String key) {
        Object obj = getMultiblockState().getMatchContext().get(key);
        if (obj instanceof List<?> list) return (List<T>) list;
        return new ArrayList<>();
    }

    protected void recalcPrimaries() {
        this.primaryCoolerType = getPrimaryCooler(this.activeCoolers);
        this.primaryModeratorType = getPrimaryModerator(this.activeModerators);
        this.primaryFuelRodType = getPrimaryFuelRod(this.activeFuelRods);
    }

    /**
     * Runs every tick while formed, regardless of having a GTRecipe.
     */
    protected void reactorTick() {
        if (getLevel() == null || getLevel().isClientSide) return;
        if (!isFormed()) return;

        lastParallels = Math.max(1, computeParallels());

        boolean running = shouldRunReactor();
        setMachineActiveSafe(running);

        handleReactorLogic(running);
        this.lastRunning = running;

        // ── HATCH TICKS ──────────────────────────────────────────────────────
        // Sensor hatches push neighbour updates so the redstone network stays
        // in sync. Advanced scram hatches need a tick to run their sustain timer.
        tickSensorHatches();
        tickAdvancedScramHatches();
        // ─────────────────────────────────────────────────────────────────────


        markDirty();

    }

    public boolean isScramActive() {
        for (var part : getParts()) {
            if (part instanceof FissionScramHatchPart basic && basic.isScrammed()) return true;
            if (part instanceof AdvancedFissionScramHatchPart advanced && advanced.isScrammed()) return true;
        }
        return false;
    }

    /**
     * Drives updateSignal() on every sensor hatch each reactor tick so their
     * redstone output stays current with the reactor's heat level.
     */
    protected void tickSensorHatches() {
        for (var part : getParts()) {
            if (part instanceof SensorHatchPartMachine sensor) {
                sensor.updateSignal();
            }
        }
    }

    /**
     * Drives the sustain timer on every advanced scram hatch each reactor tick.
     * Without this, the sustain counter only increments on neighbour-change
     * events, which are unreliable for tick-accurate timing.
     */
    protected void tickAdvancedScramHatches() {
        for (var part : getParts()) {
            if (part instanceof AdvancedFissionScramHatchPart advanced) {
                advanced.tick();
            }
        }
    }


    /**
     * Updated to act as a safety gate. If fuel is missing for the current
     * parallel count, it returns false, causing the reactor to stall safely.
     */
    protected boolean shouldRunReactor() {
        if (!isFormed()) return false;
        if (activeFuelRods.isEmpty()) return false;
        if (isScramActive()) return false;

        // Check Coolant if required
        if (cfg().coolingRequiresCoolant && !activeCoolers.isEmpty()) {
            boolean ok = canConsumeCoolantForThisTickMachineDriven();
            if (!ok) {
                // Effectively stalls the reactor if coolant is empty
                return false;
            }
        }

        // Check Fuel for the CURRENT parallel count
        // hasFuelAvailableForNextTick() already calculates based on lastParallels
        if (!hasFuelAvailableForNextTick()) {
            // This is the "Stall" point. By returning false, the reactor logic
            // in reactorTick() will call setMachineActiveSafe(false).
            return false;
        }

        return true;
    }

    /**
     * Ensures fuel consumption is handled safely.
     */
    protected void consumeFuel(IFissionFuelRodType fuelType, int parallels) {
        String itemId = fuelType.getFuelKey();
        int amountPerCycle = Math.max(0, fuelType.getAmountPerCycle());
        if (amountPerCycle <= 0 || itemId == null || itemId.isEmpty()) return;

        // Calculate total needed for this tick
        double totalNeeded = amountPerCycle;
        if (cfg().fuelUsageScalesWithRodCount) totalNeeded *= activeFuelRods.size();
        if (cfg().fuelUsageScalesWithParallels) totalNeeded *= parallels;

        int discountPct = getModeratorFuelDiscountClamped();
        totalNeeded *= (1.0 - (discountPct / 100.0));

        double perTick = totalNeeded / Math.max(1, fuelType.getDurationTicks());
        fuelRemainder += perTick;

        int toConsumeNow = (int) Math.floor(fuelRemainder);
        if (toConsumeNow > 0) {
            // SECONDARY SAFETY: If for some reason we got here but can't consume,
            // we reset heat to 0 production rather than melting down.
            if (!tryConsumeItemKey(itemId, toConsumeNow)) {
                fuelRemainder = 0; // Reset to avoid double-dipping next tick
                return;
            }
            fuelRemainder -= toConsumeNow;
        }
    }

    private boolean violatesTierRules() {
        var cfg = cfg();

        if (cfg.restrictFuelRodTier && activeFuelRods.size() > 1) {
            int t = activeFuelRods.get(0).getTier();
            for (var r : activeFuelRods) if (r.getTier() != t) return true;
        }
        if (cfg.requiredFuelRodTier >= 0) {
            for (var r : activeFuelRods) if (r.getTier() != cfg.requiredFuelRodTier) return true;
        }

        if (cfg.restrictCoolerTier && activeCoolers.size() > 1) {
            int t = activeCoolers.get(0).getTier();
            for (var c : activeCoolers) if (c.getTier() != t) return true;
        }
        if (cfg.requiredCoolerTier >= 0) {
            for (var c : activeCoolers) if (c.getTier() != cfg.requiredCoolerTier) return true;
        }

        return false;
    }

    protected void tryOutputFluidId(@NotNull String fluidId, int mb) {
        if (mb <= 0) return;

        FluidStack fs = resolveFluidStack(fluidId, mb);
        if (fs.isEmpty()) return;

        GTRecipe dummy = GTRecipeBuilder.ofRaw()
                .outputFluids(fs)
                .buildRawRecipe();

        RecipeHelper.handleRecipeIO(this, dummy, IO.OUT, getRecipeLogic().getChanceCaches());
    }

    protected void tryOutputItemId(@NotNull String itemId, int items) {
        if (items <= 0) return;

        ItemStack is = resolveItemStack(itemId, items);
        if (is.isEmpty()) return;

        GTRecipe dummy = GTRecipeBuilder.ofRaw()
                .outputItems(is)
                .buildRawRecipe();

        RecipeHelper.handleRecipeIO(this, dummy, IO.OUT, getRecipeLogic().getChanceCaches());
    }

    protected boolean canConvertCoolant(@NotNull String inFluidId, @NotNull String outFluidId, int mb) {
        if (mb <= 0) return true;
        if (inFluidId.isEmpty() || "none".equalsIgnoreCase(inFluidId)) return true;

        return canConsumeFluidId(inFluidId, mb);
    }

    protected boolean tryConvertCoolant(@NotNull String inFluidId, @NotNull String outFluidId, int mb) {
        if (mb <= 0) return true;
        if (inFluidId.isEmpty() || "none".equalsIgnoreCase(inFluidId)) return true;

        if (!canConsumeFluidId(inFluidId, mb)) return false;

        if (!tryConsumeFluidId(inFluidId, mb)) return false;

        if (!outFluidId.isEmpty() && !"none".equalsIgnoreCase(outFluidId) && !outFluidId.equalsIgnoreCase(inFluidId)) {
            tryOutputFluidId(outFluidId, mb);
        }

        return true;
    }

    protected boolean hasFuelAvailableForNextTick() {
        IFissionFuelRodType fuelType = getFuelRodForConsumption();
        if (fuelType == null) return false;

        int duration = Math.max(1, fuelType.getDurationTicks());
        int amountPerCycle = Math.max(0, fuelType.getAmountPerCycle());

        if (amountPerCycle <= 0) return true;

        double totalPerCycle = amountPerCycle;

        if (cfg().fuelUsageScalesWithRodCount) {
            totalPerCycle *= Math.max(1, activeFuelRods.size());
        }
        if (cfg().fuelUsageScalesWithParallels) {
            totalPerCycle *= Math.max(1, lastParallels);
        }

        int discountPct = getModeratorFuelDiscountClamped();
        double mult = 1.0 - (discountPct / 100.0);
        if (mult < 0.0) mult = 0.0;
        totalPerCycle *= mult;

        double perTick = totalPerCycle / duration;

        String itemId = fuelType.getFuelKey();
        if (itemId == null || itemId.isEmpty()) return false;

        if (!canConsumeItemKey(itemId, 1)) {
            return false;
        }

        double next = fuelRemainder + perTick;
        int wouldConsume = (int) Math.floor(next);

        if (wouldConsume <= 0) {
            return true;
        }

        return canConsumeItemKey(itemId, wouldConsume);
    }

    protected boolean canConsumeItemKey(String itemId, int count) {
        if (count <= 0) return true;

        ResourceLocation rl = ResourceLocation.tryParse(itemId);
        if (rl == null) return false;

        Item item = ForgeRegistries.ITEMS.getValue(rl);
        if (item == null || item == Items.AIR) return false;

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
        if (item == null || item == Items.AIR) return false;

        ItemStack stack = new ItemStack(item, count);

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

    protected void setMachineActiveSafe(boolean active) {
        try {
            Method m = this.getClass().getMethod("setActive", boolean.class);
            m.invoke(this, active);
        } catch (Throwable ignored) {}
    }

    protected void handleReactorLogic(boolean running) {
        lastHeatGainedPerTick = 0.0;
        lastHeatRemovedPerTick = 0.0;
        lastGeneratedEUt = 0;

        if (running) {
            continuousBurnTicks++;
        } else {
            continuousBurnTicks = 0;
        }

        // 1. Heat Production (Only if Running)
        if (running && !activeFuelRods.isEmpty()) {
            lastParallels = Math.max(1, computeParallels());
            applyParallelsToRecipeLogic(lastParallels);

            double heatProduced = computeHeatProducedPerTick(lastParallels);
            heat += heatProduced;
            lastHeatGainedPerTick = heatProduced;

            tickFuelConsumptionMachineDriven(lastParallels);
            tickMachineOutputs(lastParallels);
        } else {
            lastParallels = Math.max(1, computeParallels());

            // --- NEW VARIABLE PASSIVE COOLING ---
            if (cfg().passiveCooling) {
                // Calculate a 0.5% decay based on current heat
                double variableCooling = this.heat * 0.005;
                // Ensure we at least lose the idleHeatLoss so it doesn't stall near 0
                heat -= Math.max(cfg().idleHeatLoss, variableCooling);
            }
        }

        // 2. Cooling Calculation
        int totalCooling = activeCoolers.stream()
                .mapToInt(IFissionCoolerType::getCoolerTemperature)
                .sum();
        lastProvidedCooling = totalCooling;

        lastHasCoolant = true;
        if (cfg().coolingRequiresCoolant && !activeCoolers.isEmpty()) {
            lastHasCoolant = canConsumeCoolantForThisTickMachineDriven() && consumeCoolantForThisTickMachineDriven();
        }

        // 3. Apply Cooling (Even when Off!)
        double removed = 0.0;
        if (!cfg().coolingRequiresCoolant || lastHasCoolant) {
            double aboveMin = Math.max(0.0, heat - cfg().minHeat);

            // If running, use full cooling.
            // If off, perhaps coolers run at 25% efficiency (residual coolant flow)
            double coolingPower = running ? totalCooling : (totalCooling * 0.25);

            removed = Math.min(aboveMin, coolingPower);
            heat -= removed;
        }

        lastHeatRemovedPerTick = removed;
        clampHeat();
        tickPowerGeneration(running);
        tickMeltdown();
    }

    /**
     * Slow-burn reward multiplier.
     * Ramps from 1.0 to 1.0 + (burnBonusMaxPercent/100) over burnBonusRampSeconds.
     */
    protected double getBurnMultiplier() {
        var cfg = PhoenixConfigs.INSTANCE.fission;

        double maxPct = Math.max(0.0, cfg.burnBonusMaxPercent);
        double rampSec = Math.max(1.0, cfg.burnBonusRampSeconds);
        double rampTicks = rampSec * 20.0;

        double t = Math.min(1.0, continuousBurnTicks / rampTicks);
        return 1.0 + (maxPct / 100.0) * t;
    }

    protected void clampHeat() {
        if (heat < cfg().minHeat) heat = cfg().minHeat;
    }

    protected int computeParallels() {
        int rodCount = activeFuelRods.size();
        int base = cfg().parallelsPerFuelRod * Math.max(0, rodCount);

        int moderatorBonus = activeModerators.stream()
                .mapToInt(IFissionModeratorType::getParallelBonus)
                .sum();

        int heatBonus = 0;
        if (cfg().heatPerParallel > 0.0) {
            heatBonus = (int) Math.floor(Math.max(0.0, heat) / cfg().heatPerParallel);
        }

        int total = base + moderatorBonus + heatBonus;
        if (total < 1) total = 1;
        return Math.min(total, cfg().maxParallels);
    }

    protected double computeHeatProducedPerTick(int parallels) {
        double base = 0.0;
        try {

            base = Math.max(0.0, cfg().baseHeatPerTick);
        } catch (Throwable ignored) {}

        double rods = activeFuelRods.stream()
                .mapToDouble(IFissionFuelRodType::getBaseHeatProduction)
                .sum();

        double modAdd = activeModerators.stream()
                .mapToDouble(IFissionModeratorType::getHeatMultiplier)
                .sum();
        double moderatorMult = 0.1 + (modAdd / 10.0);

        double p = Math.max(1.0, parallels);

        double burn = getBurnMultiplier();

        return (base + (rods * moderatorMult)) * p * burn;
    }

    /**
     * Steam machines should override this to do nothing.
     */
    protected void tickPowerGeneration(boolean running) {
        var cfg = cfg();

        double activity;

        if (running && lastHeatGainedPerTick > 0.0) {
            activity = lastHeatGainedPerTick;
        } else {

            activity = Math.max(0.0, heat);
        }

        if (activity <= 0.0001) {
            lastGeneratedEUt = 0;
            return;
        }

        double baseEU = activity * cfg.euPerHeatUnit;

        double heatFrac = cfg.maxSafeHeat <= 0 ? 0.0 : (heat / cfg.maxSafeHeat);
        heatFrac = Math.max(0.0, Math.min(1.5, heatFrac));

        double start = Math.max(0.0, Math.min(0.99, cfg.powerStartFraction));
        double x = (heatFrac - start) / Math.max(1e-9, (1.0 - start));
        x = Math.max(0.0, Math.min(1.0, x));

        double dangerBonus = 1.0 + Math.pow(x, Math.max(0.01, cfg.powerCurveExponent)) * 1.5;

        double burnBonus = getBurnMultiplier();

        double eut = baseEU * dangerBonus * burnBonus;

        if (cfg.maxGeneratedEUt > 0) {
            eut = Math.min(eut, cfg.maxGeneratedEUt);
        }

        long out = (long) Math.floor(eut);

        if (running && cfg.minGeneratedEUt > 0) {
            out = Math.max(out, cfg.minGeneratedEUt);
        }

        if (out <= 0) {
            lastGeneratedEUt = 0;
            return;
        }

        lastGeneratedEUt = out;
        tryAddEnergy(out);
    }

    public int getMeltdownSecondsRemaining() {
        if (meltdownTimerTicks <= 0) return 0;
        return (int) Math.ceil(meltdownTimerTicks / 20.0);
    }

    public double getNetHeatPerTick() {
        return lastHeatGainedPerTick - lastHeatRemovedPerTick;
    }

    private boolean lastRunning = false;

    public boolean wasRunningLastTick() {
        return lastRunning;
    }

    protected void tryAddEnergy(long eu) {
        if (eu <= 0) return;

        try {
            Object container = this.getEnergyContainer();
            if (container != null) {
                Method add = container.getClass().getMethod("addEnergy", long.class);
                add.invoke(container, eu);
                return;
            }
        } catch (Throwable ignored) {}

        try {
            Method add = this.getClass().getMethod("addEnergy", long.class);
            add.invoke(this, eu);
        } catch (Throwable ignored) {}
    }

    @Getter
    @Persisted
    private boolean runningForHud = false;

    protected boolean tryConsumeCoolantFluidId(@NotNull IFissionCoolerType cooler, int amountMb) {
        if (amountMb <= 0) return true;
        String fluidId = getCoolerCoolantFluidIdCompat(cooler);
        if (fluidId.isEmpty()) return true;
        return tryConsumeFluidId(fluidId, amountMb);
    }

    protected boolean canConsumeFluidId(@NotNull String fluidId, int mb) {
        if (mb <= 0) return true;

        FluidStack fs = resolveFluidStack(fluidId, mb);
        if (fs.isEmpty()) return false;

        GTRecipe dummy = GTRecipeBuilder.ofRaw()
                .inputFluids(fs)
                .buildRawRecipe();

        return RecipeHelper.matchRecipe(this, dummy).isSuccess();
    }

    protected boolean tryConsumeFluidId(@NotNull String fluidId, int mb) {
        if (mb <= 0) return true;

        FluidStack fs = resolveFluidStack(fluidId, mb);
        if (fs.isEmpty()) {
            // PhoenixAPI.LOGGER.warn("[FISSION][{}] Unknown fluid id '{}' (Forge registry lookup failed)", getPos(),
            // fluidId);
            return false;
        }

        GTRecipe dummy = GTRecipeBuilder.ofRaw()
                .inputFluids(fs)
                .buildRawRecipe();

        if (!RecipeHelper.matchRecipe(this, dummy).isSuccess()) return false;

        return RecipeHelper.handleRecipeIO(this, dummy, IO.IN, getRecipeLogic().getChanceCaches()).isSuccess();
    }

    protected FluidStack resolveFluidStack(@NotNull String fluidId, int mb) {
        ResourceLocation rl = ResourceLocation.tryParse(fluidId);
        if (rl == null) return FluidStack.EMPTY;

        var fluid = ForgeRegistries.FLUIDS.getValue(rl);
        if (fluid == null) return FluidStack.EMPTY;

        return new FluidStack(fluid, Math.max(1, mb));
    }

    protected ItemStack resolveItemStack(@NotNull String itemId, int count) {
        ResourceLocation rl = ResourceLocation.tryParse(itemId);
        if (rl == null) return ItemStack.EMPTY;

        var item = ForgeRegistries.ITEMS.getValue(rl);
        if (item == null) return ItemStack.EMPTY;

        return new ItemStack(item, Math.max(1, count));
    }

    protected int getMaxStackSizeForItemId(@NotNull String itemId) {
        ResourceLocation rl = ResourceLocation.tryParse(itemId);
        if (rl == null) return 64;
        Item item = ForgeRegistries.ITEMS.getValue(rl);
        if (item == null || item == Items.AIR) return 64;
        try {
            return Math.max(1, item.getMaxStackSize());
        } catch (Throwable ignored) {
            return 64;
        }
    }

    protected String getCoolerCoolantFluidIdCompat(IFissionCoolerType cooler) {
        for (String name : new String[] {
                "getRequiredCoolantFluidId",
                "getCoolantFluidId",
                "getRequiredCoolantFluidID",
                "getCoolantFluidID"
        }) {
            try {
                Method m = cooler.getClass().getMethod(name);
                Object v = m.invoke(cooler);
                if (v instanceof String s) return s;
                if (v instanceof ResourceLocation rl) return rl.toString();
            } catch (Throwable ignored) {}
        }

        return "";
    }

    protected String getFuelOutputItemIdCompat(IFissionFuelRodType rod) {
        for (String name : new String[] {
                "getOutputFuelItemId",
                "getDepletedFuelItemId",
                "getSpentFuelItemId",
                "getFuelProductItemId",
                "getOutputItemId",
                "getOutputKey"
        }) {
            try {
                Method m = rod.getClass().getMethod(name);
                Object v = m.invoke(rod);
                if (v instanceof String s) return s;
            } catch (Throwable ignored) {}
        }

        for (String name : new String[] { "getOutputFuelKey", "getDepletedFuelKey", "getSpentFuelKey" }) {
            try {
                Method m = rod.getClass().getMethod(name);
                Object v = m.invoke(rod);
                if (v instanceof String s) return s;
            } catch (Throwable ignored) {}
        }

        return "";
    }

    protected @Nullable IFissionFuelRodType getFuelRodForConsumption() {
        return primaryFuelRodType != null ? primaryFuelRodType :
                (activeFuelRods.isEmpty() ? null : activeFuelRods.get(0));
    }

    protected void tickFuelConsumptionMachineDriven(int parallels) {
        IFissionFuelRodType fuelType = getFuelRodForConsumption();
        if (fuelType == null) return;

        int rodCount = activeFuelRods.size();
        int duration = Math.max(1, fuelType.getDurationTicks());
        int amountPerCycle = Math.max(0, fuelType.getAmountPerCycle());

        double totalPerCycle = amountPerCycle;

        if (cfg().fuelUsageScalesWithRodCount) totalPerCycle *= rodCount;
        if (cfg().fuelUsageScalesWithParallels) totalPerCycle *= Math.max(1, parallels);

        int discountPct = getModeratorFuelDiscountClamped();
        double mult = 1.0 - (discountPct / 100.0);
        if (mult < 0.0) mult = 0.0;
        totalPerCycle *= mult;

        double perTick = totalPerCycle / duration;
        fuelRemainder += perTick;

        int toConsumeNow = (int) Math.floor(fuelRemainder);
        if (toConsumeNow <= 0) return;

        fuelRemainder -= toConsumeNow;

        String itemId = getFuelItemIdCompat(fuelType);
        if (itemId.isEmpty()) return;

        if (!tryConsumeItemId(itemId, toConsumeNow)) {
            heat = Math.max(heat, cfg().maxSafeHeat + 1.0);
            return;
        }

        String outItemId = getFuelOutputItemIdCompat(fuelType);
        if (!outItemId.isEmpty() && !"none".equalsIgnoreCase(outItemId) && !outItemId.equalsIgnoreCase(itemId)) {

            int max = getMaxStackSizeForItemId(outItemId);
            int remaining = toConsumeNow;
            while (remaining > 0) {
                int batch = Math.min(max, remaining);
                tryOutputItemId(outItemId, batch);
                remaining -= batch;
            }
        }
    }

    protected boolean tryConsumeItemId(@NotNull String itemId, int count) {
        if (count <= 0) return true;

        ResourceLocation rl = ResourceLocation.tryParse(itemId);
        if (rl == null) return false;

        var item = ForgeRegistries.ITEMS.getValue(rl);
        if (item == null) return false;

        var stack = new ItemStack(item, count);
        if (stack.isEmpty()) return false;

        GTRecipe dummy = GTRecipeBuilder.ofRaw()
                .inputItems(stack)
                .buildRawRecipe();

        if (!RecipeHelper.matchRecipe(this, dummy).isSuccess()) return false;

        return RecipeHelper.handleRecipeIO(this, dummy, IO.IN, getRecipeLogic().getChanceCaches()).isSuccess();
    }

    /**
     * COMPAT: If you haven't renamed IFissionFuelRodType yet:
     * - preferred: getFuelItemId()
     * - fallback: getFuelKey()
     */
    protected String getFuelItemIdCompat(IFissionFuelRodType rod) {
        try {
            Method m = rod.getClass().getMethod("getFuelItemId");
            Object v = m.invoke(rod);
            if (v instanceof String s) return s;
        } catch (Throwable ignored) {}

        try {
            return rod.getFuelKey();
        } catch (Throwable ignored) {
            return "";
        }
    }

    /**
     * Steam machines override to output steam (and possibly consume water).
     */
    protected void tickMachineOutputs(int parallels) {}

    protected void tickMeltdown() {
        if (heat >= cfg().maxHeatClamp) {
            heat = cfg().maxHeatClamp;

            int minTicks = (int) Math.max(1, Math.floor(cfg().meltdown.minGraceSeconds * 20.0));
            meltdownTimerMax = minTicks;

            if (meltdownTimerTicks < 0) {
                meltdownTimerTicks = minTicks;
            }         } else if (meltdownTimerTicks > 0) {
            // ── SCRAM PAUSE ─────────────────────────────────────────────────
            // While scrammed the countdown is frozen. Removing the scram while
            // heat is still critical resumes the timer right where it left off
            // — hasty fixes are punished.
            if (!isScramActive()) {
                meltdownTimerTicks -= 1;
            }




        if (meltdownTimerTicks == 0) {
                doMeltdown();
            }
            return;
        }

        double safe = cfg().maxSafeHeat;

        if (heat <= safe) {
            if (cfg().meltdown.clearTimerWhenSafe) {
                meltdownTimerTicks = -1;
                meltdownTimerMax = 0;
            }
            return;
        }

        double excess = heat - safe;
        double sev = Math.max(0.0001, cfg().meltdown.excessHeatSeverity);

        double excessPct = excess / Math.max(1.0, safe);
        double scaled = excessPct * sev;

        double base = cfg().meltdown.baseGraceSeconds;
        double min = cfg().meltdown.minGraceSeconds;

        double grace = base - (base - min) * Math.min(1.0, scaled);
        if (grace < min) grace = min;

        meltdownTimerMax = (int) Math.max(1, Math.floor(grace * 20.0));

        if (meltdownTimerTicks < 0) {
            meltdownTimerTicks = meltdownTimerMax;
        } else if (meltdownTimerTicks > 0) {
            meltdownTimerTicks -= 1;
        }

        if (meltdownTimerTicks == 0) {
            doMeltdown();
        }
    }

    protected void doMeltdown() {
        if (meltdownInProgress) return;
        meltdownInProgress = true;

        if (getLevel() instanceof net.minecraft.server.level.ServerLevel world) {
            BlockPos controllerPos = getPos();
            var state = this.getMultiblockState();

            // 1. Capture the structural blocks BEFORE unforming
            List<BlockPos> structureBlocks = new ArrayList<>();
            if (state != null && this.isFormed()) {
                structureBlocks.addAll(state.getCache());
            }

            // 2. UNFORM THE MACHINE
            // This stops the reactor logic and detaches the controller from the blocks
            this.onStructureInvalid();

            // 3. TARGETED VAPORIZATION
            if (cfg().explosion.destructiveExplosion) {
                for (BlockPos structurePos : structureBlocks) {
                    if (structurePos.equals(controllerPos)) continue;

                    var blockState = world.getBlockState(structurePos);
                    var block = blockState.getBlock();

                    // Check for your specific targets
                    boolean shouldVaporize =
                            blockState.is(PhoenixFissionBlocks.FISSILE_HEAT_SAFE_CASING.get()) ||
                                    blockState.is(PhoenixFissionBlocks.FISSILE_REACTION_SAFE_CASING.get()) ||
                                    blockState.is(PhoenixFissionBlocks.FISSILE_SAFE_GEARBOX_CASING.get()) ||
                                    blockState.is(Blocks.TINTED_GLASS) ||
                                    block instanceof FissionFuelRodBlock ||
                                    block instanceof FissionModeratorBlock;

                    if (shouldVaporize) {
                        // Vaporize: remove block, no drops, spawn smoke
                        world.removeBlock(structurePos, false);
                        world.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                                structurePos.getX() + 0.5, structurePos.getY() + 0.5, structurePos.getZ() + 0.5,
                                3, 0.1, 0.1, 0.1, 0.05);
                    }
                }
            }

            // 4. THE VISUAL/AUDIO POP
            // We use NONE interaction so it doesn't break the dirt/floor
            float explosionPower = cfg().explosion.baseExplosionPower +
                    (float) (activeFuelRods.size() * cfg().explosion.explosionPowerPerFuelRod);

            world.explode(null, controllerPos.getX() + 0.5, controllerPos.getY() + 0.5, controllerPos.getZ() + 0.5,
                    explosionPower, false, Level.ExplosionInteraction.NONE);

            // 5. REMOVE CONTROLLER
            world.removeBlock(controllerPos, false);
        }

        // Reset stats
        meltdownTimerTicks = -1;
        heat = cfg().minHeat;
        meltdownInProgress = false;
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

            return canConvertCoolant(inId, primary.getOutputCoolantFluidId(), mb);
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
            if (!canConvertCoolant(keyToIn.get(key), keyToOut.get(key), e.getValue())) return false;
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

            return tryConvertCoolant(inId, primary.getOutputCoolantFluidId(), mb);
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
            if (!tryConvertCoolant(keyToIn.get(key), keyToOut.get(key), e.getValue())) return false;
        }
        return true;
    }


    protected void applyParallelsToRecipeLogic(int parallels) {
        try {
            Object logic = this.getRecipeLogic();
            if (logic == null) return;

            for (String name : new String[] { "setParallelLimit", "setMaxParallel", "setParallel" }) {
                try {
                    Method m = logic.getClass().getMethod(name, int.class);
                    m.invoke(logic, parallels);
                    return;
                } catch (NoSuchMethodException ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    public int getModeratorEUBoostClamped() {
        int sum = activeModerators.stream().mapToInt(IFissionModeratorType::getEUBoost).sum();
        return Math.min(sum, cfg().maxEUBoostPercent);
    }

    public int getModeratorFuelDiscountClamped() {
        int sum = activeModerators.stream().mapToInt(IFissionModeratorType::getFuelDiscount).sum();
        return Math.min(sum, cfg().maxFuelDiscountPercent);
    }

    public static com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction recipeModifier(
                                                                                            com.gregtechceu.gtceu.api.machine.MetaMachine machine,
                                                                                            GTRecipe recipe) {
        if (!(machine instanceof FissionWorkableElectricMultiblockMachine m))
            return com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier
                    .nullWrongType(FissionWorkableElectricMultiblockMachine.class, machine);

        if (!m.isFormed()) return com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction.IDENTITY;

        int parallels = Math.max(1, m.computeParallels());

        int euBoost = m.getModeratorEUBoostClamped();
        int fuelDiscount = m.getModeratorFuelDiscountClamped();

        double eutMultiplier = 1.0 + (euBoost / 100.0);
        double durationMultiplier = Math.max(0.01, 1.0 - (fuelDiscount / 100.0));

        var b = com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction.builder()
                .eutMultiplier(eutMultiplier)
                .durationMultiplier(durationMultiplier);

        if (parallels > 1) {
            var mult = com.gregtechceu.gtceu.api.recipe.content.ContentModifier.multiplier(parallels);
            b.inputModifier(mult)
                    .outputModifier(mult)
                    .parallels(parallels);
        }

        return b.build();
    }

    @Override
    public void addDisplayText(@NotNull List<Component> textList) {
        super.addDisplayText(textList);

        if (!isFormed()) {
            textList.add(Component.translatable("phoenixcore.not_formed")
                    .withStyle(s -> s.withColor(0xFF4444)));
            return;
        }

        final var cfg = cfg();

        final boolean overheating = heat > cfg.maxSafeHeat;
        textList.add(Component.literal(String.format("Heat: %.1f / %.1f", heat, cfg.maxSafeHeat))
                .withStyle(s -> s.withColor(overheating ? 0xFF3333 : 0x33FF33)));

        textList.add(Component.literal("EU/t: " + lastGeneratedEUt));
        textList.add(Component.literal("Parallels: " + lastParallels));

        textList.add(Component.literal("Cooling Capacity: " + lastProvidedCooling + " HU/t")
                .withStyle(s -> s.withColor(0x55FFFF)));

        final String rodName = (primaryFuelRodType != null) ? primaryFuelRodType.getName() : "None";
        final String modName = (primaryModeratorType != null) ? primaryModeratorType.getName() : "None";
        final String coolerName = (primaryCoolerType != null) ? primaryCoolerType.getName() : "None";

        textList.add(Component.literal("Fuel Rods: " + activeFuelRods.size() + " (Primary: " + rodName + ")"));
        textList.add(Component.literal("Moderators: " + activeModerators.size() + " (Primary: " + modName + ")"));
        textList.add(Component.literal("Coolers: " + activeCoolers.size() + " (Primary: " + coolerName + ")"));
        textList.add(Component.literal("Stability: " + activeStabilityHatches.size()));
        textList.add(Component.literal("Sensors: " + activeSensorHatches.size()));

        int euBoost = getModeratorEUBoostClamped();
        int fuelDiscount = getModeratorFuelDiscountClamped();
        textList.add(Component.literal("EU Boost: " + euBoost + "%"));
        textList.add(Component.literal("Fuel Discount: " + fuelDiscount + "%"));

        textList.add(Component.literal("Coolant: " + (lastHasCoolant ? "OK" : "MISSING"))
                .withStyle(s -> s.withColor(lastHasCoolant ? 0x33FF33 : 0xFF3333)));

        if (meltdownTimerTicks > 0) {
            textList.add(Component.literal("MELTDOWN IN: " + getMeltdownSecondsRemaining() + "s")
                    .withStyle(s -> s.withColor(0xFFAA00)));
        }
    }

    protected void resolvePersistedComponents() {
        activeCoolers.clear();
        activeModerators.clear();
        activeFuelRods.clear();
        activeBlankets.clear();
        activeStabilityHatches.clear();
        activeSensorHatches.clear();

        for (String id : persistedCoolerIDs) {
            IFissionCoolerType t = resolveCoolerType(id);
            if (t != null) activeCoolers.add(t);
        }
        for (String id : persistedModeratorIDs) {
            IFissionModeratorType t = resolveModeratorType(id);
            if (t != null) activeModerators.add(t);
        }
        for (String id : persistedFuelRodIDs) {
            IFissionFuelRodType t = resolveFuelRodType(id);
            if (t != null) activeFuelRods.add(t);
        }
        for (String id : persistedBlanketIDs) {
            IFissionBlanketType t = resolveBlanketType(id);
            if (t != null) activeBlankets.add(t);
        }
        for (String id : persistedStabilityIDs) {
            IFissionStabilityHatchType t = resolveStabilityHatchType(id);
            if (t != null) activeStabilityHatches.add(t);
        }
        for (String id : persistedSensorIDs) {
            IFissionSensorHatchType t = resolveSensorHatchType(id);
            if (t != null) activeSensorHatches.add(t);
        }
    }

    protected @Nullable IFissionCoolerType resolveCoolerType(String serializedName) {
        return PhoenixAPI.FISSION_COOLERS.keySet().stream()
                .filter(type -> type.getName().equals(serializedName))
                .findFirst().orElse(null);
    }

    protected @Nullable IFissionModeratorType resolveModeratorType(String serializedName) {
        return PhoenixAPI.FISSION_MODERATORS.keySet().stream()
                .filter(type -> type.getName().equals(serializedName))
                .findFirst().orElse(null);
    }

    protected @Nullable IFissionFuelRodType resolveFuelRodType(String serializedName) {
        return PhoenixAPI.FISSION_FUEL_RODS.keySet().stream()
                .filter(type -> type.getName().equals(serializedName))
                .findFirst().orElse(null);
    }

    protected @Nullable IFissionBlanketType resolveBlanketType(String serializedName) {
        return PhoenixAPI.FISSION_BLANKETS.keySet().stream()
                .filter(type -> type.getName().equals(serializedName))
                .findFirst().orElse(null);
    }

    protected @Nullable IFissionStabilityHatchType resolveStabilityHatchType(String serializedName) {
        return PhoenixAPI.FISSION_STABILITY_HATCHES.keySet().stream()
                .filter(type -> type.getName().equals(serializedName))
                .findFirst().orElse(null);
    }

    protected @Nullable IFissionSensorHatchType resolveSensorHatchType(String serializedName) {
        return PhoenixAPI.FISSION_SENSOR_HATCHES.keySet().stream()
                .filter(type -> type.getName().equals(serializedName))
                .findFirst().orElse(null);
    }

    protected @Nullable IFissionCoolerType getPrimaryCooler(List<IFissionCoolerType> list) {
        if (list.isEmpty()) return null;
        Map<IFissionCoolerType, Long> counts = list.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return counts.entrySet().stream()
                .max(Comparator.<Map.Entry<IFissionCoolerType, Long>>comparingLong(Map.Entry::getValue)
                        .thenComparingInt(e -> e.getKey().getTier()))
                .map(Map.Entry::getKey).orElse(null);
    }

    protected @Nullable IFissionModeratorType getPrimaryModerator(List<IFissionModeratorType> list) {
        if (list.isEmpty()) return null;
        Map<IFissionModeratorType, Long> counts = list.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return counts.entrySet().stream()
                .max(Comparator.<Map.Entry<IFissionModeratorType, Long>>comparingLong(Map.Entry::getValue)
                        .thenComparingInt(e -> e.getKey().getTier()))
                .map(Map.Entry::getKey).orElse(null);
    }

    protected @Nullable IFissionFuelRodType getPrimaryFuelRod(List<IFissionFuelRodType> list) {
        if (list.isEmpty()) return null;
        Map<IFissionFuelRodType, Long> counts = list.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return counts.entrySet().stream()
                .max(Comparator.<Map.Entry<IFissionFuelRodType, Long>>comparingLong(Map.Entry::getValue)
                        .thenComparingInt(e -> e.getKey().getTier()))
                .map(Map.Entry::getKey).orElse(null);
    }
}
