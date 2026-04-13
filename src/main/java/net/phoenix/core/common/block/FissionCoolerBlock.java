package net.phoenix.core.common.block;

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
import net.phoenix.core.api.block.IFissionCoolerType;

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
            tooltip.add(Component.literal("Hold §fShift§7 for details")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        String inId = coolerType.getInputCoolantFluidId();
        Component inName = getFluidDisplayName(inId);
        tooltip.add(Component.translatable("phoenixcore.coolant_required", inName));

        String outId = coolerType.getOutputCoolantFluidId();
        if (!outId.equalsIgnoreCase(inId)) {
            Component outName = getFluidDisplayName(outId);
            tooltip.add(Component.translatable("phoenixcore.coolant_output", outName));
        }

        tooltip.add(Component.literal("§7Coolant Usage: §f" +
                coolerType.getCoolantUsagePerTick() + " mB/t"));

        tooltip.add(Component.translatable("phoenixcore.cooling_power",
                coolerType.getCoolerTemperature()));
    }

    /** Registry-only fluid display name (no Material/GTMaterials). */
    public static Component getFluidDisplayName(@NotNull String fluidId) {
        if (fluidId.isEmpty() || "none".equalsIgnoreCase(fluidId)) {
            return Component.literal("None").withStyle(ChatFormatting.GRAY);
        }

        ResourceLocation rl = ResourceLocation.tryParse(fluidId);
        if (rl == null) return Component.literal(fluidId).withStyle(ChatFormatting.YELLOW);

        Fluid f = ForgeRegistries.FLUIDS.getValue(rl);
        if (f != null && f != Fluids.EMPTY) {
            return Component.translatable(f.getFluidType().getDescriptionId());
        }

        return Component.literal(fluidId).withStyle(ChatFormatting.YELLOW);
    }

    public enum FissionCoolerTypes implements StringRepresentable, IFissionCoolerType {

        COOLER_BASIC("basic_cooler", 1000, 1, 100, "minecraft:water", "phoenixcore:critical_steam",
                PhoenixCore.id("block/fission/active_cooler"), 0xFF3D80FF),
        COOLER_EV("ev_cooler", 5000, 2, 10, "gtceu:sodium_potassium", "phoenixcore:hot_sodium_potassium",
                PhoenixCore.id("block/fission/active_cooler"), 0xFFFFB03D),
        COOLER_IV("iv_cooler", 10000, 3, 30, "gtceu:sodium_potassium", "phoenixcore:hot_sodium_potassium",
                PhoenixCore.id("block/fission/active_cooler"), 0xFFFF4D3D),
        COOLER_LUV("luv_cooler", 20000, 4, 35, "gtceu:liquid_helium", "gtceu:helium",
                PhoenixCore.id("block/fission/active_cooler"), 0xFFD23DFF);

        @Getter
        @NotNull
        private final String name;
        @Getter
        private final int coolerTemperature;
        @Getter
        private final int tier;
        @Getter
        private final int coolantUsagePerTick;

        /** INPUT fluid registry id */
        @Getter
        @NotNull
        private final String requiredCoolantMaterialId;

        /** OUTPUT fluid registry id (hot return) */
        @Getter
        @NotNull
        private final String outputCoolantFluidId;

        @Getter
        @NotNull
        private final ResourceLocation texture;

        /** Per-type tint (ARGB) */
        @Getter
        private final int tintColor;

        FissionCoolerTypes(String name, int temp, int tier, int usage,
                           String inputCoolantFluidId, String outputCoolantFluidId,
                           ResourceLocation texture, int tintColor) {
            this.name = name;
            this.coolerTemperature = temp;
            this.tier = tier;
            this.coolantUsagePerTick = usage;
            this.requiredCoolantMaterialId = inputCoolantFluidId;
            this.outputCoolantFluidId = outputCoolantFluidId;
            this.texture = texture;
            this.tintColor = tintColor;
        }

        @Override
        public int getCoolantUsagePerTick() {
            return this.coolantUsagePerTick;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }

        /**
         * Legacy name: now treated as INPUT coolant fluid id.
         */
        @Override
        public @NotNull String getRequiredCoolantMaterialId() {
            return this.requiredCoolantMaterialId;
        }

        @Override
        public @NotNull String getOutputCoolantFluidId() {
            return this.outputCoolantFluidId;
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
