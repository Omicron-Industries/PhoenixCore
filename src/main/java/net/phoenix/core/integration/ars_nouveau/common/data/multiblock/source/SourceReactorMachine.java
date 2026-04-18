package net.phoenix.core.integration.ars_nouveau.common.data.multiblock.source;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.phoenix.core.api.recipe.PhoenixRecipeModifier;
import net.phoenix.core.saveddata.SoulSavedData;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SourceReactorMachine extends WorkableElectricMultiblockMachine {

    @Getter
    private float reactorStability = 1.0f;

    public SourceReactorMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        if (getLevel() instanceof ServerLevel serverLevel) {
            this.reactorStability = calculateStability(serverLevel, getPos());
        }
    }

    private float calculateStability(ServerLevel level, BlockPos pos) {
        float stabilizers = 0.0f;
        ChunkPos centerChunk = new ChunkPos(pos);

        int minY = Math.max(level.getMinBuildHeight(), pos.getY() - 40);
        int maxY = Math.min(level.getMaxBuildHeight(), pos.getY() + 40);

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                var chunk = level.getChunkSource().getChunkNow(centerChunk.x + x, centerChunk.z + z);
                if (chunk == null) continue;

                for (int bx = 0; bx < 16; bx++) {
                    for (int bz = 0; bz < 16; bz++) {
                        for (int by = minY; by <= maxY; by++) {
                            BlockState state = chunk.getBlockState(
                                    new BlockPos((centerChunk.x + x) << 4 | bx, by, (centerChunk.z + z) << 4 | bz));
                            if (state.isAir()) continue;

                            String id = state.getBlock().getDescriptionId();
                            if (id.contains("source_gem_block")) stabilizers += 0.04f;
                            else if (id.contains("magebloom_block")) stabilizers += 0.06f;
                            else if (id.contains("arcane_core")) stabilizers += 0.12f;
                            else if (id.contains(""))

                                if (stabilizers >= 4.0f) return 5.0f;
                        }
                    }
                }
            }
        }
        return 1.0f + stabilizers;
    }

    private int scanTimer = 0;

    @Override
    public boolean onWorking() {
        boolean isWorking = super.onWorking();

        if (isWorking) {
            scanTimer++;
            if (scanTimer >= 200) {
                if (getLevel() instanceof ServerLevel level) {
                    this.reactorStability = calculateStability(level, getPos());
                    this.markDirty();
                }
                scanTimer = 0;
            }
        }
        return isWorking;
    }

    public static ModifierFunction recipeModifier(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof SourceReactorMachine reactor)) {
            return RecipeModifier.nullWrongType(SourceReactorMachine.class, machine);
        }

        float soulDensity = 1.0f;

        if (reactor.getLevel() instanceof ServerLevel serverLevel) {
            soulDensity = SoulSavedData.get(serverLevel).getMultiplier(new ChunkPos(reactor.getPos()));
        }

        double speedBoost = 1.0 + (soulDensity * 0.15);
        double costReduction = 1.0 + (reactor.getReactorStability() * 0.1);

        return PhoenixRecipeModifier.builder()
                .durationMultiplier(1.0 / speedBoost)
                .sourceMultiplier(1.0 / costReduction)
                .build();
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        super.addDisplayText(textList);
        if (!isFormed()) return;

        if (getLevel() instanceof ServerLevel serverLevel) {
            float soulDensity = SoulSavedData.get(serverLevel).getMultiplier(new ChunkPos(getPos()));
            float totalPressure = soulDensity * reactorStability;

            textList.add(Component.literal("§7-".repeat(15)));
            textList.add(Component.literal("§5Reactor Core Metrics:"));
            textList.add(Component.literal("  §7Local Soul Density: §d" + String.format("%.2f", soulDensity)));

            String color = reactorStability > 2.0f ? "§b" : (reactorStability > 1.5f ? "§a" : "§e");
            textList.add(Component.literal("  §7Stability Index: " + color + String.format("%.2fx", reactorStability)));

            var recipe = recipeLogic.getLastRecipe();
            if (recipe != null && recipe.data.contains("voidic")) {
                textList.add(Component.literal("  §3Voidic Buffer: §bOPTIMAL (-25% EU)"));
            }

            textList.add(Component.literal("  §eReaction Pressure: §l" + String.format("%.2fx", totalPressure)));

            if (reactorStability < 1.1f) {
                textList.add(Component.literal("§c§l⚠ LOW STABILITY ⚠"));
                textList.add(Component.literal("§7(Source Waste Imminent)"));
            } else if (reactorStability > 2.2f) {
                textList.add(Component.literal("§b§l⚡ CRITICAL EFFICIENCY ⚡"));
            }
        }
    }
}
