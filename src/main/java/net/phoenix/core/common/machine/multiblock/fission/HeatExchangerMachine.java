package net.phoenix.core.common.machine.multiblock.fission;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.ITieredMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.phoenix.core.common.block.PhoenixFissionBlocks;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HeatExchangerMachine extends WorkableElectricMultiblockMachine implements ITieredMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            HeatExchangerMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    @Getter
    @DescSynced
    @Persisted
    private int length = 0;

    @Persisted
    private int dynamoTier = GTValues.HV;
    @Persisted
    @DescSynced
    private int heat = 0;
    @Persisted
    @DescSynced
    private int cooldownTicks = 0;
    @Persisted
    private boolean heliumActive = false;
    @Persisted
    private long maxHatchOutput = 0;

    protected final ConditionalSubscriptionHandler updateHandler;

    public HeatExchangerMachine(IMachineBlockEntity holder) {
        super(holder);
        this.updateHandler = new ConditionalSubscriptionHandler(this, this::updateLogic, this::isFormed);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        calculateExchangerLength();
        detectDynamoTier();

        this.updateHandler.updateSubscription();

        if (getRecipeLogic() != null) {
            getRecipeLogic().updateTickSubscription();
        }
    }

    private void calculateExchangerLength() {
        if (getLevel() == null) return;

        int validLayers = 0;
        Direction back = getFrontFacing().getOpposite();

        for (int depth = 1; depth <= 30; depth++) {
            BlockPos scanPos = getPos().relative(back, depth);
            Block blockAtCenter = getLevel().getBlockState(scanPos).getBlock();

            if (blockAtCenter == PhoenixFissionBlocks.FISSILE_SAFE_GEARBOX_CASING.get()) {
                validLayers++;
            } else if (depth > 5) {
                break;
            }
        }
        this.length = Math.max(1, validLayers);
    }

    private void detectDynamoTier() {
        int detectedTier = GTValues.ULV;
        long totalPower = 0;

        var parts = getParts();
        if (parts == null) return;

        for (IMultiPart part : parts) {
            var handlers = part.getRecipeHandlers();
            if (handlers == null) continue;

            for (var handler : handlers) {
                Object capObject = handler.getCapability(EURecipeCapability.CAP);

                if (handler.getHandlerIO() == IO.OUT && capObject instanceof IEnergyContainer container) {
                    long voltage = container.getOutputVoltage();
                    long amperage = container.getOutputAmperage();

                    detectedTier = Math.max(detectedTier, GTUtil.getFloorTierByVoltage(voltage));
                    totalPower += (voltage * amperage);
                }
            }
        }
        this.dynamoTier = detectedTier;
        this.maxHatchOutput = totalPower;
    }

    @Override
    public int getTier() {
        return isFormed() ? this.dynamoTier : GTValues.ULV;
    }

    public static ModifierFunction recipeModifier(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof HeatExchangerMachine exchanger))
            return ModifierFunction.IDENTITY;

        double multiplier = 1.5 + (Math.max(0, exchanger.getLength() - 1) * 0.25);

        if (exchanger.heliumActive) multiplier *= 2.0;

        return ModifierFunction.builder()
                .eutMultiplier(multiplier * multiplier)
                .build();
    }

    protected void updateLogic() {
        if (getLevel() == null || getLevel().isClientSide) return;

        if (cooldownTicks > 0) {
            cooldownTicks--;
            if (getRecipeLogic().isWorking()) {
                getRecipeLogic().interruptRecipe();
            }
            return;
        }

        if (getRecipeLogic().isWorking()) {
            if (getOffsetTimer() % 20 == 0) {
                if (consumeColdCoolant()) {
                    heat = Math.max(0, heat - 5);
                } else {
                    heat += 10;

                    if (heat >= 100) {
                        heat = 100;
                        cooldownTicks = 600;
                        getRecipeLogic().interruptRecipe();
                    }
                }
            }
        } else {
            if (getOffsetTimer() % 20 == 0 && heat > 0) {
                heat--;
            }
        }
    }

    private boolean consumeColdCoolant() {
        int effectiveLength = Math.max(1, length);
        int waterAmount = (int) (100 * (1.0 + (effectiveLength - 1) * 0.2));
        int heliumAmount = (int) (25 * (1.0 + (effectiveLength - 1) * 0.2));

        var heliumInput = GTMaterials.Helium.getFluid(FluidStorageKeys.LIQUID, heliumAmount);
        var heliumOutput = GTMaterials.Helium.getFluid(heliumAmount);

        var heliumRecipe = GTRecipeBuilder.ofRaw()
                .inputFluids(heliumInput)
                .outputFluids(heliumOutput)
                .buildRawRecipe();

        if (RecipeHelper.matchRecipe(this, heliumRecipe).isSuccess()) {
            if (RecipeHelper.handleRecipeIO(this, heliumRecipe, IO.IN, getRecipeLogic().getChanceCaches())
                    .isSuccess()) {
                this.heliumActive = true;
                return RecipeHelper.handleRecipeIO(this, heliumRecipe, IO.OUT, getRecipeLogic().getChanceCaches())
                        .isSuccess();
            }
        }

        this.heliumActive = false;
        var distilled = GTRecipeBuilder.ofRaw()
                .inputFluids(GTMaterials.DistilledWater.getFluid(waterAmount))
                .outputFluids(GTMaterials.Steam.getFluid(waterAmount))
                .buildRawRecipe();

        if (RecipeHelper.matchRecipe(this, distilled).isSuccess()) {
            if (RecipeHelper.handleRecipeIO(this, distilled, IO.IN, getRecipeLogic().getChanceCaches()).isSuccess()) {
                return RecipeHelper.handleRecipeIO(this, distilled, IO.OUT, getRecipeLogic().getChanceCaches())
                        .isSuccess();
            }
        }

        var water = GTRecipeBuilder.ofRaw()
                .inputFluids(GTMaterials.Water.getFluid(waterAmount))
                .outputFluids(GTMaterials.Steam.getFluid(waterAmount))
                .buildRawRecipe();

        if (RecipeHelper.matchRecipe(this, water).isSuccess()) {
            if (RecipeHelper.handleRecipeIO(this, water, IO.IN, getRecipeLogic().getChanceCaches()).isSuccess()) {
                return RecipeHelper.handleRecipeIO(this, water, IO.OUT, getRecipeLogic().getChanceCaches()).isSuccess();
            }
        }

        return false;
    }

    @Override
    public @NotNull ModularUI createUI(Player entityPlayer) {
        return new ModularUI(198, 208, this, entityPlayer).widget(new FancyMachineUIWidget(this, 198, 208));
    }

    @Override
    public void addDisplayText(@NotNull List<Component> textList) {
        super.addDisplayText(textList);
        if (!isFormed()) return;

        textList.add(Component.literal("Exchange Columns: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(length)).withStyle(ChatFormatting.AQUA)));

        double multiplier = 1.0 + (Math.max(0, length - 1) * 0.2);
        int scaledAmount = (int) (100 * multiplier);

        textList.add(Component.literal("Secondary Loop: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(scaledAmount + " MB/s").withStyle(ChatFormatting.BLUE)));

        int efficiencyPercent = (int) (multiplier * 100);
        textList.add(Component.literal("Heat Efficiency: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(efficiencyPercent + "%").withStyle(ChatFormatting.GREEN)));

        if (heliumActive) {
            textList.add(Component.literal("CRYO-BOOST ACTIVE")
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        } else {
            textList.add(Component.literal("Standard Thermal Exchange")
                    .withStyle(ChatFormatting.YELLOW));
        }
        ChatFormatting heatColor = heat > 75 ? ChatFormatting.RED :
                (heat > 40 ? ChatFormatting.GOLD : ChatFormatting.YELLOW);
        textList.add(Component.literal("Core Heat: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(heat + "%").withStyle(heatColor)));

        if (cooldownTicks > 0) {
            textList.add(
                    Component.literal("BURNT OUT - COOLING DOWN").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            textList.add(
                    Component.literal("Time Remaining: " + (cooldownTicks / 20) + "s").withStyle(ChatFormatting.GRAY));
        }
    }
}
