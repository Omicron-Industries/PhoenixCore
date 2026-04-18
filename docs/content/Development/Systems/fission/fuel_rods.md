---
title: Fission Fuel Rods
---


# How to add new Fuel Rods and explaining how they are done

## The Basics
Blanket rods provide the core values for the `FissionWorkableElectricMultiblockMachine` class to perform `fuel` operations.

If you are a kubejs dev who merely needs to know how to add new `Fuel Rods`, only the first two sections of this page will be useful.

```js
StartupEvents.registry('block', event => {
    event.create('high_density_driver_rod', 'phoenix_fission:fission_fuel_rod')
        .displayName('High-Density Driver Rod')
        .baseHeatProduction(50) // The amount of heat each rod produces/t before moderator effects.
        .durationTicks(1200) // The amount of time at base before it tries to consume another use of fuel.
        .amountPerCycle(1) // The amount of fuel it eats per cycle. 
        .neutronBias(1) // Refers directly to breeder rod output fuel instability, does nothing by itself. 
        .tier(3) // Handles primary rod and explosion logic.
        .fuelKey('gtceu:uranium_235_nugget') // The input fuel.
        .outputKey('gtceu:depleted_uranium_235_nugget') // The output "depleted" fuel.
        .rodMaterial(() => GTMaterials.get('stainless_steel')) // Used for some internal names.
        .texture('kubejs:block/fission/high_density_driver_rod'); // Also requires a texture with _active appended. 
});
```


## Understanding how to actually use the fuel rods

These blocks are the core of heat and power logic in the fission reactors. Along with coolers, they are two blocks that are necessary for reactors to run.

Fuel is used, and optionally given in the form of depleted fuels if there is output space, per the time chosen on the block registry.

## Understanding the classes

Now, back to talking about blanket rod blocks.

They are registered in two parts, an interface named `IFissionFuelRodType` and a block class named `FissionFuelRodBlock`.

```java
package net.phoenix.core.integration.phoenix_fission.api.block;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.Lazy;
import net.phoenix.core.PhoenixAPI;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

public interface IFissionFuelRodType {

    @NotNull
    String getName();

    default int getTintColor() {
        return 0xFFFFFFFF;
    }

    default int getNeutronBias() {
        return 0;
    }

    int getBaseHeatProduction();

    @NotNull
    String getFuelKey();

    @NotNull
    String getOutputKey();

    int getDurationTicks();

    int getAmountPerCycle();


    int getTier();

    ResourceLocation getTexture();


    Lazy<IFissionFuelRodType[]> ALL_FUEL_RODS_BY_HEAT = Lazy.of(() -> PhoenixAPI.FISSION_FUEL_RODS.keySet().stream()
            .sorted(Comparator.comparingInt(IFissionFuelRodType::getBaseHeatProduction))
            .toArray(IFissionFuelRodType[]::new));
}

```
This is the class we define/change first. Everything goes through this interface for use in `FissionFuelRodBlock` and any other classes using the same logic.

- `getName`. This controls the registry name of the `Fuel Rod Block`.
- `getTintColor`. Correctly not working attempt at `auto tinting` blocks, one day it will be real.
- `neutronBias`.  This controls the `bias` of the `Fuel Rod Block`. Does nothing outside of the breeder reactor ecosystem.
- `getBaseHeatProduction`. This controls the base `heat production` per tick per `Fuel Rod Block`.
- `getFuelKey`. This controls the `input fuel` of the `Fuel Rod Block`.
- `getOutputKey`.  This controls the `output fuel` of the `Fuel Rod Block`.
- `getDurationTicks`. This controls how long it takes for one instance of `fuel` to be used.
- `getAmountPerCycle`. This controls how much of the `input fuel` is used per cycle. 
- `getTexture`. This controls the `texture` of the `Fuel Rod Block`.

Then, finally, we have the api call. `ALL_FUEL_RODS_BY_HEAT` is passed to be stored by the PhoenixAPI class. This allows us to pass every class using this interface to the predicate.

