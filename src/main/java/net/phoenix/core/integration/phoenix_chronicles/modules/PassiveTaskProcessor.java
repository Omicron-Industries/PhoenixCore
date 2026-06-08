package net.phoenix.core.integration.phoenix_chronicles.modules;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.phoenix_chronicles.QuestTask;
import net.phoenix.core.integration.phoenix_chronicles.event.QuestEvent;
import net.phoenix.core.integration.phoenix_chronicles.tasks.ExperienceTask;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PassiveTaskProcessor {

    @SubscribeEvent
    public static void onQuestTick(QuestEvent.PlayerTick event) {
        Player player = event.getPlayer();

        for (QuestTask task : event.getNode().getTasks()) {
            // FIXED: ItemRequirementTask evaluation deleted because it's evaluated natively via isCompletedFor()!
            if (task instanceof ExperienceTask xpTask) {
                xpTask.checkPlayerLevel(player);
            }
        }
    }
}
