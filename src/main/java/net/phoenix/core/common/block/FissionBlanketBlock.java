package net.phoenix.core.common.block;

import com.gregtechceu.gtceu.api.block.ActiveBlock;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.phoenix.core.api.block.IFissionBlanketType;
import net.phoenix.core.api.block.IFissionBlanketType.BlanketOutput;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@ParametersAreNonnullByDefault
public class FissionBlanketBlock extends ActiveBlock {

    /** Needed for tinting + introspection */
    private final IFissionBlanketType blanketType;

    public FissionBlanketBlock(Properties properties, IFissionBlanketType blanketType) {
        super(properties);
        this.blanketType = blanketType;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level,
                                List<Component> tooltip, TooltipFlag flag) {
        if (!GTUtil.isShiftDown()) {
            tooltip.add(Component.literal("Hold §fShift§7 for details")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        Component inputName = FissionFuelRodBlock.getRegistryDisplayName(blanketType.getInputKey());
        tooltip.add(Component.translatable("phoenixcore.blanket_input", inputName)
                .withStyle(ChatFormatting.LIGHT_PURPLE));

        tooltip.add(Component.translatable("phoenixcore.blanket_outputs")
                .withStyle(ChatFormatting.GOLD));

        List<BlanketOutput> outs = blanketType.getOutputs();
        if (outs == null || outs.isEmpty()) {
            tooltip.add(Component.literal("• (none)")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {

            int shown = 0;
            for (BlanketOutput o : outs) {
                if (o == null) continue;
                if (shown++ >= 5) break;

                Component outName = FissionFuelRodBlock.getRegistryDisplayName(o.key());
                tooltip.add(Component.literal("• ")
                        .append(outName)
                        .append(Component.literal("  w=" + o.weight() + "  inst=" + o.instability())
                                .withStyle(ChatFormatting.DARK_GRAY))
                        .withStyle(ChatFormatting.GRAY));
            }
        }

        double seconds = blanketType.getDurationTicks() / 20.0;
        tooltip.add(Component.translatable(
                "phoenixcore.blanket_cycle",
                Component.literal(String.valueOf(blanketType.getAmountPerCycle()))
                        .withStyle(ChatFormatting.WHITE),
                Component.literal(String.format("%.2f", seconds))
                        .withStyle(ChatFormatting.GOLD))
                .withStyle(ChatFormatting.GRAY));
    }

    public enum BreederBlanketTypes implements StringRepresentable, IFissionBlanketType {

        THORIUM_BLANKET("thorium_blanket", 1, 3500, 4, "phoenixcore:thorium_fuel_pellet",
                List.of(new BlanketOutput("phoenixcore:irradiated_thorium_nugget", 100, 1)), 0xFFD2FF57),
        U236_BLANKET("u236_blanket", 2, 4500, 4, "phoenixcore:u236_fuel_pellet",
                List.of(new BlanketOutput("phoenixcore:irradiated_uranium_236_nugget", 100, 1)), 0xFF57D2FF),
        LEAD_BLANKET("lead_blanket", 3, 2000, 1, "gtceu:lead_dust",
                List.of(new BlanketOutput("gtceu:bismuth_dust", 100, 1)), 0xFFB0B0B0),
        HEAVY_BLANKET("heavy_blanket", 4, 6000, 1, "gtceu:uranium_238_dust",
                List.of(new BlanketOutput("gtceu:uranium_242_nugget", 80, 1),
                        new BlanketOutput("gtceu:plutonium_241_nugget", 20, 1)),
                0xFFFFD27D);

        @Getter
        @NotNull
        private final String name;
        @Getter
        private final int tier;
        @Getter
        private final int durationTicks;
        @Getter
        private final int amountPerCycle;
        @Getter
        @NotNull
        private final String inputKey;

        /** NEW: distribution outputs */
        @Getter
        @NotNull
        private final List<BlanketOutput> outputs;

        @Getter
        @NotNull
        private final ResourceLocation texture;

        /** Per-type tint (ARGB) */
        @Getter
        private final int tintColor;

        BreederBlanketTypes(String name, int tier, int duration, int amount,
                            String in, List<BlanketOutput> outs, int tintColor) {
            this.name = name;
            this.tier = tier;
            this.durationTicks = duration;
            this.amountPerCycle = amount;
            this.inputKey = in;
            this.outputs = outs;
            this.texture = new ResourceLocation("phoenixcore", "block/blanket/" + name);
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
    }
}
