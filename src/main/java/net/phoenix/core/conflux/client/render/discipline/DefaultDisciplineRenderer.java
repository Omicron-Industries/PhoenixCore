package net.phoenix.core.conflux.client.render.discipline;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.conflux.client.render.*;
import net.phoenix.core.conflux.research.ResearchNode;
import org.jetbrains.annotations.Nullable;

/**
 * Fallback renderer used when no discipline-specific renderer is registered.
 * Also used for non-discipline trees (plain Axiom research).
 *
 * Mimics the grid layout and clean box style of the original ResearchTerminalScreen,
 * so trees without a discipline still look polished.
 */
public class DefaultDisciplineRenderer implements DisciplineRenderer {

    // Colors
    private static final int C_NODE_DONE    = 0xFF0D1A0D;
    private static final int C_NODE_AVAIL   = 0xFF1A0E00;
    private static final int C_NODE_LOCKED  = 0xFF0D0D0D;
    private static final int C_NODE_EXCL    = 0xFF1A0606;
    private static final int C_NODE_HIDDEN  = 0xFF0A0A0A;
    private static final int C_BORD_DONE    = 0xFF00AA44;
    private static final int C_BORD_AVAIL   = 0xFFAA6600;
    private static final int C_BORD_LOCKED  = 0xFF333333;
    private static final int C_BORD_EXCL    = 0xFF661111;
    private static final int C_BORD_HIDDEN  = 0xFF222222;
    private static final int C_BORD_SEL     = 0xFFFFDD44;
    private static final int C_LINE_AND     = 0xFF225522;
    private static final int C_LINE_OR      = 0xFF553388;
    private static final int NODE_R         = 20;

    @Override public @Nullable String disciplineId()   { return null; }
    @Override public MotionClock.Signature signature() { return MotionClock.Signature.DEFAULT; }

    @Override public void tick(float dt, RenderContext ctx) {}

    @Override
    public void renderBackground(GuiGraphics g, RenderContext ctx) {
        // Minimal: no background wash needed for the default view
    }

    @Override
    public void renderEdges(GuiGraphics g, RenderContext ctx) {
        for (ResearchNode node : ctx.tree().getNodes()) {
            if (!node.isVisible(ctx.unlocked())) continue;
            float[] dst = nodePos(node, ctx);

            for (ResourceLocation prereqId : node.prerequisites) {
                ctx.tree().getNode(prereqId).ifPresent(prereq -> {
                    if (!prereq.isVisible(ctx.unlocked())) return;
                    float[] src = nodePos(prereq, ctx);
                    drawElbow(g, src[0], src[1], dst[0], dst[1], C_LINE_AND, false);
                });
            }
            for (ResourceLocation prereqId : node.prerequisitesAny) {
                ctx.tree().getNode(prereqId).ifPresent(prereq -> {
                    if (!prereq.isVisible(ctx.unlocked())) return;
                    float[] src = nodePos(prereq, ctx);
                    drawElbow(g, src[0], src[1], dst[0], dst[1], C_LINE_OR, true);
                });
            }
        }
    }

    private void drawElbow(GuiGraphics g, float x1, float y1, float x2, float y2,
                            int color, boolean dashed) {
        float midY = (y1 + y2) / 2f;
        drawSegment(g, (int)x1, (int)y1, (int)x1, (int)midY, color, dashed, 0);
        drawSegment(g, (int)x1, (int)midY, (int)x2, (int)midY, color, dashed, 100);
        drawSegment(g, (int)x2, (int)midY, (int)x2, (int)y2, color, dashed, 200);
    }

    private void drawSegment(GuiGraphics g, int x1, int y1, int x2, int y2,
                              int color, boolean dashed, int dashOffset) {
        if (!dashed) {
            if (x1 == x2) g.fill(x1 - 1, Math.min(y1,y2), x1 + 1, Math.max(y1,y2), color);
            else           g.fill(Math.min(x1,x2), y1 - 1, Math.max(x1,x2), y1 + 1, color);
            return;
        }
        int dx = x2 - x1, dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) return;
        int dashLen = 4, gapLen = 3, total = dashLen + gapLen;
        for (int i = 0; i < steps; i++) {
            if ((i + dashOffset) % total >= dashLen) continue;
            int px = x1 + dx * i / steps;
            int py = y1 + dy * i / steps;
            g.fill(px - 1, py - 1, px + 1, py + 1, color);
        }
    }

    @Override
    public void renderNodes(GuiGraphics g, RenderContext ctx) {
        for (ResearchNode node : ctx.tree().getNodes()) {
            if (!node.isVisible(ctx.unlocked())) continue;
            float[] pos = nodePos(node, ctx);
            drawNode(g, node, (int)pos[0], (int)pos[1], ctx);
        }
    }

    private void drawNode(GuiGraphics g, ResearchNode node, int cx, int cy, RenderContext ctx) {
        boolean unlocked  = ctx.isUnlocked(node.id);
        boolean lockedOut = ctx.isLockedOut(node.id);
        boolean available = ctx.isAvailable(node);
        boolean sel       = ctx.isSelected(node);
        boolean mystery   = node.hidden && !unlocked;

        int bg  = lockedOut ? C_NODE_EXCL : mystery ? C_NODE_HIDDEN : unlocked ? C_NODE_DONE : available ? C_NODE_AVAIL : C_NODE_LOCKED;
        int brd = lockedOut ? C_BORD_EXCL : mystery ? C_BORD_HIDDEN : unlocked ? C_BORD_DONE : available ? C_BORD_AVAIL : C_BORD_LOCKED;
        if (sel) brd = C_BORD_SEL;

        int r = NODE_R;
        g.fill(cx - r, cy - r, cx + r, cy + r, bg);
        g.fill(cx - r - 1, cy - r - 1, cx + r + 1, cy - r,     brd);
        g.fill(cx - r - 1, cy + r,     cx + r + 1, cy + r + 1, brd);
        g.fill(cx - r - 1, cy - r,     cx - r,     cy + r,     brd);
        g.fill(cx + r,     cy - r,     cx + r + 1, cy + r,     brd);

        if (!mystery && !lockedOut) {
            renderIcon(g, node.icon, cx - 8, cy - 8);
        } else if (mystery) {
            g.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, "?", cx, cy - 4, 0x88AAAAAA);
        } else if (lockedOut) {
            g.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, "✘", cx, cy - 4, 0x88CC2222);
        }

        if (node.isCommitmentNode) {
            g.fill(cx - 2, cy - r - 5, cx + 2, cy - r - 1, C_BORD_DONE);
        }
    }

    @Override
    public float[] nodePos(ResearchNode node, RenderContext ctx) {
        return new float[]{ node.posX * 110f, node.posY * 110f };
    }

    private static void renderIcon(GuiGraphics g, String iconId, int x, int y) {
        if (iconId == null || iconId.isEmpty()) return;
        ResourceLocation rl = ResourceLocation.tryParse(iconId);
        if (rl == null) return;
        var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(rl);
        if (item != null && item != net.minecraft.world.item.Items.AIR)
            g.renderItem(new net.minecraft.world.item.ItemStack(item), x, y);
    }
}
