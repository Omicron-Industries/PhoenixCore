package net.phoenix.core.integration.ars_nouveau.common.data.item;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.item.ComponentItem;
import com.gregtechceu.gtceu.api.item.component.IInteractionItem;
import com.gregtechceu.gtceu.api.item.component.IItemUIFactory;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;

import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIContainer;
import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.phoenix.core.api.gui.PhoenixGuiTextures;
import net.phoenix.core.integration.ars_nouveau.client.SoulMapWidget;
import net.phoenix.core.saveddata.SoulSavedData;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.annotation.Nullable;

public class SoulLensItem extends ComponentItem implements IItemUIFactory, IInteractionItem {

    public SoulLensItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult onItemUseFirst(@NotNull ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        MetaMachine machine = MetaMachine.getMachine(level, pos);

        if (machine instanceof IDataStickInteractable interactable) {
            return player.isShiftKeyDown() ?
                    interactable.onDataStickShiftUse(player, stack) :
                    interactable.onDataStickUse(player, stack);
        }

        return InteractionResult.FAIL;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player,
                                                           @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            HeldItemUIFactory.INSTANCE.openUI(new HeldItemUIFactory.HeldItemHolder(player, hand), serverPlayer);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public ModularUI createUI(HeldItemUIFactory.HeldItemHolder holder, Player player) {
        ItemStack stack = holder.getHeld();
        CompoundTag tag = stack.getOrCreateTag();

        ModularUI ui = new ModularUI(220, 200, holder, player).background(PhoenixGuiTextures.TESLA_BACKGROUND);
        ui.widget(new ImageWidget(0, 0, 220, 200, new ColorBorderTexture(2, 0xFF000000)));
        ui.widget(new LabelWidget(50, 6, "Soul Field Topography").setTextColor(0x8F00FF));

        WidgetGroup mapBorder = new WidgetGroup(48, 78, 114, 114);
        mapBorder.setBackground(new ColorBorderTexture(2, 0xFF000000));

        WidgetGroup mapGroup = new WidgetGroup(2, 2, 110, 110);
        mapGroup.setBackground(GuiTextures.DISPLAY);
        mapGroup.addWidget(new SoulMapWidget(4, 4, stack));

        mapBorder.addWidget(mapGroup);
        ui.widget(mapBorder);

        WidgetGroup infoGroup = new WidgetGroup(160, 20, 85, 110);
        String biomeName = tag.getString("BiomeName");
        float current = tag.getFloat("CurrentSoul");
        float max = tag.getFloat("MaxSoul");

        infoGroup.addWidget(new LabelWidget(-155, 0,
                "Target: §6" + (biomeName)));

        infoGroup.addWidget(new LabelWidget(-155, 20, "Density: §d" + String.format("%.2fx", current)));
        infoGroup.addWidget(new LabelWidget(-155, 40, "Status: " + getStatusString(current, max)));

        ui.widget(infoGroup);
        return ui;
    }

    private String getStatusString(float current, float max) {
        if (max <= 0) return "§7Unknown";
        float percentage = current / max;
        if (percentage >= 0.90f) return "§aVibrant";
        if (percentage >= 0.40f) return "§bStable";
        return "§cDepleted";
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, Level level, @NotNull Entity entity, int slotId,
                              boolean isSelected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) return;

        boolean isUIOpen = player.containerMenu instanceof ModularUIContainer;

        CompoundTag tag = stack.getOrCreateTag();
        SoulSavedData data = SoulSavedData.get((ServerLevel) level);
        ChunkPos center = new ChunkPos(entity.blockPosition());

        var entry = data.getSoulMap().get(center);
        if (entry != null) {
            tag.putFloat("CurrentSoul", entry.currentSoul);
            tag.putFloat("MaxSoul", entry.maxCapacity);
        }

        ResourceLocation biomeId = level.registryAccess().registryOrThrow(Registries.BIOME)
                .getKey(level.getBiome(entity.blockPosition()).value());
        if (biomeId != null) tag.putString("BiomeName", formatBiomeName(biomeId.getPath()));

        ListTag mapList = new ListTag();
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                ChunkPos currentPos = new ChunkPos(center.x + x, center.z + z);
                CompoundTag chunkTag = new CompoundTag();
                chunkTag.putInt("relX", x);
                chunkTag.putInt("relZ", z);
                chunkTag.putFloat("density", data.getMultiplier(currentPos));
                mapList.add(chunkTag);
            }
        }
        tag.put("MapData", mapList);
    }

    private int getAnimatedColor(int color1, int color2, int duration) {
        float time = (System.currentTimeMillis() % duration) / (float) duration;
        float phase = (float) Math.sin(time * 2 * Math.PI) * 0.5f + 0.5f;
        int r = (int) (((color1 >> 16) & 0xFF) + (((color2 >> 16) & 0xFF) - ((color1 >> 16) & 0xFF)) * phase);
        int g = (int) (((color1 >> 8) & 0xFF) + (((color2 >> 8) & 0xFF) - ((color1 >> 8) & 0xFF)) * phase);
        int b = (int) ((color1 & 0xFF) + ((color2 & 0xFF) - (color1 & 0xFF)) * phase);
        return (r << 16) | (g << 8) | b;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip,
                                @NotNull TooltipFlag flag) {
        int color = getAnimatedColor(0xA330FF, 0xFF66CC, 2000);
        tooltip.add(
                Component.translatable("phoenixcore.soul_lens.tooltip.flavor").withStyle(Style.EMPTY.withColor(color)));
        tooltip.add(Component.translatable("phoenixcore.soul_lens.tooltip.1").withStyle(Style.EMPTY.withColor(color)));
    }

    private String formatBiomeName(String path) {
        if (path == null || path.isEmpty()) return "Unknown";
        String[] words = path.split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    @Override
    public boolean onEntitySwing(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        return false;
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack, @NotNull Player player,
                                                           @NotNull LivingEntity target,
                                                           @NotNull InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public boolean hurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity target, @NotNull LivingEntity attacker) {
        return false;
    }
}
