package net.phoenix.core.integration.phoenix_fission.common.data.block;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TooltipBlockItem extends BlockItem {

    private final String baseTooltipKey;
    private final ChatFormatting[] styles;

    public TooltipBlockItem(Block block, Properties properties, String baseTooltipKey, ChatFormatting... styles) {
        super(block, properties);
        this.baseTooltipKey = baseTooltipKey;
        this.styles = styles.length > 0 ? styles : new ChatFormatting[] { ChatFormatting.GRAY, ChatFormatting.ITALIC };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        // Logic: Keep adding lines as long as the translation key exists
        int i = 0;
        while (true) {
            String lineKey = baseTooltipKey + "." + i;
            if (I18n.exists(lineKey)) {
                tooltip.add(Component.translatable(lineKey).withStyle(styles));
                i++;
            } else {
                // If line.0 doesn't exist, check if the base key itself exists (fallback for single lines)
                if (i == 0 && I18n.exists(baseTooltipKey)) {
                    tooltip.add(Component.translatable(baseTooltipKey).withStyle(styles));
                }
                break;
            }
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
