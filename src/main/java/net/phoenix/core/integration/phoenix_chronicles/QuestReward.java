package net.phoenix.core.integration.phoenix_chronicles;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * A reward that can be granted to a player when they complete a quest.
 *
 * Subclasses cover items, XP, and arbitrary server commands.
 * Each type knows how to serialize itself to/from SNBT for disk persistence.
 *
 * SNBT shape inside a quest file:
 * 
 * <pre>
 * rewards: [{
 *   type: "item",
 *   item_id: "minecraft:diamond",
 *   count: 3
 * }, {
 *   type: "xp",
 *   levels: 5
 * }, {
 *   type: "command",
 *   command: "give %player% minecraft:netherite_ingot 1"
 * }]
 * </pre>
 */
public abstract class QuestReward {

    public enum RewardType {
        ITEM,
        XP,
        COMMAND
    }

    public abstract RewardType getType();

    /** Human-readable one-line summary shown in the UI. */
    public abstract Component getSummary();

    /** Grants the reward to a player. Called server-side only. */
    public abstract void grant(ServerPlayer player);

    public abstract CompoundTag serializeNBT();

    // ── Factory ───────────────────────────────────────────────────────────────

    /**
     * Deserializes a reward from a CompoundTag read out of the quest SNBT.
     * Returns null and logs a warning if the type is unknown or data is malformed.
     */
    public static QuestReward deserializeNBT(CompoundTag tag) {
        String type = tag.getString("type");
        return switch (type) {
            case "item" -> ItemReward.fromNBT(tag);
            case "xp" -> XPReward.fromNBT(tag);
            case "command" -> CommandReward.fromNBT(tag);
            default -> null;
        };
    }

    // =========================================================================
    // Item reward
    // =========================================================================

    public static class ItemReward extends QuestReward {

        private final Item item;
        private final int count;

        public ItemReward(Item item, int count) {
            this.item = item;
            this.count = Math.max(1, count);
        }

        public Item getItem() {
            return item;
        }

        public int getCount() {
            return count;
        }

        @Override
        public RewardType getType() {
            return RewardType.ITEM;
        }

        @Override
        public Component getSummary() {
            return Component.literal(count + "x " + item.getDescription().getString());
        }

        @Override
        public void grant(ServerPlayer player) {
            ItemStack stack = new ItemStack(item, count);
            if (!player.addItem(stack)) {
                // Drop at feet if inventory is full
                player.drop(stack, false);
            }
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString("type", "item");
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            tag.putString("item_id", id != null ? id.toString() : "minecraft:air");
            tag.putInt("count", count);
            return tag;
        }

        public static ItemReward fromNBT(CompoundTag tag) {
            ResourceLocation itemId = new ResourceLocation(tag.getString("item_id"));
            Item item = ForgeRegistries.ITEMS.getValue(itemId);
            if (item == null) return null;
            int count = tag.contains("count") ? tag.getInt("count") : 1;
            return new ItemReward(item, count);
        }
    }

    // =========================================================================
    // XP reward
    // =========================================================================

    public static class XPReward extends QuestReward {

        private final int levels;

        public XPReward(int levels) {
            this.levels = Math.max(1, levels);
        }

        public int getLevels() {
            return levels;
        }

        @Override
        public RewardType getType() {
            return RewardType.XP;
        }

        @Override
        public Component getSummary() {
            return Component.literal(levels + " XP level" + (levels != 1 ? "s" : ""));
        }

        @Override
        public void grant(ServerPlayer player) {
            player.giveExperienceLevels(levels);
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString("type", "xp");
            tag.putInt("levels", levels);
            return tag;
        }

        public static XPReward fromNBT(CompoundTag tag) {
            return new XPReward(tag.getInt("levels"));
        }
    }

    // =========================================================================
    // Command reward
    // =========================================================================

    public static class CommandReward extends QuestReward {

        /** %player% is replaced with the player's username at grant time. */
        private final String command;

        public CommandReward(String command) {
            this.command = command;
        }

        public String getCommand() {
            return command;
        }

        @Override
        public RewardType getType() {
            return RewardType.COMMAND;
        }

        @Override
        public Component getSummary() {
            String preview = command.length() > 32 ? command.substring(0, 29) + "…" : command;
            return Component.literal("/" + preview);
        }

        @Override
        public void grant(ServerPlayer player) {
            String resolved = command.replace("%player%", player.getName().getString());
            player.getServer().getCommands().performPrefixedCommand(
                    player.createCommandSourceStack().withSuppressedOutput().withMaximumPermission(4),
                    resolved);
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString("type", "command");
            tag.putString("command", command);
            return tag;
        }

        public static CommandReward fromNBT(CompoundTag tag) {
            return new CommandReward(tag.getString("command"));
        }
    }
}
