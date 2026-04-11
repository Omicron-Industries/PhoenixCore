package net.phoenix.core.common.machine.multiblock.source;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;

import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.phoenix.core.api.recipe.PhoenixRecipeModifier;
import net.phoenix.core.common.data.PTags;
import net.phoenix.core.common.data.PhoenixRecipeTypes;
import net.phoenix.core.saveddata.SoulSavedData;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AlchemicalImbuerMachine extends WorkableElectricMultiblockMachine {

    private static final long HARMONIZATION_THRESHOLD = 72000L;

    @Getter
    private float cachedFloraBoost = 0.0f;

    @Getter
    @Persisted
    private long totalWorkTicks = 0L;

    @Persisted
    private boolean wasWorking = false;

    private int scanTimer = 0;

    public AlchemicalImbuerMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    // -------------------------------------------------------------------------
    // Structure Lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        if (getLevel() instanceof ServerLevel level) {
            this.cachedFloraBoost = scanEnvironment(level, getPos());
        }
    }

    // -------------------------------------------------------------------------
    // Main Machine Logic
    // -------------------------------------------------------------------------

    @Override
    public boolean onWorking() {
        boolean isWorking = super.onWorking();

        if (isWorking) {
            // 1. Progress Harmonization
            if (totalWorkTicks < HARMONIZATION_THRESHOLD) {
                totalWorkTicks++;
            }

            // 2. Periodic Environment Scan (Every 10 seconds)
            scanTimer++;
            if (scanTimer >= 200) {
                if (getLevel() instanceof ServerLevel level) {
                    this.cachedFloraBoost = scanEnvironment(level, getPos());
                    this.markDirty();
                }
                scanTimer = 0;
            }
        }

        wasWorking = isWorking;
        return isWorking;
    }

    // -------------------------------------------------------------------------
    // Tag-Based Environment Scanning
    // -------------------------------------------------------------------------

    private float scanEnvironment(ServerLevel level, BlockPos pos) {
        float boost = 0.0f;
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

                            boost += getBlockBoost(state);
                            if (boost >= 3.0f) return 3.0f; // Max Cap
                        }
                    }
                }
            }
        }
        return Math.min(boost, 3.0f);
    }

    private float getBlockBoost(BlockState state) {
        // if (state.is(PTags.MAGICAL_STRUCTURE_BLOCKS)) return 0.05f;
        if (state.is(PTags.SOUL_FLOWERS)) return 0.01f;
        if (state.is(BlockTags.FLOWERS)) return 0.005f;
        return 0.0f;
    }

    // -------------------------------------------------------------------------
    // Recipe Modifiers
    // -------------------------------------------------------------------------

    public static ModifierFunction recipeModifier(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof AlchemicalImbuerMachine imbuer)) {
            return RecipeModifier.nullWrongType(AlchemicalImbuerMachine.class, machine);
        }

        float resonance = 1.0f;
        if (imbuer.getLevel() instanceof ServerLevel serverLevel) {
            resonance = SoulSavedData.get(serverLevel).getMultiplier(new ChunkPos(imbuer.getPos())) +
                    imbuer.getCachedFloraBoost();
            if (imbuer.getTotalWorkTicks() >= HARMONIZATION_THRESHOLD) resonance += 0.5f;
        }

        double efficiencyFactor = 1.0 + ((resonance - 1.0) * 0.9);

        if (recipe.recipeType == PhoenixRecipeTypes.SOURCE_EXTRACTION_RECIPES) {
            return PhoenixRecipeModifier.builder()
                    .durationMultiplier(0.8)
                    .sourceMultiplier(1.1 * efficiencyFactor)
                    .build();
        } else {
            return PhoenixRecipeModifier.builder()
                    .durationMultiplier(1.0 / efficiencyFactor)
                    .sourceMultiplier(1.0 / efficiencyFactor)
                    .build();
        }
    }

    // -------------------------------------------------------------------------
    // Display Text
    // -------------------------------------------------------------------------

    @Override
    public void addDisplayText(@NotNull List<Component> textList) {
        super.addDisplayText(textList);
        if (!isFormed()) return;

        if (getLevel() instanceof ServerLevel serverLevel) {
            float baseSoul = SoulSavedData.get(serverLevel).getMultiplier(new ChunkPos(getPos()));
            float harmonicBonus = (totalWorkTicks >= HARMONIZATION_THRESHOLD) ? 0.5f : 0.0f;
            float totalPotency = baseSoul + cachedFloraBoost + harmonicBonus;

            textList.add(Component.literal("§7-".repeat(15)));
            textList.add(Component.literal("§5Alchemical Analysis:"));
            textList.add(Component.literal("  §7Soul Resonance: §d" + String.format("%.2f", baseSoul)));
            textList.add(Component.literal("  §7Garden Boost:   §b+" + String.format("%.2f", cachedFloraBoost)));
            textList.add(Component.literal("  §eTotal Potency:  §l" + String.format("%.2fx", totalPotency)));

            if (harmonicBonus > 0) {
                textList.add(Component.literal("§6Status: §l§nCHUNK HARMONIZED §a(+0.50)"));
            } else {
                int percent = (int) ((totalWorkTicks / (double) HARMONIZATION_THRESHOLD) * 100);
                textList.add(Component.literal("§8Harmonizing: " + percent + "%"));
            }
        }
    }
}
