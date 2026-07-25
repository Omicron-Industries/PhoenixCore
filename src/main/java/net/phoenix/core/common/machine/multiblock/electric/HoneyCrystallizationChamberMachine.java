package net.phoenix.core.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.api.sync_system.annotations.RerenderOnChanged;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.TextWidget;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault

public class HoneyCrystallizationChamberMachine extends WorkableElectricMultiblockMachine {

    @Getter
    @Setter
    @SyncToClient
    @RerenderOnChanged
    private @NotNull Set<BlockPos> fluidOffsets = new HashSet<>();

    private static final FluidStack HONEY_STACK;
    static {
        Fluid honeyFluid = ForgeRegistries.FLUIDS
                .getValue(ResourceLocation.fromNamespaceAndPath("productivebees", "honey"));
        if (honeyFluid != null) {
            HONEY_STACK = new FluidStack(honeyFluid, 1000);
        } else {
            HONEY_STACK = FluidStack.EMPTY;
        }
    }

    public HoneyCrystallizationChamberMachine(BlockEntityCreationInfo holder) {
        super(holder, new RecipeLogic());
    }

    @Override
    public void formStructure(@org.jetbrains.annotations.NotNull String substructureName) {
        super.formStructure(substructureName);
        this.fluidOffsets = saveOffsets();
    }

    @Override
    public void invalidateStructure(@org.jetbrains.annotations.NotNull String substructureName) {
        super.invalidateStructure(substructureName);
        this.fluidOffsets.clear();
    }

    @NotNull
    public Set<BlockPos> saveOffsets() {
        Direction up = RelativeDirection.UP.getRelativeFacing(getFrontFacing(), getUpwardsFacing(), isFlipped());
        Direction back = getFrontFacing().getOpposite();
        Direction right = RelativeDirection.RIGHT.getRelativeFacing(getFrontFacing(), getUpwardsFacing(), isFlipped());

        BlockPos pos = getBlockPos();
        Set<BlockPos> offsets = new HashSet<>();

        BlockPos startPos = pos
                .relative(up, -1)
                .relative(back, 3)
                .relative(right.getOpposite(), 2);

        int width = 5;
        int depth = 5;
        int height = 1;

        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                for (int dy = 0; dy < height; dy++) {

                    BlockPos currentPos = startPos.offset(
                            right.getStepX() * dx + back.getStepX() * dz,
                            up.getStepY() * dy,
                            right.getStepZ() * dx + back.getStepZ() * dz);

                    offsets.add(currentPos.subtract(pos));
                }
            }
        }

        return offsets;
    }

    private record PlasmaBoost(String name, double durationMultiplier, double eutMultiplier, int consumeAmount,
                               int ticksPerConsumption) {}

    private static final Map<Fluid, HoneyCrystallizationChamberMachine.PlasmaBoost> PLASMA_BOOSTS = new HashMap<>();
    static {
        PLASMA_BOOSTS.put(GTMaterials.Helium.getFluid(FluidStorageKeys.PLASMA),
                new HoneyCrystallizationChamberMachine.PlasmaBoost("Helium Plasma", 0.9, 0.8, 1, 40));
        PLASMA_BOOSTS.put(GTMaterials.Iron.getFluid(FluidStorageKeys.PLASMA),
                new HoneyCrystallizationChamberMachine.PlasmaBoost("Iron Plasma", 0.7, 0.85, 200, 20));
        PLASMA_BOOSTS.put(GTMaterials.Nickel.getFluid(FluidStorageKeys.PLASMA),
                new HoneyCrystallizationChamberMachine.PlasmaBoost("Nickel Plasma", 0.6, 0.9, 50, 10));
    }

    @SyncToClient
    private boolean isPlasmaBoosted = false;

    @Nullable
    private HoneyCrystallizationChamberMachine.PlasmaBoost activeBoost = null;

    private int consumptionTimer = 0;

    public Set<BlockPos> getFluidOffsets() {
        return this.fluidOffsets;
    }

    @Override
    public boolean onWorking() {
        if (this.consumptionTimer % (activeBoost == null ? 1 : activeBoost.ticksPerConsumption()) == 0) {
            isPlasmaBoosted = false;
            activeBoost = null;

            var fluidInputs = this.getCapabilitiesFlat(com.gregtechceu.gtceu.api.capability.recipe.IO.IN,
                    com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability.CAP);

            if (fluidInputs != null && !fluidInputs.isEmpty()) {

                if (fluidInputs.get(0) instanceof NotifiableFluidTank inputFluidHandler) {

                    for (var entry : PLASMA_BOOSTS.entrySet()) {
                        var fluid = entry.getKey();
                        var boost = entry.getValue();
                        FluidStack requiredStack = new FluidStack(fluid, boost.consumeAmount());

                        if (inputFluidHandler
                                .drain(requiredStack,
                                        net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE)
                                .getAmount() == boost.consumeAmount()) {

                            inputFluidHandler.drain(requiredStack,
                                    net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                            isPlasmaBoosted = true;
                            activeBoost = boost;
                            break;
                        }
                    }
                }
            }
        }

        boolean value = super.onWorking();

        this.consumptionTimer++;
        if (this.consumptionTimer > 72000) this.consumptionTimer = 0;

        return value;
    }

    public static ModifierFunction recipeModifier(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof HoneyCrystallizationChamberMachine furnace)) {
            return RecipeModifier.nullWrongType(HoneyCrystallizationChamberMachine.class, machine);
        }

        if (furnace.isPlasmaBoosted && furnace.activeBoost != null) {
            HoneyCrystallizationChamberMachine.PlasmaBoost boost = furnace.activeBoost;
            return ModifierFunction.builder()
                    .durationMultiplier(boost.durationMultiplier)
                    .eutMultiplier(boost.eutMultiplier)
                    .build();
        }

        return ModifierFunction.IDENTITY;
    }

    @Override
    public List<IWidget> getWidgetsForDisplay(PanelSyncManager syncManager) {
        List<IWidget> widgets = super.getWidgetsForDisplay(syncManager);
        if (!isFormed()) return widgets;
        widgets.add(new TextWidget<>(Text.dynamic(() -> isPlasmaBoosted ? Component.literal("§bPlasma Boost Active§r") :
                Component.literal("§7No Plasma Catalyst§r"))));
        return widgets;
    }

    public List<FluidStack> getRenderFluids() {
        return List.of(HONEY_STACK);
    }
}
