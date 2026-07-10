package net.phoenix.core.conflux.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.phoenix.core.conflux.ConfluxDataType;
import net.phoenix.core.conflux.client.render.*;
import net.phoenix.core.conflux.client.render.discipline.DisciplineRendererRegistry;
import net.phoenix.core.conflux.network.C2SResearchUnlockPacket;
import net.phoenix.core.conflux.network.ConfluxNetwork;
import net.phoenix.core.conflux.research.ResearchNode;
import net.phoenix.core.conflux.research.ResearchTree;
import net.phoenix.core.conflux.research.ResearchTreeRegistry;
import net.phoenix.core.conflux.terminal.ResearchTerminalBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class ResearchTerminalScreen extends Screen {

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int TAB_H    = 20;
    private static final int BOTTOM_H = 48;
    private static final int DETAIL_W = 224;
    private static final int NODE_R   = 22; // used for label placement

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final int C_BG     = 0xFF07070F;
    private static final int C_CANVAS = 0xFF09090E;
    private static final int C_PANEL  = 0xFF0F0F1C;
    private static final int C_HEADER = 0xFF0B0B18;
    private static final int C_BORDER = 0xFF252540;
    private static final int C_ACCENT = 0xFF5577FF;
    private static final int C_TEXT   = 0xFFCCCCEE;
    private static final int C_DIM    = 0xFF555577;

    // ── State ─────────────────────────────────────────────────────────────────
    @Nullable private final ResearchTerminalBlockEntity terminal;

    private float panX = 0, panY = 0;
    private float zoom  = 1.0f;
    private boolean dragging  = false;
    private double lastDragX, lastDragY;

    private ResearchNode selected = null;
    private int activeTreeIdx = 0;
    private List<ResearchTree> trees = new ArrayList<>();

    // ── Visual system ─────────────────────────────────────────────────────────
    private final MotionClock clock = new MotionClock();
    private final IntensityController intensity = new IntensityController();
    private DisciplineRenderer activeRenderer = DisciplineRendererRegistry.getDefault();
    private String lastDisciplineId = null;
    private long lastFrameNanos = -1;

    public ResearchTerminalScreen(ResearchTerminalBlockEntity terminal) {
        super(Component.literal("Axiom Research Terminal"));
        this.terminal = terminal;
    }

    public ResearchTerminalScreen() { this(null); }

    @Override
    protected void init() {
        trees = new ArrayList<>(ResearchTreeRegistry.INSTANCE.getAllTrees());
        resetPan();
        lastFrameNanos = -1;
        // Activate renderer for the initially-selected tree
        syncRenderer();
    }

    @Override
    public void onClose() {
        AxiomShaderManager.deactivate();
        super.onClose();
    }

    private void resetPan() {
        panX = canvasW() / 2f;
        panY = canvasH() / 2f;
    }

    // ── Canvas bounds ─────────────────────────────────────────────────────────

    private int canvasW()   { return width - DETAIL_W; }
    private int canvasH()   { return height - TAB_H - BOTTOM_H; }
    private int canvasTop() { return TAB_H; }
    private int bottomTop() { return height - BOTTOM_H; }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        // Delta time
        long now = System.nanoTime();
        float dt = lastFrameNanos < 0 ? 0.016f : (float)((now - lastFrameNanos) / 1_000_000_000.0);
        lastFrameNanos = now;
        dt = Math.min(dt, 0.1f); // clamp to avoid spiral-of-death on lag

        // Sync discipline renderer if tree switched
        syncRenderer();

        // Tick motion systems
        clock.tick(dt);
        intensity.tick(dt);
        AxiomShaderManager.tick(dt);
        pushShaderData(mx, my);

        // Hover detection for intensity
        int cw = canvasW(), ct = canvasTop();
        boolean hoveringCanvas = mx >= 0 && mx < cw && my >= ct && my < ct + canvasH();
        if (hoveringCanvas) intensity.onHover(); else intensity.onHoverEnd();

        // Build RenderContext for active tree (null if no trees loaded)
        RenderContext ctx = buildContext(mx, my);
        if (ctx == null) {
            g.fill(0, 0, width, height, C_BG);
            renderTabBar(g, mx, my);
            renderDetailPanel(g, mx, my);
            renderBottomBar(g);
            super.render(g, mx, my, pt);
            return;
        }

        // Tick the renderer
        activeRenderer.tick(dt, ctx);

        // Draw UI layers
        g.fill(0, 0, width, height, C_BG);
        renderTabBar(g, mx, my);
        renderCanvas(g, mx, my, ctx);
        AxiomShaderManager.applyPostChain();
        renderDetailPanel(g, mx, my);
        renderBottomBar(g);
        super.render(g, mx, my, pt);
    }

    private void syncRenderer() {
        if (trees.isEmpty()) return;
        ResearchTree tree = trees.get(activeTreeIdx);
        String discId = tree.isDisciplineTree() ? tree.discipline : null;
        if (!java.util.Objects.equals(discId, lastDisciplineId)) {
            lastDisciplineId = discId;
            activeRenderer = DisciplineRendererRegistry.get(discId);
            activeRenderer.onActivate(clock);
            AxiomShaderManager.activate(activeRenderer.shaderLocation());
        }
    }

    private RenderContext buildContext(int mx, int my) {
        if (trees.isEmpty()) return null;
        ResearchTree tree = trees.get(activeTreeIdx);
        Set<ResourceLocation> unlocked  = ClientResearchCache.unlocked;
        Set<ResourceLocation> lockedOut = ClientResearchCache.lockedOut;
        int ct = canvasTop();
        float cmx = (float)((mx - panX) / zoom);
        float cmy = (float)((my - ct - panY) / zoom);
        return new RenderContext(tree, unlocked, lockedOut, selected,
                cmx, cmy, canvasW(), canvasH(), clock, intensity);
    }

    // ── Tab bar ───────────────────────────────────────────────────────────────

    private void renderTabBar(GuiGraphics g, int mx, int my) {
        g.fill(0, 0, canvasW(), TAB_H, C_HEADER);
        g.fill(0, TAB_H - 1, canvasW(), TAB_H, C_BORDER);

        int tabX = 4;
        for (int i = 0; i < trees.size(); i++) {
            ResearchTree tree = trees.get(i);
            int tw = font.width(tree.title) + 20;
            boolean active = (i == activeTreeIdx);
            boolean hover  = mx >= tabX && mx < tabX + tw && my >= 0 && my < TAB_H;

            g.fill(tabX, 1, tabX + tw, TAB_H, active ? C_PANEL : hover ? 0xFF111120 : C_HEADER);
            if (active) g.fill(tabX, TAB_H - 2, tabX + tw, TAB_H, C_ACCENT);
            g.fill(tabX + tw, 2, tabX + tw + 1, TAB_H - 2, C_BORDER);

            int textColor = active ? C_TEXT : hover ? 0xFF9999BB : C_DIM;
            g.drawString(font, tree.title, tabX + 10, (TAB_H - 8) / 2, textColor, false);
            tabX += tw + 1;
        }
    }

    // ── Canvas ────────────────────────────────────────────────────────────────

    private void renderCanvas(GuiGraphics g, int mx, int my, RenderContext ctx) {
        int cw = canvasW(), ch = canvasH(), ct = canvasTop();
        g.fill(0, ct, cw, ct + ch, C_CANVAS);
        g.fill(cw - 1, ct, cw, ct + ch, C_BORDER);

        if (trees.isEmpty()) {
            g.drawCenteredString(font, "§7No research trees loaded.", cw / 2, ct + ch / 2 - 4, C_TEXT);
            return;
        }

        double scale = minecraft.getWindow().getGuiScale();
        RenderSystem.enableScissor(
                0,
                (int)((height - ct - ch) * scale),
                (int)(cw * scale),
                (int)(ch * scale));

        // ── Background (canvas-space independent — no pan/zoom) ───────────────
        g.pose().pushPose();
        g.pose().translate(0, ct, 0);
        activeRenderer.renderBackground(g, ctx);
        g.pose().popPose();

        // ── Edges + Nodes in pan/zoom space ────────────────────────────────────
        g.pose().pushPose();
        g.pose().translate(panX, ct + panY, 0);
        g.pose().scale(zoom, zoom, 1);

        activeRenderer.renderEdges(g, ctx);
        activeRenderer.renderNodes(g, ctx);

        // Node labels (always on top, use default style)
        for (ResearchNode node : ctx.tree().getNodes()) {
            if (!node.isVisible(ctx.unlocked())) continue;
            float[] pos = activeRenderer.nodePos(node, ctx);
            drawNodeLabel(g, node, (int)pos[0], (int)pos[1], ctx);
        }

        g.pose().popPose();

        // ── Foreground particles (canvas-space) ───────────────────────────────
        g.pose().pushPose();
        g.pose().translate(panX, ct + panY, 0);
        g.pose().scale(zoom, zoom, 1);
        activeRenderer.renderForeground(g, ctx);
        g.pose().popPose();

        RenderSystem.disableScissor();
    }

    private void drawNodeLabel(GuiGraphics g, ResearchNode node, int cx, int cy, RenderContext ctx) {
        boolean unlocked  = ctx.isUnlocked(node.id);
        boolean lockedOut = ctx.isLockedOut(node.id);
        boolean available = ctx.isAvailable(node);
        boolean isMystery = node.hidden && !unlocked;

        int r = NODE_R;
        String rawLabel = isMystery ? "§8???" : lockedOut ? "§c" + node.title : node.title;
        String stripped  = rawLabel.replaceAll("§.", "");
        String label     = stripped.length() > 16
                ? rawLabel.substring(0, rawLabel.startsWith("§") ? 17 : 15) + "…"
                : rawLabel;
        int lw = font.width(label);
        // Shadow
        g.drawString(font, label, cx - lw / 2 + 1, cy + r + 4, 0xFF000000, false);
        g.drawString(font, label, cx - lw / 2, cy + r + 3,
                unlocked ? C_TEXT : available ? 0xFF88CC88 : C_DIM, false);
    }

    // ── Detail panel ──────────────────────────────────────────────────────────

    private void renderDetailPanel(GuiGraphics g, int mx, int my) {
        int px   = canvasW();
        int pTop = 0;
        int pBot = bottomTop();
        int ph   = pBot - pTop;

        g.fill(px, pTop, width, pBot, C_PANEL);
        g.fill(px, pTop, px + 1, pBot, C_BORDER);

        if (selected == null) {
            g.drawCenteredString(font, "§7Select a node to view details",
                    px + DETAIL_W / 2, pTop + ph / 2 - 4, C_DIM);
            return;
        }

        Set<ResourceLocation> unlockedSet = ClientResearchCache.unlocked;
        boolean unlocked  = ClientResearchCache.isUnlocked(selected.id);
        boolean lockedOut = ClientResearchCache.isLockedOut(selected.id);
        boolean isMystery = selected.hidden && !unlocked;  // revealed hidden node not yet researched
        boolean available = !unlocked && !lockedOut
                && selected.canUnlock(unlockedSet, ClientResearchCache.lockedOut);
        boolean canResearch = available && costsAffordable(selected);

        int x    = px + 12;
        int y    = pTop + 14;
        int maxW = DETAIL_W - 24;

        // ── Title ──────────────────────────────────────────────────────────
        String titleStr = isMystery ? "???" : selected.title;
        int titleColor  = unlocked ? 0xFF88BBFF : available ? 0xFF88EE88
                        : lockedOut ? 0xFF884444 : C_TEXT;
        g.drawString(font, titleStr, x, y, titleColor, false);

        // Commitment badge
        if (selected.isCommitmentNode) {
            String badge = "§6◆ Commitment";
            g.drawString(font, badge, width - font.width(badge) - 10, y, C_TEXT, false);
        }
        y += 13;

        g.fill(px + 8, y, width - 8, y + 1, C_BORDER);
        y += 7;

        // ── Lore / Hint ────────────────────────────────────────────────────
        // Lore shows for ALL visible non-locked-out nodes (locked, available, or done).
        // For mystery nodes: show the hint field instead, until they're unlocked.
        if (lockedOut) {
            for (String line : wrapText("§8This path has been sealed by a prior choice.", maxW)) {
                g.drawString(font, line, x, y, C_TEXT, false);
                y += 11;
            }
            y += 4;
        } else if (isMystery && !selected.hint.isEmpty()) {
            // Hint shown in italic-style dim colour — teases but doesn't reveal
            for (String line : wrapText("§o§7" + selected.hint, maxW)) {
                g.drawString(font, line, x, y, C_TEXT, false);
                y += 11;
            }
            y += 4;
        } else if (!selected.lore.isEmpty()) {
            // Lore visible for locked, available, and unlocked nodes alike —
            // acts as a hint system: read the lore to understand what you're unlocking.
            for (String line : wrapText("§7" + selected.lore, maxW)) {
                g.drawString(font, line, x, y, C_TEXT, false);
                y += 11;
            }
            y += 6;
        }

        // ── Prerequisites (any) hint ───────────────────────────────────────
        // Tell the player they only need ONE of these paths, and which they already have.
        if (!selected.prerequisitesAny.isEmpty() && !unlocked && !lockedOut) {
            g.fill(px + 8, y, width - 8, y + 1, C_BORDER);
            y += 6;
            g.drawString(font, "§7Unlock via any of:", x, y, C_DIM, false);
            y += 11;
            for (ResourceLocation anyId : selected.prerequisitesAny) {
                boolean met = unlockedSet.contains(anyId);
                String label = met ? "§a✔ " : "§8◦ ";

                // Create an effectively final copy of y for the lambdas to use safely
                final int currentY = y;

                // Try to get the node title; fall back to the raw ID path
                ResearchTreeRegistry.INSTANCE.getNode(anyId).ifPresentOrElse(
                        n -> g.drawString(font, label + n.title, x + 6, currentY, C_TEXT, false),
                        () -> g.drawString(font, label + anyId.getPath(), x + 6, currentY, C_TEXT, false));

                // You can still safely mutate the original y out here
                y += 10;
            }
            y += 4;
        }

        // ── Cost section ───────────────────────────────────────────────────
        if (!selected.cost.isEmpty() && !lockedOut) {
            g.fill(px + 8, y, width - 8, y + 1, C_BORDER);
            y += 7;
            g.drawString(font, "§eCost", x, y, C_TEXT, false);
            y += 12;

            for (var entry : selected.cost.entrySet()) {
                ConfluxDataType type = entry.getKey();
                long cost = entry.getValue();
                long have = terminal != null ? terminal.getStored(type) : 0L;
                boolean met = have >= cost;

                g.drawString(font, type.displayComponent(), x + 4, y, C_TEXT, false);

                String amtStr = formatAmount(have) + " / " + formatAmount(cost);
                int amtW = font.width(amtStr);
                g.drawString(font, (met ? "§a" : "§c") + amtStr, width - 10 - amtW, y, C_TEXT, false);

                y += 11;
                int barW = maxW;
                g.fill(x, y, x + barW, y + 3, 0xFF111122);
                int filled = (int)(barW * Math.min(have / (float) cost, 1f));
                g.fill(x, y, x + filled, y + 3, met ? 0xFF33AA33 : 0xFF993333);
                y += 7;
            }
            y += 6;
        }

        // ── Unlocks section (only after unlocking — lore is the pre-unlock hint) ──
        if (!selected.unlocks.isEmpty() && unlocked) {
            g.fill(px + 8, y, width - 8, y + 1, C_BORDER);
            y += 7;
            g.drawString(font, "§eUnlocks", x, y, C_TEXT, false);
            y += 12;
            for (var unlock : selected.unlocks) {
                String prefix = switch (unlock.type()) {
                    case "flag"        -> "§b⚑ ";
                    case "multiblock"  -> "§6⬡ ";
                    case "recipe_tag"  -> "§d⚗ ";
                    default            -> "§7• ";
                };
                for (String line : wrapText(prefix + unlock.value(), maxW - 8)) {
                    g.drawString(font, line, x + 4, y, C_TEXT, false);
                    y += 11;
                }
            }
        }

        // ── Research button ────────────────────────────────────────────────
        int btnH = 18;
        int btnY = pBot - btnH - 10;
        int btnX = px + 10;
        int btnW = DETAIL_W - 20;

        if (unlocked) {
            g.fill(btnX, btnY, btnX + btnW, btnY + btnH, 0xFF0A1A0A);
            g.renderOutline(btnX, btnY, btnW, btnH, 0xFF336633);
            g.drawCenteredString(font, "§a✔ Already Researched", px + DETAIL_W / 2, btnY + 5, C_TEXT);
        } else if (lockedOut) {
            g.fill(btnX, btnY, btnX + btnW, btnY + btnH, 0xFF1A0808);
            g.renderOutline(btnX, btnY, btnW, btnH, 0xFF662222);
            g.drawCenteredString(font, "§c✘ Path Locked", px + DETAIL_W / 2, btnY + 5, C_TEXT);
        } else {
            boolean hov = mx >= btnX && mx < btnX + btnW && my >= btnY && my < btnY + btnH;
            int btnBg  = canResearch ? (hov ? 0xFF1A3A1A : 0xFF112211) : 0xFF1A1020;
            int btnBrd = canResearch ? (hov ? 0xFF55CC55 : 0xFF338833) : 0xFF443344;
            g.fill(btnX, btnY, btnX + btnW, btnY + btnH, btnBg);
            g.renderOutline(btnX, btnY, btnW, btnH, btnBrd);
            String lbl = terminal == null      ? "§7No Terminal Connected"
                    : isMystery               ? "§7Unknown Research"
                    : canResearch             ? "§aResearch"
                    : !available              ? "§7Prerequisites Missing"
                    :                           "§7Insufficient Data";
            g.drawCenteredString(font, lbl, px + DETAIL_W / 2, btnY + 5, C_TEXT);
        }
    }

    // ── Bottom bar ────────────────────────────────────────────────────────────

    private void renderBottomBar(GuiGraphics g) {
        int y0 = bottomTop();
        g.fill(0, y0, width, height, C_HEADER);
        g.fill(0, y0, width, y0 + 1, C_BORDER);

        List<ConfluxDataType> available = new ArrayList<>();
        for (ConfluxDataType t : ConfluxDataType.values())
            if (t.isAvailable()) available.add(t);
        if (available.isEmpty()) return;

        int padding = 10;
        int totalW  = canvasW() - padding * 2;
        int slotW   = totalW / available.size();
        int barH    = 6;
        int x       = padding;
        int textY   = y0 + 6;
        int barY    = textY + 11;

        for (ConfluxDataType type : available) {
            long stored   = terminal != null ? terminal.getStored(type)   : 0L;
            long capacity = terminal != null ? terminal.getCapacity(type) : 1_000_000L;
            int  col      = typeBarColor(type);

            g.drawString(font, type.displayComponent(), x, textY, C_TEXT, false);
            String amtStr = formatAmount(stored);
            int amtW = font.width(amtStr);
            g.drawString(font, "§7" + amtStr, x + slotW - amtW - 2, textY, C_TEXT, false);

            int barW   = slotW - 4;
            int filled = capacity > 0 ? (int)(barW * stored / (float) capacity) : 0;
            g.fill(x, barY, x + barW, barY + barH, 0xFF111122);
            if (filled > 0) g.fill(x, barY, x + filled, barY + barH, col);
            g.renderOutline(x, barY, barW, barH, C_BORDER);

            x += slotW;
        }
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (super.mouseClicked(mx, my, btn)) return true;

        int cw = canvasW(), ct = canvasTop(), ch = canvasH();

        if (my < TAB_H) {
            int tabX = 4;
            for (int i = 0; i < trees.size(); i++) {
                int tw = font.width(trees.get(i).title) + 20;
                if (mx >= tabX && mx < tabX + tw) {
                    activeTreeIdx = i; selected = null; resetPan(); return true;
                }
                tabX += tw + 1;
            }
            return false;
        }

        if (mx >= cw) {
            int pBot = bottomTop();
            int btnH = 18;
            int btnY = pBot - btnH - 10;
            int btnX = cw + 10;
            int btnW = DETAIL_W - 20;
            if (my >= btnY && my < btnY + btnH && mx >= btnX && mx < btnX + btnW) {
                tryResearch(); return true;
            }
        }

        if (mx < cw && my >= ct && my < ct + ch && !trees.isEmpty()) {
            ResearchTree tree = trees.get(activeTreeIdx);
            Set<ResourceLocation> unlockedSet = ClientResearchCache.unlocked;
            float cx = (float)((mx - panX) / zoom);
            float cy = (float)((my - ct - panY) / zoom);
            RenderContext ctx = buildContext((int)mx, (int)my);
            ResearchNode hit = null;
            if (ctx != null) {
                for (ResearchNode node : tree.getNodes()) {
                    if (!node.isVisible(unlockedSet)) continue;
                    if (activeRenderer.hitsNode(node, cx, cy, ctx)) { hit = node; break; }
                }
            }
            if (hit != null) {
                selected = (selected != null && selected.id.equals(hit.id)) ? null : hit;
            } else {
                selected = null;
                if (btn == 0) { dragging = true; lastDragX = mx; lastDragY = my; }
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (btn == 0) dragging = false;
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (dragging && btn == 0) {
            panX += (float)(mx - lastDragX);
            panY += (float)(my - lastDragY);
            lastDragX = mx; lastDragY = my;
            return true;
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx < canvasW() && my >= canvasTop()) {
            float oldZoom = zoom;
            zoom = Math.max(0.4f, Math.min(2.5f, zoom + (float)(delta * 0.1f)));
            float factor = zoom / oldZoom;
            panX = (float)(mx - factor * (mx - panX));
            panY = (float)(my - canvasTop() - factor * (my - canvasTop() - panY));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean keyPressed(int kc, int sc, int mod) {
        if (kc == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        float step = 30f / zoom;
        if (kc == GLFW.GLFW_KEY_W || kc == GLFW.GLFW_KEY_UP)    { panY += step; return true; }
        if (kc == GLFW.GLFW_KEY_S || kc == GLFW.GLFW_KEY_DOWN)  { panY -= step; return true; }
        if (kc == GLFW.GLFW_KEY_A || kc == GLFW.GLFW_KEY_LEFT)  { panX += step; return true; }
        if (kc == GLFW.GLFW_KEY_D || kc == GLFW.GLFW_KEY_RIGHT) { panX -= step; return true; }
        return super.keyPressed(kc, sc, mod);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void tryResearch() {
        if (selected == null || terminal == null) return;
        if (selected.hidden && !selected.isVisible(ClientResearchCache.unlocked)) return;
        if (!costsAffordable(selected)) return;
        if (!selected.canUnlock(ClientResearchCache.unlocked, ClientResearchCache.lockedOut)) return;
        ConfluxNetwork.CHANNEL.sendToServer(
                new C2SResearchUnlockPacket(selected.id, terminal.getBlockPos()));
    }

    private boolean costsAffordable(ResearchNode node) {
        if (terminal == null) return false;
        return node.cost.entrySet().stream()
                .allMatch(e -> terminal.getStored(e.getKey()) >= e.getValue());
    }

    public boolean hasTerminal() { return terminal != null; }

    // ── Shader data ───────────────────────────────────────────────────────────

    private void pushShaderData(int mx, int my) {
        if (!AxiomShaderManager.isActive() || trees.isEmpty()) return;
        ResearchTree tree = trees.get(activeTreeIdx);

        float sw = (float) width;
        float sh = (float) height;
        int ct   = canvasTop();

        // Collect up to 8 visible node positions in screen space
        RenderContext sCtx = buildContext(mx, my);
        if (sCtx == null) return;
        java.util.List<float[]> nodePosScreen = new java.util.ArrayList<>();
        java.util.List<Float> heatStrList     = new java.util.ArrayList<>();
        float[] heatStrAll = activeRenderer.nodeHeatStrength(sCtx);
        int nodeIdx = 0;
        for (ResearchNode node : tree.getNodes()) {
            if (!node.isVisible(ClientResearchCache.unlocked)) continue;
            if (nodePosScreen.size() >= 8) break;
            float[] cp = activeRenderer.nodePos(node, sCtx);
            // canvas → screen
            float sx = panX + cp[0] * zoom;
            float sy = ct + panY + cp[1] * zoom;
            nodePosScreen.add(new float[]{ sx, sy });
            float str = (heatStrAll != null && nodeIdx < heatStrAll.length) ? heatStrAll[nodeIdx] : 1f;
            heatStrList.add(str);
            nodeIdx++;
        }

        int nc = nodePosScreen.size();
        float[] flatNodes = new float[16];
        float[] flatStr   = new float[8];
        for (int i = 0; i < nc; i++) {
            flatNodes[i*2]   = nodePosScreen.get(i)[0];
            flatNodes[i*2+1] = nodePosScreen.get(i)[1];
            flatStr[i]       = heatStrList.get(i);
        }

        // Ripple data (Sculk)
        float[] ripOrig = new float[8];
        float[] ripAge  = new float[0];
        int ripCount    = activeRenderer.rippleCount();
        if (ripCount > 0) {
            float[] origCanvas = activeRenderer.rippleOriginsCanvas();
            ripAge = activeRenderer.rippleAges();
            for (int i = 0; i < Math.min(ripCount, 4); i++) {
                ripOrig[i*2]   = panX + origCanvas[i*2]   * zoom;
                ripOrig[i*2+1] = ct + panY + origCanvas[i*2+1] * zoom;
            }
        }

        AxiomShaderManager.pushFrameData(
                sw, sh, (float) mx, (float) my,
                flatNodes, nc, flatStr,
                ripOrig, ripAge, ripCount);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int typeBarColor(ConfluxDataType type) {
        return switch (type) {
            case MATERIAL      -> 0xFFCC8800;
            case BIOLOGICAL    -> 0xFF22AA44;
            case ENERGETIC     -> 0xFF2299CC;
            case COMPUTATIONAL -> 0xFF9933CC;
            case ARCANE        -> 0xFF660099;
        };
    }

    private String formatAmount(long v) {
        if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000.0);
        if (v >= 1_000)     return String.format("%.1fk", v / 1_000.0);
        return String.valueOf(v);
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String stripped = text.replaceAll("§.", "");
        String[] words  = stripped.split(" ");
        String[] colored = text.split(" ");
        StringBuilder cur = new StringBuilder();
        int wi = 0;
        for (String word : words) {
            String coloredWord = wi < colored.length ? colored[wi] : word;
            if (font.width(cur + coloredWord) > maxWidth && !cur.isEmpty()) {
                lines.add(cur.toString().trim());
                cur.setLength(0);
            }
            cur.append(coloredWord).append(" ");
            wi++;
        }
        if (!cur.isEmpty()) lines.add(cur.toString().trim());
        return lines;
    }

    @Override public boolean isPauseScreen() { return false; }
}
