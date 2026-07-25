package net.phoenix.core.integration.phoenix_chronicles.tasks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.integration.phoenix_chronicles.QuestTask;

import java.util.ArrayList;
import java.util.List;

public class ItemRequirementTask extends QuestTask {

    private Item item;
    private int requiredCount;
    private boolean consume;
    
    private CompoundTag nbtFilter = null;

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

    public CompoundTag getNbtFilter() {
        return nbtFilter;
    }

    public void setNbtFilter(CompoundTag filter) {
        this.nbtFilter = filter;
    }

    @Override
    public ResourceLocation getDisplayItemId() {
        return item == null ? null : ForgeRegistries.ITEMS.getKey(item);
    }

    private boolean stackMatches(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != item) return false;
        if (nbtFilter == null || nbtFilter.isEmpty()) return true;
        CompoundTag stackTag = stack.getTag();
        if (stackTag == null) return false;
        
        for (String key : nbtFilter.getAllKeys()) {
            if (!stackTag.contains(key)) return false;
            if (!stackTag.get(key).equals(nbtFilter.get(key))) return false;
        }
        return true;
    }

    private Iterable<ItemStack> allSlots(Player player) {
        List<ItemStack> all = new ArrayList<>(player.getInventory().items);
        all.addAll(player.getInventory().offhand);
        all.addAll(player.getInventory().armor);
        return all;
    }

    @Override
    public boolean isCompletedFor(Player player) {
        if (item == null || requiredCount <= 0) return false;
        int found = 0;
        for (ItemStack stack : allSlots(player)) {
            if (stackMatches(stack)) {
                found += stack.getCount();
                if (found >= requiredCount) return true;
            }
        }
        return false;
    }

    public void tryConsume(Player player) {
        if (item == null || !consume) return;
        int remaining = requiredCount;
        
        for (ItemStack stack : allSlots(player)) {
            if (!stackMatches(stack)) continue;
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
            if (remaining <= 0) break;
        }
        player.getInventory().setChanged();
    }

    @Override
    public String getProgressString(Player player) {
        if (item == null) return "0/" + requiredCount;
        int found = 0;
        for (ItemStack stack : allSlots(player)) {
            if (stackMatches(stack)) found += stack.getCount();
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
        tag.putBoolean("consume", consume);
        if (nbtFilter != null && !nbtFilter.isEmpty()) tag.put("nbt_filter", nbtFilter);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("item_id"))
            this.item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(nbt.getString("item_id")));
        this.requiredCount = nbt.contains("count") ? nbt.getInt("count") : nbt.getInt("amount");
        this.consume = nbt.getBoolean("consume");
        if (nbt.contains("nbt_filter")) this.nbtFilter = nbt.getCompound("nbt_filter");
    }
}