```java
package net.phoenix.core.common.block;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.block.ActiveBlock;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.utils.GTUtil;

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
import net.phoenix.core.PhoenixFission;
import net.phoenix.core.integration.phoenix_fission.api.block.IFissionFuelRodType;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

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
            tooltip.add(Component.literal("Hold §fShift§7 for details")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        Component fuelName = getRegistryDisplayName(fuelRodType.getFuelKey());

        tooltip.add(Component.translatable("phoenix.fission.fuel_required", fuelName)
                .withStyle(ChatFormatting.AQUA));

        Component outputName = getRegistryDisplayName(fuelRodType.getOutputKey());

        tooltip.add(Component.translatable("phoenix.fission.depleted_fuel", outputName)
                .withStyle(ChatFormatting.DARK_GREEN));

        tooltip.add(Component.translatable("phoenix.fission.heat_production",
                        Component.literal(String.valueOf(fuelRodType.getBaseHeatProduction()))
                                .withStyle(ChatFormatting.RED))
                .append(Component.literal(" HU/t").withStyle(ChatFormatting.GRAY)));

        double seconds = fuelRodType.getDurationTicks() / 20.0;
        tooltip.add(Component.translatable("phoenix.fission.fuel_cycle",
                        Component.literal(String.valueOf(fuelRodType.getAmountPerCycle()))
                                .withStyle(ChatFormatting.WHITE),
                        Component.literal(String.format("%.2f", seconds))
                                .withStyle(ChatFormatting.GOLD))
                .withStyle(ChatFormatting.GRAY));

        int bias = 0;
        try {
            bias = fuelRodType.getNeutronBias();
        } catch (Throwable ignored) {
        }
        tooltip.add(Component.translatable("phoenix.fission.neutron_bias",
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

        URANIUM("uranium_fuel_rod",
                500, 1,
                1200, 1,
                "gtceu:uranium_nugget",
                "gtceu:plutonium_nugget",
                0xFF7DE7FF,
                4);

        @Getter
        @NotNull
        private final String name;
        @Getter
        private final int baseHeatProduction;
        @Getter
        private final int tier;
        @Getter
        private final int durationTicks;
        @Getter
        private final int amountPerCycle;
        @Getter
        @NotNull
        private final String fuelKey;
        @Getter
        @NotNull
        private final ResourceLocation texture;
        @Getter
        @NotNull
        private final String outputKey;

        @Getter
        private final int neutronBias;
        @Getter
        private final int tintColor;

        FissionFuelRodTypes(String name, int heat, int tier, int duration, int amount,
                            String fuelKey, String outputKey, int tintColor,
                            int neutronBias) {
            this.name = name;
            this.baseHeatProduction = heat;
            this.tier = tier;
            this.durationTicks = duration;
            this.amountPerCycle = amount;
            this.fuelKey = fuelKey;
            this.texture = PhoenixFission.id("block/fission/fuel_rod/" + name);
            this.tintColor = tintColor;
            this.outputKey = outputKey;
            this.neutronBias = neutronBias;
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

```

For completeness, we will assume you already know how to make active blocks in the gtm api. So we will focus on what makes this actually unique.

Tooltips

- `Shift` to show the full info, helps to keep inventory cleaner.
- The `fuel` used and the output `fuel`.
- How often/how much of the input `fuel` is used.
- The amount of `heat` each `Fuel Rod Block` provides.
- The `bias` each rod has.

Values

- `String name`. A `String` designed to hold the registration name of the `Breeder Block`, still needs to follow a-z, 0-9.
- `int heat`. 
- `int tier`. An `int` designed to hold the `tier` of the `Breeder Block`, handles `primary fuel rod` logic in reactor.
- `int duration`. 
- `int amount`.
- `String fuelKey`. A `String` designed to handle one fully realized `Forge ID` for `input fuel`.
- `String outputKey`. A `String` designed to handle one fully realized `Forge ID` for `output fuel`.
- `int neutronBias` A `String` designed to handle the `neutronBias` of the `Fuel Rod Block`. Only applies when you also have `Blanket Rod Blocks` in the `BreederWorkableElectricMultiblockMachine`. 
- `int tintColor`. Currently, doesn't do anything, just put 0xFFFFFF.


