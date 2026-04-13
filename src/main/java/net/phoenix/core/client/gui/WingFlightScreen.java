package net.phoenix.core.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.network.packet.UpdateWingSettingsPacket;

@OnlyIn(Dist.CLIENT)
public class WingFlightScreen extends Screen {

    private String flightMode;
    private int flightSpeed;
    private int flightDrift;

    private static final int W = 200;
    private static final int H = 220;
    private static final int STEPS = 10;

    // Colors
    private static final int COLOR_TITLE = 0xBF00FF;
    private static final int COLOR_LABEL = 0xAAAAAA;
    private static final int COLOR_BASIC = 0x00FF88;
    private static final int COLOR_POWERED = 0xFFAA00;
    private static final int COLOR_CREATIVE = 0xFF55FF;
    private static final int COLOR_WINGED = 0x55FFFF;
    private static final int COLOR_FILLED = 0x8800CC;
    private static final int COLOR_EMPTY = 0x333333;
    private static final int COLOR_PANEL_BG = 0xEE050010;
    private static final int COLOR_BORDER = 0x66BF00FF;

    public WingFlightScreen() {
        super(Component.literal("Wing Flight Control"));
        ItemStack chest = Minecraft.getInstance().player.getItemBySlot(EquipmentSlot.CHEST);
        CompoundTag tag = chest.getOrCreateTag();
        this.flightMode = tag.contains("FlightMode") ? tag.getString("FlightMode") : "basic";
        this.flightSpeed = tag.contains("FlightSpeed") ? tag.getInt("FlightSpeed") : 5;
        this.flightDrift = tag.contains("FlightDrift") ? tag.getInt("FlightDrift") : 5;
    }

    @Override
    protected void init() {
        int left = (width - W) / 2;
        int top = (height - H) / 2;
        this.clearWidgets();

        // 1. Mode Button
        addRenderableWidget(Button.builder(Component.literal("Mode: " + getModeDisplayName()), btn -> {
            cycleMode();
            sendUpdate();
            rebuildWidgets();
        }).bounds(left + 5, top + 72, W - 10, 20).build());

        // Visibility Logic
        boolean isCreativeType = flightMode.startsWith("creative");
        boolean showSpeed = isCreativeType || flightMode.equals("powered");
        boolean showDrift = isCreativeType;

        // 2. Speed Controls
        if (showSpeed) {
            createSliderRow(left, top + 118, true);
        }

        // 3. Drift Controls
        if (showDrift) {
            createSliderRow(left, top + 168, false);
        }
    }

    private void createSliderRow(int left, int y, boolean isSpeed) {
        int barLeft = left + 27;
        int barWidth = W - 10 - 44;
        int segW = barWidth / STEPS;

        addRenderableWidget(Button.builder(Component.literal("-"), btn -> {
            if (isSpeed) flightSpeed = Math.max(1, flightSpeed - 1);
            else flightDrift = Math.max(1, flightDrift - 1);
            sendUpdate();
        }).bounds(left + 5, y, 18, 18).build());

        addRenderableWidget(Button.builder(Component.literal("+"), btn -> {
            if (isSpeed) flightSpeed = Math.min(STEPS, flightSpeed + 1);
            else flightDrift = Math.min(STEPS, flightDrift + 1);
            sendUpdate();
        }).bounds(left + W - 23, y, 18, 18).build());

        for (int i = 0; i < STEPS; i++) {
            final int seg = i + 1;
            addRenderableWidget(Button.builder(Component.empty(), btn -> {
                if (isSpeed) flightSpeed = seg;
                else flightDrift = seg;
                sendUpdate();
            }).bounds(barLeft + (i * segW), y, segW - 1, 18).build());
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx);

        boolean isCreativeType = flightMode.startsWith("creative");
        boolean showSpeed = isCreativeType || flightMode.equals("powered");
        boolean showDrift = isCreativeType;

        int currentH = 100;
        if (showSpeed) currentH = 150;
        if (showDrift) currentH = 205;

        int left = (width - W) / 2;
        int top = (height - H) / 2;

        gfx.fill(left, top, left + W, top + currentH, COLOR_PANEL_BG);
        renderBorders(gfx, left, top, W, currentH);

        gfx.drawString(font, "Wing Flight Control", left + 8, top + 6, COLOR_TITLE, false);

        gfx.fill(left + 5, top + 18, left + W - 5, top + 66, 0x55000000);
        gfx.drawString(font, "Current: " + getModeDisplayName(), left + 10, top + 24, getModeColor(), false);
        gfx.drawString(font, getDrainDescription(), left + 10, top + 36, COLOR_LABEL, false);
        gfx.drawString(font, "Press ESC to save", left + 10, top + 48, 0x77AAAAAA, false);

        if (showSpeed) {
            gfx.drawString(font, "Flight Speed", left + 8, top + 104, COLOR_LABEL, false);
            renderSegmentedBar(gfx, left + 27, top + 118, flightSpeed);
        }

        if (showDrift) {
            gfx.drawString(font, "Flight Drift", left + 8, top + 154, COLOR_LABEL, false);
            renderSegmentedBar(gfx, left + 27, top + 168, flightDrift);
        }

        super.render(gfx, mouseX, mouseY, partialTick);
    }

