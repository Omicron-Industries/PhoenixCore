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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.phoenix_fission.api.block.IFissionCoolerType;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@ParametersAreNonnullByDefault
public class FissionCoolerBlock extends ActiveBlock {

    private final IFissionCoolerType coolerType;

    public FissionCoolerBlock(Properties props, IFissionCoolerType type) {
        super(props);
        this.coolerType = type;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level,
                                List<Component> tooltip, TooltipFlag flag) {
        if (!GTUtil.isShiftDown()) {
            tooltip.add(Component.translatable("block.phoenixcore.fission_moderator.shift")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        tooltip.add(Component.translatable("block.phoenixcore.fission_cooler.info_header")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));

        String inId = coolerType.getInputCoolantFluidId();
        tooltip.add(Component.translatable("phoenixcore.coolant_required", getFluidDisplayName(inId))
                .withStyle(ChatFormatting.WHITE));

        String outId = coolerType.getOutputCoolantFluidId();
        if (!outId.equalsIgnoreCase(inId)) {
            tooltip.add(Component.translatable("phoenixcore.coolant_output", getFluidDisplayName(outId))
                    .withStyle(ChatFormatting.GRAY));
        }

        tooltip.add(Component.translatable("phoenixcore.coolant_usage_value", coolerType.getCoolantUsagePerTick())
                .withStyle(ChatFormatting.LIGHT_PURPLE));

        tooltip.add(Component.translatable("phoenixcore.cooling_power",
                Component.literal(String.valueOf(coolerType.getCoolerTemperature()))
                        .withStyle(ChatFormatting.BLUE))
                .append(Component.literal(" HU/t").withStyle(ChatFormatting.GRAY)));
    }

    public static Component getFluidDisplayName(@NotNull String fluidId) {
        if (fluidId.isEmpty() || "none".equalsIgnoreCase(fluidId)) {
            return Component.literal("None").withStyle(ChatFormatting.GRAY);
        }

        ResourceLocation rl = ResourceLocation.tryParse(fluidId);

        if (rl == null) return Component.translatable(fluidId).withStyle(ChatFormatting.YELLOW);

        Fluid f = ForgeRegistries.FLUIDS.getValue(rl);
        if (f != null && f != Fluids.EMPTY) {
            return Component.translatable(f.getFluidType().getDescriptionId());
        }

        return Component.translatable(fluidId).withStyle(ChatFormatting.YELLOW);
    }

    public enum FissionCoolerTypes implements StringRepresentable, IFissionCoolerType {

        COOLER_BASIC("cooler_basic", 10000, 1, 100, "minecraft:water", "phoenixcore:critical_steam", 0xFF3D80FF),
        COOLER_EV("cooler_ev", 20000, 2, 10, "gtceu:sodium_potassium", "phoenixcore:hot_sodium_potassium", 0xFFFFB03D),
        COOLER_IV("cooler_iv", 30000, 3, 30, "gtceu:sodium_potassium", "phoenixcore:hot_sodium_potassium", 0xFFFF4D3D),
        COOLER_LUV("cooler_luv", 40000, 4, 35, "gtceu:liquid_helium", "gtceu:helium", 0xFFD23DFF);

        @Getter
        @NotNull
        private final String name;
        private final int defaultTemp;
        @Getter
        private final int tier;
        private final int defaultUsage;
        @Getter
        @NotNull
        private final String requiredCoolantMaterialId;
        @Getter
        @NotNull
        private final String outputCoolantFluidId;
        @Getter
        private final int tintColor;

        FissionCoolerTypes(String name, int temp, int tier, int usage, String inputCoolantFluidId,
                           String outputCoolantFluidId, int tintColor) {
            this.name = name;
            this.defaultTemp = temp;
            this.tier = tier;
            this.defaultUsage = usage;
            this.requiredCoolantMaterialId = inputCoolantFluidId;
            this.outputCoolantFluidId = outputCoolantFluidId;
            this.tintColor = tintColor;
        }

        @Override
        public int getCoolerTemperature() {
            return defaultTemp;
        }

        @Override
        public int getCoolantUsagePerTick() {
            return defaultUsage;
        }

        @Override
        public @NotNull ResourceLocation getTexture() {
            return PhoenixCore.id("block/fission/cooler_base");
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
