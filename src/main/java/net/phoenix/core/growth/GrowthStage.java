package net.phoenix.core.growth;

import net.minecraft.world.item.ItemStack;

import it.unimi.dsi.fastutil.ints.IntList;

import java.util.List;

public record GrowthStage(IntList bounds, List<ItemStack> cost) {}
