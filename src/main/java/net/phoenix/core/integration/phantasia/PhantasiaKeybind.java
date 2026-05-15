package net.phoenix.core.integration.phantasia;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.item.MetaMachineItem;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.phoenix.core.client.keybind.PhoenixKeybinds;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.phantasia.client.PhantasiaSceneScreen;
import net.phoenix.core.integration.phantasia.client.PhantasiaSceneSelectionScreen;

import com.mojang.blaze3d.platform.InputConstants;

@OnlyIn(Dist.CLIENT)
public class PhantasiaKeybind {

    private static int holdTimer = 0;
    private static int fadeTimer = 0;
    private static final int MAX_FADE_TICKS = 120; // 3 seconds
    private static final int FADE_OUT_TICKS = 40;  // 1 second actual fade
    private static MultiblockMachineDefinition lastDef = null;
    private static boolean wasLookingAtValid = false;

    // Refreshed every render frame by onItemTooltip while a tooltip is visible.
    private static MultiblockMachineDefinition tooltipTarget = null;

    // -------------------------------------------------------------------------
    // KEY CHECK — raw GLFW, works even when a GUI is open
    // -------------------------------------------------------------------------
    private static boolean isPhantasiaKeyDown() {
        Minecraft mc = Minecraft.getInstance();
        InputConstants.Key key = PhoenixKeybinds.OPEN_PHANTASIA_MENU.getKey();
        return switch (key.getType()) {
            case KEYSYM -> InputConstants.isKeyDown(mc.getWindow().getWindow(), key.getValue());
            case MOUSE -> org.lwjgl.glfw.GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), key.getValue()) ==
                    org.lwjgl.glfw.GLFW.GLFW_PRESS;
            default -> false;
        };
    }

    // -------------------------------------------------------------------------
    // TOOLTIP
    // -------------------------------------------------------------------------
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        var mode = PhoenixConfigs.INSTANCE.phantasiaUI.displayMode;
        if (mode == PhoenixConfigs.PhantasiaUIConfig.DisplayMode.JADE_ONLY ||
                mode == PhoenixConfigs.PhantasiaUIConfig.DisplayMode.HOTBAR_ONLY)
            return;

        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof MetaMachineItem machineItem) {
            if (machineItem.getDefinition() instanceof MultiblockMachineDefinition multiDef &&
                    PhantasiaSceneSelectionScreen.PHANTASIA_SCENES.contains(multiDef)) {

                tooltipTarget = multiDef;

                String keyName = PhoenixKeybinds.OPEN_PHANTASIA_MENU.getTranslatedKeyMessage().getString();
                event.getToolTip().add(Component.literal("§6§l» §b[" + keyName + "] §7Hold to Phantasize"));

                if (holdTimer > 0) {
                    float progress = (float) holdTimer / PhoenixConfigs.INSTANCE.phantasiaUI.activationTicks;
                    int barLen = 12;
                    int filled = (int) (barLen * progress);
                    event.getToolTip().add(Component.literal(
                            "  §b" + "▬".repeat(filled) + "§8" + "▬".repeat(barLen - filled)));
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // CLIENT TICK — hold detection, runs regardless of open GUI
    // -------------------------------------------------------------------------
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        MultiblockMachineDefinition currentTarget = getTargetDefinition(mc);
        if (currentTarget == null) currentTarget = tooltipTarget;
        if (currentTarget == null) tooltipTarget = null;

        if (currentTarget != null && isPhantasiaKeyDown()) {
            holdTimer++;
            if (holdTimer >= PhoenixConfigs.INSTANCE.phantasiaUI.activationTicks) {
                final MultiblockMachineDefinition defToOpen = currentTarget;
                mc.tell(() -> mc.setScreen(new PhantasiaSceneScreen(defToOpen, null)));
                holdTimer = 0;
                fadeTimer = 0;
                tooltipTarget = null;
            }
        } else {
            holdTimer = Math.max(0, holdTimer - 2);
        }
    }

    // -------------------------------------------------------------------------
    // OVERLAY — fade logic (original) + hotbar toast render
    // -------------------------------------------------------------------------
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() != VanillaGuiOverlay.PLAYER_LIST.type()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        MultiblockMachineDefinition lookedAt = getLookedAtDefinition(mc);

        // Sticky fade: reset to full whenever we look at a (possibly new) machine
        if (lookedAt != null) {
            if (lookedAt != lastDef || !wasLookingAtValid) fadeTimer = MAX_FADE_TICKS;
            lastDef = lookedAt;
            wasLookingAtValid = true;
        } else {
            wasLookingAtValid = false;
        }
        if (fadeTimer > 0) fadeTimer--;

        var mode = PhoenixConfigs.INSTANCE.phantasiaUI.displayMode;
        boolean canShowHotbar = (mode == PhoenixConfigs.PhantasiaUIConfig.DisplayMode.HOTBAR_ONLY ||
                mode == PhoenixConfigs.PhantasiaUIConfig.DisplayMode.TOOLTIP_HOTBAR);

        if (canShowHotbar && fadeTimer > 0 && lastDef != null && mc.screen == null) {
            renderPhantasiaToast(event.getGuiGraphics(), mc, lastDef, holdTimer, fadeTimer);
        }
    }

    // -------------------------------------------------------------------------
    // TOAST RENDERING
    // -------------------------------------------------------------------------
    private static void renderPhantasiaToast(GuiGraphics g, Minecraft mc,
                                             MultiblockMachineDefinition def,
                                             int timer, int fade) {
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        float alphaMult = (fade <= FADE_OUT_TICKS) ? (float) fade / FADE_OUT_TICKS : 1.0f;

        int alphaBG = (int) (0x88 * alphaMult) << 24;
        int alphaText = (int) (0xFF * alphaMult) << 24;
        int accent = (int) (0xFF * alphaMult) << 24 | 0x4FC3F7;

        int barH = 28;
        int barW = 160;
        int barX = (screenW - barW) / 2;
        int barY = screenH - 68;

        g.pose().pushPose();
        g.pose().translate(0, 0, 500);

        // Background
        g.fill(barX, barY, barX + barW, barY + barH, alphaBG | 0x08080F);
        // Bottom accent line
        g.fill(barX, barY + barH - 1, barX + barW, barY + barH, accent);

        // Progress fill
        float progress = (float) timer / Math.max(1, PhoenixConfigs.INSTANCE.phantasiaUI.activationTicks);
        if (progress > 0) {
            g.fill(barX, barY + barH - 2,
                    barX + (int) (barW * Math.min(progress, 1.0f)), barY + barH,
                    (int) (0xAA * alphaMult) << 24 | 0x4FC3F7);
        }

        // Machine icon — only rendered while fully opaque (not fading).
        // renderItem() ignores GL alpha entirely, so we hide it the moment
        // the fade starts rather than letting it linger at full brightness
        // while everything else fades out.
        if (fade > FADE_OUT_TICKS) {
            g.renderItem(def.asStack(), barX + 6, barY + 4);
        }

        // Name
        String name = def.getLangValue();
        if (name == null || name.isEmpty() || name.contains(".")) {
            name = Component.translatable(def.getDescriptionId()).getString();
        }
        name = truncate(name, 120, mc);

        String keyName = PhoenixKeybinds.OPEN_PHANTASIA_MENU.getTranslatedKeyMessage().getString();

        g.drawString(mc.font, name,
                barX + 28, barY + 4, alphaText | 0xFFFFFF, false);
        g.drawString(mc.font, "§b[" + keyName + "] §7to Phantasize",
                barX + 28, barY + 15, alphaText | 0xBBBBBB, false);

        g.pose().popPose();
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------
    private static String truncate(String s, int maxPx, Minecraft mc) {
        if (mc.font.width(s) <= maxPx) return s;
        String e = "...";
        while (mc.font.width(s + e) > maxPx && s.length() > 1) {
            s = s.substring(0, s.length() - 1);
        }
        return s + e;
    }

    private static MultiblockMachineDefinition getTargetDefinition(Minecraft mc) {
        ItemStack stack = mc.player.getMainHandItem();
        if (stack.getItem() instanceof MetaMachineItem machineItem) {
            if (machineItem.getDefinition() instanceof MultiblockMachineDefinition multiDef &&
                    PhantasiaSceneSelectionScreen.PHANTASIA_SCENES.contains(multiDef)) {
                return multiDef;
            }
        }
        return getLookedAtDefinition(mc);
    }

    public static MultiblockMachineDefinition getLookedAtDefinition(Minecraft mc) {
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return null;
        BlockPos pos = ((BlockHitResult) mc.hitResult).getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        if (state.getBlock() instanceof MetaMachineBlock machineBlock) {
            if (machineBlock.getDefinition() instanceof MultiblockMachineDefinition multiDef &&
                    PhantasiaSceneSelectionScreen.PHANTASIA_SCENES.contains(multiDef)) {
                return multiDef;
            }
        }
        return null;
    }
}
