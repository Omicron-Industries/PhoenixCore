package net.phoenix.core.common.machine.multiblock.electric.research;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.*;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.util.TimedProgressSupplier;
import com.gregtechceu.gtceu.api.gui.widget.ExtendedProgressWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMaintenanceMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockDisplayText;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.misc.EnergyContainerList;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.api.transfer.fluid.FluidHandlerList;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GTTransferUtils;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.IManaged;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.phoenix.core.api.gui.PhoenixGuiTextures;
import net.phoenix.core.configs.PhoenixConfigs;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

import static net.phoenix.core.configs.PhoenixConfigs.INSTANCE;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class PhoenixHPCAMachine extends WorkableElectricMultiblockMachine
                                implements IOpticalComputationProvider, IControllable {

    private static final double IDLE_TEMPERATURE = 200;
    private static final double DAMAGE_TEMPERATURE = 1000;

    private IMaintenanceMachine maintenance;
    private IEnergyContainer energyContainer;
    private IFluidHandler coolantHandler;
    @Persisted
    @DescSynced
    private final HPCAGridHandler hpcaHandler;

    private boolean hasNotEnoughEnergy;

    @Persisted
    private double temperature = IDLE_TEMPERATURE;

    private final TimedProgressSupplier progressSupplier;

    @Nullable
    protected TickableSubscription tickSubs;

    public PhoenixHPCAMachine(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
        this.energyContainer = new EnergyContainerList(new ArrayList<>());
        this.progressSupplier = new TimedProgressSupplier(200, 47, false);
        this.hpcaHandler = new HPCAGridHandler(this);
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        List<IEnergyContainer> energyContainers = new ArrayList<>();
        List<IFluidHandler> coolantContainers = new ArrayList<>();
        List<IHPCAComponentHatch> componentHatches = new ArrayList<>();
        Long2ObjectMap<IO> ioMap = getMultiblockState().getMatchContext().getOrCreate("ioMap",
                Long2ObjectMaps::emptyMap);
        for (IMultiPart part : getParts()) {
            IO io = ioMap.getOrDefault(part.self().getPos().asLong(), IO.BOTH);
            if (part instanceof IHPCAComponentHatch componentHatch) {
                componentHatches.add(componentHatch);
            }
            if (part instanceof IMaintenanceMachine maintenanceMachine) {
                this.maintenance = maintenanceMachine;
            }
            if (io == IO.NONE || io == IO.OUT) continue;
            var handlerLists = part.getRecipeHandlers();
            for (var handlerList : handlerLists) {
                if (!handlerList.isValid(io)) continue;

                handlerList.getCapability(EURecipeCapability.CAP).stream()
                        .filter(IEnergyContainer.class::isInstance)
                        .map(IEnergyContainer.class::cast)
                        .forEach(energyContainers::add);
                handlerList.getCapability(FluidRecipeCapability.CAP).stream()
                        .filter(IFluidHandler.class::isInstance)
                        .map(IFluidHandler.class::cast)
                        .forEach(coolantContainers::add);
            }
        }
        this.energyContainer = new EnergyContainerList(energyContainers);
        this.coolantHandler = new FluidHandlerList(coolantContainers);
        this.hpcaHandler.onStructureForm(componentHatches);

        if (getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().tell(new TickTask(0, this::updateTickSubscription));
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().tell(new TickTask(0, this::updateTickSubscription));
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (tickSubs != null) {
            tickSubs.unsubscribe();
            tickSubs = null;
        }
    }

    protected void updateTickSubscription() {
        if (isFormed) {
            tickSubs = subscribeServerTick(tickSubs, this::tick);
        } else if (tickSubs != null) {
            tickSubs.unsubscribe();
            tickSubs = null;
        }
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        this.energyContainer = new EnergyContainerList(new ArrayList<>());
        this.hpcaHandler.onStructureInvalidate();
    }

    @Override
    public int requestCWUt(int cwut, boolean simulate, Collection<IOpticalComputationProvider> seen) {
        seen.add(this);
        return isActive() && isWorkingEnabled() && !hasNotEnoughEnergy ? hpcaHandler.allocateCWUt(cwut, simulate) : 0;
    }

    @Override
    public int getMaxCWUt(@NotNull Collection<IOpticalComputationProvider> seen) {
        seen.add(this);
        return isActive() && isWorkingEnabled() ? hpcaHandler.getMaxCWUt() : 0;
    }

    @Override
    public boolean canBridge(@NotNull Collection<IOpticalComputationProvider> seen) {
        seen.add(this);
        return !isFormed() || hpcaHandler.hasHPCABridge();
    }

    public void tick() {
        if (isWorkingEnabled()) consumeEnergy();
        if (isActive()) {
            // forcibly use active coolers at full rate if temperature is half-way to damaging temperature
            double midpoint = (DAMAGE_TEMPERATURE - IDLE_TEMPERATURE) / 2;
            double temperatureChange = hpcaHandler.calculateTemperatureChange(coolantHandler, temperature >= midpoint) /
                    2.0;
            if (temperature + temperatureChange <= IDLE_TEMPERATURE) {
                temperature = IDLE_TEMPERATURE;
            } else {
                temperature += temperatureChange;
            }
            if (temperature >= DAMAGE_TEMPERATURE) {
                hpcaHandler.attemptDamageHPCA();
            }
            hpcaHandler.tick();
        } else {
            hpcaHandler.clearComputationCache();
            // passively cool (slowly) if not active
            temperature = Math.max(IDLE_TEMPERATURE, temperature - 0.25);
        }
    }

    private void consumeEnergy() {
        long energyToConsume = hpcaHandler.getCurrentEUt();
        boolean hasMaintenance = ConfigHolder.INSTANCE.machines.enableMaintenance && this.maintenance != null;
        if (hasMaintenance) {
            // 10% more energy per maintenance problem
            energyToConsume += maintenance.getNumMaintenanceProblems() * energyToConsume / 10;
        }

        if (this.hasNotEnoughEnergy && energyContainer.getInputPerSec() > 19L * energyToConsume) {
            this.hasNotEnoughEnergy = false;
        }

        if (this.energyContainer.getEnergyStored() >= energyToConsume) {
            if (!hasNotEnoughEnergy) {
                long consumed = this.energyContainer.removeEnergy(energyToConsume);
                if (consumed == energyToConsume) {
                    getRecipeLogic().setStatus(RecipeLogic.Status.WORKING);
                } else {
                    this.hasNotEnoughEnergy = true;
                    getRecipeLogic().setStatus(RecipeLogic.Status.WAITING);
                }
            }
        } else {
            this.hasNotEnoughEnergy = true;
            getRecipeLogic().setStatus(RecipeLogic.Status.WAITING);
        }
    }

    @Override
    public Widget createUIWidget() {
        WidgetGroup builder = (WidgetGroup) super.createUIWidget();
        // Create the hover grid
        builder.addWidget(new ExtendedProgressWidget(
                () -> hpcaHandler.getAllocatedCWUt() > 0 ? progressSupplier.getAsDouble() : 0,
                74, 65, 47, 47, PhoenixGuiTextures.PHOENIX_HPCA_COMPONENT_OUTLINE)
                .setServerTooltipSupplier(hpcaHandler::addInfo)
                .setFillDirection(ProgressTexture.FillDirection.LEFT_TO_RIGHT));
        int startX = 76;
        int startY = 59;

        // we need to know what components we have on the client
        if (getLevel().isClientSide) {
            if (isFormed) {
                hpcaHandler.tryGatherClientComponents(this.getLevel(), this.getPos(), this.getFrontFacing(),
                        this.getUpwardsFacing(), this.isFlipped);
            } else {
                hpcaHandler.clearClientComponents();
            }
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                final int index = i * 3 + j;
                Supplier<IGuiTexture> textureSupplier = () -> hpcaHandler.getComponentTexture(index);
                builder.addWidget(new ImageWidget(startX + (15 * j), startY + (15 * i), 13, 13, textureSupplier));
            }
        }
        return builder;
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        MultiblockDisplayText.builder(textList, isFormed())
                .setWorkingStatus(true, hpcaHandler.getAllocatedCWUt() > 0) // transform into two-state system for
                // display
                .setWorkingStatusKeys(
                        "gtceu.multiblock.idling",
                        "gtceu.multiblock.idling",
                        "gtceu.multiblock.data_bank.providing")
                .addCustom(tl -> {
                    if (isFormed()) {
                        // Energy Usage
                        tl.add(Component.translatable(
                                "gtceu.multiblock.hpca.energy",
                                FormattingUtil.formatNumbers(hpcaHandler.cachedEUt),
                                FormattingUtil.formatNumbers(hpcaHandler.getMaxEUt()),
                                GTValues.VNF[GTUtil.getTierByVoltage(hpcaHandler.getMaxEUt())])
                                .withStyle(ChatFormatting.GRAY));

                        // Provided Computation
                        Component cwutInfo = Component.literal(
                                hpcaHandler.cachedCWUt + " / " + hpcaHandler.getMaxCWUt() + " CWU/t")
                                .withStyle(ChatFormatting.AQUA);
                        tl.add(Component.translatable(
                                "gtceu.multiblock.hpca.computation",
                                cwutInfo).withStyle(ChatFormatting.GRAY));
                    }
                })
                .addWorkingStatusLine();
    }

    private ChatFormatting getDisplayTemperatureColor() {
        if (temperature < 500) {
            return ChatFormatting.GREEN;
        } else if (temperature < 750) {
            return ChatFormatting.YELLOW;
        }
        return ChatFormatting.RED;
    }

    // Handles the logic of this structure's specific HPCA component grid
    public static class HPCAGridHandler implements IManaged {

        public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(HPCAGridHandler.class);
        @Getter
        private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

        @Nullable // for testing
        private final PhoenixHPCAMachine controller;

        // structure info
        private final List<IHPCAComponentHatch> components = new ObjectArrayList<>();
        private final Set<IHPCACoolantProvider> coolantProviders = new ObjectOpenHashSet<>();
        private final Set<IHPCAComputationProvider> computationProviders = new ObjectOpenHashSet<>();
        private int numBridges;

        // transaction info
        /** How much CWU/t is currently allocated for this tick. */
        @Getter
        private int allocatedCWUt;

        // cached gui info
        // holding these values past the computation clear because GUI is too "late" to read the state in time
        @DescSynced
        private long cachedEUt;
        @DescSynced
        private int cachedCWUt;

        public HPCAGridHandler(@Nullable PhoenixHPCAMachine controller) {
            this.controller = controller;
        }

        public void onStructureForm(Collection<IHPCAComponentHatch> components) {
            reset();
            for (var component : components) {
                this.components.add(component);
                if (component instanceof IHPCACoolantProvider coolantProvider) {
                    this.coolantProviders.add(coolantProvider);
                }
                if (component instanceof IHPCAComputationProvider computationProvider) {
                    this.computationProviders.add(computationProvider);
                }
                if (component.isBridge()) {
                    this.numBridges++;
                }
            }
        }

        private void onStructureInvalidate() {
            reset();
        }

        private void reset() {
            clearComputationCache();
            components.clear();
            coolantProviders.clear();
            computationProviders.clear();
            numBridges = 0;
        }

        private void clearComputationCache() {
            allocatedCWUt = 0;
        }

        public void tick() {
            if (cachedCWUt != allocatedCWUt) {
                cachedCWUt = allocatedCWUt;
            }
            cachedEUt = getCurrentEUt();
            if (allocatedCWUt != 0) {
                allocatedCWUt = 0;
            }
        }

        /**
         * Calculate the temperature differential this tick given active computation and consume coolant.
         *
         * @param coolantTank         The tank to drain coolant from.
         * @param forceCoolWithActive Whether active coolers should forcibly cool even if temperature is already
         *                            decreasing due to passive coolers. Used when the HPCA is running very hot.
         * @return The temperature change, can be positive or negative.
         */
        public double calculateTemperatureChange(IFluidHandler coolantTank, boolean forceCoolWithActive) {
            int maxCWUt = Math.max(1, getMaxCWUt()); // avoids dividing by 0 and the behavior is no different
            int maxCoolingDemand = getMaxCoolingDemand();

            // temperature increase is proportional to the amount of actively used computation
            // a * (b / c)
            int temperatureIncrease = (int) Math.round(1.0 * maxCoolingDemand * allocatedCWUt / maxCWUt);

            // calculate temperature decrease
            long maxPassiveCooling = 0;
            long maxActiveCooling = 0;
            int maxCoolantDrain = 0;

            for (var coolantProvider : coolantProviders) {
                if (coolantProvider.isActiveCooler()) {
                    maxActiveCooling += coolantProvider.getCoolingAmount();
                    maxCoolantDrain += coolantProvider.getMaxCoolantPerTick();
                } else {
                    maxPassiveCooling += coolantProvider.getCoolingAmount();
                }
            }

            double temperatureChange = temperatureIncrease - maxPassiveCooling;
            if (maxActiveCooling == 0 && maxCoolantDrain == 0) {
                return temperatureChange;
            }
            if (forceCoolWithActive || maxActiveCooling <= temperatureChange) {
                FluidStack coolantStack = GTTransferUtils.drainFluidAccountNotifiableList(
                        coolantTank,
                        getCoolantStack(maxCoolantDrain, coolantTank),
                        IFluidHandler.FluidAction.EXECUTE);
                if (!coolantStack.isEmpty()) {
                    long coolantDrained = coolantStack.getAmount();
                    if (coolantDrained == maxCoolantDrain) {
                        temperatureChange -= maxActiveCooling;
                    } else {
                        temperatureChange -= maxActiveCooling * (1.0 * coolantDrained / maxCoolantDrain);
                    }
                }
            } else if (temperatureChange > 0) {
                double temperatureToDecrease = Math.min(temperatureChange, maxActiveCooling);
                int coolantToDrain = Math.max(1, (int) (maxCoolantDrain * (temperatureToDecrease / maxActiveCooling)));
                FluidStack coolantStack = GTTransferUtils.drainFluidAccountNotifiableList(
                        coolantTank,
                        getCoolantStack(coolantToDrain, coolantTank),
                        IFluidHandler.FluidAction.EXECUTE);
                if (!coolantStack.isEmpty()) {
                    int coolantDrained = coolantStack.getAmount();
                    if (coolantDrained == coolantToDrain) {
                        return 0;
                    } else {
                        temperatureChange -= temperatureToDecrease * (1.0 * coolantDrained / coolantToDrain);
                    }
                }
            }
            return temperatureChange;
        }

        private int getStrongestAvailableCoolantSlot(IFluidHandler tank) {
            Fluid[] fluids = new Fluid[] {
                    GTMaterials.get(PhoenixConfigs.INSTANCE.features.ActiveCoolerCoolant2).getFluid(),
                    GTMaterials.get(PhoenixConfigs.INSTANCE.features.ActiveCoolerCoolant1).getFluid(),
                    GTMaterials.get(PhoenixConfigs.INSTANCE.features.ActiveCoolerCoolantBase).getFluid()
            };
            for (int slot = 0; slot < fluids.length; slot++) {
                int tanks = tank.getTanks();
                for (int i = 0; i < tanks; i++) {
                    FluidStack stack = tank.getFluidInTank(i);
                    if (!stack.isEmpty() && stack.getFluid() == fluids[slot]) {
                        return 2 - slot;
                    }
                }
            }
            return 0;
        }

        public FluidStack getCoolantStack(int amount, IFluidHandler tank) {
            int slot = getStrongestAvailableCoolantSlot(tank);
            return new FluidStack(getCoolant(slot), amount);
        }

        private Fluid getCoolant(int slot) {
            return switch (slot) {
                case 1 -> GTMaterials.get(PhoenixConfigs.INSTANCE.features.ActiveCoolerCoolant1).getFluid();
                case 2 -> GTMaterials.get(PhoenixConfigs.INSTANCE.features.ActiveCoolerCoolant2).getFluid();
                default -> GTMaterials.get(PhoenixConfigs.INSTANCE.features.ActiveCoolerCoolantBase).getFluid();
            };
        }

        /**
         * Roll a 1/200 chance to damage a HPCA component marked as damageable. Randomly selects the component.
         * If called every tick, this succeeds on average once every 10 seconds.
         */
        public void attemptDamageHPCA() {
            // 1% chance each tick to damage a component if running too hot
            if (GTValues.RNG.nextInt(200) == 0) {
                // randomize which component is actually damaged
                List<IHPCAComponentHatch> candidates = new ArrayList<>();
                for (var component : components) {
                    if (component.canBeDamaged()) {
                        candidates.add(component);
                    }
                }
                if (!candidates.isEmpty()) {
                    candidates.get(GTValues.RNG.nextInt(candidates.size())).setDamaged(true);
                }
            }
        }

        /** Allocate computation on a given request. Allocates for one tick. */
        public int allocateCWUt(int cwut, boolean simulate) {
            if (cwut == 0) return 0;
            int maxCWUt = getMaxCWUt();
            int availableCWUt = maxCWUt - this.allocatedCWUt;
            int toAllocate = Math.min(cwut, availableCWUt);
            if (!simulate) {
                this.allocatedCWUt += toAllocate;
            }
            return toAllocate;
        }

        private double getCoolantCWUMultiplier(IFluidHandler tank) {
            try {
                int slot = getStrongestAvailableCoolantSlot(tank);
                switch (slot) {
                    case 2:
                        return PhoenixConfigs.INSTANCE.features.CoolantBoost2;
                    case 1:
                        return PhoenixConfigs.INSTANCE.features.CoolantBoost1;
                    default:
                        return PhoenixConfigs.INSTANCE.features.BaseCoolantBoost;
                }
            } catch (Throwable t) {
                return 1.0D;
            }
        }

        /** The maximum amount of CWUs (Compute Work Units) created per tick. */
        public int getMaxCWUt() {
            int maxCWUt = 0;
            for (var computationProvider : computationProviders) {
                maxCWUt += computationProvider.getCWUPerTick();
            }
            if (controller != null && controller.coolantHandler != null) {
                return (int) Math.max(0, Math.round(maxCWUt * getCoolantCWUMultiplier(controller.coolantHandler)));
            }
            return (int) Math.max(0, Math.round(maxCWUt));
        }

        /** The current EU/t this HPCA should use, considering passive drain, current computation, etc.. */
        public long getCurrentEUt() {
            long maximumCWUt = Math.max(1, getMaxCWUt()); // behavior is no different setting this to 1 if it is 0
            long maximumEUt = getMaxEUt();
            long upkeepEUt = getUpkeepEUt();

            if (maximumEUt == upkeepEUt) {
                return maximumEUt;
            }

            // energy draw is proportional to the amount of actively used computation
            // a + c(b - a) / d
            return upkeepEUt + ((maximumEUt - upkeepEUt) * allocatedCWUt / maximumCWUt);
        }

        /** The amount of EU/t this HPCA uses just to stay on with 0 output computation. */
        public long getUpkeepEUt() {
            long upkeepEUt = 0;
            for (var component : components) {
                upkeepEUt += component.getUpkeepEUt();
            }
            return upkeepEUt;
        }

        /** The maximum EU/t that this HPCA could ever use with the given configuration. */
        public long getMaxEUt() {
            long maximumEUt = 0;
            for (var component : components) {
                maximumEUt += component.getMaxEUt();
            }
            return maximumEUt;
        }

        /** Whether this HPCA has a Bridge to allow connecting to other HPCA's */
        public boolean hasHPCABridge() {
            return numBridges > 0;
        }

        /** Whether this HPCA has any cooling providers which are actively cooled. */
        public boolean hasActiveCoolers() {
            for (var coolantProvider : coolantProviders) {
                if (coolantProvider.isActiveCooler()) return true;
            }
            return false;
        }

        /** How much cooling this HPCA can provide. NOT related to coolant fluid consumption. */
        public int getMaxCoolingAmount() {
            int maxCooling = 0;
            for (var coolantProvider : coolantProviders) {
                maxCooling += coolantProvider.getCoolingAmount();
            }
            return maxCooling;
        }

        /** How much cooling this HPCA can require. NOT related to coolant fluid consumption. */
        public int getMaxCoolingDemand() {
            int maxCooling = 0;
            for (var computationProvider : computationProviders) {
                maxCooling += computationProvider.getCoolingPerTick();
            }
            return maxCooling;
        }

        /** How much coolant this HPCA can consume in a tick, in mB/t. */
        public int getMaxCoolantDemand() {
            int maxCoolant = 0;
            for (var coolantProvider : coolantProviders) {
                maxCoolant += coolantProvider.getMaxCoolantPerTick();
            }
            return maxCoolant;
        }

        public void addInfo(List<Component> textList) {
            // Max Computation
            MutableComponent data = Component.literal(Integer.toString(getMaxCWUt())).withStyle(ChatFormatting.AQUA);
            textList.add(Component.translatable("gtceu.multiblock.hpca.info_max_computation", data)
                    .withStyle(ChatFormatting.GRAY));

            // Cooling
            ChatFormatting coolingColor = getMaxCoolingAmount() < getMaxCoolingDemand() ? ChatFormatting.RED :
                    ChatFormatting.GREEN;
            data = Component.literal(Integer.toString(getMaxCoolingDemand())).withStyle(coolingColor);
            textList.add(Component.translatable("gtceu.multiblock.hpca.info_max_cooling_demand", data)
                    .withStyle(ChatFormatting.GRAY));

            data = Component.literal(Integer.toString(getMaxCoolingAmount())).withStyle(coolingColor);
            textList.add(Component.translatable("gtceu.multiblock.hpca.info_max_cooling_available", data)
                    .withStyle(ChatFormatting.GRAY));

            // Coolant Required
            if (getMaxCoolantDemand() > 0) {
                data = Component.translatable("gtceu.universal.liters", getMaxCoolantDemand())
                        .withStyle(ChatFormatting.YELLOW).append(" ");
                Component coolantName = Component
                        .translatable("gtceu.tooltip.custom_coolant",
                                GTMaterials.get(INSTANCE.features.ActiveCoolerCoolantBase).getLocalizedName(),
                                GTMaterials.get(INSTANCE.features.ActiveCoolerCoolant1).getLocalizedName(),
                                GTMaterials.get(INSTANCE.features.ActiveCoolerCoolant2).getLocalizedName())
                        .withStyle(ChatFormatting.YELLOW);
                data.append(coolantName);
            } else {
                data = Component.literal("0").withStyle(ChatFormatting.GREEN);
            }
            textList.add(Component.translatable("gtceu.multiblock.hpca.info_max_coolant_required", data)
                    .withStyle(ChatFormatting.GRAY));

            // Bridging
            if (numBridges > 0) {
                textList.add(Component.translatable("gtceu.multiblock.hpca.info_bridging_enabled")
                        .withStyle(ChatFormatting.GREEN));
            } else {
                textList.add(Component.translatable("gtceu.multiblock.hpca.info_bridging_disabled")
                        .withStyle(ChatFormatting.RED));
            }
        }

        public void addWarnings(List<Component> textList) {
            List<Component> warnings = new ArrayList<>();
            if (numBridges > 1) {
                warnings.add(Component.translatable("gtceu.multiblock.hpca.warning_multiple_bridges")
                        .withStyle(ChatFormatting.GRAY));
            }
            if (computationProviders.isEmpty()) {
                warnings.add(Component.translatable("gtceu.multiblock.hpca.warning_no_computation")
                        .withStyle(ChatFormatting.GRAY));
            }
            if (getMaxCoolingDemand() > getMaxCoolingAmount()) {
                warnings.add(Component.translatable("gtceu.multiblock.hpca.warning_low_cooling")
                        .withStyle(ChatFormatting.GRAY));
            }
            if (!warnings.isEmpty()) {
                textList.add(Component.translatable("gtceu.multiblock.hpca.warning_structure_header")
                        .withStyle(ChatFormatting.YELLOW));
                textList.addAll(warnings);
            }
        }

        public void addErrors(List<Component> textList) {
            if (components.stream().anyMatch(IHPCAComponentHatch::isDamaged)) {
                textList.add(
                        Component.translatable("gtceu.multiblock.hpca.error_damaged").withStyle(ChatFormatting.RED));
            }
        }

        public ResourceTexture getComponentTexture(int index) {
            if (components.size() <= index) {
                return GuiTextures.BLANK_TRANSPARENT;
            }
            return components.get(index).getComponentIcon();
        }

        public void tryGatherClientComponents(Level world, BlockPos pos, Direction frontFacing,
                                              Direction upwardsFacing, boolean flip) {
            Direction relativeUp = RelativeDirection.UP.getRelative(frontFacing, upwardsFacing, flip);

            if (components.isEmpty()) {
                BlockPos testPos = pos
                        .relative(frontFacing.getOpposite(), 3)
                        .relative(relativeUp, 3);

                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        BlockPos tempPos = testPos.relative(frontFacing, j).relative(relativeUp.getOpposite(), i);
                        BlockEntity be = world.getBlockEntity(tempPos);
                        if (be instanceof IHPCAComponentHatch hatch) {
                            components.add(hatch);
                        } else if (be instanceof IMachineBlockEntity machineBE) {
                            MetaMachine machine = machineBE.getMetaMachine();
                            if (machine instanceof IHPCAComponentHatch hatch) {
                                components.add(hatch);
                            }
                        }
                        // if here without a hatch, something went wrong, better to skip than add a null into the mix.
                    }
                }
            }
        }

        public void clearClientComponents() {
            components.clear();
        }

        @Override
        public ManagedFieldHolder getFieldHolder() {
            return MANAGED_FIELD_HOLDER;
        }

        @Override
        public void onChanged() {
            controller.onChanged();
        }
    }
}
