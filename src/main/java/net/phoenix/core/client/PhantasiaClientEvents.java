package net.phoenix.core.client;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.phoenix.core.client.keybind.PhoenixKeybinds;
import net.phoenix.core.integration.phantasia.PhantasiaSceneScreen;
import net.phoenix.core.integration.phantasia.PhantasiaSceneSelectionScreen;
import net.phoenix.core.integration.phantasia.PhantasiaScripts;

/**
 * PhantasiaClientEvents
 *
 * Forge event handler (registered on MinecraftForge.EVENT_BUS in PhoenixClient.init).
 * Handles:
 * - Client tick: track how long OPEN_PHANTASIA_MENU is held while looking at a
 * registered multiblock. Opens PhantasiaSceneScreen after HOLD_TICKS ticks.
 * - RenderGuiOverlayEvent.Post (CROSSHAIR): draws the "Hold [P] to Phantasize"
 * bar with a filling progress arc when looking at a registered multiblock.
 *
 * The keybind used is PhoenixKeybinds.OPEN_PHANTASIA_MENU (default: P).
 * No separate KeyMapping is defined here — the key is already registered in
 * PhoenixKeybinds and wired to the mod event bus from PhoenixClient.
 */
@OnlyIn(Dist.CLIENT)
public class PhantasiaClientEvents {

    // How many ticks the key must be held before the screen opens (~1 second)
    private static final int HOLD_TICKS = 20;

    // Current hold duration in ticks; resets when key is released or target changes
    private static int holdTicks = 0;

    // The definition we were holding toward last tick (used to detect target change)
    private static MultiblockMachineDefinition lastTarget = null;

    // Whether we already opened the screen this hold (prevents re-opening on same hold)
    private static boolean openedThisHold = false;

