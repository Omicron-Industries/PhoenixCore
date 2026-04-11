package net.phoenix.core.client.tooltips;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialStack;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.PhoenixCore;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PhoenixTooltips {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        var item = stack.getItem();

        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (id == null) return;

        String namespace = id.getNamespace();
        if (!namespace.equals("phoenixcore") && !namespace.equals("gtceu")) return;

        String path = id.getPath();

        MaterialStack matStack = ChemicalHelper.getMaterialStack(stack);
        Material mat = !matStack.isEmpty() ? matStack.material() : null;

        if (path.endsWith("_crystal_rose")) {
            Component generic = Component.translatable("tooltip.phoenixcore.crystal_rose.generic");
            if (mat != null) {
                generic = generic.copy().setStyle(Style.EMPTY.withColor(TextColor.fromRgb(mat.getMaterialARGB())));
            }
            event.getToolTip().add(generic);

            if (mat != null) {
                if (Screen.hasShiftDown()) {
                    Component materialName = mat.getLocalizedName()
                            .copy()
                            .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(mat.getMaterialARGB())));
                    Component madeFrom = Component.translatable("tooltip.phoenixcore.crystal_rose.made_from",
                            materialName);
                    event.getToolTip().add(madeFrom);
                } else {
                    event.getToolTip().add(Component.literal("§7Hold §eShift§7 to see what it's forged from."));
                }
            }
        }

        if (path.endsWith("_nanites")) {
            Component generic = Component.translatable("tooltip.phoenixcore.nanites.generic");
            if (mat != null) {
                generic = generic.copy().setStyle(Style.EMPTY.withColor(TextColor.fromRgb(mat.getMaterialARGB())));
            }
            event.getToolTip().add(generic);

            if (mat != null) {
                if (Screen.hasShiftDown()) {
                    Component materialName = mat.getLocalizedName()
                            .copy()
                            .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(mat.getMaterialARGB())));
                    Component madeFrom = Component.translatable("tooltip.phoenixcore.nanites.made_from", materialName);
                    event.getToolTip().add(madeFrom);
                } else {
                    event.getToolTip().add(Component.literal("§7Hold §eShift§7 to see what they're constructed from."));
                }
            }
        }
    }
}
