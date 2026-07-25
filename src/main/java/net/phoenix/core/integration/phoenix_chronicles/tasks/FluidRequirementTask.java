package net.phoenix.core.integration.phoenix_chronicles.tasks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.integration.phoenix_chronicles.QuestTask;

public class FluidRequirementTask extends QuestTask {

    private ResourceLocation fluidId;
    private int requiredAmount;
    private boolean consume; 

    public FluidRequirementTask(ResourceLocation taskId, Component description, ResourceLocation fluidId,
                                int requiredAmount, boolean consume) {
        super(taskId, description);
        this.fluidId = fluidId;
        this.requiredAmount = requiredAmount;
        this.consume = consume;
    }

    public ResourceLocation getFluidId() {
        return fluidId;
    }

    public int getRequiredAmount() {
        return requiredAmount;
    }

    public boolean shouldConsume() {
        return consume;
    }

    @Override
    public boolean isCompletedFor(Player player) {
        if (fluidId == null || requiredAmount <= 0) return false;
        return getTotalFluidInInventory(player) >= requiredAmount;
    }

    private int getTotalFluidInInventory(Player player) {
        int totalFound = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) continue;

            var fluidHandlerCap = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
            if (fluidHandlerCap.isPresent()) {
                IFluidHandlerItem handler = fluidHandlerCap.orElseThrow(IllegalStateException::new);
                for (int i = 0; i < handler.getTanks(); i++) {
                    FluidStack fluidStack = handler.getFluidInTank(i);
                    ResourceLocation currentFluidId = ForgeRegistries.FLUIDS.getKey(fluidStack.getFluid());

                    if (currentFluidId != null && currentFluidId.equals(this.fluidId)) {
                        totalFound += fluidStack.getAmount();
                        if (totalFound >= requiredAmount) return totalFound;
                    }
                }
            }
        }
        return totalFound;
    }

    public void tryConsume(Player player) {
        if (fluidId == null || !consume || requiredAmount <= 0) return; 

        int remainingToDrain = requiredAmount;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || remainingToDrain <= 0) break;

            var fluidHandlerCap = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
            if (fluidHandlerCap.isPresent()) {
                IFluidHandlerItem handler = fluidHandlerCap.orElseThrow(IllegalStateException::new);

                FluidStack simulatedDrain = handler.drain(remainingToDrain, IFluidHandler.FluidAction.SIMULATE);
                if (simulatedDrain.isEmpty()) continue;

                ResourceLocation drainedId = ForgeRegistries.FLUIDS.getKey(simulatedDrain.getFluid());
                if (drainedId != null && drainedId.equals(this.fluidId)) {
                    FluidStack actualDrain = handler.drain(remainingToDrain, IFluidHandler.FluidAction.EXECUTE);
                    remainingToDrain -= actualDrain.getAmount();

                    player.getInventory().setItem(i, handler.getContainer());
                }
            }
        }
        player.getInventory().setChanged();
    }

    @Override
    public String getProgressString(Player player) {
        int found = Math.min(getTotalFluidInInventory(player), requiredAmount);
        return String.format("%,d / %,d mB", found, requiredAmount);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "fluid_check");
        tag.putString("fluid_id", fluidId != null ? fluidId.toString() : "minecraft:empty");
        tag.putInt("amount", requiredAmount);
        tag.putBoolean("consume", consume); 
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("fluid_id")) {
            this.fluidId = new ResourceLocation(nbt.getString("fluid_id"));
        }
        this.requiredAmount = nbt.getInt("amount");
        this.consume = nbt.getBoolean("consume"); 
    }
}
