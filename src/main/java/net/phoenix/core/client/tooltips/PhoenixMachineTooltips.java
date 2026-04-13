package net.phoenix.core.client.tooltips;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class PhoenixMachineTooltips {

    public static void addMultiline(List<Component> tooltip, String baseKey) {
        int i = 0;
        while (true) {
            String indexedKey = baseKey + "." + i;
            Component line = Component.translatable(indexedKey);

            if (line.getString().equals(indexedKey)) {
                break;
            }

            tooltip.add(line);
            i++;
        }
    }

    public static void appendStructure(List<Component> tooltip, Map<String, String> structureMap) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("multiblock.structureadvtooltip"));
            structureMap.forEach((key, value) -> {
                tooltip.add(Component.translatable("multiblock.tooltip." + key, Component.literal(value)));
            });
        } else {
            tooltip.add(Component.translatable("multiblock.yellowline"));
            tooltip.add(Component.translatable("multiblock.underyellowline"));
        }
    }

    public static final BiConsumer<ItemStack, List<Component>> DIMENSIONAL_ANCHOR_TOOLTIPS = (stack, tooltip) -> {
        if (!Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("multiblock.tooltip.machinetype",
                    Component.literal("§eDimensional Anchoring")));
            tooltip.add(Component.translatable("multiblock.yellowline"));

            addMultiline(tooltip, "gtultimate.custom.tooltip_dimensional_anchor");
        }

        PhoenixMachineTooltips.appendStructure(tooltip, TooltipConstants.STABLE_CASING);
    };
    public static final BiConsumer<ItemStack, List<Component>> AETHERIAL_FABRICATOR_TOOLTIPS = (stack, tooltip) -> {
        if (!Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("multiblock.tooltip.machinetype",
                    Component.literal("§eAetherial Fabricaton")));
            tooltip.add(Component.translatable("multiblock.yellowline"));

            addMultiline(tooltip, "gtultimate.custom.tooltip_aetherial_fabricator");

        }

        PhoenixMachineTooltips.appendStructure(tooltip, TooltipConstants.CLEAN_CASING);
    };

    public static final BiConsumer<ItemStack, List<Component>> ALCHEMICAL_IMBUER_TOOLTIPS = (stack, tooltip) -> {
        if (!Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("multiblock.tooltip.machinetype",
                    Component.literal("§eSource Extraction/Source Imbuement")));
            tooltip.add(Component.translatable("multiblock.yellowline"));

            addMultiline(tooltip, "gtultimate.custom.tooltip_alchemical_imbuer");
            tooltip.add(Component.translatable("multiblock.yellowline"));

            tooltip.add(Component.translatable("multiblock.sourceoutputaccess1"));
            tooltip.add(Component.translatable("multiblock.sourceoutputaccess2"));
            tooltip.add(Component.translatable("multiblock.sourceinputaccess1"));
            tooltip.add(Component.translatable("multiblock.sourceinputaccess2"));
            tooltip.add(Component.translatable("multiblock.subtickaccess1"));
            tooltip.add(Component.translatable("multiblock.subtickaccess2"));

        }

        PhoenixMachineTooltips.appendStructure(tooltip, TooltipConstants.SOURCE_FIBER_CASING);
    };
    public static final BiConsumer<ItemStack, List<Component>> BIO_ENGINE_TOOLTIPS = (stack, tooltip) -> {
        if (!Screen.hasShiftDown()) {
            tooltip.add(
                    Component.translatable("multiblock.tooltip.machinetype", Component.literal("Bio-Aetheric Engine")));
            tooltip.add(Component.translatable("multiblock.yellowline"));

            addMultiline(tooltip, "gtultimate.custom.tooltip_bio_engine");
            tooltip.add(Component.translatable("multiblock.yellowline"));

            tooltip.add(Component.translatable("multiblock.sourceinputaccess1"));
            tooltip.add(Component.translatable("multiblock.sourceinputaccess2"));
            tooltip.add(Component.translatable("multiblock.energyoutputaccess1"));
            tooltip.add(Component.translatable("multiblock.energyoutputaccess2"));
            tooltip.add(Component.translatable("multiblock.subtickaccess1"));
            tooltip.add(Component.translatable("multiblock.subtickaccess2"));

        }

        PhoenixMachineTooltips.appendStructure(tooltip, TooltipConstants.SOURCE_FIBER_CASING);
    };
    public static final BiConsumer<ItemStack, List<Component>> SOURCE_REACTOR_TOOLTIPS = (stack, tooltip) -> {
        if (!Screen.hasShiftDown()) {
            tooltip.add(
                    Component.translatable("multiblock.tooltip.machinetype", Component.literal("§eSource Reactor")));
            tooltip.add(Component.translatable("multiblock.yellowline"));

            addMultiline(tooltip, "gtultimate.custom.tooltip_source_reactor");
            tooltip.add(Component.translatable("multiblock.yellowline"));

            tooltip.add(Component.translatable("multiblock.sourceinputaccess1"));
            tooltip.add(Component.translatable("multiblock.sourceinputaccess2"));
            tooltip.add(Component.translatable("multiblock.subtickaccess1"));
            tooltip.add(Component.translatable("multiblock.subtickaccess2"));

        }

        PhoenixMachineTooltips.appendStructure(tooltip, TooltipConstants.SOURCE_FIBER_CASING);
    };
}
