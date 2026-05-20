package net.phoenix.core.integration.phoenix_fission.common.data.block;

import com.gregtechceu.gtceu.api.block.ActiveBlock;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.phoenix_fission.api.block.IMSRCoreLinerType;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@ParametersAreNonnullByDefault
public class MSRCoreLinerBlock extends ActiveBlock {

    private final IMSRCoreLinerType linerType;

    public MSRCoreLinerBlock(Properties props, IMSRCoreLinerType type) {
        super(props);
        this.linerType = type;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level,
                                List<Component> tooltip, TooltipFlag flag) {
        if (!GTUtil.isShiftDown()) {
            tooltip.add(Component.translatable("block.phoenixcore.fission_moderator.shift")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        tooltip.add(Component.translatable("block.phoenixcore.msr_liner.info_header")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));

        tooltip.add(Component.literal("Structural Tier: MK" + linerType.getTier())
                .withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.literal(String.format("Flow Efficiency: %d mb/t per block", linerType.getFluidFlowRate()))
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal(String.format("Thermal Dissipation: %.1f Heat/mb", linerType.getHeatPerMb()))
                .withStyle(ChatFormatting.RED));
    }

    public enum MSRLinerTypes implements IMSRCoreLinerType {

        LINER_GRAPHITE("liner_graphite", 1, 10, 10.0,
                "phoenixcore:u235_molten_salt", "phoenixcore:depleted_u235_molten_salt"),

        LINER_HASTELLOY("liner_hastelloy", 2, 25, 15.0,
                "phoenixcore:thorium_u233_molten_salt", "phoenixcore:depleted_thorium_molten_salt"),

        LINER_TITANIUM("liner_titanium", 3, 50, 25.0,
                "phoenixcore:plutonium_molten_salt", "phoenixcore:irradiated_actinide_waste"),

        LINER_NETHERITE("liner_netherite", 4, 100, 40.0,
                "phoenixcore:californium_molten_salt", "phoenixcore:transuranic_sludge_waste");

        @Getter
        @NotNull
        private final String name;
        @Getter
        private final int tier;
        @Getter
        private final int fluidFlowRate;
        @Getter
        private final double heatPerMb;
        @Getter
        private final String inputFluidId;
        @Getter
        private final String outputFluidId;

        MSRLinerTypes(String name, int tier, int flow, double heat, String inputFluid, String outputFluid) {
            this.name = name;
            this.tier = tier;
            this.fluidFlowRate = flow;
            this.heatPerMb = heat;
            this.inputFluidId = inputFluid;
            this.outputFluidId = outputFluid;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }

        @Override
        public ResourceLocation getTexture() {
            return PhoenixCore.id("block/fission/msr/liners/" + name);
        }
    }
}
