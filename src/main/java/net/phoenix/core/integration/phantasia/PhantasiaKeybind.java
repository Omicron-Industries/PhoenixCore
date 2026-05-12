package net.phoenix.core.integration.phantasia;

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
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

import net.phoenix.core.client.keybind.PhoenixKeybinds;
import net.phoenix.core.integration.phantasia.client.PhantasiaSceneScreen;
import net.phoenix.core.integration.phantasia.client.PhantasiaSceneSelectionScreen;

/**
 * PhantasiaKeybind
 *
 * Handles the look-at HUD bar and context-sensitive key behaviour for Phantasia.
 *
 * KEY BEHAVIOUR (uses the unified OPEN_PHANTASIA_MENU binding from PhoenixKeybinds):
 *   - Looking at a registered multiblock controller → opens that machine's PhantasiaSceneScreen directly.
 *   - Not looking at one                           → opens PhantasiaSceneSelectionScreen.
 *
 * HUD BAR:
 *   - Drawn directly above the vanilla hotbar when the player looks at a registered multiblock.
 *   - Styled to match the Phantasia theme (C_BG / C_ACCENT).
 *   - Suppressed while any screen is open or the F3 debug overlay is active.
 *
 * REGISTRATION — call from PhoenixClient.init():
 *   MinecraftForge.EVENT_BUS.register(PhantasiaClientEvents.class);
 *   modBus.addListener(PhoenixKeybinds::register);   // already done
 *
 * Do NOT register a second keybind here. PHANTASIZE_KEY has been removed;
 * everything is unified under PhoenixKeybinds.OPEN_PHANTASIA_MENU (default: P).
 */
@OnlyIn(Dist.CLIENT)
public class PhantasiaKeybind {

    // ── Key press → open screen ───────────────────────────────────────────────

    /**
     * Call from a Forge-bus InputEvent.Key subscriber.
     * Dispatches to the machine-specific scene or the selection screen depending on look target.
     */
    public static void onKeyInput(InputEvent.Key event) {
        if (!PhoenixKeybinds.OPEN_PHANTASIA_MENU.consumeClick()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.screen != null) return;

        MultiblockMachineDefinition def = getLookedAtDefinition(mc);
        if (def != null) {
            // Context-sensitive: jump straight to this machine's scene
            mc.setScreen(new PhantasiaSceneScreen(def, null));
        } else {
            // Fallback: open the full selection screen
            mc.setScreen(new PhantasiaSceneSelectionScreen(null));
        }
    }

    // ── HUD overlay ───────────────────────────────────────────────────────────

    /**
     * Call from RenderGuiOverlayEvent.Post, checking overlay == VanillaGuiOverlay.CROSSHAIR.
     * Draws the "Press [P] to Phantasize" bar only when looking at a registered multiblock.
     */
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type()) return;

        Minecraft mc = Minecraft.getInstance();
        // Suppress while any screen is open or the F3 debug screen is active
        if (mc.level == null || mc.player == null || mc.screen != null) return;
        if (mc.options.renderDebug) return;

        MultiblockMachineDefinition def = getLookedAtDefinition(mc);
        if (def == null) return;

        renderPhantasiaBar(event.getGuiGraphics(), mc, def);
    }

    // ── Bar rendering ─────────────────────────────────────────────────────────

    private static void renderPhantasiaBar(GuiGraphics g, Minecraft mc, MultiblockMachineDefinition def) {
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        // FIX (B7): sit directly above the vanilla hotbar (hotbar = bottom 22 px + 2 px gap)
        int barH = 22;
        int barW = Math.min(screenW - 40, 320);
        int barX = (screenW - barW) / 2;
        int barY = screenH - 24 - barH;   // was: screenH - 60 - barH

        // Background — matches PhantasiaThemeUtils.C_BG / C_PANEL
        g.fill(barX, barY, barX + barW, barY + barH, 0xCC08080F);
        // Accent border — top only (bottom sits flush with hotbar gap)
        g.fill(barX, barY, barX + barW, barY + 1, 0xFF4FC3F7);
        // Subtle bottom fade
        g.fill(barX, barY + barH - 1, barX + barW, barY + barH, 0x554FC3F7);

        String keyName    = PhoenixKeybinds.OPEN_PHANTASIA_MENU.getTranslatedKeyMessage().getString();
        String machineName = def.getLangValue();

        int textMidY = barY + (barH - 8) / 2;

        // Key badge
        String badge  = " [" + keyName + "] ";
        int badgeW    = mc.font.width(badge) + 4;
        int badgeX    = barX + 8;
        g.fill(badgeX - 2, textMidY - 2, badgeX + badgeW, textMidY + 10, 0xFF1A2840);
        g.fill(badgeX - 2, textMidY - 2, badgeX + badgeW, textMidY - 1, 0xFF4FC3F7);
        g.drawString(mc.font, badge, badgeX, textMidY, 0xFF4FC3F7, false);

        // "to Phantasize:" label
        String action = " to Phantasize: ";
        int actionX   = badgeX + badgeW + 2;
        g.drawString(mc.font, action, actionX, textMidY, 0xFFBBBBBB, false);

        // Machine name (truncated to fit remaining bar width)
        int nameX   = actionX + mc.font.width(action);
        int maxNameW = barX + barW - nameX - 14;   // leave room for the script dot
        String name = machineName;
        while (mc.font.width(name) > maxNameW && name.length() > 3)
            name = name.substring(0, name.length() - 2) + "\u2026";
        g.drawString(mc.font, name, nameX, textMidY, 0xFFDDDDDD, false);

        // "Has script" dot — green if a custom script is registered
        if (PhantasiaScripts.has(def)) {
            int dotX = barX + barW - 10;
            int dotY = barY + barH / 2 - 2;
            g.fill(dotX, dotY, dotX + 4, dotY + 4, 0xFF66BB6A);
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    /**
     * Returns the MultiblockMachineDefinition the player is looking at, if it is
     * registered in PhantasiaSceneSelectionScreen.PHANTASIA_SCENES, otherwise null.
     */
    public static MultiblockMachineDefinition getLookedAtDefinition(Minecraft mc) {
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return null;

        BlockPos pos = ((BlockHitResult) mc.hitResult).getBlockPos();
        if (mc.level == null) return null;

        BlockState state = mc.level.getBlockState(pos);
        if (!(state.getBlock() instanceof MetaMachineBlock machineBlock)) return null;

        var rawDef = machineBlock.getDefinition();
        if (!(rawDef instanceof MultiblockMachineDefinition multiDef)) return null;

        // Only show for definitions that are registered in Phantasia
        if (!PhantasiaSceneSelectionScreen.PHANTASIA_SCENES.contains(multiDef)) return null;

        return multiDef;
    }
}