    private void renderSegmentedBar(GuiGraphics gfx, int x, int y, int val) {
        int barWidth = W - 10 - 44;
        int segW = barWidth / STEPS;

        for (int i = 0; i < STEPS; i++) {
            int step = i + 1;
            int xPos = x + (i * segW);
            int color = step <= val ? COLOR_FILLED : COLOR_EMPTY;

            // Draw segment
            gfx.fill(xPos, y, xPos + segW - 2, y + 18, color);

            if (step == val) {
                String s = String.valueOf(step);
                gfx.drawCenteredString(font, s, xPos + (segW / 2), y + 5, 0xFFFFFF);
            }
        }
    }

    private void renderBorders(GuiGraphics gfx, int left, int top, int w, int h) {
        gfx.fill(left, top, left + w, top + 1, COLOR_BORDER);
        gfx.fill(left, top + h - 1, left + w, top + h, COLOR_BORDER);
        gfx.fill(left, top, left + 1, top + h, COLOR_BORDER);
        gfx.fill(left + w - 1, top, left + w, top + h, COLOR_BORDER);
    }

    private void cycleMode() {
        this.flightMode = switch (this.flightMode) {
            case "basic" -> "powered";
            case "powered" -> "creative";
            case "creative" -> "creative+wings";
            default -> "basic";
        };
    }

    private String getModeDisplayName() {
        return switch (flightMode) {
            case "basic" -> "Vanilla Elytra";
            case "powered" -> "Powered Elytra";
            case "creative" -> "Creative";
            case "creative+wings" -> "Creative + Wings";
            default -> "Unknown";
        };
    }

    private String formatTeslaEnergy(java.math.BigInteger energy) {
        String[] units = new String[] { "", "K", "M", "G", "T", "P", "E" };
        java.math.BigDecimal display = new java.math.BigDecimal(energy);
        int unitIndex = 0;

        while (display.compareTo(new java.math.BigDecimal(1000)) >= 0 && unitIndex < units.length - 1) {
            display = display.divide(new java.math.BigDecimal(1000), 2, java.math.RoundingMode.HALF_UP);
            unitIndex++;
        }
        return String.format("%.2f %sEU", display.floatValue(), units[unitIndex]);
    }

    private String getDrainDescription() {
        var cfg = PhoenixConfigs.wingFlight;

        java.util.function.Function<Long, String> fmt = (val) -> formatTeslaEnergy(java.math.BigInteger.valueOf(val)) +
                " EU/t";

        return switch (flightMode) {
            case "basic" -> "No EU drain";

            case "powered" -> {
                long base = cfg.poweredFlightEUt;
                long actualDrain = base + (long) (base * ((flightSpeed - 1) / 9.0));
                yield fmt.apply(actualDrain) + " - High EU Sonic Flight";
            }

            case "creative" -> {
                if (cfg.creativeFlightEUt <= 0) yield "FREE - Precision Hover";
                long actualDrain = (long) (cfg.creativeFlightEUt * (flightSpeed / 5.0));
                yield fmt.apply(actualDrain) + " - Precision Hover";
            }

            case "creative+wings" -> {
                if (cfg.creativeFlightEUt <= 0) yield "FREE - Hover & Glide";
                long actualDrain = (long) (cfg.creativeFlightEUt * (flightSpeed / 5.0));
                yield fmt.apply(actualDrain) + " - Hover & Glide";
            }

            default -> "N/A";
        };
    }

    private int getModeColor() {
        return switch (flightMode) {
            case "basic" -> COLOR_BASIC;
            case "powered" -> COLOR_POWERED;
            case "creative" -> COLOR_CREATIVE;
            case "creative+wings" -> COLOR_WINGED;
            default -> COLOR_LABEL;
        };
    }

    private void sendUpdate() {
        PhoenixNetwork.CHANNEL.sendToServer(new UpdateWingSettingsPacket(flightMode, flightSpeed, flightDrift));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
