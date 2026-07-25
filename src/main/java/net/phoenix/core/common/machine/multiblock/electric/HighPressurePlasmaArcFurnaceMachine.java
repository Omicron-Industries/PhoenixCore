package net.phoenix.core.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.phoenix.core.common.machine.multiblock.Shield;
import net.phoenix.core.common.machine.multiblock.ShieldedMachine;
import net.phoenix.core.common.machine.multiblock.part.fluid.PlasmaHatchPartMachine;
import net.phoenix.core.common.machine.trait.NotifiableShieldContainer;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.TextWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("all")
public class HighPressurePlasmaArcFurnaceMachine extends WorkableElectricMultiblockMachine implements ShieldedMachine {

    private static final int DECAY_TICK_RATE = 20;

    @SaveField
    @SyncToClient
    private Shield.ShieldTypes shieldType = Shield.ShieldTypes.INACTIVE;

    @SaveField
    @SyncToClient
    private int shieldHealth = 0;

    @SaveField
    @SyncToClient
    private int shieldCooldownTimer = 0;

    @SaveField
    @SyncToClient
    private int shieldDecayTimer = DECAY_TICK_RATE;

    private final ConditionalSubscriptionHandler shieldHandler;

    @SaveField
    private final NotifiableShieldContainer shieldContainer;

    @SyncToClient
    public boolean isPlasmaBoosted = false;

    @Nullable
    public PlasmaBoost activeBoost = null;

    private int consumptionTimer = 0;

    public HighPressurePlasmaArcFurnaceMachine(BlockEntityCreationInfo holder) {
        super(holder, new RecipeLogic());

        this.shieldHandler = new ConditionalSubscriptionHandler(this, this::shieldTick, this::isFormed);

        this.shieldContainer = attachTrait(new NotifiableShieldContainer());
    }

    public Shield.ShieldTypes getShieldType() {
        return this.shieldType;
    }

    public void updateShield(int pKey, boolean setCooldown) {
        this.shieldType = Shield.ShieldTypes.getShieldFromKey(pKey);
        this.shieldHealth = this.shieldType.shieldHealth;
        if (setCooldown) {
            this.shieldCooldownTimer = this.shieldType.shieldCooldownTicks;
        }
        if (this.getSyncDataHolder() != null) {
            this.getSyncDataHolder().markClientSyncFieldDirty("boundTeam");
        }
    }

    @Override
    public void formStructure(@org.jetbrains.annotations.NotNull String substructureName) {
        super.formStructure(substructureName);
        shieldHandler.updateSubscription();
    }

    @Override
    public void invalidateStructure(@org.jetbrains.annotations.NotNull String substructureName) {
        super.invalidateStructure(substructureName);
        shieldHandler.updateSubscription();
    }

    public void shieldTick() {
        if (this.shieldType == Shield.ShieldTypes.NORMAL) {

            this.shieldDecayTimer--;

            if (this.shieldDecayTimer <= 0) {
                this.shieldHealth -= this.shieldType.decayRate;
                this.shieldDecayTimer = DECAY_TICK_RATE;

                if (this.shieldHealth <= 0) {
                    this.shieldType = Shield.ShieldTypes.DECAYED;
                    this.shieldHealth = 0;
                    this.shieldCooldownTimer = this.shieldType.shieldCooldownTicks;
                    if (this.getSyncDataHolder() != null) {
                        this.getSyncDataHolder().markClientSyncFieldDirty("boundTeam");
                    }
                }
                if (this.getSyncDataHolder() != null) {
                    this.getSyncDataHolder().markClientSyncFieldDirty("boundTeam");
                }
            }
        } else {
            this.shieldDecayTimer = DECAY_TICK_RATE;
        }

        if (this.shieldCooldownTimer > 0) {
            this.shieldCooldownTimer--;
            if (this.getSyncDataHolder() != null) {
                this.getSyncDataHolder().markClientSyncFieldDirty("boundTeam");
            }
        }
    }

    @Override
    public boolean beforeWorking(@Nullable GTRecipe recipe) {
        if (recipe == null) return false;

        boolean isActivationRecipe = recipe.data.contains("shield_activation");

        if (this.shieldType != Shield.ShieldTypes.NORMAL) {
            if (!isActivationRecipe) {
                return false;
            }

            if (this.shieldCooldownTimer > 0) {
                return false;
            }
        } else {
            if (isActivationRecipe) {
                return false;
            }
        }

        return super.beforeWorking(recipe);
    }

    @Override
    public void afterWorking() {
        super.afterWorking();

        var activeRecipe = recipeLogic.getLastRecipe();
        if (activeRecipe != null && activeRecipe.data.contains("shield_activation")) {
            updateShield(activeRecipe.data.getInt("updated_shield_key"), true);
        }
    }

    @Override
    public boolean onWorking() {
        if (this.consumptionTimer % (activeBoost == null ? 1 : activeBoost.ticksPerConsumption()) == 0) {
            isPlasmaBoosted = false;
            activeBoost = null;

            for (var entry : PLASMA_BOOSTS.entrySet()) {
                var fluid = entry.getKey();
                var boost = entry.getValue();

                if (tryConsumePlasmaFromHatch(fluid, boost.consumeAmount())) {
                    isPlasmaBoosted = true;
                    activeBoost = boost;
                    break;
                }
            }
        }

        boolean value = super.onWorking();

        this.consumptionTimer++;
        if (this.consumptionTimer > 72000) this.consumptionTimer = 0;

        return value;
    }

