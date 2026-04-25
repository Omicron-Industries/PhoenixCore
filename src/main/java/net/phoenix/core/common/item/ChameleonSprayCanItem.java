package net.phoenix.core.common.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.NotNull;
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

    // Inside ChameleonSprayCanItem.java
    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack,
                                                           @NotNull Player player,
                                                           @NotNull LivingEntity target,
                                                           @NotNull InteractionHand hand) {
        return this.behaviour.interactLivingEntity(stack, player, target, hand);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltipComponents,
                                @NotNull TooltipFlag isAdvanced) {
        this.behaviour.appendHoverText(stack, level, tooltipComponents, isAdvanced);
    }
}
