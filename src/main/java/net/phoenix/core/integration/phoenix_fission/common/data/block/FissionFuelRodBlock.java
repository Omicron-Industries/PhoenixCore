package net.phoenix.core.integration.phoenix_fission.common.data.block;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.block.ActiveBlock;
import com.gregtechceu.gtceu.utils.GTUtil;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.phoenix_fission.api.block.IFissionFuelRodType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@Getter
@ParametersAreNonnullByDefault
public class FissionFuelRodBlock extends ActiveBlock {

    private final IFissionFuelRodType fuelRodType;

    public FissionFuelRodBlock(Properties props, IFissionFuelRodType type) {
        super(props);
        this.fuelRodType = type;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level,
                                List<Component> tooltip, TooltipFlag flag) {
        if (!GTUtil.isShiftDown()) {
            tooltip.add(Component.translatable("block.phoenixcore.fission_moderator.shift")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        tooltip.add(Component.translatable("block.phoenixcore.fission_fuel_rod.info_header")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));

        // Fuel and Output Info
        tooltip.add(Component.translatable("phoenixcore.fuel_required", getRegistryDisplayName(fuelRodType.getFuelKey()))
                .withStyle(ChatFormatting.WHITE));

        tooltip.add(Component.translatable("phoenixcore.depleted_fuel", getRegistryDisplayName(fuelRodType.getOutputKey()))
                .withStyle(ChatFormatting.DARK_GRAY));

        // Stats from Config
        tooltip.add(Component.translatable("phoenixcore.heat_production",
                        Component.literal(String.valueOf(fuelRodType.getBaseHeatProduction()))
                                .withStyle(ChatFormatting.RED))
                .append(Component.literal(" HU/t").withStyle(ChatFormatting.GRAY)));

        double seconds = fuelRodType.getDurationTicks() / 20.0;
        tooltip.add(Component.translatable("phoenixcore.fuel_cycle",
                        Component.literal(String.valueOf(fuelRodType.getAmountPerCycle()))
                                .withStyle(ChatFormatting.WHITE),
                        Component.literal(String.format("%.2f", seconds))
                                .withStyle(ChatFormatting.GOLD))
                .withStyle(ChatFormatting.GRAY));

        int bias = fuelRodType.getNeutronBias();
        tooltip.add(Component.translatable("phoenixcore.neutron_bias",
                        Component.literal((bias >= 0 ? "+" : "") + bias + "%")
                                .withStyle(bias >= 0 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.BLUE))
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.translatable("gtceu.tooltip.tier",
                Component.literal(GTValues.VNF[fuelRodType.getTier()])
                        .withStyle(ChatFormatting.DARK_PURPLE)));
    }

    public static Component getRegistryDisplayName(@NotNull String key) {
        ResourceLocation rl = ResourceLocation.tryParse(key);
        if (rl == null) return Component.literal(key).withStyle(ChatFormatting.YELLOW);

        Item item = ForgeRegistries.ITEMS.getValue(rl);
        if (item != null && item != Items.AIR) {
            return item.getName(new ItemStack(item));
        }

        Fluid fluid = ForgeRegistries.FLUIDS.getValue(rl);
        if (fluid != null && fluid != Fluids.EMPTY) {
            return Component.translatable(fluid.getFluidType().getDescriptionId());
        }

        return Component.literal(key).withStyle(ChatFormatting.YELLOW);
    }

    public enum FissionFuelRodTypes implements StringRepresentable, IFissionFuelRodType {
        T1_FUEL_ROD("t1_fuel_rod", 50, 1, 2500, 1, "phoenixcore:basic_fuel_rod", "phoenixcore:low_level_radioactive_waste", 0xFF62FF57, 0),
        T2_FUEL_ROD("t2_fuel_rod", 150, 2, 3000, 1, "phoenixcore:basic_fuel_rod", "phoenixcore:low_level_radioactive_waste", 0xFF8AFF57, 1),
        T3_FUEL_ROD("t3_fuel_rod", 500, 3, 3500, 1, "phoenixcore:u235_fuel_pellet", "phoenixcore:spent_uranium_235_nugget", 0xFF57FFD2, 5),
        T4_FUEL_ROD("t4_fuel_rod", 1200, 4, 4500, 1, "phoenixcore:plutonium_241_fuel_pellet", "phoenixcore:depleted_plutonium_241_nugget", 0xFF57A8FF, 12),
        T5_FUEL_ROD("t5_fuel_rod", 3000, 5, 8000, 1, "phoenixcore:u242_fuel_pellet", "phoenixcore:spent_uranium_242_nugget", 0xFFFF5757, 30);

        @Getter @NotNull private final String name;
        private final int defaultHeat;
        @Getter private final int tier;
        private final int defaultDuration;
        @Getter private final int amountPerCycle;
        @Getter @NotNull private final String fuelKey;
        @Getter @NotNull private final String outputKey;
        private final int defaultBias;
        @Getter private final int tintColor;

        FissionFuelRodTypes(String name, int heat, int tier, int duration, int amount, String fuelKey, String outputKey, int tintColor, int neutronBias) {
            this.name = name;
            this.defaultHeat = heat;
            this.tier = tier;
            this.defaultDuration = duration;
            this.amountPerCycle = amount;
            this.fuelKey = fuelKey;
            this.outputKey = outputKey;
            this.tintColor = tintColor;
            this.defaultBias = neutronBias;
        }

        @Override
        public int getBaseHeatProduction() {
            return PhoenixConfigs.INSTANCE.fissionStats.fuelRods.baseHeat.getOrDefault(this.name, this.defaultHeat);
        }

        @Override
        public int getDurationTicks() {
            return PhoenixConfigs.INSTANCE.fissionStats.fuelRods.cycleDuration.getOrDefault(this.name, this.defaultDuration);
        }

        @Override
        public int getNeutronBias() {
            return PhoenixConfigs.INSTANCE.fissionStats.fuelRods.neutronBias.getOrDefault(this.name, this.defaultBias);
        }

        @Override
        public @NotNull ResourceLocation getTexture() {
            return PhoenixCore.id("block/fission/fuel_rod_base");
        }

        @Override
        public @NotNull String getSerializedName() { return name; }
    }
}