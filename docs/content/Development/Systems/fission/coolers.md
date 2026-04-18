---
title: Fission Coolers
---


# How to add new Coolers and explaining how they are done

## The Basics

Coolers provide cooling power for the use of running reactors. Each cooler uses a different coolant and provides a different cooling power.

If you are a KubeJS dev who merely needs to know how to add new `Coolers`, only the first two sections of this page will be useful. 

```js
StartupEvents.registry("block", event => {
    event.create('aether_flow_cooler', 'phoenixcore:fission_cooler')
        .displayName('Aether-Flow Cooler') 
        .coolerTemperature(1025)
        // Cooling power given per cooler (HU/t). If total cooling doesn’t meet reactor needs,
        // core heat rises and meltdown logic will eventually trigger.
        .coolantUsagePerTick(10) // The amount of coolant this cooler uses per tick. Additive logic is a config value.
        .tier(3) // Used for explosion scaling + progression/tier logic.
        .requiredCoolantMaterialId('phoenixcore:frost') // The "cold" coolant from running reactors, only the primary cooler defines this.
        .outputCoolantFluidId('phoenixcore:warm_frost') // The "hot" coolant from running reactors, only the primary cooler defines this.
        .coolerMaterial(() => GTMaterials.get('tungsten_steel'))
        .texture('kubejs:block/fission/aether_flow_cooler'); // Also needs a texture the same name with _active appended to the end.
});
```

## Understanding how to actually use the coolers

The output coolant will be given if the multiblock reactor has an output hatch. 
The input coolant is still necessary for the `Cooler` to provide cooling power. 

Any amount of `Coolers` will provide cooling power in an additive form.
This also includes different tiers of `Coolers`. They look for coolant inside input hatches and me input hatches, without said coolant they will not provide any cooling power.

## Understanding the classes

Now, back to talking about `Cooler Blocks`.

They are registered in two parts, an interface named `IFissionCoolerType` and a block class named `FissionCoolerBlock`.

```java
package net.phoenix.core.integration.phoenix_fission.api.block;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.Lazy;
import net.phoenix.core.PhoenixAPI;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;

public interface IFissionCoolerType {

    @NotNull
    String getName();

    int getTier();

    int getCoolerTemperature();

    @NotNull
    String getRequiredCoolantMaterialId();

    @NotNull
    default String getOutputCoolantFluidId() {
        return getRequiredCoolantMaterialId();
    }

    @NotNull
    default String getInputCoolantFluidId() {
        return getRequiredCoolantMaterialId();
    }

    int getCoolantUsagePerTick();

    default int getCoolantPerTick() {
        return getCoolantUsagePerTick();
    }

    default int getTintColor() {
        return 0xFFFFFFFF;
    }

    Material getMaterial();

    ResourceLocation getTexture();

    Lazy<IFissionCoolerType[]> ALL_COOLER_TEMPERATURES_SORTED = Lazy
            .of(() -> PhoenixAPI.FISSION_COOLERS.keySet().stream()
                    .sorted(Comparator.comparingInt(IFissionCoolerType::getCoolerTemperature))
                    .toArray(IFissionCoolerType[]::new));

    @Nullable
    static IFissionCoolerType getMinRequiredType(int requiredTemperature) {
        return Arrays.stream(ALL_COOLER_TEMPERATURES_SORTED.get())
                .filter(cooler -> cooler.getCoolerTemperature() >= requiredTemperature)
                .findFirst()
                .orElse(null);
    }
}
```
This is the class we define/change first. Everything goes through this interface for use in `FissionCoolerBlock` and any other classes using the same logic.

- `getName`. This controls the registry name of the `Cooler Block`.
- `getTier`. This controls the `Tier` of the `Cooler Block`.
- `getCoolerTemperature`. This controls the cooling power of the `Cooler Block`.
- `getRequiredCoolantMaterialId`. This handles the caching of the forge registry for use in the `Cooler Block` class.
- `getOutputCoolantFluidId`. This controls the "hot" coolant given when a `Cooler Block` is running. 
- `getInputCoolantFluidId`. This controls the "cold" coolant used when a `Cooler Block` is running.
- `getCoolantUsagePerTick`. This controls amount of the coolant used/given when a `Cooler Block` is running.
- `getCoolantPerTick`. This passes `getCoolantUsagePerTick` in a more usable form. 
- `getTintColor`. Currently not working attempt at `auto tinting` blocks, one day it will be real.
- `getTexture`. Controls the texture used by the `Cooler Block`.
- `getMaterial`. Controls the gtm material linked to it, used for some internal names.

Then, finally, we have the api call. `ALL_COOLER_TEMPERATURES_SORTED` is passed to be stored by the PhoenixAPI class. This allows us to pass every class using this interface to the predicate.

```java
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
import net.phoenix.core.PhoenixFission;
import net.phoenix.core.integration.phoenix_fission.api.block.IFissionCoolerType;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@ParametersAreNonnullByDefault
public class FissionCoolerBlock extends ActiveBlock {

    /** Needed for tinting + introspection */
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
        tooltip.add(Component.translatable("phoenix.fission.coolant_required", inName));

        String outId = coolerType.getOutputCoolantFluidId();
        if (!outId.equalsIgnoreCase(inId)) {
            Component outName = getFluidDisplayName(outId);
            tooltip.add(Component.translatable("phoenix.fission.coolant_output", outName));
        }

        tooltip.add(Component.literal("§7Coolant Usage: §f" +
                coolerType.getCoolantUsagePerTick() + " mB/t"));

        tooltip.add(Component.translatable("phoenix.fission.cooling_power",
                coolerType.getCoolerTemperature()));
    }

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

        COOLER_BASIC(
                "basic_cooler",
                50500, 1, 10,
                "gtceu:distilled_water",
                "gtceu:steam",
                PhoenixFission.id("block/fission/basic_cooler_block"),
                0xFF7DE7FF);

        @Getter
        @NotNull
        private final String name;
        @Getter
        private final int coolerTemperature;
        @Getter
        private final int tier;
        @Getter
        private final int coolantUsagePerTick;

        @Getter
        @NotNull
        private final String requiredCoolantMaterialId;

        @Getter
        @NotNull
        private final String outputCoolantFluidId;

        @Getter
        @NotNull
        private final ResourceLocation texture;

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
```
Now, it's time to discuss what this class actually does for our coolers. 

Tooltips

- `Hold Shift` to show the full info, helps to keep inventory cleaner.
- The `coolant` used for consumption.
- The `coolant` used for output.
- How often/how much of the `coolant` is used.
- The cooling power of the `Coooler Block`

Values

- `String name`. A `String` designed to hold the registration name of the `Cooler Block`, still needs to follow a-z, 0-9.
- `int temp`. An `int` designed to hold the `cooling power` of the `Cooler Block`.
- `int tier`. An `int` designed to hold the `tier` of the `Cooler Block`, handles `primary cooler` logic in reactor.
- `int usage`. An `int` designed to hold the amount of `coolant` used per cycle.
- `String inputCoolantFluidId`. A `String` designed to handle one fully realized `Forge ID` for coolant input.
- `String outputCoolantFluidId`. A `String` designed to handle one fully realized `Forge ID` for coolant output.
- `int tintColor`. Currently doesn't do anything, just put 0xFFFFFF.
