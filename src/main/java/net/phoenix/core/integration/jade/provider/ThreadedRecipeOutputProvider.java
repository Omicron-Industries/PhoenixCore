package net.phoenix.core.integration.jade.provider;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderFluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.integration.jade.GTElementHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.machine.multiblock.unique.BasicThreadedMachine;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.ui.IElementHelper;
import snownee.jade.api.ui.IProgressStyle;
import snownee.jade.util.FluidTextHelper;

import java.util.ArrayList;
import java.util.List;

public class ThreadedRecipeOutputProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    public static final ResourceLocation UID = PhoenixCore.id("imbuer_threads_info");

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        MetaMachine machine = MetaMachine.getMachine(accessor.getLevel(), accessor.getPosition());
        if (!(machine instanceof BasicThreadedMachine imbuer)) return;

        BasicThreadedMachine.RecipeThread[] threads = imbuer.getThreads();

        ListTag threadsList = new ListTag();
        HolderLookup.Provider registries = accessor.getLevel().registryAccess();

        for (int i = 0; i < threads.length; i++) {
            BasicThreadedMachine.RecipeThread thread = threads[i];
            if (!thread.isActive() || thread.recipe == null) continue;

            CompoundTag threadTag = new CompoundTag();
            threadTag.putInt("ID", i + 1);
            threadTag.putFloat("Progress", thread.getProgressPercent());
            threadTag.putLong("CurrentTick", thread.progress);
            threadTag.putLong("MaxTick", thread.duration);
            captureOutputs(threadTag, thread.recipe, registries);
            threadsList.add(threadTag);
        }

        if (!threadsList.isEmpty()) {
            tag.put("ActiveThreads", threadsList);
        }
    }

    private void captureOutputs(CompoundTag tag, GTRecipe recipe, HolderLookup.Provider registries) {
        int tier = RecipeHelper.getPreOCRecipeEuTier(recipe);
        int runs = recipe.getTotalRuns();

        ListTag itemTags = new ListTag();
        for (var out : recipe.getOutputContents(ItemRecipeCapability.CAP)) {
            CompoundTag itemTag = new CompoundTag();
            if (out.content() instanceof IntProviderIngredient provider) {
                IntProviderIngredient chanced = provider;
                if (out.chance() < out.maxChance()) {
                    double countD = (double) runs * out.chance() / out.maxChance();
                    chanced = (IntProviderIngredient) ItemRecipeCapability.CAP.copyWithModifier(provider,
                            ContentModifier.multiplier(countD));
                }
                itemTag = (CompoundTag) JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, chanced.toJson());
            } else {
                ItemStack[] stacks = ItemRecipeCapability.CAP.of(out.content()).getItems();
                if (stacks.length == 0 || stacks[0].isEmpty()) continue;

                itemTag = new CompoundTag();
                stacks[0].save(itemTag);

                if (out.chance() < out.maxChance()) {
                    double countD = (double) stacks[0].getCount() * runs * out.chance() / out.maxChance();
                    itemTag.putInt("Count", Math.max(1, (int) Math.round(countD)));
                }
            }
            itemTags.add(itemTag);
        }
        if (!itemTags.isEmpty()) tag.put("OutputItems", itemTags);

        ListTag fluidTags = new ListTag();
        for (var out : recipe.getOutputContents(FluidRecipeCapability.CAP)) {
            CompoundTag fluidTag = new CompoundTag();
            if (out.content() instanceof IntProviderFluidIngredient provider) {
                IntProviderFluidIngredient chanced = provider;
                if (out.chance() < out.maxChance()) {
                    double countD = (double) runs * out.chance() / out.maxChance();
                    chanced = (IntProviderFluidIngredient) FluidRecipeCapability.CAP.copyWithModifier(provider,
                            ContentModifier.multiplier(countD));
                }
                fluidTag = chanced.toNBT();
            } else {
                FluidStack[] stacks = FluidRecipeCapability.CAP.of(out.content()).getStacks();
                if (stacks.length == 0 || stacks[0].isEmpty()) continue;
                stacks[0].writeToNBT(fluidTag);
                if (out.chance() < out.maxChance()) {
                    double amountD = (double) stacks[0].getAmount() * runs * out.chance() / out.maxChance();
                    fluidTag.putInt("Amount", Math.max(1, (int) Math.round(amountD)));
                }
            }
            fluidTags.add(fluidTag);
        }
        if (!fluidTags.isEmpty()) tag.put("OutputFluids", fluidTags);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains("ActiveThreads")) return;

        ListTag threadsNbt = data.getList("ActiveThreads", Tag.TAG_COMPOUND);
        if (threadsNbt.isEmpty()) return;

        IElementHelper helper = tooltip.getElementHelper();
        HolderLookup.Provider registries = accessor.getLevel().registryAccess();

        for (int i = 0; i < threadsNbt.size(); i++) {
            CompoundTag tag = threadsNbt.getCompound(i);

            tooltip.add(Component.literal("§6Thread #" + tag.getInt("ID") + ":"));

            String timerText = formatTicks(tag.getLong("CurrentTick")) + " / " + formatTicks(tag.getLong("MaxTick"));
            Component timerComponent = Component.literal(timerText).withStyle(ChatFormatting.WHITE);

            IProgressStyle style = helper.progressStyle().color(0xFF4A90D9, 0xFF1A3A5C);

            tooltip.add(helper.progress(
                    tag.getFloat("Progress"),
                    timerComponent,
                    style,
                    snownee.jade.api.ui.BoxStyle.DEFAULT,
                    true));

            boolean hasItems = tag.contains("OutputItems");
            boolean hasFluids = tag.contains("OutputFluids");

            if (hasItems || hasFluids) {
                tooltip.add(Component.translatable("gtceu.top.recipe_output"));
            }

            if (hasItems) {
                List<Ingredient> items = new ArrayList<>();
                for (Tag it : tag.getList("OutputItems", Tag.TAG_COMPOUND)) {
                    CompoundTag ct = (CompoundTag) it;
                    if (ct.contains("count_provider") || ct.contains("ingredient")) {
                        items.add(IntProviderIngredient.SERIALIZER.parse(
                                (JsonObject) NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, ct)));
                    } else {

                        ItemStack stack = ItemStack.of(ct);
                        if (!stack.isEmpty()) items.add(SizedIngredient.create(stack));
                    }
                }
                addItemTooltips(tooltip, helper, items);
            }

            if (hasFluids) {
                List<FluidIngredient> fluids = new ArrayList<>();
                for (Tag ft : tag.getList("OutputFluids", Tag.TAG_COMPOUND)) {
                    CompoundTag ct = (CompoundTag) ft;
                    if (ct.contains("count_provider")) {
                        fluids.add(IntProviderFluidIngredient.fromNBT(ct));
                    } else {
                        FluidStack stack = FluidStack.loadFluidStackFromNBT(ct);
                        if (!stack.isEmpty()) fluids.add(FluidIngredient.of(stack));
                    }
                }
                addFluidTooltips(tooltip, fluids);
            }
        }
    }

    private void addItemTooltips(ITooltip tooltip, IElementHelper helper, List<Ingredient> items) {
        for (Ingredient ing : items) {
            if (ing == null || ing.isEmpty()) continue;
            ItemStack stack;
            MutableComponent text = CommonComponents.space();
            if (ing instanceof IntProviderIngredient provider) {
                stack = provider.getInner().getItems()[0];
                text.append(Component.translatable("gtceu.gui.content.range",
                        String.valueOf(provider.getCountProvider().getMinValue()),
                        String.valueOf(provider.getCountProvider().getMaxValue())));
            } else {
                stack = ing.getItems()[0].copy();
                text.append(String.valueOf(stack.getCount()));
                stack.setCount(1);
            }
            text.append(Component.translatable("gtceu.gui.content.times_item",
                    stack.getDisplayName().copy().withStyle(ChatFormatting.WHITE)));
            tooltip.add(helper.smallItem(stack));
            tooltip.append(text);
        }
    }

    private void addFluidTooltips(ITooltip tooltip, List<FluidIngredient> fluids) {
        for (FluidIngredient ing : fluids) {
            if (ing == null || ing.isEmpty()) continue;
            FluidStack stack;
            MutableComponent text = CommonComponents.space();
            if (ing instanceof IntProviderFluidIngredient provider) {
                stack = provider.getInner().getStacks()[0];
                text.append(Component.translatable("gtceu.gui.content.range",
                        FluidTextHelper.getUnicodeMillibuckets(provider.getCountProvider().getMinValue(), true),
                        FluidTextHelper.getUnicodeMillibuckets(provider.getCountProvider().getMaxValue(), true)));
            } else {
                stack = ing.getStacks()[0];
                text.append(FluidTextHelper.getUnicodeMillibuckets(stack.getAmount(), true));
            }
            text.append(CommonComponents.space())
                    .append(ComponentUtils.wrapInSquareBrackets(stack.getDisplayName()))
                    .withStyle(ChatFormatting.WHITE);
            tooltip.add(GTElementHelper.smallFluid(JadeFluidObject.of(stack.getFluid(), stack.getAmount())));
            tooltip.append(text);
        }
    }

    private static String formatTicks(long ticks) {
        long secs = ticks / 20;
        return String.format("%02d:%02d", secs / 60, secs % 60);
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
