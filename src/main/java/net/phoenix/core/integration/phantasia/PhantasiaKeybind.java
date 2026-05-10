package net.phoenix.core.integration.phantasia;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

import org.lwjgl.glfw.GLFW;

/**
 * PhantasiaKeybind
 *
 * Registers the "Phantasize" keybind (default: G) and draws an in-world HUD bar
 * when the player looks at a registered multiblock controller.
 *
 * HOW IT WORKS:
 * - Every render tick: check the player's crosshair target.
 * If it's a MetaMachineBlock whose definition is in PhantasiaScripts,
 * draw the "Hold [G] to Phantasize" bar at the bottom of the screen.
 * - When the key is pressed (not just looked at): open PhantasiaSceneScreen.
 *
 * REGISTRATION (call from your mod's client event bus subscriber):
 *
 * // In your mod's constructor or FMLJavaModLoadingContext.get().getModEventBus():
 * ModLoadingContext.get().getActiveContainer(); // (just to show context)
 * FMLJavaModLoadingContext.get().getModEventBus().addListener(PhantasiaKeybind::onRegisterKeyMappings);
 *
 * // On the FORGE event bus (in your ClientEvents class annotated @Mod.EventBusSubscriber):
 * 
 * @SubscribeEvent
 *                 public static void onKeyInput(InputEvent.Key event) { PhantasiaKeybind.onKeyInput(event); }
 *
 * @SubscribeEvent
 *                 public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
 *                 PhantasiaKeybind.onRenderOverlay(event); }
 */
@OnlyIn(Dist.CLIENT)
public class PhantasiaKeybind {

    public static final KeyMapping PHANTASIZE_KEY = new KeyMapping(
            "key.phantasia.phantasize",   // translation key
            GLFW.GLFW_KEY_G,              // default: G
            "key.categories.phantasia"    // category
    );

    // ── Registration ──────────────────────────────────────────────────────────

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(PHANTASIZE_KEY);
    }

    // ── Key press → open screen ───────────────────────────────────────────────

    public static void onKeyInput(InputEvent.Key event) {
        if (!PHANTASIZE_KEY.consumeClick()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.screen != null) return;

        MultiblockMachineDefinition def = getLookedAtDefinition(mc);
        if (def == null) return;

        mc.setScreen(new PhantasiaSceneScreen(def, null));
    }

    // ── HUD overlay ───────────────────────────────────────────────────────────

    /**
     * Call from RenderGuiOverlayEvent.Post, checking overlay == VanillaGuiOverlay.CROSSHAIR.
     * Draws the "Hold [G] to Phantasize" bar only when looking at a registered multiblock.
     */
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

        String keyName = PHANTASIZE_KEY.getTranslatedKeyMessage().getString();
        String machineName = def.getLangValue();

        // Bar layout
        int barH = 22;
        int barY = screenH - 60 - barH;
        int barW = Math.min(screenW - 40, 320);
        int barX = (screenW - barW) / 2;

        // Background
        g.fill(barX, barY, barX + barW, barY + barH, 0xCC08080F);
        // Accent border top
        g.fill(barX, barY, barX + barW, barY + 1, 0xFF4FC3F7);
        // Accent border bottom
        g.fill(barX, barY + barH - 1, barX + barW, barY + barH, 0x554FC3F7);

        // Key badge
        String badge = " [" + keyName + "] ";
        int badgeW = mc.font.width(badge) + 4;
        int badgeX = barX + 8;
        int badgeMidY = barY + (barH - 8) / 2;
        g.fill(badgeX - 2, badgeMidY - 2, badgeX + badgeW, badgeMidY + 10, 0xFF1A2840);
        g.fill(badgeX - 2, badgeMidY - 2, badgeX + badgeW, badgeMidY - 1, 0xFF4FC3F7);
        g.drawString(mc.font, badge, badgeX, badgeMidY, 0xFF4FC3F7, false);

        // "to Phantasize:" label
        String action = " to Phantasize: ";
        g.drawString(mc.font, action, badgeX + badgeW + 2, badgeMidY, 0xFFBBBBBB, false);

        // Machine name (truncated)
        String name = machineName;
        int nameX = badgeX + badgeW + 2 + mc.font.width(action);
        int maxNameW = barX + barW - nameX - 6;
        while (mc.font.width(name) > maxNameW && name.length() > 3) {
            name = name.substring(0, name.length() - 2) + "\u2026";
        }
        g.drawString(mc.font, name, nameX, badgeMidY, 0xFFDDDDDD, false);

        // "Has script" dot
        if (PhantasiaScripts.has(def)) {
            g.fill(barX + barW - 8, barY + barH / 2 - 2, barX + barW - 4, barY + barH / 2 + 2, 0xFF66BB6A);
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    /**
     * Returns the MultiblockMachineDefinition the player is looking at if it
     * is registered in PhantasiaSceneSelectionScreen.PHANTASIA_SCENES,
     * or null if not looking at one.
     */
    private static MultiblockMachineDefinition getLookedAtDefinition(Minecraft mc) {
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return null;

        BlockPos pos = ((BlockHitResult) mc.hitResult).getBlockPos();
        if (mc.level == null) return null;

        BlockState state = mc.level.getBlockState(pos);
        if (!(state.getBlock() instanceof MetaMachineBlock machineBlock)) return null;

        var rawDef = machineBlock.getDefinition();
        if (!(rawDef instanceof MultiblockMachineDefinition multiDef)) return null;

        // Only show for definitions that are in the Phantasia registry
        if (!PhantasiaSceneSelectionScreen.PHANTASIA_SCENES.contains(multiDef)) return null;

        return multiDef;
    }
}
