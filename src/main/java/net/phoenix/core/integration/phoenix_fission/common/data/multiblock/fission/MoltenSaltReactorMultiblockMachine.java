package net.phoenix.core.integration.phoenix_fission.common.data.multiblock.fission;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fluids.FluidStack;
import net.phoenix.core.integration.phoenix_fission.api.block.IFissionCoolerType;
import net.phoenix.core.integration.phoenix_fission.api.block.IMSRCoreLinerType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@MethodsReturnNonnullByDefault
public class MoltenSaltReactorMultiblockMachine extends FissionWorkableElectricMultiblockMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            MoltenSaltReactorMultiblockMachine.class,
            FissionWorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    @Persisted
    protected double xenonPoisonLevel = 0.0;

    protected int structuralLinerCount = 0;

    protected @Nullable IMSRCoreLinerType coreLinerSpec = null;

    public MoltenSaltReactorMultiblockMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onStructureFormed() {
        var context = getMultiblockState().getMatchContext();
        if (context != null) {
            this.structuralLinerCount = context.get("MSRLinerCount") instanceof Integer val ? val : 0;
            this.coreLinerSpec = context.get("MSRWeakestLiner") instanceof IMSRCoreLinerType type ? type : null;
        }

        this.activeCoolers = getListFromContext("CoolerTypes");
        this.persistedCoolerIDs = new ArrayList<>(
                this.activeCoolers.stream().map(IFissionCoolerType::getName).toList());

        recalcPrimaries();

        this.lastHasCoolant = true;
        this.reactorTickHandler.updateSubscription();
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        this.structuralLinerCount = 0;
        this.coreLinerSpec = null;
    }

    @Override
    protected boolean shouldRunReactor() {
        if (!isFormed() || coreLinerSpec == null) return false;
        if (isScramActive()) return false;

        int parallels = Math.max(1, this.structuralLinerCount);
        int totalRequiredMb = parallels * coreLinerSpec.getFluidFlowRate();

        return canConvertFuelSalt(coreLinerSpec.getInputFluidId(), coreLinerSpec.getOutputFluidId(), totalRequiredMb);
    }

    @Override
    protected void handleReactorLogic(boolean running) {
        lastHeatGainedPerTick = 0.0;
        lastHeatRemovedPerTick = 0.0;
        lastGeneratedEUt = 0;

        if (coreLinerSpec == null) return;

        int parallels = Math.max(1, this.structuralLinerCount);
        int saltToProcess = parallels * coreLinerSpec.getFluidFlowRate();

        String inputSalt = coreLinerSpec.getInputFluidId();
        String outputSalt = coreLinerSpec.getOutputFluidId();

        if (running) {
            continuousBurnTicks++;
            xenonPoisonLevel = Math.min(0.50, xenonPoisonLevel + 0.00005);
        } else {
            continuousBurnTicks = 0;
            xenonPoisonLevel = Math.max(0.0, xenonPoisonLevel - 0.0002);
        }

        applyParallelsToRecipeLogic(parallels);

        if (running) {
            if (tryConvertFuelSalt(inputSalt, outputSalt, saltToProcess)) {
                double poisonDamping = 1.0 - xenonPoisonLevel;
                double heatProduced = (saltToProcess * coreLinerSpec.getHeatPerMb()) * poisonDamping;

                this.heat += heatProduced;
                this.lastHeatGainedPerTick = heatProduced;

                if (continuousBurnTicks % 20 == 0 && xenonPoisonLevel > 0.10) {
                    int xenonVolume = Math.max(1, (int) (saltToProcess * 0.1));

                    if (canOutputFluidId("phoenixcore:radioactive_xenon_gas", xenonVolume)) {
                        tryOutputFluidId("phoenixcore:radioactive_xenon_gas", xenonVolume);
                        xenonPoisonLevel = Math.max(0.0, xenonPoisonLevel - 0.02);
                    }
                }
            } else {
                running = false;
            }
        } else {
            if (cfg().passiveCooling) {
                double coolingFactor = 0.010 / coreLinerSpec.getTier();
                double variableCooling = this.heat * coolingFactor;
                heat -= Math.max(cfg().idleHeatLoss, variableCooling);
            }
        }

        int totalCoolingCapacity = activeCoolers.stream()
                .mapToInt(IFissionCoolerType::getCoolerTemperature)
                .sum();

        lastProvidedCooling = totalCoolingCapacity;
        lastHasCoolant = true;

        if (cfg().coolingRequiresCoolant && !activeCoolers.isEmpty()) {
            lastHasCoolant = canConsumeCoolantForThisTickMachineDriven() && consumeCoolantForThisTickMachineDriven();
        }

        double removedHeat = 0.0;
        if (!cfg().coolingRequiresCoolant || lastHasCoolant) {
            double aboveMin = Math.max(0.0, heat - cfg().minHeat);
            double functionalCooling = running ? totalCoolingCapacity : (totalCoolingCapacity * 0.50);

            removedHeat = Math.min(aboveMin, functionalCooling);
            heat -= removedHeat;
        }

        lastHeatRemovedPerTick = removedHeat;
        clampHeat();

        tickPowerGeneration(running);
        tickMeltdown();
    }

    protected boolean canConvertFuelSalt(@NotNull String inFluid, @NotNull String outFluid, int mb) {
        if (mb <= 0) return true;
        return canConsumeFluidId(inFluid, mb);
    }

    protected boolean tryConvertFuelSalt(@NotNull String inFluid, @NotNull String outFluid, int mb) {
        if (mb <= 0) return true;

        if (!canConsumeFluidId(inFluid, mb)) return false;
        if (!tryConsumeFluidId(inFluid, mb)) return false;

        tryOutputFluidId(outFluid, mb);
        return true;
    }

    protected boolean canOutputFluidId(@NotNull String fluidId, int mb) {
        if (mb <= 0) return true;

        FluidStack fs = resolveFluidStack(fluidId, mb);
        if (fs.isEmpty()) return false;

        GTRecipe dummy = GTRecipeBuilder.ofRaw()
                .outputFluids(fs)
                .buildRawRecipe();

        return RecipeHelper.matchRecipe(this, dummy).isSuccess();
    }

    @Override
    protected void tickFuelConsumptionMachineDriven(int parallels) {
        
    }

    @Override
    public void addDisplayText(@NotNull List<Component> textList) {
        if (!isFormed() || coreLinerSpec == null) {
            textList.add(Component.translatable("phoenixcore.not_formed").withStyle(s -> s.withColor(0xFF4444)));
            return;
        }

        final var cfg = cfg();
        final boolean overheating = heat > cfg.maxSafeHeat;

        textList.add(Component.translatable("phoenixcore.current_heat_display",
                String.format("%.1f", heat), String.format("%.1f", cfg.maxSafeHeat))
                .withStyle(s -> s.withColor(overheating ? 0xFF3333 : 0x33FF33)));

        try {
            java.lang.reflect.Method getVoltage = FissionWorkableElectricMultiblockMachine.class
                    .getDeclaredMethod("getVoltageFormattedOutput", long.class);
            getVoltage.setAccessible(true);
            textList.add((Component) getVoltage.invoke(this, lastGeneratedEUt));
        } catch (Throwable ignored) {
            textList.add(Component.literal(String.format("Generated Power: %d EU/t", lastGeneratedEUt)));
        }

        textList.add(Component.translatable("phoenixcore.parallels", lastParallels));
        textList.add(Component.translatable("phoenixcore.cooling_power", lastProvidedCooling)
                .withStyle(s -> s.withColor(0x55FFFF)));

        int totalFlowRate = Math.max(1, this.structuralLinerCount) * coreLinerSpec.getFluidFlowRate();
        textList.add(Component.literal(String.format("MSR Core Structural Tier: MK%d (%s)", coreLinerSpec.getTier(),
                coreLinerSpec.getName().toUpperCase())).withStyle(s -> s.withColor(0x55FF55)));
        textList.add(Component.literal(String.format("Active Core Liner Blocks: %d", this.structuralLinerCount))
                .withStyle(s -> s.withColor(0x55AAFF)));
        textList.add(Component.literal(String.format("Total Salt Processing Rate: %d mb/t", totalFlowRate))
                .withStyle(s -> s.withColor(0xFFAA00)));
        textList.add(Component.literal(String.format("Xenon Poisoning Interference: %.1f%%", xenonPoisonLevel * 100))
                .withStyle(s -> s.withColor(0xAA00AA)));

        textList.add(Component
                .translatable(lastHasCoolant ? "phoenixcore.coolant_status.ok" : "phoenixcore.coolant_status.empty"));

        if (meltdownTimerTicks > 0) {
            textList.add(Component.translatable("phoenixcore.status.danger_timer", getMeltdownSecondsRemaining()));
        }
    }
}
