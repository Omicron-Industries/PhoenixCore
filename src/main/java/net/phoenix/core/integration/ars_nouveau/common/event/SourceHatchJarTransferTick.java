package net.phoenix.core.integration.ars_nouveau.common.event;

import com.gregtechceu.gtceu.api.capability.recipe.IO;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.ars_nouveau.common.data.multiblock.part.source.SourceHatchPartMachine;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import com.hollingsworth.arsnouveau.client.particle.ParticleUtil;
import com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile;

import java.util.Set;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SourceHatchJarTransferTick {

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        if (level.getGameTime() % 20L != 0L) return;

        Set<BlockPos> hatches = SourceHatchTracker.get(level.dimension());
        if (hatches.isEmpty()) return;

        int radius = PhoenixConfigs.INSTANCE.sourceHatch.sourceJarCheckRadius;
        if (radius <= 0) return;

        for (BlockPos hatchPos : hatches) {
            if (!level.isLoaded(hatchPos)) continue;

            BlockEntity be = level.getBlockEntity(hatchPos);

            if (!(be instanceof SourceHatchPartMachine hatch)) continue;
            if (!hatch.isWorkingEnabled()) continue;

            ISourceTile tank = hatch.getSource();
            if (tank == null) {

                continue;
            }

            int rate = tank.getTransferRate();
            if (rate <= 0) continue;

            if (hatch.getIo() == IO.IN) {
                handleInbound(level, hatchPos, hatch, tank, radius, rate);
            } else {
                handleOutbound(level, hatchPos, hatch, tank, radius, rate);
            }
        }
    }

    private static void handleInbound(ServerLevel level, BlockPos hatchPos, SourceHatchPartMachine hatch,
                                      ISourceTile tank, int radius, int rate) {
        if (!tank.canAcceptSource()) return;

        int remaining = Math.min(tank.getMaxSource() - tank.getSource(), rate);

        for (BlockPos targetPos : BlockPos.betweenClosed(hatchPos.offset(-radius, -radius, -radius),
                hatchPos.offset(radius, radius, radius))) {
            if (remaining <= 0) break;

            if (level.getBlockEntity(targetPos) instanceof SourceJarTile jar) {
                int available = jar.getSource();
                if (available <= 0) continue;

                int toMove = Math.min(remaining, available);
                jar.removeSource(toMove);
                tank.addSource(toMove);

                jar.setChanged();
                level.sendBlockUpdated(targetPos, jar.getBlockState(), jar.getBlockState(), 3);

                hatch.markAsChanged();

                ParticleUtil.spawnFollowProjectile(level, targetPos, hatchPos, jar.getColor());
                remaining -= toMove;
            }
        }
    }

    private static void handleOutbound(ServerLevel level, BlockPos hatchPos, SourceHatchPartMachine hatch,
                                       ISourceTile tank, int radius, int rate) {
        int remaining = Math.min(tank.getSource(), rate);
        if (remaining <= 0) return;

        for (BlockPos targetPos : BlockPos.betweenClosed(hatchPos.offset(-radius, -radius, -radius),
                hatchPos.offset(radius, radius, radius))) {
            if (remaining <= 0) break;

            if (level.getBlockEntity(targetPos) instanceof SourceJarTile jar) {
                int jarSpace = jar.getMaxSource() - jar.getSource();
                if (jarSpace <= 0) continue;

                int toMove = Math.min(remaining, jarSpace);
                tank.removeSource(toMove);
                jar.addSource(toMove);

                jar.setChanged();
                level.sendBlockUpdated(targetPos, jar.getBlockState(), jar.getBlockState(), 3);

                hatch.markAsChanged();

                ParticleUtil.spawnFollowProjectile(level, hatchPos, targetPos, jar.getColor());
                remaining -= toMove;
            }
        }
    }
}
