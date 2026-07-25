package net.phoenix.core.integration.phoenix_fission.common.data.block;

import com.gregtechceu.gtceu.api.block.ActiveBlock;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.phoenix_fission.api.block.IFissionModeratorType;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@ParametersAreNonnullByDefault
public class FissionModeratorBlock extends ActiveBlock {

    private final IFissionModeratorType moderatorType;

    public FissionModeratorBlock(Properties properties, IFissionModeratorType moderatorType) {
        super(properties);
        this.moderatorType = moderatorType;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level,
                                List<Component> tooltip, TooltipFlag flag) {
        if (!GTUtil.isShiftDown()) {
            tooltip.add(Component.translatable("block.phoenixcore.fission_moderator.shift")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        tooltip.add(Component.translatable("block.phoenixcore.fission_moderator.info_header")
                .withStyle(ChatFormatting.AQUA));

        tooltip.add(Component.translatable("block.phoenixcore.fission_moderator.boost",
                moderatorType.getEUBoost()).withStyle(ChatFormatting.GREEN));

        tooltip.add(Component.translatable("block.phoenixcore.fission_moderator.fuel_discount",
                moderatorType.getFuelDiscount()).withStyle(ChatFormatting.YELLOW));
    }

    public enum FissionModeratorTypes implements StringRepresentable, IFissionModeratorType {

        GRAPHITE("graphite_moderator", PhoenixConfigs.INSTANCE.fissionStats.moderators.euBoostGraphiteModerator,
                PhoenixConfigs.INSTANCE.fissionStats.moderators.fuelDiscountGraphiteModerator,
                PhoenixConfigs.INSTANCE.fissionStats.moderators.tierGraphiteModerator, 0xFFB07CFF),
        BERYLLIUM("beryllium_moderator", PhoenixConfigs.INSTANCE.fissionStats.moderators.euBoostBerylliumModerator,
                PhoenixConfigs.INSTANCE.fissionStats.moderators.fuelDiscountBerylliumModerator,
                PhoenixConfigs.INSTANCE.fissionStats.moderators.tierBerylliumModerator, 0xFFE7FF7D),
        HEAVY_WATER("heavy_water_moderator", PhoenixConfigs.INSTANCE.fissionStats.moderators.euBoostHeavyWaterModerator,
                PhoenixConfigs.INSTANCE.fissionStats.moderators.fuelDiscountHeavyWaterModerator,
                PhoenixConfigs.INSTANCE.fissionStats.moderators.tierHeavyWaterModerator, 0xFF7DFFB0),
        NIOBIUM_SIC("niobium_sic_moderator", PhoenixConfigs.INSTANCE.fissionStats.moderators.euBoostNiobiumSicModerator,
                PhoenixConfigs.INSTANCE.fissionStats.moderators.fuelDiscountNiobiumSicModerator,
                PhoenixConfigs.INSTANCE.fissionStats.moderators.tierNiobiumSicModerator, 0xFFFF7D7D);

        @Getter
        @NotNull
        private final String name;
        private final int defaultEUBoost;
        private final int defaultFuelDiscount;
        @Getter
        private final int tier;
        @Getter
        private final int tintColor;

        FissionModeratorTypes(String name, int EUBoost, int fuelDiscount, int tier, int tintColor) {
            this.name = name;
            this.defaultEUBoost = EUBoost;
            this.defaultFuelDiscount = fuelDiscount;
            this.tier = tier;
            this.tintColor = tintColor;
        }

        @Override
        public int getEUBoost() {
            return defaultEUBoost;
        }

        @Override
        public int getFuelDiscount() {
            return defaultFuelDiscount;
        }

        @Override
        public @NotNull ResourceLocation getTexture() {
            return PhoenixCore.id("block/fission/moderator_base");
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }

        @Override
        public Material getMaterial() {
            return GTMaterials.NULL;
        }
    }
}
