---
title: Fission Blanket Rods
---


# How to add new Blanket Rods and explaining how they are done

## The Basics
Blanket rods provide the core values for the `BreederWorkableElectricMultiblockMachine` class to perform breeder operations. 

If you are a KubeJS dev who merely needs to know how to add new `Breeder Rods`, only the first two sections of this page will be useful. 

```js
StartupEvents.registry("block", event => {
    event.create('uranium_blanket_rod', 'phoenix_fission:fission_blanket_rod')
        .displayName('U-238 Blanket Rod')
        .tier(2) // Used to decide what rod is primary. 
        .durationTicks(2400) // The amount of time in ticks it takes at base to consume one instance of the input fuel item.
        .amountPerCycle(1) // The amount of fuel used per durationTicks.
        .inputKey('gtceu:uranium_238_nugget') // Fully realized Forge ID for input fuel.
        .addOutput('gtceu:plutonium_nugget', 70, 1) // Fully realized list of Forge IDs for output fuel,  
        .addOutput('gtceu:plutonium_241_nugget', 20, 3) // The first digit for the outputs list is the weight, aka the chance of said output.
        .addOutput('gtceu:plutonium_238_nugget', 10, 4) // The second digit for the list is the instability. 
        // If the driver rod has a high spectrum bias, a higher instability means this fuel output will have a higher weight.
        .blanketMaterial(() => GTMaterials.get('uranium_238'))
        .texture('kubejs:block/fission/uranium_blanket_rod'); // Also needs a texture the same name with _active appended to the end.
});
```

To expound on weight/instability, I will give an example of it in practice. 

The formula used to determine the effect fuel rods have on blanket outputs is

  - `adjustedWeight` = baseWeight * exp(bias * instability * k);

Where `exp` is the truly random roulette chance, `bias` is the value defined by duel rod, `instability` is defined by our blanket rod, and `k` is a flat variable defined as 0.45.

Say we have a `Fuel Rod` with a spectrum bias of 4, that turns our outputs into

- `Plutonium -> 71.26`
- `Plutonium 241 -> 21.12`
- `Plutonium 238 -> 10.75`

## Understanding how to actually use the blanket rods

These blocks are designed to go in instances of the `BreederWorkableElectricMultiblockMachine` class, while they can be placed in other multiblocks they will not do anything special.
Only the highest tier of `Breeder Rod` decides the timer on which inputs are consumed and outputs are given. However, all `Breeder Rods` will still tick for inputs/outputs.

The primary `Breeder Rod` also is the one chosen to show in the machine ui/jade. While you do not technically need coolers/fuel rods to run a breeder reactor, the reactor will not give power without them. So it is still recommended to encourage their use.

Anything past this point is purely `Java` , meant to teach those who intend to help out with the `Fission` system here. 
You have been warned.

## Understanding the classes

Now, back to talking about blanket rod blocks.

They are registered in two parts, an interface named `IFissionBlanketType` and a block class named `FissionBlanketBlock`.

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
import java.util.List;

import static net.phoenix.core.common.data.PhoenixMaterialRegistry.getMaterial;

public interface IFissionBlanketType {

    @NotNull
    String getName();

    default int getTintColor() {
        Material m = getMaterial();
        if (m != null && m != GTMaterials.NULL) {
            try {
                return 0xFF000000 | m.getMaterialRGB();
            } catch (Throwable ignored) {
            }
        }
        return 0xFFFFFFFF;
    }

    int getTier();

    int getDurationTicks();

    int getAmountPerCycle();

    @NotNull
    String getInputKey();

    public record BlanketOutput(String key, int weight, int instability) {
    }

    List<BlanketOutput> getOutputs();

    @NotNull
    ResourceLocation getTexture();

    @Nullable
    default Material tryResolveMaterial() {
        Material mat = getMaterial();
        if (mat == null || mat == GTMaterials.NULL) return null;
        return mat;
    }

    Material getMaterial();

