package net.phoenix.core.integration.phoenix_chronicles.tasks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.integration.phoenix_chronicles.QuestTask;

public class ItemRequirementTask extends QuestTask {

    private Item item;
    private int requiredCount;
    private boolean consume; // NEW: Toggle rule variable

    public ItemRequirementTask(ResourceLocation taskId, Component description, Item item, int requiredCount,
                               boolean consume) {
        super(taskId, description);
        this.item = item;
        this.requiredCount = requiredCount;
        this.consume = consume;
    }

    public Item getItem() {
        return item;
    }

    public int getRequiredCount() {
        return requiredCount;
    }

    public boolean shouldConsume() {
        return consume;
    }

    @Override
    public boolean isCompletedFor(Player player) {
        if (item == null || requiredCount <= 0) return false;

        int found = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                found += stack.getCount();
                if (found >= requiredCount) return true;
            }
        }
        return false;
    }

    /**
     * Call this when claiming rewards. It explicitly respects the 'consume' config toggle.
     */
    public void tryConsume(Player player) {
        if (item == null || !consume) return; // Skip completely if consume is false

        int remaining = requiredCount;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty() || stack.getItem() != item) continue;

            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
            if (remaining <= 0) break;
        }
        player.getInventory().setChanged();
    }

    public String getProgressString(Player player) {
        if (item == null) return "0/" + requiredCount;

        int found = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() == item) found += stack.getCount();
        }
        return Math.min(found, requiredCount) + "/" + requiredCount;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "item_check");
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        tag.putString("item_id", id != null ? id.toString() : "minecraft:air");
        tag.putInt("count", requiredCount);
        tag.putBoolean("consume", consume); // Save parameter config
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("item_id")) {
            this.item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(nbt.getString("item_id")));
        }
        this.requiredCount = nbt.getInt("count");
        this.consume = nbt.getBoolean("consume"); // Load parameter config
    }
}
