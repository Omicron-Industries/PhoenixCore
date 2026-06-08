package net.phoenix.core.integration.phoenix_chronicles;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.phoenix_chronicles.capability.QuestCapabilityProvider;
import net.phoenix.core.integration.phoenix_chronicles.network.packet.S2CSyncQuestsPacket;
import net.phoenix.core.integration.phoenix_chronicles.tasks.CraftItemTask;
import net.phoenix.core.integration.phoenix_chronicles.tasks.KillEntityTask;
import net.phoenix.core.network.PhoenixNetwork;

import java.util.Map;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChronicleEvents {

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ChronicleDataLoader());
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(event.getCrafting().getItem());
        if (itemId == null) return;
        int amount = event.getCrafting().getCount();

        player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).ifPresent(data -> {
            for (QuestNode node : QuestTreeRegistry.getAllQuests().values()) {
                QuestState state = data.getQuestState(node.getId(), QuestState.LOCKED);
                if (state == QuestState.COMPLETED || state == QuestState.LOCKED) continue;

                for (QuestTask task : node.getTasks()) {
                    if (task instanceof CraftItemTask craftTask) {
                        // FIXED: Passing player instance context safely down to mutable capability layers
                        craftTask.onItemCrafted(player, itemId, amount);
                    }
                }
                // Check if this crafting event satisfied the remaining conditions for the quest
                QuestProgressTracker.checkAndTryComplete(player, node);
            }
        });
    }

    @SubscribeEvent
    public static void onEntityKilled(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            if (player.level().isClientSide) return;

            ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType());
            if (entityId == null) return;

            player.getCapability(QuestCapabilityProvider.PLAYER_QUESTS).ifPresent(data -> {
                for (QuestNode node : QuestTreeRegistry.getAllQuests().values()) {
                    QuestState state = data.getQuestState(node.getId(), QuestState.LOCKED);
                    if (state == QuestState.COMPLETED || state == QuestState.LOCKED) continue;

                    for (QuestTask task : node.getTasks()) {
                        if (task instanceof KillEntityTask killTask) {
                            // FIXED: Added player context to comply with refactored stateless parameters
                            killTask.onEntityKilled(player, entityId);
                        }
                    }
                    QuestProgressTracker.checkAndTryComplete(player, node);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide) return;

        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            Map<ResourceLocation, QuestNode> serverQuests = QuestTreeRegistry.getAllQuests();
            PhoenixNetwork.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new S2CSyncQuestsPacket(serverQuests));
        }
    }
}
