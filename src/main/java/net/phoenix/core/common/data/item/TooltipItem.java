package net.phoenix.core.common.data.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

public class TooltipItem extends Item {

    private final Supplier<Component>[] tooltipBuilders;

    @SafeVarargs
    public TooltipItem(Properties properties, Supplier<Component>... tooltipBuilders) {
        super(properties);
        this.tooltipBuilders = tooltipBuilders;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced) {
        for (Supplier<Component> builder : tooltipBuilders) {
            if (builder != null) {
                tooltipComponents.add(builder.get());
            }
        }

        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
    }
}
