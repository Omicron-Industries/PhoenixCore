package net.phoenix.core.integration.phoenix_chronicles;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.integration.phoenix_chronicles.event.PhoenixQuestScriptRewardEvent;

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
        COMMAND,
        LOOT_TABLE,
        SCRIPT_EVENT
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
            case "loot_table" -> LootTableReward.fromNBT(tag);
            case "script_event" -> ScriptEventReward.fromNBT(tag);
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

    // =========================================================================
    // Loot table reward
    // =========================================================================

    /**
     * Rolls a named loot table and gives every resulting ItemStack to the player.
     * Overflowing items are dropped at the player's feet.
     *
     * SNBT shape: { type: "loot_table", loot_table: "minecraft:chests/simple_dungeon" }
     */
    public static class LootTableReward extends QuestReward {

        private final ResourceLocation lootTableId;

        public LootTableReward(ResourceLocation lootTableId) {
            this.lootTableId = lootTableId;
        }

        public ResourceLocation getLootTableId() {
            return lootTableId;
        }

        @Override
        public RewardType getType() {
            return RewardType.LOOT_TABLE;
        }

        @Override
        public Component getSummary() {
            return Component.literal("Loot: " + lootTableId.getPath());
        }

        @Override
        public void grant(ServerPlayer player) {
            LootTable table = player.getServer().getLootData().getLootTable(lootTableId);
            LootParams params = new LootParams.Builder(player.serverLevel())
                    .withParameter(LootContextParams.THIS_ENTITY, player)
                    .withParameter(LootContextParams.ORIGIN, player.position())
                    .create(LootContextParamSets.GIFT);
            table.getRandomItems(params).forEach(stack -> {
                if (!player.addItem(stack)) player.drop(stack, false);
            });
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString("type", "loot_table");
            tag.putString("loot_table", lootTableId.toString());
            return tag;
        }

        public static LootTableReward fromNBT(CompoundTag tag) {
            if (!tag.contains("loot_table")) return null;
            return new LootTableReward(new ResourceLocation(tag.getString("loot_table")));
        }
    }

    // =========================================================================
    // Script event reward
    // =========================================================================

    /**
     * Fires a {@link PhoenixQuestScriptRewardEvent} on the Forge event bus.
     *
     * Subscribe from KubeJS or Java to run arbitrary code when the quest is completed.
     *
     * SNBT shape:
     * 
     * <pre>
     * {type: "script_event", event_id: "unlock_end"}
     * {type: "script_event", event_id: "give_reward", data: {item: "minecraft:diamond", count: 5}}
     * </pre>
     *
     * KubeJS subscription (server_scripts/quest_rewards.js):
     * 
     * <pre>
     * ForgeEvents.onEvent(
     *   'net.phoenix.core.integration.phoenix_chronicles.event.PhoenixQuestScriptRewardEvent',
     *   event => {
     *     if (event.eventId === 'unlock_end') {
     *       event.player.stages.add('end_unlocked')
     *     }
     *   }
     * )
     * </pre>
     */
    public static class ScriptEventReward extends QuestReward {

        private final String eventId;
        private final CompoundTag data;

        public ScriptEventReward(String eventId, CompoundTag data) {
            this.eventId = eventId;
            this.data = data != null ? data : new CompoundTag();
        }

        public String getEventId() {
            return eventId;
        }

        public CompoundTag getData() {
            return data;
        }

        @Override
        public RewardType getType() {
            return RewardType.SCRIPT_EVENT;
        }

        @Override
        public Component getSummary() {
            return Component.literal("Script: " + eventId);
        }

        @Override
        public void grant(ServerPlayer player) {
            MinecraftForge.EVENT_BUS.post(
                    new PhoenixQuestScriptRewardEvent(player, eventId, data.copy()));
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString("type", "script_event");
            tag.putString("event_id", eventId);
            if (!data.isEmpty()) tag.put("data", data.copy());
            return tag;
        }

        public static ScriptEventReward fromNBT(CompoundTag tag) {
            String id = tag.getString("event_id");
            if (id.isBlank()) return null;
            CompoundTag data = tag.contains("data") ? tag.getCompound("data") : new CompoundTag();
            return new ScriptEventReward(id, data);
        }
    }
}