    @Override
    public boolean isWorkingEnabled() {
        if (this.isPlasmaBoosted && this.activeBoost != null) {
            net.minecraft.world.level.material.Fluid currentFluid = null;
            for (var entry : PLASMA_BOOSTS.entrySet()) {
                if (entry.getValue().equals(this.activeBoost)) {
                    currentFluid = entry.getKey();
                    break;
                }
            }
            if (currentFluid == null || !hasPlasmaInHatch(currentFluid, this.activeBoost.consumeAmount())) {
                return false;
            }
        }
        return super.isWorkingEnabled();
    }

    private boolean hasPlasmaInHatch(Fluid fluid, int requiredAmount) {
        return getParts().stream()
                .filter(PlasmaHatchPartMachine.class::isInstance)
                .map(PlasmaHatchPartMachine.class::cast)
                .flatMap(hatch -> hatch.getRecipeHandlers().stream())
                .flatMap(handler -> handler.getCapability(FluidRecipeCapability.CAP).stream())
                .map(cap -> (NotifiableFluidTank) cap)
                .anyMatch(
                        tank -> !tank.isEmpty() && ((FluidStack) tank.getContents().get(0)).getFluid().isSame(fluid) &&
                                ((FluidStack) tank.getContents().get(0)).getAmount() >= requiredAmount);
    }

    private boolean tryConsumePlasmaFromHatch(Fluid fluid, int consumeAmount) {
        for (var hatch : getParts().stream()
                .filter(PlasmaHatchPartMachine.class::isInstance)
                .map(PlasmaHatchPartMachine.class::cast)
                .toList()) {

            var tank = (NotifiableFluidTank) hatch.getRecipeHandlers().get(0)
                    .getCapability(FluidRecipeCapability.CAP).get(0);

            if (!tank.isEmpty() && ((FluidStack) tank.getContents().get(0)).getFluid().isSame(fluid) &&
                    ((FluidStack) tank.getContents().get(0)).getAmount() >= consumeAmount) {

                tank.drain(consumeAmount, IFluidHandler.FluidAction.EXECUTE);
                return true;
            }
        }
        return false;
    }

    public record PlasmaBoost(String name, double durationMultiplier, double eutMultiplier, int consumeAmount,
                              int ticksPerConsumption) {}

    private static final Map<net.minecraft.world.level.material.Fluid, PlasmaBoost> PLASMA_BOOSTS = new HashMap<>();

    static {
        PLASMA_BOOSTS.put(GTMaterials.Helium.getFluid(FluidStorageKeys.PLASMA),
                new PlasmaBoost("Helium Plasma", 0.9, 0.8, 1, 40));

        PLASMA_BOOSTS.put(GTMaterials.Iron.getFluid(FluidStorageKeys.PLASMA),
                new PlasmaBoost("Iron Plasma", 0.7, 0.85, 200, 20));

        PLASMA_BOOSTS.put(GTMaterials.Nickel.getFluid(FluidStorageKeys.PLASMA),
                new PlasmaBoost("Nickel Plasma", 0.6, 0.9, 50, 10));
    }

    @Override
    public List<IWidget> getWidgetsForDisplay(PanelSyncManager syncManager) {
        List<IWidget> widgets = super.getWidgetsForDisplay(syncManager);
        if (!isFormed()) return widgets;
        widgets.add(new TextWidget<>(Text.dynamic(() -> Component.translatable("shield.phoenixcore.current_shield",
                Component.translatable(this.shieldType.langKey)))));
        widgets.add(new TextWidget<>(Text.dynamic(() -> {
            if (this.shieldType == Shield.ShieldTypes.NORMAL)
                return Component.translatable("shield.phoenixcore.health", this.shieldHealth);
            if (this.shieldType == Shield.ShieldTypes.DECAYED && this.shieldCooldownTimer > 0)
                return Component.translatable("shield.phoenixcore.cooldown", this.shieldCooldownTimer / 20);
            return Component.literal("");
        })));
        widgets.add(
                new TextWidget<>(Text.dynamic(() -> (this.shieldType == Shield.ShieldTypes.NORMAL && !isPlasmaBoosted) ?
                        Component.literal("§7No Plasma Catalyst§r") : Component.literal(""))));
        return widgets;
    }

    public int getShieldHealth() {
        return this.shieldHealth;
    }

    public int getShieldCooldownTimer() {
        return this.shieldCooldownTimer;
    }

    public static ModifierFunction recipeModifier(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof HighPressurePlasmaArcFurnaceMachine furnace)) {
            return RecipeModifier.nullWrongType(HighPressurePlasmaArcFurnaceMachine.class, machine);
        }
        if (furnace.isPlasmaBoosted && furnace.activeBoost != null && furnace.shieldType == Shield.ShieldTypes.NORMAL) {
            PlasmaBoost boost = furnace.activeBoost;
            return ModifierFunction.builder()
                    .durationMultiplier(boost.durationMultiplier())
                    .eutMultiplier(boost.eutMultiplier())
                    .build();
        }
        return ModifierFunction.IDENTITY;
    }
}
