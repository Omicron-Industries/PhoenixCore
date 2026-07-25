package net.phoenix.core.integration.ars_nouveau.common.data.item;

import com.gregtechceu.gtceu.api.item.ComponentItem;
import com.gregtechceu.gtceu.api.item.component.IInteractionItem;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import com.gregtechceu.gtceu.api.mui.IItemUIHolder;
import com.gregtechceu.gtceu.common.mui.GTGuiTextures;

import net.minecraft.ChatFormatting;
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

import brachy.modularui.factory.PlayerInventoryGuiData;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.TextWidget;
import brachy.modularui.widgets.layout.Flow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SoulLensItem extends ComponentItem implements IItemUIHolder, IInteractionItem {

    private static final int UI_WIDTH = 220;
    private static final int UI_HEIGHT = 200;

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
        return super.use(level, player, hand);
    }

    @Override
    public boolean shouldOpenUI() {
        return true;
    }

    @Override
    public ModularPanel<?> buildUI(PlayerInventoryGuiData<?> guiData, PanelSyncManager panelSyncManager,
                                   UISettings settings) {
        Player player = guiData.getPlayer();
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        CompoundTag tag = stack.getOrCreateTag();

        ModularPanel<?> panel = ModularPanel.defaultPanel("soul_lens", UI_WIDTH, UI_HEIGHT)
                .background(PhoenixGuiTextures.TESLA_BACKGROUND);

        TextWidget titleText = new TextWidget(Component.literal("Soul Field Topography")
                .withStyle(Style.EMPTY.withColor(0x8F00FF)));
        titleText.pos(50, 6);
        panel.child(titleText);

        Flow mapBox = Flow.col();
        mapBox.pos(48, 78);
        mapBox.size(114, 114);
        mapBox.background(GTGuiTextures.DISPLAY);

        SoulMapWidget mapWidget = new SoulMapWidget(stack);
        mapWidget.pos(6, 6);
        mapBox.child(mapWidget);

        panel.child(mapBox);

        TextWidget biomeLabel = new TextWidget(() -> Component.literal("Target: ")
                .append(Component.literal(stack.getOrCreateTag().getString("BiomeName"))
                        .withStyle(ChatFormatting.GOLD)));
        biomeLabel.pos(160, 20);

        TextWidget densityLabel = new TextWidget(() -> {
            float current = stack.getOrCreateTag().getFloat("CurrentSoul");
            return Component.literal("Density: ")
                    .append(Component.literal(String.format("%.2fx", current))
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
        });
        densityLabel.pos(160, 40);

        TextWidget statusLabel = new TextWidget(() -> {
            float current = stack.getOrCreateTag().getFloat("CurrentSoul");
            float max = stack.getOrCreateTag().getFloat("MaxSoul");
            return Component.literal("Status: ").append(getStatusComponent(current, max));
        });
        statusLabel.pos(160, 60);

        panel.child(biomeLabel);
        panel.child(densityLabel);
        panel.child(statusLabel);

        return panel;
    }

    private Component getStatusComponent(float current, float max) {
        if (max <= 0) {
            return Component.literal("Unknown").withStyle(ChatFormatting.GRAY);
        }
        float percentage = current / max;
        if (percentage >= 0.90f) {
            return Component.literal("Vibrant").withStyle(ChatFormatting.GREEN);
        }
        if (percentage >= 0.40f) {
            return Component.literal("Stable").withStyle(ChatFormatting.AQUA);
        }
        return Component.literal("Depleted").withStyle(ChatFormatting.RED);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, Level level, @NotNull Entity entity, int slotId,
                              boolean isSelected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) return;

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
