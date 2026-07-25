package net.phoenix.core.integration.matter_manipulater.api;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.network.packet.PacketPhoenixModeSync;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class PhoenixRadialMenu extends Screen {

    private final int radius = 100;
    private final int innerRadius = 35;

    public PhoenixRadialMenu() {
        super(Component.literal("Phoenix Manipulator Modes"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        
        graphics.fill(0, 0, this.width, this.height, 0x55000000);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        PhoenixManipulatorMode[] modes = PhoenixManipulatorMode.values();
        float angleStep = 360.0f / modes.length;

        drawDonut(graphics, centerX, centerY, innerRadius, radius, 0xCC111111);

        PhoenixManipulatorMode hoveredMode = null;
        for (int i = 0; i < modes.length; i++) {
            double angle = Math.toRadians(i * angleStep - 90);
            double stepRad = Math.toRadians(angleStep);

            boolean hovered = isMouseInSector(mouseX, mouseY, centerX, centerY, angle, stepRad);
            if (hovered) {
                hoveredMode = modes[i];
                
                drawArc(graphics, centerX, centerY, innerRadius, radius, (float) angle, (float) stepRad, 0x66FFAA00);
            }

            float textRadius = (innerRadius + radius) / 2.0f;
            int textX = centerX + (int) (Math.cos(angle) * textRadius);
            int textY = centerY + (int) (Math.sin(angle) * textRadius);

            int color = hovered ? 0xFFFFAA00 : 0xFFFFFFFF;
            graphics.drawCenteredString(this.font, modes[i].getName(), textX, textY - 4, color);
        }

        graphics.fill(centerX - innerRadius + 2, centerY - innerRadius + 2, centerX + innerRadius - 2,
                centerY + innerRadius - 2, 0xFF880000);
        graphics.drawCenteredString(this.font, "CORE", centerX, centerY - 4, 0xFFFFFFFF);

        if (hoveredMode != null) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("§6" + hoveredMode.getName()));
            tooltip.add(Component.literal("§7" + getModeDescription(hoveredMode)));
            graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    private String getModeDescription(PhoenixManipulatorMode mode) {
        return switch (mode) {
            case LINE -> "Places pipes in a single axis line.";
            case WALL -> "Creates a 2D plane of pipes.";
            case GRID -> "Fills the entire 3D selection.";
            case CONNECT_ONLY -> "Forces connections without placing blocks.";
            case DISCONNECT -> "Severs all connections in the area.";
            default -> "Blocks manipulation mode.";
        };
    }

    private void drawDonut(GuiGraphics graphics, int cx, int cy, int inner, int outer, int color) {
        RenderSystem.enableBlend();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = graphics.pose().last().pose();

        for (int i = 0; i <= 360; i += 5) {
            float rad = (float) Math.toRadians(i);
            buffer.vertex(matrix, cx + Mth.cos(rad) * outer, cy + Mth.sin(rad) * outer, 0).color(color).endVertex();
            buffer.vertex(matrix, cx + Mth.cos(rad) * inner, cy + Mth.sin(rad) * inner, 0).color(color).endVertex();
        }
        tesselator.end();
    }

    private void drawArc(GuiGraphics graphics, int cx, int cy, int inner, int outer, float startAngle, float step,
                         int color) {
        RenderSystem.enableBlend();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = graphics.pose().last().pose();

        for (float a = startAngle - step / 2; a <= startAngle + step / 2; a += 0.05f) {
            buffer.vertex(matrix, cx + Mth.cos(a) * outer, cy + Mth.sin(a) * outer, 0).color(color).endVertex();
            buffer.vertex(matrix, cx + Mth.cos(a) * inner, cy + Mth.sin(a) * inner, 0).color(color).endVertex();
        }
        tesselator.end();
    }

    private boolean isMouseInSector(int mx, int my, int cx, int cy, double angle, double step) {
        double dist = Math.sqrt(Math.pow(mx - cx, 2) + Math.pow(my - cy, 2));
        if (dist < innerRadius || dist > radius) return false;
        double mouseAngle = Math.atan2(my - cy, mx - cx);
        double diff = mouseAngle - angle;
        while (diff < -Math.PI) diff += Math.PI * 2;
        while (diff > Math.PI) diff -= Math.PI * 2;
        return Math.abs(diff) < step / 2;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = width / 2;
        int centerY = height / 2;
        PhoenixManipulatorMode[] modes = PhoenixManipulatorMode.values();
        double angleStep = Math.PI * 2 / modes.length;

        for (int i = 0; i < modes.length; i++) {
            double angle = -Math.PI / 2 + (i * angleStep);
            if (isMouseInSector((int) mouseX, (int) mouseY, centerX, centerY, angle, angleStep)) {
                PhoenixNetwork.CHANNEL.sendToServer(new PacketPhoenixModeSync(i));
                Minecraft.getInstance().getSoundManager()
                        .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                this.onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