    // ── Client tick ───────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.screen != null) {
            reset();
            return;
        }

        boolean keyHeld = PhoenixKeybinds.OPEN_PHANTASIA_MENU.isDown();
        MultiblockMachineDefinition target = getLookedAtDefinition(mc);

        // If target changed while holding, reset
        if (target != lastTarget) {
            reset();
            lastTarget = target;
        }

        if (target == null || !keyHeld) {
            // Key released or not looking at a target — reset progress
            holdTicks = 0;
            openedThisHold = false;
            return;
        }

        // Key is held and we have a valid target
        if (!openedThisHold) {
            holdTicks++;
            if (holdTicks >= HOLD_TICKS) {
                mc.setScreen(new PhantasiaSceneScreen(target, null));
                openedThisHold = true;
                holdTicks = 0;
            }
        }
    }

    private static void reset() {
        holdTicks = 0;
        openedThisHold = false;
        lastTarget = null;
    }

    // ── HUD overlay ───────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.screen != null) return;

        MultiblockMachineDefinition def = getLookedAtDefinition(mc);
        if (def == null) return;

        renderPhantasiaBar(event.getGuiGraphics(), mc, def);
    }

    private static void renderPhantasiaBar(GuiGraphics g, Minecraft mc, MultiblockMachineDefinition def) {
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        String keyName = PhoenixKeybinds.OPEN_PHANTASIA_MENU.getTranslatedKeyMessage().getString();

        // ── Bar geometry ──
        int barH = 24;
        int barW = Math.min(screenW - 40, 300);
        int barX = (screenW - barW) / 2;
        int barY = screenH - 55 - barH;

        // ── Background ──
        g.fill(barX, barY, barX + barW, barY + barH, 0xCC06060E);
        g.fill(barX, barY, barX + barW, barY + 1, 0xFF4FC3F7);         // top accent
        g.fill(barX, barY + barH - 1, barX + barW, barY + barH, 0x334FC3F7); // bottom fade

        int midY = barY + (barH - 8) / 2;

        // ── Key badge ──
        String badge = "[" + keyName + "]";
        int badgeW = mc.font.width(badge) + 6;
        int badgeX = barX + 6;
        g.fill(badgeX - 1, midY - 2, badgeX + badgeW + 1, midY + 10, 0xFF122030);
        g.fill(badgeX - 1, midY - 2, badgeX + badgeW + 1, midY - 1, 0xFF4FC3F7);
        g.drawString(mc.font, badge, badgeX + 1, midY, 0xFF4FC3F7, false);

        // ── "Hold to Phantasize:" label ──
        String label = " Hold to Phantasize: ";
        int labelX = badgeX + badgeW + 3;
        g.drawString(mc.font, label, labelX, midY, 0xFFAAAAAA, false);

        // ── Machine name ──
        String name = def.getLangValue();
        int nameX = labelX + mc.font.width(label);
        int maxNameW = barX + barW - nameX - 8;
        while (mc.font.width(name) > maxNameW && name.length() > 3)
            name = name.substring(0, name.length() - 2) + "\u2026";
        g.drawString(mc.font, name, nameX, midY, 0xFFDDDDDD, false);

        // ── Script indicator dot (green = has script, grey = no script) ──
        int dotColor = PhantasiaScripts.has(def) ? 0xFF66BB6A : 0xFF445566;
        g.fill(barX + barW - 7, barY + barH / 2 - 2, barX + barW - 3, barY + barH / 2 + 2, dotColor);

        // ── Hold progress bar (only shown while key is held) ──
        boolean keyHeld = PhoenixKeybinds.OPEN_PHANTASIA_MENU.isDown();
        if (keyHeld && holdTicks > 0 && !openedThisHold) {
            float progress = (float) holdTicks / HOLD_TICKS;

            // Progress track (below main bar)
            int trackY = barY + barH + 2;
            int trackH = 3;
            g.fill(barX, trackY, barX + barW, trackY + trackH, 0x33FFFFFF);

            // Filled portion — interpolate slate → cyan
            int fillW = (int) (barW * progress);
            int fillColor = lerpColor(0xFF1A3040, 0xFF4FC3F7, progress);
            g.fill(barX, trackY, barX + fillW, trackY + trackH, fillColor);

            // Pulsing tip
            if (fillW < barW) {
                int tipAlpha = (int) (0xBB + 0x44 * Math.sin(System.currentTimeMillis() / 120.0));
                tipAlpha = Math.max(0xBB, Math.min(0xFF, tipAlpha));
                g.fill(barX + fillW - 1, trackY - 1, barX + fillW + 2, trackY + trackH + 1,
                        (tipAlpha << 24) | 0x4FC3F7);
            }

            // "Almost ready" flash when >75%
            if (progress > 0.75f) {
                int flashAlpha = (int) (0x40 * Math.abs(Math.sin(System.currentTimeMillis() / 80.0)));
                g.fill(barX, barY, barX + barW, barY + barH, (flashAlpha << 24) | 0x4FC3F7);
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Utility
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns the MultiblockMachineDefinition the player is looking at,
     * only if it is registered in PhantasiaSceneSelectionScreen.PHANTASIA_SCENES.
     * Returns null otherwise.
     */
    static MultiblockMachineDefinition getLookedAtDefinition(Minecraft mc) {
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return null;

        BlockPos pos = ((BlockHitResult) mc.hitResult).getBlockPos();
        if (mc.level == null) return null;

        BlockState state = mc.level.getBlockState(pos);
        if (!(state.getBlock() instanceof MetaMachineBlock machineBlock)) return null;

        var rawDef = machineBlock.getDefinition();
        if (!(rawDef instanceof MultiblockMachineDefinition multiDef)) return null;

        return PhantasiaSceneSelectionScreen.PHANTASIA_SCENES.contains(multiDef) ? multiDef : null;
    }

    /** Linear interpolate between two ARGB colors. */
    private static int lerpColor(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = (int) (ar + t * (br - ar));
        int gr = (int) (ag + t * (bg - ag));
        int bl = (int) (ab + t * (bb - ab));
        return 0xFF000000 | (r << 16) | (gr << 8) | bl;
    }
}