    Lazy<IFissionBlanketType[]> ALL_BLANKETS_BY_TIER = Lazy.of(() -> PhoenixAPI.FISSION_BLANKETS.keySet().stream()
            .sorted(Comparator.comparingInt(IFissionBlanketType::getTier))
            .toArray(IFissionBlanketType[]::new));
}
```
This is the class we define/change first. Everything goes through this interface for use in `FissionBlanketBlock` and any other classes using the same logic.

- `getName`. This controls the registry name of the `Breeder Block`.
- `getTintColor`. Currently not working attempt at `auto tinting` blocks, one day it will be real.
- `getTier`. This controls the `Tier` of the `Breeder Block`.
- `getDurationTicks`. This controls the blanket fuel's use in `ticks`.
- `getAmountPerCycle`. This controls the blanket fuel's use per `duration ticks` cycle.
- `getInputKey`. This controls the `blanket fuel` itself.
- `BlanketOutput`. This controls the list of outputs.
- `getOutputs`. Works togethor with `BlanketOutput` as backwards compat.
- `getTexture`. Controls the texture used by the `Breeder Block`.
- `tryResolveMaterial`. Tries to resolve the forge registry fuel as a gtm material. 
- `getMaterial`. Controls the gtm material linked to it, used for some internal names.

Then, finally, we have the api call. `ALL_BLANKETS_BY_TIER` is passed to be stored by the PhoenixAPI class. This allows us to pass every class using this interface to the predicate.

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
            tooltip.add(Component.literal("Hold §fShift§7 for details")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        Component inputName = FissionFuelRodBlock.getRegistryDisplayName(blanketType.getInputKey());
        tooltip.add(Component.translatable("phoenix.fission.blanket_input", inputName)
                .withStyle(ChatFormatting.LIGHT_PURPLE));

        tooltip.add(Component.translatable("phoenix.fission.blanket_outputs")
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
                        "phoenix.fission.blanket_cycle",
                        Component.literal(String.valueOf(blanketType.getAmountPerCycle()))
                                .withStyle(ChatFormatting.WHITE),
                        Component.literal(String.format("%.2f", seconds))
                                .withStyle(ChatFormatting.GOLD))
                .withStyle(ChatFormatting.GRAY));
    }

    public enum BreederBlanketTypes implements StringRepresentable, IFissionBlanketType {

        PLUTONIUM_BREEDER("plutonium_breeder",
                2, 2400, 1,
                "gtceu:uranium_238_nugget",
                List.of(
                        new BlanketOutput("gtceu:plutonium_nugget", 70, 1),
                        new BlanketOutput("gtceu:plutonium_241_nugget", 20, 3),
                        new BlanketOutput("gtceu:plutonium_238_nugget", 10, 4)
                ),
                0xFFB07CFF),

        THORIUM_BREEDER("thorium_breeder",
                1, 240, 1,
                "gtceu:uranium_235_nugget",
                List.of(
                        new BlanketOutput("gtceu:plutonium_nugget", 85, 1),
                        new BlanketOutput("gtceu:plutonium_241_nugget", 15, 3)
                ),
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

        @Getter
        @NotNull
        private final List<BlanketOutput> outputs;

        @Getter
        @NotNull
        private final ResourceLocation texture;

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
            this.texture = new ResourceLocation("phoenix_fission", "block/blanket/" + name);
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
```
For completeness, we will assume you already know how to make active blocks in the gtm api. So we will focus on what makes this actually unique.

Tooltips

- `Hold Shift` to show the full info, helps to keep inventory cleaner.
- The `fuel` used and the list of output `fuels`.
- How often/how much of the input `fuel` is used.

Values

- `String name`. A `String` designed to hold the registration name of the `Breeder Block`, still needs to follow a-z, 0-9.
- `int tier`. An `int` designed to hold the `tier` of the `Breeder Block`, handles `primary blanket` logic in reactor.
- `int duration`. An `int` designed to hold the full duration in `ticks` of `fuel` use. 
- `int amount`. An `int` designed to hold the amount of `fuel` used per cycle.
- `String in`. A `String` designed to handle one fully realized `Forge ID`.
- `List BlanketOutputs`. A `List` of all the `blanket outputs` and their `weight/instability` fields. 
- `int tintColor`. Currently doesn't do anything, just put 0xFFFFFF.

