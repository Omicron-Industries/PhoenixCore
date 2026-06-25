package net.phoenix.core.integration.phoenix_chronicles.tasks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.integration.phoenix_chronicles.QuestTask;

/**
 * Task: have (or collect) a required count of items matching a given item tag.
 * Counts items currently in the player's inventory — does not consume them.
 *
 * SNBT shape: { type: "tag_item", tag: "c:ores/iron", required: 64 }
 */
public class TagItemTask extends QuestTask {

    private TagKey<Item> tag;
    private int required = 1;

    public TagItemTask(ResourceLocation taskId, Component description, TagKey<Item> tag, int required) {
        super(taskId, description);
        this.tag = tag;
        this.required = Math.max(1, required);
    }

    public TagKey<Item> getTag() {
        return tag;
    }

    public int getRequired() {
        return required;
    }

    private int countMatching(Player player) {
        if (tag == null) return 0;
        Inventory inv = player.getInventory();
        int count = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.is(tag)) count += stack.getCount();
        }
        return count;
    }

    @Override
    public boolean isCompletedFor(Player player) {
        return countMatching(player) >= required;
    }

    @Override
    public String getProgressString(Player player) {
        return countMatching(player) + "/" + required;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag t = new CompoundTag();
        t.putString("type", "tag_item");
        t.putString("tag", tag != null ? tag.location().toString() : "");
        t.putInt("required", required);
        return t;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("tag")) {
            String raw = nbt.getString("tag");
            if (!raw.isBlank()) tag = ItemTags.create(new ResourceLocation(raw));
        }
        if (nbt.contains("required")) required = Math.max(1, nbt.getInt("required"));
    }
}
