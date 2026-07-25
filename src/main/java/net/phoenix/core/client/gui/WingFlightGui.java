package net.phoenix.core.client.gui;

import com.gregtechceu.gtceu.api.mui.IItemUIHolder;
import com.gregtechceu.gtceu.common.mui.GTGuiTextures;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.common.data.item.PhoenixArmorItem;

import brachy.modularui.api.drawable.IDrawable;
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

public class WingFlightGui implements IItemUIHolder {

    public static final String NBT_MODE = "FlightMode";
    public static final String NBT_SPEED = "FlightSpeed";
    public static final String NBT_DRIFT = "FlightDrift";

    public static final String MODE_POWERED = "powered";
    public static final String MODE_CREATIVE = "creative";

    private static final int STEPS = 10;

    @Override
    public ModularPanel<?> buildUI(PlayerInventoryGuiData<?> guiData, PanelSyncManager panelSyncManager,
                                   UISettings settings) {
        Player player = guiData.getPlayer();
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (!(stack.getItem() instanceof PhoenixArmorItem)) {
            return ModularPanel.defaultPanel("empty", 100, 100);
        }

        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(NBT_MODE)) tag.putString(NBT_MODE, MODE_POWERED);
        if (!tag.contains(NBT_SPEED)) tag.putInt(NBT_SPEED, 5);
        if (!tag.contains(NBT_DRIFT)) tag.putInt(NBT_DRIFT, 5);

        StringSyncValue modeValue = new StringSyncValue(
                () -> stack.getOrCreateTag().getString(NBT_MODE),
                (val) -> stack.getOrCreateTag().putString(NBT_MODE, val));
        IntSyncValue speedValue = new IntSyncValue(
                () -> stack.getOrCreateTag().getInt(NBT_SPEED),
                (val) -> stack.getOrCreateTag().putInt(NBT_SPEED, val));
        IntSyncValue driftValue = new IntSyncValue(
                () -> stack.getOrCreateTag().getInt(NBT_DRIFT),
                (val) -> stack.getOrCreateTag().putInt(NBT_DRIFT, val));

        panelSyncManager.syncValue("flight_mode", 0, modeValue);
        panelSyncManager.syncValue("flight_speed", 1, speedValue);
        panelSyncManager.syncValue("flight_drift", 2, driftValue);

        Flow layoutSpacer = Flow.row();
        layoutSpacer.resizer().expanded(true);

        TextWidget headerText = new TextWidget(
                Component.literal("Wing Flight Control").withStyle(ChatFormatting.DARK_PURPLE));
        headerText.margin(2, 0, 4, 0);

        TextWidget infoStatusBox = new TextWidget(() -> {
            boolean isPowered = MODE_POWERED.equals(modeValue.getStringValue());
            String modeText = isPowered ? "⚡ Powered" : "✦ Creative";
            ChatFormatting modeColor = isPowered ? ChatFormatting.GOLD : ChatFormatting.LIGHT_PURPLE;
            String drainText = isPowered ? "Drain: 5,000 EU/t" : "Drain: None (Creative)";

            return Component.literal("Mode: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(modeText).withStyle(modeColor))
                    .append(Component.literal("\n" + drainText)
                            .withStyle(isPowered ? ChatFormatting.RED : ChatFormatting.GREEN));
        });
        infoStatusBox.widthRel(1f).height(32).margin(0, 0, 6, 0);

        TextWidget speedHeader = new TextWidget(Component.literal("Flight Speed").withStyle(ChatFormatting.GRAY));
        speedHeader.margin(0, 0, 2, 0);

        TextWidget driftHeader = new TextWidget(Component.literal("Flight Drift").withStyle(ChatFormatting.GRAY));
        driftHeader.margin(6, 0, 2, 0);

        return ModularPanel.defaultPanel("wing_flight_control", 200, 240)
                .margin(6)
                .child(Flow.col()
                        .widthRel(1f)
                        .heightRel(1f)
                        .crossAxisAlignment(Alignment.CrossAxis.START)

                        .child(headerText)

                        .child(infoStatusBox)

                        .child(new ButtonWidget<>()
                                .onMousePressed((context, button) -> {
                                    modeValue.setStringValue(MODE_POWERED.equals(modeValue.getStringValue()) ?
                                            MODE_CREATIVE : MODE_POWERED);
                                    return true;
                                })
                                .background(new IDrawable[] { GTGuiTextures.BUTTON })
                                .widthRel(1f)
                                .height(20)
                                .margin(0, 0, 8, 0)
                                .child(new TextWidget(() -> MODE_POWERED.equals(modeValue.getStringValue()) ?
                                        Component.literal("Switch to ✦ Creative Flight") :
                                        Component.literal("Switch to ⚡ Powered Flight"))
                                        .alignment(Alignment.CENTER))
                                .getThis())

                        .child(speedHeader)
                        .child(buildStepControl(speedValue, "Speed"))

                        .child(driftHeader)
                        .child(buildStepControl(driftValue, "Drift"))

                        .child(layoutSpacer)

                        .child(new TextWidget(Component.literal("Press Numpad9 or Esc to close")
                                .withStyle(ChatFormatting.DARK_GRAY))));
    }

    private static Flow buildStepControl(IntSyncValue valueTracker, String label) {
        Flow controlRow = Flow.row()
                .widthRel(1f)
                .height(18)
                .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN)
                .crossAxisAlignment(Alignment.CrossAxis.CENTER);

        controlRow.child(new ButtonWidget<>()
                .onMousePressed((context, button) -> {
                    valueTracker.setIntValue(Math.max(0, valueTracker.getIntValue() - 1));
                    return true;
                })
                .background(new IDrawable[] { GTGuiTextures.BUTTON })
                .size(18)
                .child(new TextWidget(Component.literal("-")).alignment(Alignment.CENTER))
                .getThis());

        Flow segmentsGroup = Flow.row();
        segmentsGroup.resizer().expanded(true);
        segmentsGroup.margin(4, 0)
                .crossAxisAlignment(Alignment.CrossAxis.CENTER)
                .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN);

        for (int i = 0; i < STEPS; i++) {
            final int targetValue = i + 1;

            ButtonWidget<?> stepSegmentButton = new ButtonWidget<>();
            stepSegmentButton.onMousePressed((context, button) -> {
                valueTracker.setIntValue(targetValue);
                return true;
            })
                    .background(new IDrawable[] {
                            valueTracker.getIntValue() >= targetValue ? GTGuiTextures.BUTTON : GTGuiTextures.SLOT })
                    .margin(0, 1, 0, 0);

            stepSegmentButton.resizer().expanded(true);

            segmentsGroup.child(stepSegmentButton.getThis());
        }
        controlRow.child(segmentsGroup);

        controlRow.child(new ButtonWidget<>()
                .onMousePressed((context, button) -> {
                    valueTracker.setIntValue(Math.min(STEPS, valueTracker.getIntValue() + 1));
                    return true;
                })
                .background(new IDrawable[] { GTGuiTextures.BUTTON })
                .size(18)
                .child(new TextWidget(Component.literal("+")).alignment(Alignment.CENTER))
                .getThis());

        TextWidget footerText = new TextWidget(() -> Component.literal(
                String.format("%s: %d / %d", label, valueTracker.getIntValue(), STEPS))
                .withStyle(ChatFormatting.DARK_GRAY));
        footerText.height(12).margin(2, 0, 0, 0);

        return Flow.col()
                .widthRel(1f)
                .child(controlRow)
                .child(footerText);
    }
}
