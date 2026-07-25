package net.phoenix.core.integration.jade.provider;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.ars_nouveau.api.capability.ISourceProviderCapability;
import net.phoenix.core.integration.ars_nouveau.api.capability.SourceRecipeCapability;
import net.phoenix.core.integration.ars_nouveau.common.data.multiblock.source.SourceMultiblockTankMachine;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;

import java.util.List;

public class SourceMachineProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    public static final ResourceLocation UID = PhoenixCore.id("source_machine_info");

    private static final String KEY_STORED = "SourceStored";
    private static final String KEY_CAP = "SourceCapacity";
    private static final String KEY_RECIPE_VAL = "SourceRecipeVal";
    private static final String KEY_IS_INPUT = "SourceIsInput";

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof MetaMachine machine)) return;

        if (machine instanceof SourceMultiblockTankMachine) return;

        if (machine instanceof IRecipeLogicMachine rlm) {
            var logic = rlm.getRecipeLogic();
            if (logic != null && logic.isWorking()) {
                GTRecipe recipe = logic.getLastRecipe();
                if (recipe != null) {
                    long totalSource = 0;
                    boolean isInput = true;

                    var inputContents = recipe.getInputContents(SourceRecipeCapability.CAP);
                    if (!inputContents.isEmpty()) {
                        totalSource = sumSource(inputContents);
                        isInput = true;
                    } else {
                        var outputContents = recipe.getOutputContents(SourceRecipeCapability.CAP);
                        if (!outputContents.isEmpty()) {
                            totalSource = sumSource(outputContents);
                            isInput = false;
                        }
                    }

                    if (totalSource > 0) {
                        tag.putLong(KEY_RECIPE_VAL, totalSource);
                        tag.putBoolean(KEY_IS_INPUT, isInput);
                    }
                }
            }
        }

        if (machine instanceof ISourceProviderCapability provider) {
            addSourceData(tag, provider.getSource());
        } else if (machine instanceof MultiblockControllerMachine controller && controller.isFormed()) {
            var parts = controller.getParts();
            if (parts != null) {
                int totalStored = 0;
                int totalCap = 0;
                boolean found = false;
                for (MultiblockPartMachine part : parts) {
                    if (part instanceof ISourceProviderCapability p) {
                        ISourceTile source = p.getSource();
                        if (source != null) {
                            totalStored += source.getSource();
                            totalCap += source.getMaxSource();
                            found = true;
                        }
                    }
                }
                if (found) {
                    tag.putInt(KEY_STORED, totalStored);
                    tag.putInt(KEY_CAP, totalCap);
                }
            }
        }
    }

    private long sumSource(List<com.gregtechceu.gtceu.api.recipe.content.Content> contents) {
        long sum = 0;
        for (var c : contents) {

            Object inner = c.content();
            if (inner != null) {
                sum += SourceRecipeCapability.CAP.of(inner).getSource();
            }
        }
        return sum;
    }

    private void addSourceData(CompoundTag tag, ISourceTile source) {
        if (source != null) {
            tag.putInt(KEY_STORED, source.getSource());
            tag.putInt(KEY_CAP, source.getMaxSource());
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!config.get(UID)) return;

        if (!(accessor.getBlockEntity() instanceof MetaMachine)) return;

        CompoundTag data = accessor.getServerData();
        if (data == null || data.isEmpty()) return;

        if (data.contains(KEY_STORED) && data.contains(KEY_CAP)) {
            int stored = data.getInt(KEY_STORED);
            int cap = data.getInt(KEY_CAP);
            if (cap > 0) {
                float pct = Math.min(1f, (float) stored / cap);
                tooltip.add(tooltip.getElementHelper().progress(
                        pct,
                        Component.literal(stored + " / " + cap),
                        tooltip.getElementHelper().progressStyle().color(0x8F00FF, 0x8F00FF).textColor(0xFFFFFFFF),
                        BoxStyle.DEFAULT,
                        true));
            }
        }

        if (data.contains(KEY_RECIPE_VAL)) {
            long val = data.getLong(KEY_RECIPE_VAL);
            boolean isInput = data.getBoolean(KEY_IS_INPUT);

            String translationKey = isInput ? "jade.phoenixcore.source_taking" : "jade.phoenixcore.source_giving";
            ChatFormatting color = isInput ? ChatFormatting.RED : ChatFormatting.GREEN;

            tooltip.add(Component.translatable(translationKey)
                    .withStyle(color)
                    .append(Component.literal(": " + val).withStyle(ChatFormatting.WHITE)));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
