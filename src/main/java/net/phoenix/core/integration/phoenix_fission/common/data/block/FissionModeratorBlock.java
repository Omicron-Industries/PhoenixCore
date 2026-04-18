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

        tooltip.add(Component.translatable("block.phoenixcore.fission_moderator.info_header"));

        tooltip.add(Component.translatable("block.phoenixcore.fission_moderator.boost",
                moderatorType.getEUBoost()));

        tooltip.add(Component.translatable("block.phoenixcore.fission_moderator.fuel_discount",
                moderatorType.getFuelDiscount()));
    }

    public enum FissionModeratorTypes implements StringRepresentable, IFissionModeratorType {

        GRAPHITE("graphite_moderator", 1, 3, 1, PhoenixCore.id("block/fission/graphite_moderator"), 0xFFB07CFF),
        BERYLLIUM("beryllium_moderator", 5, 2, 2, PhoenixCore.id("block/fission/graphite_moderator"), 0xFFE7FF7D),
        HEAVY_WATER("heavy_water_moderator", 12, 5, 3, PhoenixCore.id("block/fission/graphite_moderator"), 0xFF7DFFB0),
        NIOBIUM_SIC("niobium_sic_moderator", 30, 10, 4, PhoenixCore.id("block/fission/graphite_moderator"), 0xFFFF7D7D);

        @Getter
        @NotNull
        private final String name;
        @Getter
        private final int EUBoost;
        @Getter
        private final int fuelDiscount;
        @Getter
        private final int tier;
        @Getter
        @NotNull
        private final ResourceLocation texture;

        private final int tintColor;

        FissionModeratorTypes(String name, int EUBoost, int fuelDiscount, int tier,
                              ResourceLocation texture, int tintColor) {
            this.name = name;
            this.EUBoost = EUBoost;
            this.fuelDiscount = fuelDiscount;
            this.tier = tier;
            this.texture = texture;
            this.tintColor = tintColor;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }

        @Override
        public int getTintColor() {
            return tintColor;
        }

        @Override
        public Material getMaterial() {
            return GTMaterials.NULL;
        }
    }
}
