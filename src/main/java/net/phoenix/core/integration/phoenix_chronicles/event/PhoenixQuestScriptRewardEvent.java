package net.phoenix.core.integration.phoenix_chronicles.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;

import lombok.Getter;

/**
 * Fired on the Forge event bus when a quest's {@code script_event} reward is granted.
 *
 * This is how pack devs run arbitrary code in response to quest completion —
 * they subscribe to this event from a KubeJS server script or a Java mod.
 *
 * ── KubeJS server script ─────────────────────────────────────────────────────
 *
 * // server_scripts/quest_rewards.js
 * ForgeEvents.onEvent(
 * 'net.phoenix.core.integration.phoenix_chronicles.event.PhoenixQuestScriptRewardEvent',
 * event => {
 * const id = event.eventId
 * const player = event.player
 * const data = event.data // CompoundTag — your custom NBT
 *
 * if (id === 'unlock_end') {
 * player.stages.add('end_unlocked')
 * player.tell('§aThe End is now unlocked!')
 * }
 *
 * if (id === 'give_choice_reward') {
 * // data contains whatever you put in the reward's "data" field
 * player.give(Item.of(data.getString('item'), data.getInt('count')))
 * }
 * }
 * )
 *
 * ── Quest SNBT ───────────────────────────────────────────────────────────────
 *
 * rewards: [
 * {type: "script_event", event_id: "unlock_end"}
 * {type: "script_event", event_id: "give_choice_reward",
 * data: {item: "minecraft:diamond", count: 3}}
 * ]
 *
 * ── Java mod subscription ─────────────────────────────────────────────────────
 *
 * @SubscribeEvent
 *                 public static void onQuestScriptReward(PhoenixQuestScriptRewardEvent e) {
 *                 if ("unlock_dimension".equals(e.getEventId())) {
 *                 MyDimensionHelper.unlock(e.getServerPlayer());
 *                 }
 *                 }
 */
public class PhoenixQuestScriptRewardEvent extends PlayerEvent {

    private final ServerPlayer player;
    private final String eventId;
    /**
     * -- GETTER --
     * Arbitrary NBT data from the reward's
     * field.
     * Pack devs can put anything here — item IDs, counts, flags, etc.
     * Returns an empty CompoundTag if no data was specified.
     */
    @Getter
    private final CompoundTag data;

    public PhoenixQuestScriptRewardEvent(ServerPlayer player, String eventId, CompoundTag data) {
        super(player);
        this.player = player;
        this.eventId = eventId;
        this.data = data != null ? data : new CompoundTag();
    }

    public ServerPlayer getServerPlayer() {
        return player;
    }

    /** The event_id string from the reward SNBT. Used to route to the right handler. */
    public String getEventId() {
        return eventId;
    }
}
