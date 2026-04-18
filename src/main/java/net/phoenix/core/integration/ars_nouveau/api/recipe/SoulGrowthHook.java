package net.phoenix.core.integration.ars_nouveau.api.recipe;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.phoenix.core.saveddata.SoulSavedData;

public class SoulGrowthHook {

    public static void handle(GTRecipe recipe, ServerLevel level, BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        boolean dirty = false;

        if (recipe.data.contains("soul_growth_perm")) {
            float amount = recipe.data.getFloat("soul_growth_perm");
            SoulSavedData.get(level).modifySoul(chunkPos, amount, true);
            dirty = true;
        }

        if (recipe.data.contains("soul_growth_temp")) {
            float amount = recipe.data.getFloat("soul_growth_temp");
            SoulSavedData.get(level).modifySoul(chunkPos, amount, false);
            dirty = true;
        }

        if (dirty && level.random.nextFloat() < 0.1f) {
            level.sendParticles(ParticleTypes.WITCH,
                    pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5,
                    5, 0.2, 0.5, 0.2, 0.02);
        }
    }
}
