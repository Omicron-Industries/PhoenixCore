package net.phoenix.core.integration.phoenix_fission.common.data.block;

import com.gregtechceu.gtceu.api.block.ActiveBlock;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.phoenix_fission.api.block.IFissionBlanketType;
import net.phoenix.core.integration.phoenix_fission.api.block.IFissionBlanketType.BlanketOutput;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@ParametersAreNonnullByDefault
public class FissionBlanketBlock extends ActiveBlock {

    private final IFissionBlanketType blanketType;

    public FissionBlanketBlock(Properties properties, IFissionBlanketType blanketType) {
        super(properties);
        this.blanketType = blanketType;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level,
                                List<Component> tooltip, TooltipFlag flag) {
        if (!GTUtil.isShiftDown()) {
            tooltip.add(Component.translatable("block.phoenixcore.fission_moderator.shift")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        tooltip.add(Component.translatable("block.phoenixcore.fission_blanket.info_header")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));

        Component inputName = FissionFuelRodBlock.getRegistryDisplayName(blanketType.getInputKey());
        
        tooltip.add(Component.literal(" • ")
                .append(Component.translatable("phoenixcore.tooltip.required_fuel_tier"))
                .append(Component.literal(": Tier " + blanketType.getRequiredFuelTier() + "+")
                        .withStyle(ChatFormatting.DARK_RED)));

        double seconds = blanketType.getDurationTicks() / 20.0;
        tooltip.add(Component.translatable("gtceu.multiblock.generation_features")
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.literal(" • ")
                .append(Component.translatable("phoenixcore.tooltip.amount"))
                .append(Component.literal(": " + blanketType.getAmountPerCycle()).withStyle(ChatFormatting.WHITE)));

        tooltip.add(Component.literal(" • ")
                .append(Component.translatable("gtceu.recipe.duration"))
                .append(Component.literal(": " + String.format("%.2f", seconds) + "s").withStyle(ChatFormatting.GOLD)));

        tooltip.add(Component.literal(" • ")
                .append(Component.translatable("phoenixcore.tooltip.required_fuel_tier"))
                .append(Component.literal(": Tier " + blanketType.getRequiredFuelTier() + "+")
                        .withStyle(ChatFormatting.DARK_RED)));

        tooltip.add(Component.empty());

        tooltip.add(Component.translatable("phoenixcore.blanket.potential_outputs")
                .withStyle(ChatFormatting.YELLOW));

        List<BlanketOutput> outs = blanketType.getOutputs();
        if (outs == null || outs.isEmpty()) {
            tooltip.add(Component.literal(" • (none)").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            for (BlanketOutput o : outs) {
                Component outName = FissionFuelRodBlock.getRegistryDisplayName(o.key());

                ChatFormatting instabilityColor = o.instability() > 3 ? ChatFormatting.RED :
                        o.instability() > 0 ? ChatFormatting.GOLD :
                                ChatFormatting.BLUE;

                tooltip.add(Component.literal(" • ")
                        .append(outName.copy().withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(" (")
                                .append(Component.literal("W:" + o.weight()).withStyle(ChatFormatting.GRAY))
                                .append(Component.literal(", "))
                                .append(Component.literal("Inst:" + o.instability()).withStyle(instabilityColor))
                                .append(Component.literal(")"))));
            }
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("phoenixcore.blanket.bias_hint")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }

    public enum BreederBlanketTypes implements StringRepresentable, IFissionBlanketType {

        THORIUM_BLANKET("thorium_blanket", 1, 1, 3500, 4, "phoenixcore:thorium_fuel_pellet",
                List.of(new BlanketOutput("gtceu:uranium_233_dust", 60, 2),
                        new BlanketOutput("gtceu:uranium_235_dust", 15, 1),
                        new BlanketOutput("gtceu:neptunium_dust", 5, 3), new BlanketOutput("gtceu:lead_dust", 20, 0)),
                0xFFD2FF57),

        URANIUM_BLANKET("uranium_blanket", 2, 2, 4500, 4, "gtceu:uranium_dust",
                List.of(new BlanketOutput("gtceu:plutonium_dust", 50, 2),
                        new BlanketOutput("gtceu:neptunium_dust", 20, 1),
                        new BlanketOutput("gtceu:plutonium_241_dust", 10, 3),
                        new BlanketOutput("gtceu:cadmium_dust", 20, 0)),
                0xFF57D2FF),

        NEPTUNIUM_BLANKET("neptunium_blanket", 3, 3, 5000, 2, "gtceu:neptunium_dust",
                List.of(new BlanketOutput("gtceu:plutonium_241_dust", 40, 2),
                        new BlanketOutput("gtceu:americium_dust", 30, 3), new BlanketOutput("gtceu:curium_dust", 10, 4),
                        new BlanketOutput("gtceu:silver_dust", 20, 0)),
                0xFF32A852),

        PLUTONIUM_BLANKET("plutonium_blanket", 4, 4, 6000, 2, "gtceu:plutonium_dust",
                List.of(new BlanketOutput("gtceu:curium_dust", 50, 3), new BlanketOutput("gtceu:berkelium_dust", 10, 5),
                        new BlanketOutput("gtceu:americium_dust", 20, 2),
                        new BlanketOutput("gtceu:caesium_dust", 20, 0)),
                0xFFFFD27D),

        AMERICIUM_BLANKET("americium_blanket", 5, 3, 8000, 1, "gtceu:americium_dust",
                List.of(new BlanketOutput("gtceu:curium_dust", 60, 3), new BlanketOutput("californium_dust", 5, 6),
                        new BlanketOutput("gtceu:berkelium_dust", 15, 4),
                        new BlanketOutput("gtceu:cadmium_dust", 20, 0)),
                0xFFA83232);

        @Getter
        @NotNull
        private final String name;
        @Getter
        private final int tier;
        private final int requiredFuelTier;
        private final int defaultDuration;
        private final int defaultAmount;
        @NotNull
        private final String defaultInputKey;
        @Getter
        @NotNull
        private final List<BlanketOutput> outputs;
        @Getter
        private final int tintColor;

        BreederBlanketTypes(String name, int tier, int requiredFuelTier, int duration, int amount, String in,
                            List<BlanketOutput> outs, int tintColor) {
            this.name = name;
            this.tier = tier;
            this.requiredFuelTier = requiredFuelTier;
            this.defaultDuration = duration;
            this.defaultAmount = amount;
            this.defaultInputKey = in;
            this.outputs = outs;
            this.tintColor = tintColor;
        }

        @Override
        public int getRequiredFuelTier() {
            return this.requiredFuelTier;
        }

        @Override
        public int getDurationTicks() {
            return defaultDuration;
        }

        @Override
        public int getAmountPerCycle() {
            return defaultAmount;
        }

        @Override
        public @NotNull String getInputKey() {
            return defaultInputKey;
        }

        @Override
        public @NotNull ResourceLocation getTexture() {
            return PhoenixCore.id("block/fission/blanket_base");
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}
