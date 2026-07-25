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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.phoenix.core.api.recipe.PhoenixRecipeModifier;
import net.phoenix.core.saveddata.SoulSavedData;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.TextWidget;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SourceReactorMachine extends WorkableElectricMultiblockMachine {

    @Getter
    @SaveField
    @SyncToClient
    private float reactorStability = 1.0f;

    @SaveField
    @SyncToClient
    private float cachedSoulDensity = 1.0f;

    private int scanTimer = 0;

    public SourceReactorMachine(BlockEntityCreationInfo info) {
        super(info);
    }

    @Override
    public void formStructure(@NotNull String substructureName) {
        super.formStructure(substructureName);
        if (getLevel() instanceof ServerLevel serverLevel) {
            this.reactorStability = calculateStability(serverLevel, getBlockPos());
            this.cachedSoulDensity = SoulSavedData.get(serverLevel).getMultiplier(new ChunkPos(getBlockPos()));
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

                            if (stabilizers >= 4.0f) return 5.0f;
                        }
                    }
                }
            }
        }
        return 1.0f + stabilizers;
    }

    @Override
    public boolean onWorking() {
        boolean isWorking = super.onWorking();

        if (isWorking) {
            scanTimer++;
            if (scanTimer >= 200) {
                if (getLevel() instanceof ServerLevel level) {
                    this.reactorStability = calculateStability(level, getBlockPos());
                    this.cachedSoulDensity = SoulSavedData.get(level).getMultiplier(new ChunkPos(getBlockPos()));
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
            soulDensity = SoulSavedData.get(serverLevel).getMultiplier(new ChunkPos(reactor.getBlockPos()));
        }

        double speedBoost = 1.0 + (soulDensity * 0.15);
        double costReduction = 1.0 + (reactor.getReactorStability() * 0.1);

        return PhoenixRecipeModifier.builder()
                .durationMultiplier(1.0 / speedBoost)
                .sourceMultiplier(1.0 / costReduction)
                .build();
    }

    @Override
    public List<IWidget> getWidgetsForDisplay(PanelSyncManager syncManager) {
        List<IWidget> widgets = super.getWidgetsForDisplay(syncManager);
        if (!isFormed()) return widgets;
        widgets.add(new TextWidget<>(Text.of(Component.literal("§5Reactor Core Metrics:"))));
        widgets.add(new TextWidget<>(Text.dynamic(
                () -> Component.literal("  §7Local Soul Density: §d" + String.format("%.2f", cachedSoulDensity)))));
        widgets.add(new TextWidget<>(Text.dynamic(() -> {
            String color = reactorStability > 2.0f ? "§b" : (reactorStability > 1.5f ? "§a" : "§e");
            return Component.literal("  §7Stability Index: " + color + String.format("%.2fx", reactorStability));
        })));
        widgets.add(new TextWidget<>(Text.dynamic(() -> Component
                .literal("  §eReaction Pressure: §l" + String.format("%.2fx", cachedSoulDensity * reactorStability)))));
        widgets.add(new TextWidget<>(Text.dynamic(() -> {
            if (reactorStability < 1.1f) return Component.literal("§c§l⚠ LOW STABILITY — Source Waste Imminent ⚠");
            if (reactorStability > 2.2f) return Component.literal("§b§l⚡ CRITICAL EFFICIENCY ⚡");
            return Component.literal("");
        })));
        return widgets;
    }
}
