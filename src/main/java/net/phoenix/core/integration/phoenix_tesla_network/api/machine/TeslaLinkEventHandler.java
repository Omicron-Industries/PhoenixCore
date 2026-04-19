package net.phoenix.core.integration.phoenix_tesla_network.api.machine;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TieredEnergyMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.integration.phoenix_tesla_network.saveddata.TeslaTeamEnergyData;

@Mod.EventBusSubscriber(modid = "PhoenixCore", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TeslaLinkEventHandler {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockPos brokenPos = event.getPos();
        MetaMachine machine = MetaMachine.getMachine(level, brokenPos);

        if (machine instanceof TieredEnergyMachine) {
            TeslaTeamEnergyData data = TeslaTeamEnergyData.get(level);

            data.removeMachineFromAllTeams(brokenPos);
        }
    }
}
