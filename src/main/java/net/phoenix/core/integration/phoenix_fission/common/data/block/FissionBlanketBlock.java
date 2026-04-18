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
            tooltip.add(Component.translatable("gtceu.tooltip.item_details_expect")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        // --- Input Info ---
        Component inputName = FissionFuelRodBlock.getRegistryDisplayName(blanketType.getInputKey());
        tooltip.add(Component.translatable("phoenixcore.blanket.input")
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(inputName.copy().withStyle(ChatFormatting.AQUA)));

        // --- Cycle Stats ---
        double seconds = blanketType.getDurationTicks() / 20.0;
        tooltip.add(Component.translatable("gtceu.multiblock.generation_features")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(" • ")
                .append(Component.translatable("gtceu.phi.amount"))
                .append(Component.literal(": " + blanketType.getAmountPerCycle()).withStyle(ChatFormatting.WHITE)));
        tooltip.add(Component.literal(" • ")
                .append(Component.translatable("gtceu.recipe.duration"))
                .append(Component.literal(": " + String.format("%.2f", seconds) + "s").withStyle(ChatFormatting.GOLD)));

        tooltip.add(Component.empty());

        // --- Potential Outputs ---
        tooltip.add(Component.translatable("phoenixcore.blanket.potential_outputs")
                .withStyle(ChatFormatting.YELLOW));

        List<BlanketOutput> outs = blanketType.getOutputs();
        if (outs == null || outs.isEmpty()) {
            tooltip.add(Component.literal(" • (none)").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            for (BlanketOutput o : outs) {
                Component outName = FissionFuelRodBlock.getRegistryDisplayName(o.key());

                // Color code based on instability: Higher instability = more Red/Fast spectrum
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

        // --- Mechanic Hint ---
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("phoenixcore.blanket.bias_hint")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }

    public enum BreederBlanketTypes implements StringRepresentable, IFissionBlanketType {



            THORIUM_BLANKET("thorium_blanket", 1, 3500, 4, "phoenixcore:thorium_fuel_pellet",
                    List.of(
                            new BlanketOutput("gtceu:uranium_233_dust", 60, 2), // Main path
                            new BlanketOutput("gtceu:uranium_235_dust", 15, 1),
                            new BlanketOutput("gtceu:neptunium_dust", 5, 3),
                            new BlanketOutput("gtceu:lead_dust", 20, 0)         // Byproduct
                    ), 0xFFD2FF57),

            URANIUM_BLANKET("uranium_blanket", 2, 4500, 4, "gtceu:uranium_dust", // Standard U238/U
                    List.of(
                            new BlanketOutput("gtceu:plutonium_dust", 50, 2),    // Pu-239 path
                            new BlanketOutput("gtceu:neptunium_dust", 20, 1),    // Less than Thorium path
                            new BlanketOutput("gtceu:plutonium_241_dust", 10, 3),
                            new BlanketOutput("gtceu:cadmium_dust", 20, 0)       // Fission product
                    ), 0xFF57D2FF),

            NEPTUNIUM_BLANKET("neptunium_blanket", 3, 5000, 2, "gtceu:neptunium_dust",
                    List.of(
                            new BlanketOutput("gtceu:plutonium_241_dust", 40, 2),
                            new BlanketOutput("gtceu:americium_dust", 30, 3),
                            new BlanketOutput("gtceu:curium_dust", 10, 4),
                            new BlanketOutput("gtceu:silver_dust", 20, 0)
                    ), 0xFF32A852),

            PLUTONIUM_BLANKET("plutonium_blanket", 4, 6000, 2, "gtceu:plutonium_dust",
                    List.of(
                            new BlanketOutput("gtceu:curium_dust", 50, 3),
                            new BlanketOutput("gtceu:berkelium_dust", 10, 5),    // Small amounts
                            new BlanketOutput("gtceu:americium_dust", 20, 2),
                            new BlanketOutput("gtceu:caesium_dust", 20, 0)
                    ), 0xFFFFD27D),

            AMERICIUM_BLANKET("americium_blanket", 5, 8000, 1, "gtceu:americium_dust",
                    List.of(
                            new BlanketOutput("gtceu:curium_dust", 60, 3),
                            new BlanketOutput("gtceu:californium_dust", 5, 6),   // Very rare
                            new BlanketOutput("gtceu:berkelium_dust", 15, 4),
                            new BlanketOutput("gtceu:cadmium_dust", 20, 0)
                    ), 0xFFA83232);

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
