package net.phoenix.core.common.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ChameleonSprayCanItem extends Item {

    private final ChameleonSprayCanBehaviour behaviour = new ChameleonSprayCanBehaviour();

    public ChameleonSprayCanItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return this.behaviour.onItemUseFirst(stack, context);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents,
                                TooltipFlag isAdvanced) {
        this.behaviour.appendHoverText(stack, level, tooltipComponents, isAdvanced);
    }
}
