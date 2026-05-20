package net.phoenix.core.common.data.item;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.item.ComponentItem;
import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.api.item.component.IInteractionItem;
import com.gregtechceu.gtceu.api.item.component.IItemUIFactory;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TieredEnergyMachine;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import com.gregtechceu.gtceu.common.machine.electric.BatteryBufferMachine;

import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.*;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
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
import net.phoenix.core.integration.phoenix_tesla_network.client.renderer.machine.TeslaHighlightRenderer;
import net.phoenix.core.integration.phoenix_tesla_network.common.machine.singleblock.electric.TeslaWirelessChargerMachine;
import net.phoenix.core.integration.phoenix_tesla_network.saveddata.TeslaTeamEnergyData;
import net.phoenix.core.utils.TeamUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Consumer;

import static net.phoenix.core.api.gui.PhoenixGuiTextures.TESLA_BACKGROUND;

public class TeslaBinderItem extends ComponentItem
                             implements IItemUIFactory, IInteractionItem, IAddInformation {

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

    // Handles the linking of the player/team data onto the Telsa Binder.
    private void bindToPlayer(Player player, ItemStack stack) {
        // 1. Resolve the correct ID (Party ID or Player ID)
        UUID resolvedId = TeamUtils.getTeamIdOrPlayerFallback(player.getUUID());
        var tag = stack.getOrCreateTag();

        // 2. Get the actual name of the team (e.g., "Phoenix" or "PlayerA")
        String teamName = TeamUtils.getTeamName(resolvedId);

        tag.putUUID("TargetTeam", resolvedId);
        tag.putString("TeamName", teamName);
        // Keeping track of who last synchronized the binder
        tag.putString("OwnerName", player.getName().getString());

        if (player.level() instanceof ServerLevel server) {
            // 3. Initialize the shared data entry if it doesn't exist
            TeslaTeamEnergyData.get(server).getOrCreate(resolvedId);

            // Visual/Audio feedback
            server.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.1, player.getZ(), 20, 0.2, 0.2,
                    0.2, 0.02);
            player.playSound(SoundEvents.PLAYER_LEVELUP, 0.5f, 1.5f);

            // 4. Clearer messaging so players know they are sharing a network
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

    // Handles the naming of linked binders, and the name/team name fields on the tooltip.
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

    // The main ui of the binder.
    @Override
    public ModularUI createUI(HeldItemUIFactory.HeldItemHolder holder, Player player) {
        ItemStack stack = holder.getHeld();
        CompoundTag tag = stack.getOrCreateTag();

        if (!tag.contains("FilterMode")) tag.putInt("FilterMode", 0);
        int filterMode = tag.getInt("FilterMode");

        int windowWidth = 260;
        int windowHeight = 300;

        ModularUI ui = new ModularUI(windowWidth, windowHeight, holder, player).background(GuiTextures.BACKGROUND);
        WidgetGroup mainLayer = new WidgetGroup(0, 0, windowWidth, windowHeight);
        WidgetGroup detailLayer = new WidgetGroup(0, 0, windowWidth, windowHeight);
        detailLayer.setVisible(false);

        Consumer<CompoundTag> openDetail = (hTag) -> {
            detailLayer.clearAllWidgets();
            detailLayer.addWidget(new ButtonWidget(5, 5, 18, 18, GuiTextures.BUTTON_LEFT, c -> {
                detailLayer.setVisible(false);
                mainLayer.setVisible(true);
            }));

            boolean isHatch = hTag.contains("isOut");
            boolean isCharger = hTag.getBoolean("isCharger");

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

            detailLayer
                    .addWidget(new LabelWidget(28, 9, hardwareTitle + " Details").setTextColor(titleColor.getColor()));

            WidgetGroup displayBox = new WidgetGroup(5, 30, windowWidth - 10, 95);
            displayBox.setBackground(GuiTextures.DISPLAY);
            displayBox.addWidget(new ComponentPanelWidget(8, 8, list -> {
                list.add(Component.literal(hardwareTitle).withStyle(titleColor, ChatFormatting.BOLD));

                BlockPos p = BlockPos.of(hTag.getLong("pos"));
                list.add(Component.literal("Location: ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(String.format("%d, %d, %d", p.getX(), p.getY(), p.getZ()))
                                .withStyle(ChatFormatting.WHITE)));

                if (!isCharger && hTag.contains("buf")) {

                    list.add(Component.literal("Internal Buffer: ").withStyle(ChatFormatting.GRAY)
                            .append(Component.literal(compactTeslaValue(hTag.getString("buf")) + "EU")
                                    .withStyle(ChatFormatting.GOLD)));
                }

                String transferStr = hTag.getString("transfer");
                long flowVal = Long.parseLong(transferStr.isEmpty() ? "0" : transferStr);

                String sign;
                if (isCharger) {
                    sign = "-";
                } else if (isHatch) {
                    sign = hTag.getBoolean("isOut") ? "+" : "-";
                } else {
                    sign = (flowVal < 0) ? "+" : (flowVal > 0 ? "-" : "");
                }

                String flowLabel = isCharger ? "Wireless Output: " : "Current Flow: ";

                list.add(Component.literal(flowLabel).withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(sign + compactTeslaValue(String.valueOf(Math.abs(flowVal))) + "EU/t")
                                .withStyle(flowVal != 0 ? titleColor : ChatFormatting.WHITE)));

                String statusText = (flowVal != 0) ? "OPERATIONAL" : "IDLE / NO LOAD";
                list.add(Component.literal("Status: ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(statusText)
                                .withStyle(flowVal != 0 ? titleColor : ChatFormatting.DARK_GRAY)));
            }));
            detailLayer.addWidget(displayBox);

            int btnY = 130;
            detailLayer.addWidget(new ButtonWidget(5, btnY, windowWidth - 10, 18, GuiTextures.BUTTON, c -> {
                if (player.level().isClientSide)
                    TeslaHighlightRenderer.highlight(BlockPos.of(hTag.getLong("pos")), 200);
            }));
            detailLayer.addWidget(new LabelWidget(windowWidth / 2 - 45, btnY + 5, "§bHighlight Device"));

            mainLayer.setVisible(false);
            detailLayer.setVisible(true);
        };

        mainLayer.addWidget(new LabelWidget(8, 6, "Tesla Network Management").setTextColor(0xB000FF));

        WidgetGroup header = new WidgetGroup(5, 18, windowWidth - 10, 80);
        header.setBackground(TESLA_BACKGROUND);
        header.addWidget(new ComponentPanelWidget(5, 4, text -> {
            text.add(Component.literal("Network: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(tag.getString("TeamName")).withStyle(ChatFormatting.AQUA)));

            text.add(Component.literal("Stored: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(compactTeslaValue(tag.getString("StoredEU")) + "EU")
                            .withStyle(ChatFormatting.GOLD)));

            text.add(Component.literal("Capacity: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(compactTeslaValue(tag.getString("CapacityEU")) + "EU")
                            .withStyle(ChatFormatting.YELLOW)));

            text.add(Component.literal("Net Input: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("+" + compactTeslaValue(tag.getString("NetInput")) + "EU/t")
                            .withStyle(ChatFormatting.GREEN)));

            text.add(Component.literal("Net Output: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("-" + compactTeslaValue(tag.getString("NetOutput")) + "EU/t")
                            .withStyle(ChatFormatting.RED)));
        }));
        mainLayer.addWidget(header);

        String[] filters = { "ALL", "[I]", "[O]", "[S]", "[C]" };
        int btnWidth = 48;
        for (int i = 0; i < filters.length; i++) {
            int targetMode = i;
            mainLayer.addWidget(new ButtonWidget(7 + (i * (btnWidth + 2)), 102, btnWidth, 18, GuiTextures.BUTTON, c -> {
                stack.getOrCreateTag().putInt("FilterMode", targetMode);
                if (player instanceof ServerPlayer sp) HeldItemUIFactory.INSTANCE.openUI(holder, sp);
            }));
            LabelWidget fl = new LabelWidget(7 + (i * (btnWidth + 2)) + (btnWidth / 2 - 8), 106, filters[i]);
            if (filterMode == targetMode) fl.setTextColor(ChatFormatting.YELLOW.getColor());
            mainLayer.addWidget(fl);
        }

        DraggableScrollableWidgetGroup listGroup = new DraggableScrollableWidgetGroup(7, 125, windowWidth - 14, 165);
        listGroup.setBackground(GuiTextures.DISPLAY);
        mainLayer.addWidget(listGroup);

        ListTag hatchData = tag.getList("HatchData", Tag.TAG_COMPOUND);
        ListTag machineData = tag.getList("MachineData", Tag.TAG_COMPOUND);
        ListTag chargerData = tag.getList("ChargerData", Tag.TAG_COMPOUND);
        int currentY = 0;

        for (int i = 0; i < hatchData.size(); i++) {
            CompoundTag hTag = hatchData.getCompound(i);
            boolean isInput = hTag.getBoolean("isOut");
            if (filterMode == 0 || (filterMode == 1 && isInput) || (filterMode == 2 && !isInput)) {
                currentY = addListRow(listGroup, hTag, player, openDetail, currentY, "hatch", windowWidth);
            }
        }
        if (filterMode == 0 || filterMode == 3) {
            for (int i = 0; i < machineData.size(); i++) {
                currentY = addListRow(listGroup, machineData.getCompound(i), player, openDetail, currentY, "machine",
                        windowWidth);
            }
        }
        if (filterMode == 0 || filterMode == 4) {
            for (int i = 0; i < chargerData.size(); i++) {
                currentY = addListRow(listGroup, chargerData.getCompound(i), player, openDetail, currentY, "charger",
                        windowWidth);
            }
        }

        ui.widget(mainLayer);
        ui.widget(detailLayer);
        return ui;
    }

    private int addListRow(WidgetGroup group, CompoundTag data, Player player, Consumer<CompoundTag> clickAction, int y,
                           String type, int windowWidth) {
        BlockPos pos = BlockPos.of(data.getLong("pos")).immutable();

        String typeLabel;
        String colorCode;
        String sign;

        // 1. Parse the flow value to determine if it's a generator or consumer
        String transferRaw = data.getString("transfer");
        long flowVal = 0;
        try {
            flowVal = Long.parseLong(transferRaw.isEmpty() ? "0" : transferRaw);
        } catch (NumberFormatException ignored) {}

        switch (type) {
            case "hatch" -> {
                boolean isInput = data.getBoolean("isOut"); // [I] is Uplink, [O] is Downlink
                typeLabel = isInput ? "[I]" : "[O]";
                colorCode = isInput ? "§a" : "§c";
                sign = isInput ? "+" : "-";
            }
            case "charger" -> {
                typeLabel = "[C]";
                colorCode = "§b";
                sign = "-";
            }
            default -> { // Soul-linked machines
                typeLabel = "[S]";
                if (flowVal < 0) {
                    colorCode = "§a";
                    sign = "+";
                } else {
                    colorCode = "§d";
                    sign = "-";
                }
            }
        }

        String statusIcon;
        boolean hasFlow = flowVal != 0;

        if (type.equals("charger")) {
            statusIcon = hasFlow ? "§3波" : "§7波";
        } else if (!hasFlow) {
            statusIcon = "§7△";
        } else if (type.equals("hatch")) {
            statusIcon = data.getBoolean("isOut") ? "§2▲" : "§4▼";
        } else {
            statusIcon = "§b⚡";
        }

        String rawName = data.contains("name") ? data.getString("name") :
                (type.equals("hatch") ? "Tesla Hatch" : (type.equals("charger") ? "Wireless Charger" : "Soul Machine"));

        String flowStr = " §8(" + colorCode + sign + compactTeslaValue(String.valueOf(Math.abs(flowVal))) + "EU§8)";

        String displayName = rawName;
        if (displayName.length() > 20) {
            displayName = displayName.substring(0, 18) + "..";
        }

        String finalRowText = String.format("%s %s%s§r %s%s", statusIcon, colorCode, typeLabel, displayName, flowStr);

        int rowWidth = windowWidth - 20;
        WidgetGroup row = new WidgetGroup(0, y, rowWidth, 18);

        row.addWidget(new ButtonWidget(0, 0, rowWidth - 22, 18, GuiTextures.BUTTON, c -> clickAction.accept(data)));
        row.addWidget(new LabelWidget(4, 5, finalRowText));
        row.addWidget(new LabelWidget(rowWidth - 68, 5, getDistanceString(pos, player, data)));

        int gearX = rowWidth - 18;
        row.addWidget(new ButtonWidget(gearX, 0, 18, 18, GuiTextures.BUTTON, c -> {
            if (player.level().isClientSide) TeslaHighlightRenderer.highlight(pos, 200);
        }));
        row.addWidget(new ImageWidget(gearX + 1, 1, 16, 16, GuiTextures.IO_CONFIG_COVER_SETTINGS));

        group.addWidget(row);
        return y + 19;
    }

    private String getDistanceString(BlockPos pos, Player player, CompoundTag data) {
        if (data.contains("dim") && !player.level().dimension().location().toString().equals(data.getString("dim"))) {
            String dim = data.getString("dim");
            return "§8" + (dim.contains(":") ? dim.split(":")[1] : dim);
        }
        return "§7" + (int) Math.sqrt(player.blockPosition().distSqr(pos)) + "m";
    }

    private String compactTeslaValue(String value) {
        try {
            if (value == null || value.isEmpty()) return "0 ";
            java.math.BigInteger n = new java.math.BigInteger(value);

            if (n.equals(java.math.BigInteger.ZERO)) return "0 ";

            boolean negative = n.signum() == -1;
            if (negative) n = n.abs();

            java.math.BigInteger thousand = java.math.BigInteger.valueOf(1000);

            if (n.compareTo(thousand) < 0) {
                return (negative ? "-" : "") + n.toString() + " ";
            }

            String result = getString(n);

            return (negative ? "-" : "") + result;
        } catch (Exception e) {
            return "0 ";
        }
    }

    private static @NotNull String getString(BigInteger n) {
        String[] suffixes = new String[] { " ", " k", " M", " G", " T", " P", " Ei", " Z", " Y" };
        int tier = 0;

        java.math.BigDecimal dN = new java.math.BigDecimal(n);
        java.math.BigDecimal dThousand = new java.math.BigDecimal(1000);

        while (dN.compareTo(dThousand) >= 0 && tier < suffixes.length - 1) {
            dN = dN.divide(dThousand, 2, java.math.RoundingMode.HALF_UP);
            tier++;
        }

        String result;

        if (dN.compareTo(java.math.BigDecimal.valueOf(100)) >= 0) {
            result = String.format("%.0f%s", dN, suffixes[tier]);
        } else {
            result = String.format("%.1f%s", dN, suffixes[tier]);
        }
        return result;
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

            // 1. Sync Hatch Data
            ListTag hatchList = new ListTag();
            for (TeslaTeamEnergyData.HatchInfo hatch : globalData.getHatches(teamUUID)) {
                if (hatch == null || hatch.pos == null || hatch.dimension == null) continue;
                CompoundTag hTag = new CompoundTag();
                hTag.putLong("pos", hatch.pos.asLong());
                hTag.putBoolean("isOut", hatch.isPhysicalOutput);
                hTag.putString("dim", hatch.dimension.location().toString());
                hTag.putString("buf", hatch.buffered != null ? hatch.buffered.toString() : "0");
                hTag.putString("transfer", String.valueOf(team.machineDisplayFlow.getOrDefault(hatch.pos, 0L)));
                hatchList.add(hTag);
            }
            tag.put("HatchData", hatchList);

            // 2. Sync Machine Data (EV Lathes, etc)
            ListTag machineList = new ListTag();
            for (BlockPos mPos : team.soulLinkedMachines) {
                if (mPos == null) continue;
                ResourceKey<Level> mDim = team.posToDimension.getOrDefault(mPos, Level.OVERWORLD);
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

            // 3. Sync Chargers
            ListTag chargerList = new ListTag();
            for (BlockPos cPos : team.activeChargers) {
                if (cPos == null) continue;
                CompoundTag cTag = new CompoundTag();
                cTag.putLong("pos", cPos.asLong());
                cTag.putBoolean("isCharger", true);

                MetaMachine machine = MetaMachine.getMachine(level, cPos);
                if (machine == null) machine = MetaMachine.getMachine(overworld, cPos);

                if (machine instanceof TeslaWirelessChargerMachine) {
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
