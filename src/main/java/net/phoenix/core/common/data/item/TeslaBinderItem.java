package net.phoenix.core.common.data.item;

import com.gregtechceu.gtceu.api.item.ComponentItem;
import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.api.item.component.IInteractionItem;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TieredEnergyMachine;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import com.gregtechceu.gtceu.api.mui.IItemUIHolder;
import com.gregtechceu.gtceu.common.machine.electric.BatteryBufferMachine;
import com.gregtechceu.gtceu.common.mui.GTGuiTextures;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.phoenix.core.api.gui.TeslaBackground;
import net.phoenix.core.integration.phoenix_tesla_network.client.renderer.machine.TeslaHighlightRenderer;
import net.phoenix.core.integration.phoenix_tesla_network.common.machine.singleblock.electric.TeslaWirelessBatteryBufferMachine;
import net.phoenix.core.integration.phoenix_tesla_network.saveddata.TeslaTeamEnergyData;
import net.phoenix.core.utils.TeamUtils;

import brachy.modularui.api.widget.IWidget;
import brachy.modularui.factory.PlayerInventoryGuiData;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.UISettings;
import brachy.modularui.utils.Alignment;
import brachy.modularui.value.sync.IntSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.value.sync.StringSyncValue;
import brachy.modularui.widgets.ButtonWidget;
import brachy.modularui.widgets.TextWidget;
import brachy.modularui.widgets.layout.Flow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class TeslaBinderItem extends ComponentItem
                             implements IItemUIHolder, IInteractionItem, IAddInformation {

    private static final int FILTER_ALL = 0;
    private static final int FILTER_IN = 1;
    private static final int FILTER_OUT = 2;
    private static final int FILTER_SOUL = 3;
    private static final int FILTER_CHARGER = 4;
    private static final String[] FILTER_LABELS = { "ALL", "[I]", "[O]", "[S]", "[C]" };

    private static final int UI_WIDTH = 260;
    private static final int UI_HEIGHT = 300;

    public TeslaBinderItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onEntitySwing(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        return false;
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack,
                                                           @NotNull Player player,
                                                           @NotNull LivingEntity interactionTarget,
                                                           @NotNull InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public @NotNull InteractionResult onItemUseFirst(@NotNull ItemStack itemStack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        MetaMachine machine = MetaMachine.getMachine(level, clickedPos);

        if (machine instanceof IDataStickInteractable interactable) {

            return player.isShiftKeyDown() ?
                    interactable.onDataStickShiftUse(player, itemStack) :
                    interactable.onDataStickUse(player, itemStack);
        }

        if (!level.isClientSide && machine != null) {
            if (level instanceof ServerLevel serverLevel &&
                    machine instanceof TieredEnergyMachine tiered && tiered.energyContainer != null) {

                CompoundTag tag = itemStack.getOrCreateTag();
                if (tag.hasUUID("TargetTeam")) {
                    if (machine instanceof BatteryBufferMachine) {
                        player.sendSystemMessage(
                                Component.literal("Danger: High Risk of Voiding. Connection terminated.")
                                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                        serverLevel.playSound(null, clickedPos, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5f,
                                1.2f);
                        return InteractionResult.FAIL;
                    }

                    UUID teamUUID = tag.getUUID("TargetTeam");
                    TeslaTeamEnergyData data = TeslaTeamEnergyData.get(serverLevel);

                    boolean isNowLinked = data.toggleSoulLink(teamUUID, level, clickedPos);

                    if (isNowLinked) {
                        player.sendSystemMessage(
                                Component.literal("Core Synchronized: Machine linked to soul frequency.")
                                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
                        serverLevel.playSound(null, clickedPos, SoundEvents.BEACON_POWER_SELECT,
                                SoundSource.PLAYERS, 1f, 1.5f);
                    } else {
                        if (tag.contains("MachineData")) {
                            ListTag machineList = tag.getList("MachineData", Tag.TAG_COMPOUND);
                            long targetPosLong = clickedPos.asLong();

                            for (int i = 0; i < machineList.size(); i++) {
                                if (machineList.getCompound(i).getLong("pos") == targetPosLong) {
                                    machineList.remove(i);
                                    break;
                                }
                            }
                        }

                        player.sendSystemMessage(
                                Component.literal("Connection Severed: Machine removed from soul network.")
                                        .withStyle(ChatFormatting.GRAY));
                        serverLevel.playSound(null, clickedPos, SoundEvents.BEACON_DEACTIVATE,
                                SoundSource.PLAYERS, 1f, 0.8f);
                    }
                    return InteractionResult.SUCCESS;

                } else {

                    player.sendSystemMessage(
                            Component.literal("Binder is not initialized. Shift-Right Click the air first.")
                                    .withStyle(ChatFormatting.RED));
                    return InteractionResult.FAIL;
                }
            } else {
                player.sendSystemMessage(Component.literal("Invalid Target: Machine has no internal soul-buffer.")
                        .withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        if (player.isShiftKeyDown()) {
            if (!context.getLevel().isClientSide) {
                bindToPlayer(player, context.getItemInHand());
                context.getLevel().playSound(null, context.getClickedPos(),
                        SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6f, 1.2f);
            }
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }

        return InteractionResult.PASS;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player,
                                                           @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                bindToPlayer(player, stack);
                level.playSound(null, player.blockPosition(),
                        SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6f, 1.2f);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        if (!level.isClientSide) {
            CompoundTag tag = stack.getOrCreateTag();
            if (tag.hasUUID("TargetTeam")) {
                level.playSound(null, player.blockPosition(),
                        SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 0.4f, 1.5f);
            }
        }

        return super.use(level, player, hand);
    }

    private void bindToPlayer(Player player, ItemStack stack) {
        UUID resolvedId = TeamUtils.getTeamIdOrPlayerFallback(player.getUUID());
        var tag = stack.getOrCreateTag();

        String teamName = TeamUtils.getTeamName(resolvedId);

        tag.putUUID("TargetTeam", resolvedId);
        tag.putString("TeamName", teamName);

        tag.putString("OwnerName", player.getName().getString());

        if (player.level() instanceof ServerLevel server) {

            TeslaTeamEnergyData.get(server).getOrCreate(resolvedId);

            server.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.1, player.getZ(), 20, 0.2, 0.2,
                    0.2, 0.02);
            player.playSound(SoundEvents.PLAYER_LEVELUP, 0.5f, 1.5f);

            player.sendSystemMessage(Component.literal("Tesla frequency linked to: ")
                    .withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(teamName).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)));
        }
    }

    @Override
    public @NotNull Component getName(ItemStack stack) {
        if (stack.hasTag()) {
            assert stack.getTag() != null;
            if (stack.getTag().contains("TargetTeam")) {
                return Component.literal("Tesla Binder (Bound)").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC);
            }
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip,
                                @NotNull TooltipFlag flag) {
        if (stack.hasTag() && Objects.requireNonNull(stack.getTag()).contains("OwnerName")) {
            int color = getAnimatedColor(0xA330FF, 0xFF66CC, 2000);

            tooltip.add(Component.literal("Right-Click to open the Tesla Network UI.")
                    .withStyle(Style.EMPTY.withColor(color)));

            tooltip.add(Component.literal("Bound to Player: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(stack.getTag().getString("OwnerName"))
                            .withStyle(Style.EMPTY.withColor(color))));

            if (stack.getTag().contains("TeamName")) {
                tooltip.add(Component.literal("Bound to Team: ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(stack.getTag().getString("TeamName"))
                                .withStyle(Style.EMPTY.withColor(color))));
            }
        } else {
            tooltip.add(Component.literal("Currently not bound to a soul.")
                    .withStyle(Style.EMPTY.withColor(0x8F00FF)));
            tooltip.add(Component.literal("Shift + Right-Click to bind to your frequency.")
                    .withStyle(Style.EMPTY.withColor(0xA330FF)));
        }
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
    public boolean shouldOpenUI() {
        return true;
    }

    @Override
    public ModularPanel<?> buildUI(PlayerInventoryGuiData<?> guiData, PanelSyncManager panelSyncManager,
                                   UISettings settings) {
        Player player = guiData.getPlayer();
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);

        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains("FilterMode")) tag.putInt("FilterMode", FILTER_ALL);
        if (!tag.contains("SelectedHardwareKey")) tag.putString("SelectedHardwareKey", "");

        IntSyncValue filterValue = new IntSyncValue(
                () -> stack.getOrCreateTag().getInt("FilterMode"),
                (val) -> stack.getOrCreateTag().putInt("FilterMode", val));

        StringSyncValue selectedKey = new StringSyncValue(
                () -> stack.getOrCreateTag().getString("SelectedHardwareKey"),
                (val) -> stack.getOrCreateTag().putString("SelectedHardwareKey", val));

        panelSyncManager.syncValue("tesla_filter_mode", 0, filterValue);
        panelSyncManager.syncValue("tesla_selected_key", 1, selectedKey);

        Flow root = Flow.col()
                .widthRel(1f)
                .heightRel(1f)
                .crossAxisAlignment(Alignment.CrossAxis.START)
                .collapseDisabledChildren();
        root.background(new TeslaBackground());

        root.child(buildMainView(stack, player, filterValue, selectedKey));
        root.child(buildDetailView(stack, player, selectedKey));

        return ModularPanel.defaultPanel("tesla_binder", UI_WIDTH, UI_HEIGHT)
                .margin(6)
                .child(root);
    }

    private IWidget buildMainView(ItemStack stack, Player player, IntSyncValue filterValue,
                                  StringSyncValue selectedKey) {
        Flow mainView = Flow.col()
                .widthRel(1f)
                .heightRel(1f)
                .crossAxisAlignment(Alignment.CrossAxis.START);

        mainView.setEnabledIf(w -> selectedKey.getStringValue().isEmpty());

        TextWidget titleText = new TextWidget(Component.literal("Tesla Network Management")
                .withStyle(ChatFormatting.DARK_PURPLE));
        titleText.margin(0, 0, 4, 0);
        mainView.child(titleText);

        mainView.child(buildHeaderBox(stack));
        mainView.child(buildFilterTabs(filterValue));
        mainView.child(buildHardwareList(stack, player, filterValue, selectedKey));

        return mainView;
    }

    private IWidget buildHeaderBox(ItemStack stack) {
        TextWidget headerText = new TextWidget(() -> {
            CompoundTag tag = stack.getOrCreateTag();
            return Component.literal("Network: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(tag.getString("TeamName")).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal("\nStored: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(compactTeslaValue(tag.getString("StoredEU")) + "EU")
                            .withStyle(ChatFormatting.GOLD))
                    .append(Component.literal("\nCapacity: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(compactTeslaValue(tag.getString("CapacityEU")) + "EU")
                            .withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal("\nNet Input: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("+" + compactTeslaValue(tag.getString("NetInput")) + "EU/t")
                            .withStyle(ChatFormatting.GREEN))
                    .append(Component.literal("\nNet Output: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("-" + compactTeslaValue(tag.getString("NetOutput")) + "EU/t")
                            .withStyle(ChatFormatting.RED));
        });
        headerText.widthRel(1f);
        headerText.height(80);
        headerText.margin(0, 0, 4, 0);
        headerText.background(GTGuiTextures.DISPLAY);
        return headerText;
    }

    private IWidget buildFilterTabs(IntSyncValue filterValue) {
        Flow tabRow = Flow.row()
                .widthRel(1f)
                .height(18)
                .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN)
                .margin(0, 0, 4, 0);

        for (int i = 0; i < FILTER_LABELS.length; i++) {
            final int targetMode = i;
            ButtonWidget<?> tabButton = new ButtonWidget<>();
            TextWidget tabLabel = new TextWidget(() -> Component.literal(FILTER_LABELS[targetMode])
                    .withStyle(filterValue.getIntValue() == targetMode ? ChatFormatting.YELLOW : ChatFormatting.WHITE));
            tabLabel.alignment(Alignment.CENTER);
            tabButton.onMousePressed((context, button) -> {
                filterValue.setIntValue(targetMode);
                return true;
            })
                    .background(GTGuiTextures.BUTTON)
                    .child(tabLabel);
            tabButton.resizer().expanded(true);
            tabRow.child(tabButton.getThis());
        }

        return tabRow;
    }

    private IWidget buildHardwareList(ItemStack stack, Player player, IntSyncValue filterValue,
                                      StringSyncValue selectedKey) {
        Flow listViewport = Flow.col()
                .widthRel(1f)
                .height(165)
                .background(GTGuiTextures.DISPLAY);

        Flow listContent = Flow.col()
                .widthRel(1f)
                .collapseDisabledChildren();

        CompoundTag tag = stack.getOrCreateTag();
        ListTag hatchData = tag.getList("HatchData", Tag.TAG_COMPOUND);
        ListTag machineData = tag.getList("MachineData", Tag.TAG_COMPOUND);
        ListTag chargerData = tag.getList("ChargerData", Tag.TAG_COMPOUND);

        for (int i = 0; i < hatchData.size(); i++) {
            CompoundTag hTag = hatchData.getCompound(i);
            boolean isUplink = hTag.getBoolean("isOut");
            Flow row = buildRow(hTag, player, "hatch:" + i, "hatch", selectedKey);
            row.setEnabledIf(w -> {
                int mode = filterValue.getIntValue();
                return mode == FILTER_ALL || (mode == FILTER_IN && isUplink) || (mode == FILTER_OUT && !isUplink);
            });
            listContent.child(row);
        }
        for (int i = 0; i < machineData.size(); i++) {
            Flow row = buildRow(machineData.getCompound(i), player, "machine:" + i, "machine", selectedKey);
            row.setEnabledIf(w -> {
                int mode = filterValue.getIntValue();
                return mode == FILTER_ALL || mode == FILTER_SOUL;
            });
            listContent.child(row);
        }
        for (int i = 0; i < chargerData.size(); i++) {
            Flow row = buildRow(chargerData.getCompound(i), player, "charger:" + i, "charger", selectedKey);
            row.setEnabledIf(w -> {
                int mode = filterValue.getIntValue();
                return mode == FILTER_ALL || mode == FILTER_CHARGER;
            });
            listContent.child(row);
        }

        listViewport.child(listContent);
        return listViewport;
    }

    private Flow buildRow(CompoundTag data, Player player, String rowKey, String type,
                          StringSyncValue selectedKey) {
        BlockPos pos = BlockPos.of(data.getLong("pos")).immutable();

        String transferRaw = data.getString("transfer");
        long flowVal = 0L;
        try {
            flowVal = Long.parseLong(transferRaw.isEmpty() ? "0" : transferRaw);
        } catch (NumberFormatException ignored) {}

        String typeLabel;
        ChatFormatting color;
        String sign;
        switch (type) {
            case "hatch" -> {
                boolean isUplink = data.getBoolean("isOut");
                typeLabel = isUplink ? "[I]" : "[O]";
                color = isUplink ? ChatFormatting.GREEN : ChatFormatting.RED;
                sign = isUplink ? "+" : "-";
            }
            case "charger" -> {
                typeLabel = "[C]";
                color = ChatFormatting.AQUA;
                sign = "-";
            }
            default -> {
                typeLabel = "[S]";
                if (flowVal < 0) {
                    color = ChatFormatting.GREEN;
                    sign = "+";
                } else {
                    color = ChatFormatting.LIGHT_PURPLE;
                    sign = "-";
                }
            }
        }

        String rawName = data.contains("name") ? data.getString("name") :
                (type.equals("hatch") ? "Tesla Hatch" : (type.equals("charger") ? "Wireless Charger" : "Soul Machine"));
        String displayName = rawName.length() > 20 ? rawName.substring(0, 18) + ".." : rawName;
        final long finalFlowVal = flowVal;
        final ChatFormatting finalColor = color;

        Flow row = Flow.row()
                .widthRel(1f)
                .height(18)
                .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN);

        ButtonWidget<?> rowButton = new ButtonWidget<>();
        TextWidget rowLabel = new TextWidget(Component.literal(typeLabel + " ").withStyle(finalColor)
                .append(Component.literal(displayName).withStyle(ChatFormatting.WHITE))
                .append(Component
                        .literal("  (" + sign + compactTeslaValue(String.valueOf(Math.abs(finalFlowVal))) + "EU)")
                        .withStyle(ChatFormatting.DARK_GRAY)));
        rowLabel.margin(4, 0, 0, 0);
        rowButton.onMousePressed((context, button) -> {
            boolean alreadySelected = rowKey.equals(selectedKey.getStringValue());
            selectedKey.setStringValue(alreadySelected ? "" : rowKey);
            return true;
        })
                .background(GTGuiTextures.BUTTON)
                .child(rowLabel);
        rowButton.resizer().expanded(true);
        row.child(rowButton.getThis());

        ButtonWidget<?> highlightButton = new ButtonWidget<>();
        highlightButton.onMousePressed((context, button) -> {
            if (player.level().isClientSide) {
                TeslaHighlightRenderer.highlight(pos, 200);
            }
            return true;
        })
                .background(GTGuiTextures.TOOL_COVER_SETTINGS)
                .size(18);
        row.child(highlightButton.getThis());

        return row;
    }

    private IWidget buildDetailView(ItemStack stack, Player player, StringSyncValue selectedKey) {
        Flow detailView = Flow.col()
                .widthRel(1f)
                .heightRel(1f)
                .crossAxisAlignment(Alignment.CrossAxis.START);

        detailView.setEnabledIf(w -> !selectedKey.getStringValue().isEmpty());

        Flow backRow = Flow.row().widthRel(1f).height(18).margin(0, 0, 4, 0);
        ButtonWidget<?> backButton = new ButtonWidget<>();
        TextWidget backLabel = new TextWidget(Component.literal("< Back").withStyle(ChatFormatting.GRAY));
        backLabel.alignment(Alignment.CENTER);
        backButton.onMousePressed((context, button) -> {
            selectedKey.setStringValue("");
            return true;
        })
                .background(GTGuiTextures.BUTTON)
                .width(40)
                .height(18)
                .child(backLabel);
        backRow.child(backButton.getThis());

        TextWidget detailTitle = new TextWidget(() -> resolveDetailTitle(stack, selectedKey));
        detailTitle.margin(4, 0, 0, 0);
        backRow.child(detailTitle);
        detailView.child(backRow);

        TextWidget detailBody = new TextWidget(() -> resolveDetailBody(stack, selectedKey));
        detailBody.widthRel(1f);
        detailBody.height(95);
        detailBody.margin(0, 0, 6, 0);
        detailBody.background(GTGuiTextures.DISPLAY);
        detailView.child(detailBody);

        ButtonWidget<?> highlightDeviceButton = new ButtonWidget<>();
        TextWidget highlightDeviceLabel = new TextWidget(Component.literal("Highlight Device")
                .withStyle(ChatFormatting.AQUA));
        highlightDeviceLabel.alignment(Alignment.CENTER);
        highlightDeviceButton.onMousePressed((context, button) -> {
            if (player.level().isClientSide) {
                CompoundTag hTag = resolveSelected(stack, selectedKey);
                if (hTag != null) {
                    TeslaHighlightRenderer.highlight(BlockPos.of(hTag.getLong("pos")), 200);
                }
            }
            return true;
        })
                .background(GTGuiTextures.BUTTON)
                .widthRel(1f)
                .height(18)
                .child(highlightDeviceLabel);
        detailView.child(highlightDeviceButton.getThis());

        return detailView;
    }

    private CompoundTag resolveSelected(ItemStack stack, StringSyncValue selectedKey) {
        String key = selectedKey.getStringValue();
        if (key.isEmpty()) return null;

        String[] parts = key.split(":", 2);
        if (parts.length != 2) return null;
        String type = parts[0];
        int index;
        try {
            index = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }

        CompoundTag tag = stack.getOrCreateTag();
        ListTag list = switch (type) {
            case "hatch" -> tag.getList("HatchData", Tag.TAG_COMPOUND);
            case "machine" -> tag.getList("MachineData", Tag.TAG_COMPOUND);
            case "charger" -> tag.getList("ChargerData", Tag.TAG_COMPOUND);
            default -> new ListTag();
        };
        if (index < 0 || index >= list.size()) return null;
        return list.getCompound(index);
    }

    private Component resolveDetailTitle(ItemStack stack, StringSyncValue selectedKey) {
        CompoundTag hTag = resolveSelected(stack, selectedKey);
        if (hTag == null) return Component.literal("");

        String key = selectedKey.getStringValue();
        boolean isHatch = key.startsWith("hatch:");
        boolean isCharger = key.startsWith("charger:");

        String hardwareTitle;
        ChatFormatting titleColor;
        if (isCharger) {
            hardwareTitle = "Wireless Charger";
            titleColor = ChatFormatting.AQUA;
        } else if (isHatch) {
            boolean isUplink = hTag.getBoolean("isOut");
            hardwareTitle = isUplink ? "Tesla Uplink" : "Tesla Downlink";
            titleColor = isUplink ? ChatFormatting.GREEN : ChatFormatting.RED;
        } else {
            hardwareTitle = hTag.contains("name") ? hTag.getString("name") : "Soul Consumer";
            titleColor = ChatFormatting.LIGHT_PURPLE;
        }

        return Component.literal(hardwareTitle + " Details").withStyle(titleColor);
    }

    private Component resolveDetailBody(ItemStack stack, StringSyncValue selectedKey) {
        CompoundTag hTag = resolveSelected(stack, selectedKey);
        if (hTag == null) return Component.literal("");

        String key = selectedKey.getStringValue();
        boolean isHatch = key.startsWith("hatch:");
        boolean isCharger = key.startsWith("charger:");

        ChatFormatting titleColor;
        String hardwareTitle;
        if (isCharger) {
            hardwareTitle = "Wireless Charger";
            titleColor = ChatFormatting.AQUA;
        } else if (isHatch) {
            boolean isUplink = hTag.getBoolean("isOut");
            hardwareTitle = isUplink ? "Tesla Uplink" : "Tesla Downlink";
            titleColor = isUplink ? ChatFormatting.GREEN : ChatFormatting.RED;
        } else {
            hardwareTitle = hTag.contains("name") ? hTag.getString("name") : "Soul Consumer";
            titleColor = ChatFormatting.LIGHT_PURPLE;
        }

        BlockPos p = BlockPos.of(hTag.getLong("pos"));
        MutableComponent body = Component.literal(hardwareTitle).withStyle(titleColor, ChatFormatting.BOLD)
                .append(Component.literal("\nLocation: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.format("%d, %d, %d", p.getX(), p.getY(), p.getZ()))
                        .withStyle(ChatFormatting.WHITE));

        if (!isCharger && hTag.contains("buf")) {
            body.append(Component.literal("\nInternal Buffer: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(compactTeslaValue(hTag.getString("buf")) + "EU")
                            .withStyle(ChatFormatting.GOLD));
        }

        String transferStr = hTag.getString("transfer");
        long flowVal = 0L;
        try {
            flowVal = Long.parseLong(transferStr.isEmpty() ? "0" : transferStr);
        } catch (NumberFormatException ignored) {}

        String sign;
        if (isCharger) {
            sign = "-";
        } else if (isHatch) {
            sign = hTag.getBoolean("isOut") ? "+" : "-";
        } else {
            sign = (flowVal < 0) ? "+" : (flowVal > 0 ? "-" : "");
        }

        String flowLabel = isCharger ? "Wireless Output: " : "Current Flow: ";
        body.append(Component.literal("\n" + flowLabel).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(sign + compactTeslaValue(String.valueOf(Math.abs(flowVal))) + "EU/t")
                        .withStyle(flowVal != 0 ? titleColor : ChatFormatting.WHITE));

        String statusText = (flowVal != 0) ? "OPERATIONAL" : "IDLE / NO LOAD";
        body.append(Component.literal("\nStatus: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(statusText).withStyle(flowVal != 0 ? titleColor : ChatFormatting.DARK_GRAY));

        return body;
    }

    private String compactTeslaValue(String value) {
        try {
            if (value == null || value.isEmpty()) return "0 ";
            BigInteger n = new BigInteger(value);
            if (n.equals(BigInteger.ZERO)) return "0 ";

            boolean negative = n.signum() == -1;
            if (negative) n = n.abs();

            BigInteger thousand = BigInteger.valueOf(1000);
            if (n.compareTo(thousand) < 0) {
                return (negative ? "-" : "") + n + " ";
            }

            return (negative ? "-" : "") + formatWithSuffix(n);
        } catch (Exception e) {
            return "0 ";
        }
    }

    private static String formatWithSuffix(BigInteger n) {
        String[] suffixes = { " ", " k", " M", " G", " T", " P", " Ei", " Z", " Y" };
        int tier = 0;

        BigDecimal dN = new BigDecimal(n);
        BigDecimal dThousand = new BigDecimal(1000);

        while (dN.compareTo(dThousand) >= 0 && tier < suffixes.length - 1) {
            dN = dN.divide(dThousand, 2, RoundingMode.HALF_UP);
            tier++;
        }

        return dN.compareTo(BigDecimal.valueOf(100)) >= 0 ? String.format("%.0f%s", dN, suffixes[tier]) :
                String.format("%.1f%s", dN, suffixes[tier]);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, Level level, @NotNull Entity entity, int slotId,
                              boolean isSelected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer serverPlayer)) return;
        if (level.getGameTime() % 5 != 0) return;

        boolean isUIOpen = serverPlayer.containerMenu instanceof com.lowdragmc.lowdraglib.gui.modular.ModularUIContainer;
        if (!isSelected && !isUIOpen) return;

        CompoundTag tag = stack.getOrCreateTag();
        if (tag.hasUUID("TargetTeam")) {
            UUID teamUUID = tag.getUUID("TargetTeam");
            ServerLevel overworld = serverPlayer.server.getLevel(Level.OVERWORLD);
            if (overworld == null) return;

            TeslaTeamEnergyData globalData = TeslaTeamEnergyData.get(overworld);
            TeslaTeamEnergyData.TeamEnergy team = globalData.getOrCreate(teamUUID);

            tag.putString("StoredEU", team.stored != null ? team.stored.toString() : "0");
            tag.putString("CapacityEU", team.capacity != null ? team.capacity.toString() : "0");
            tag.putString("TeamName",
                    TeamUtils.getTeamName(teamUUID) != null ? TeamUtils.getTeamName(teamUUID) : "Unknown Team");
            tag.putString("NetInput", String.valueOf(team.lastNetInput));
            tag.putString("NetOutput", String.valueOf(team.lastNetOutput));

            ListTag hatchList = new ListTag();
            for (TeslaTeamEnergyData.HatchInfo hatch : globalData.getHatches(teamUUID)) {
                if (hatch == null || hatch.isSoulLinked) continue;
                CompoundTag hTag = new CompoundTag();
                hTag.putLong("pos", hatch.pos.asLong());
                hTag.putBoolean("isOut", hatch.isPhysicalOutput);
                hTag.putString("dim", hatch.dimension.location().toString());
                hTag.putString("buf", hatch.buffered.toString());
                hTag.putString("transfer", String.valueOf(hatch.displayFlow));
                hatchList.add(hTag);
            }
            tag.put("HatchData", hatchList);

            ListTag machineList = new ListTag();
            for (BlockPos mPos : team.soulLinkedMachines) {
                if (mPos == null) continue;
                ResourceKey<Level> mDim = team.getMachineDimension(mPos);
                ServerLevel mLevel = serverPlayer.server.getLevel(mDim);

                CompoundTag mTag = new CompoundTag();
                mTag.putLong("pos", mPos.asLong());
                mTag.putString("dim", mDim != null ? mDim.location().toString() : "minecraft:overworld");

                if (mLevel != null && mLevel.isLoaded(mPos)) {
                    MetaMachine machine = MetaMachine.getMachine(mLevel, mPos);
                    if (machine != null) {
                        String langVal = machine.getDefinition().getLangValue();
                        mTag.putString("name", langVal != null ? langVal : "Soul Consumer");
                        if (machine instanceof TieredEnergyMachine tiered && tiered.energyContainer != null) {
                            mTag.putString("buf", String.valueOf(tiered.energyContainer.getEnergyStored()));
                        } else {
                            mTag.putString("buf", "0");
                        }
                    } else {
                        mTag.putString("name", "§c[Missing Machine]");
                        mTag.putString("buf", "0");
                    }
                } else {
                    mTag.putString("name", "§8[Unloaded]");
                    mTag.putString("buf", "0");
                }
                mTag.putString("transfer", String.valueOf(team.machineDisplayFlow.getOrDefault(mPos, 0L)));
                machineList.add(mTag);
            }
            tag.put("MachineData", machineList);

            ListTag chargerList = new ListTag();
            for (BlockPos cPos : team.activeChargers) {
                if (cPos == null) continue;
                CompoundTag cTag = new CompoundTag();
                cTag.putLong("pos", cPos.asLong());
                cTag.putBoolean("isCharger", true);

                MetaMachine machine = MetaMachine.getMachine(level, cPos);
                if (machine == null) machine = MetaMachine.getMachine(overworld, cPos);

                if (machine instanceof TeslaWirelessBatteryBufferMachine) {
                    String langVal = machine.getDefinition().getLangValue();
                    cTag.putString("name", langVal != null ? langVal : "Wireless Charger");
                    cTag.putString("transfer", String.valueOf(team.machineDisplayFlow.getOrDefault(cPos, 0L)));
                    chargerList.add(cTag);
                }
            }
            tag.put("ChargerData", chargerList);

            serverPlayer.containerMenu.broadcastChanges();
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public boolean hurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity target, @NotNull LivingEntity attacker) {
        target.hurt(target.damageSources().playerAttack((Player) attacker), 16.0f);

        target.level().playSound(null, target.blockPosition(),
                SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.5f, 2.0f);

        return true;
    }
}
