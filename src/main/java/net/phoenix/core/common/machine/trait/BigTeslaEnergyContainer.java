package net.phoenix.core.common.machine.trait;

import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.misc.EnergyContainerList;
import com.gregtechceu.gtceu.api.recipe.ingredient.EnergyStack;

import net.minecraft.core.Direction;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class BigTeslaEnergyContainer {

    @Getter
    private BigInteger energyStored;
    @Getter
    private BigInteger energyCapacity;

    private final int inputAmperage;
    private final int outputAmperage;
    private final long inputVoltage;
    private final long outputVoltage;

    private final Predicate<Direction> sideInputCondition;
    private final Predicate<Direction> sideOutputCondition;

    @Setter
    private Consumer<BigTeslaEnergyContainer> onChange;

    public BigTeslaEnergyContainer(BigInteger capacity, int inputAmperage, int outputAmperage,
                                   long inputVoltage, long outputVoltage,
                                   Predicate<Direction> inputCondition,
                                   Predicate<Direction> outputCondition) {
        this.energyCapacity = capacity;
        this.energyStored = BigInteger.ZERO;
        this.inputAmperage = inputAmperage;
        this.outputAmperage = outputAmperage;
        this.inputVoltage = inputVoltage;
        this.outputVoltage = outputVoltage;
        this.sideInputCondition = inputCondition;
        this.sideOutputCondition = outputCondition;
    }

    public BigInteger acceptEnergyFromNetwork(Direction side, long voltage, long amperage) {
        if (voltage <= 0 || !inputsEnergy(side)) return BigInteger.ZERO;

        BigInteger availableCapacity = energyCapacity.subtract(energyStored);
        long canAcceptAmperes = Math.min(amperage, inputAmperage);

        BigInteger toAdd = BigInteger.valueOf(voltage).multiply(BigInteger.valueOf(canAcceptAmperes));
        if (toAdd.compareTo(availableCapacity) > 0) {
            toAdd = availableCapacity;
        }

        energyStored = energyStored.add(toAdd);
        notifyChanged();

        return toAdd;
    }

    public boolean inputsEnergy(Direction side) {
        return !outputsEnergy(side) && inputVoltage > 0 &&
                (sideInputCondition == null || sideInputCondition.test(side));
    }

    public boolean outputsEnergy(Direction side) {
        return outputVoltage > 0 && (sideOutputCondition == null || sideOutputCondition.test(side));
    }

    public BigInteger changeEnergy(BigInteger amount) {
        BigInteger old = energyStored;
        energyStored = energyStored.add(amount);
        if (energyStored.compareTo(BigInteger.ZERO) < 0) energyStored = BigInteger.ZERO;
        if (energyStored.compareTo(energyCapacity) > 0) energyStored = energyCapacity;
        notifyChanged();
        return energyStored.subtract(old);
    }

    public void setEnergyCapacity(BigInteger capacity) {
        this.energyCapacity = capacity;
        if (energyStored.compareTo(capacity) > 0) {
            energyStored = capacity;
        }
        notifyChanged();
    }

    private void notifyChanged() {
        if (onChange != null) onChange.accept(this);
    }

    public @NotNull List<Object> getContents() {
        long amperage = Math.max(inputAmperage, outputAmperage);
        return Collections
                .singletonList(EnergyContainerList.calculateVoltageAmperage(energyStored.longValue(), amperage));
    }

    public double getTotalContentAmount() {
        return energyStored.doubleValue();
    }

    public RecipeCapability<EnergyStack> getCapability() {
        return EURecipeCapability.CAP;
    }

    public long changeEnergy(long delta) {
        return changeEnergy(BigInteger.valueOf(delta)).longValue();
    }
}
