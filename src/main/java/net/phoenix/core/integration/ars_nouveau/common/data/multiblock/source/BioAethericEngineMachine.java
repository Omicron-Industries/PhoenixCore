package net.phoenix.core.integration.ars_nouveau.common.data.multiblock.source;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.ITieredMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.phoenix.core.api.recipe.PhoenixRecipeModifier;
import net.phoenix.core.common.data.PTags;
import net.phoenix.core.saveddata.SoulSavedData;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BioAethericEngineMachine extends WorkableElectricMultiblockMachine implements ITieredMachine {

    @Getter
    private float lastBotanicalBoost = 0.0f;

    public BioAethericEngineMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Persisted
    private int dynamoTier = GTValues.HV;
    private long maxHatchOutput = 0;

    @Override
    public int getTier() {
        return isFormed() ? dynamoTier : GTValues.LV;
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        detectDynamoTier();
        if (getLevel() instanceof ServerLevel serverLevel) {
            this.lastBotanicalBoost = calculateFloraBoost(serverLevel, getPos());
        }
    }

    private void detectDynamoTier() {
        int detectedTier = GTValues.ULV;
        long totalPower = 0;

        var parts = getParts();
        if (parts == null) return;

        for (IMultiPart part : parts) {
            var handlers = part.getRecipeHandlers();
            if (handlers == null) continue;

            for (var handler : handlers) {
                Object capObject = handler.getCapability(EURecipeCapability.CAP);
                if (handler.getHandlerIO() == IO.OUT && capObject instanceof IEnergyContainer container) {
                    long voltage = container.getOutputVoltage();
                    long amperage = container.getOutputAmperage();
                    detectedTier = Math.max(detectedTier, GTUtil.getFloorTierByVoltage(voltage));
                    totalPower += (voltage * amperage);
                }
            }
        }

        this.dynamoTier = detectedTier;
        this.maxHatchOutput = totalPower;
    }

    private float calculateFloraBoost(ServerLevel level, BlockPos pos) {
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

                            boost += getFloraBoost(state);

                            if (boost >= 5.0f) return 5.0f;
                        }
                    }
                }
            }
        }
        return Math.min(boost, 5.0f);
    }

    private float getFloraBoost(BlockState state) {
        if (state.is(PTags.SOUL_FLOWERS)) return 0.01f;
        if (state.is(BlockTags.FLOWERS)) return 0.005f;
        return 0.0f;
    }

    public static ModifierFunction recipeModifier(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof BioAethericEngineMachine engine)) {
            return RecipeModifier.nullWrongType(BioAethericEngineMachine.class, machine);
        }

        float baseSoul = 1.0f;
        if (engine.getLevel() instanceof ServerLevel serverLevel) {

            baseSoul = SoulSavedData.get(serverLevel).getMultiplier(new ChunkPos(engine.getPos()));
        }

        float totalResonance = baseSoul + engine.getLastBotanicalBoost();

        double durationMultiplier = 1.0 / (1.0 + (totalResonance * 0.1));

        if (engine.getLevel() != null && engine.getLevel().isNight()) {
            durationMultiplier /= 1.25;
        }

        double euBoost = (double) totalResonance;

        return PhoenixRecipeModifier.builder()
                .durationMultiplier(durationMultiplier)
                .euOutputMultiplier(euBoost)
                .sourceMultiplier(1.0 + (totalResonance * 0.05))
                .build();
    }

    private int scanTimer = 0;

    @Override
    public boolean onWorking() {
        boolean isWorking = super.onWorking();

        if (isWorking) {
            scanTimer++;
            if (scanTimer >= 200) {
                if (getLevel() instanceof ServerLevel level) {
                    this.lastBotanicalBoost = calculateFloraBoost(level, getPos());
                    this.markDirty();
                }
                scanTimer = 0;
            }
        }
        return isWorking;
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        super.addDisplayText(textList);
        if (!isFormed()) return;

        if (getLevel() instanceof ServerLevel serverLevel) {
            float baseSoul = SoulSavedData.get(serverLevel).getMultiplier(new ChunkPos(getPos()));
            float totalResonance = baseSoul + lastBotanicalBoost;

            textList.add(Component.literal("§7-".repeat(15)));
            textList.add(Component.literal("§5Aetheric Analysis:"));
            textList.add(Component.literal("  §7Chunk Base Soul: §d" + String.format("%.2f", baseSoul)));
            textList.add(Component.literal("  §7Flora Bonus: §b+" + String.format("%.2f", lastBotanicalBoost)));

            boolean isNight = serverLevel.isNight();
            String timeStatus = isNight ? "§aActive (1.25x)" : "§6Dormant";
            textList.add(Component.literal("  §7Veil Status: " + timeStatus));

            textList.add(Component.literal("  §eTotal EU Multiplier: §l" + String.format("%.2fx", totalResonance)));

            if (lastBotanicalBoost > 1.0f) {
                textList.add(Component.literal("§b§l» SOUL SATURATED «"));
            }
        }
    }
}
