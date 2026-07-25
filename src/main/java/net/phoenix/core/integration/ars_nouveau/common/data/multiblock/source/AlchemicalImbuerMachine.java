package net.phoenix.core.integration.ars_nouveau.common.data.multiblock.source;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.phoenix.core.api.recipe.PhoenixRecipeModifier;
import net.phoenix.core.common.data.PhoenixRecipeTypes;
import net.phoenix.core.saveddata.SoulSavedData;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.TextWidget;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AlchemicalImbuerMachine extends WorkableElectricMultiblockMachine {

    private static final long HARMONIZATION_THRESHOLD = 72000L;

    @Getter
    @SaveField
    @SyncToClient
    private float cachedFloraBoost = 0.0f;

    @SaveField
    @SyncToClient
    private float cachedSoulDensity = 0.0f;

    @Getter
    @SaveField
    @SyncToClient
    private long totalWorkTicks = 0L;

    @SaveField
    private boolean wasWorking = false;

    private int scanTimer = 0;

    public AlchemicalImbuerMachine(BlockEntityCreationInfo info) {
        super(info);
    }

    @Override
    public void formStructure(@NotNull String substructureName) {
        super.formStructure(substructureName);
        if (getLevel() instanceof ServerLevel level) {
            this.cachedFloraBoost = scanEnvironment(level, getBlockPos());
            this.cachedSoulDensity = SoulSavedData.get(level).getMultiplier(new ChunkPos(getBlockPos()));
        }
    }

    @Override
    public boolean onWorking() {
        boolean isWorking = super.onWorking();

        if (isWorking) {

            if (totalWorkTicks < HARMONIZATION_THRESHOLD) {
                totalWorkTicks++;
            }

            scanTimer++;
            if (scanTimer >= 200) {
                if (getLevel() instanceof ServerLevel level) {
                    this.cachedFloraBoost = scanEnvironment(level, getBlockPos());
                    this.cachedSoulDensity = SoulSavedData.get(level).getMultiplier(new ChunkPos(getBlockPos()));
                }
                scanTimer = 0;
            }
        }

        wasWorking = isWorking;
        return isWorking;
    }

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
                            if (boost >= 6.0f) return 6.0f;
                        }
                    }
                }
            }
        }
        return Math.min(boost, 6.0f);
    }

    private float getBlockBoost(BlockState state) {
        String registryName = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();

        switch (registryName) {
            case "ars_nouveau:whirlisprig_flower":
                return 0.20f;
            case "ars_nouveau:magebloom_crop":
                return 0.05f;
            case "ars_nouveau:sourceberry_bush":
                return 0.01f;
            default:

                if (state.is(BlockTags.FLOWERS)) {
                    return 0.002f;
                }
                return 0.0f;
        }
    }

    public static ModifierFunction recipeModifier(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof AlchemicalImbuerMachine imbuer)) {
            return RecipeModifier.nullWrongType(AlchemicalImbuerMachine.class, machine);
        }

        float resonance = 1.0f;
        if (imbuer.getLevel() instanceof ServerLevel serverLevel) {
            resonance = SoulSavedData.get(serverLevel).getMultiplier(new ChunkPos(imbuer.getBlockPos())) +
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

    @Override
    public List<IWidget> getWidgetsForDisplay(PanelSyncManager syncManager) {
        List<IWidget> widgets = super.getWidgetsForDisplay(syncManager);
        if (!isFormed()) return widgets;
        widgets.add(new TextWidget<>(Text.of(Component.literal("§5Alchemical Analysis:"))));
        widgets.add(new TextWidget<>(Text.dynamic(
                () -> Component.literal("  §7Soul Resonance: §d" + String.format("%.2f", cachedSoulDensity)))));
        widgets.add(new TextWidget<>(Text.dynamic(
                () -> Component.literal("  §7Garden Boost:   §b+" + String.format("%.2f", cachedFloraBoost)))));
        widgets.add(new TextWidget<>(Text.dynamic(() -> {
            float harmonicBonus = (totalWorkTicks >= HARMONIZATION_THRESHOLD) ? 0.5f : 0.0f;
            float totalPotency = cachedSoulDensity + cachedFloraBoost + harmonicBonus;
            return Component.literal("  §eTotal Potency:  §l" + String.format("%.2fx", totalPotency));
        })));
        widgets.add(new TextWidget<>(Text.dynamic(() -> {
            if (totalWorkTicks >= HARMONIZATION_THRESHOLD)
                return Component.literal("§6Status: §l§nCHUNK HARMONIZED §a(+0.50)");
            int percent = (int) ((totalWorkTicks / (double) HARMONIZATION_THRESHOLD) * 100);
            return Component.literal("§8Harmonizing: " + percent + "%");
        })));
        return widgets;
    }
}
