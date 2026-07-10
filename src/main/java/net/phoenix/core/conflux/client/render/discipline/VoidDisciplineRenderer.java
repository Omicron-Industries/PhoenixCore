package net.phoenix.core.conflux.client.render.discipline;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.conflux.client.render.*;
import net.phoenix.core.conflux.research.ResearchNode;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import static net.phoenix.core.conflux.client.render.AxiomSprites.*;

import java.util.*;

/**
 * Void Discipline — The Singularity.
 *
 * Renders a gravitational singularity at canvas center: star field, rotating
 * dust lanes, an elliptical accretion disk (orange-yellow-white gradient), a
 * dark void center, and two braided chain rings counter-rotating at different
 * radii. Nodes orbit the singularity in an elliptical path, slowly drifting.
 * Per-node glitch displacement fires randomly.
 */
public class VoidDisciplineRenderer implements DisciplineRenderer {

    // Geometry
    private static final float VOID_R    = 70f;
    private static final float DISK_IN   = 70f;
    private static final float DISK_OUT  = 118f;
    private static final float DISK_SQSH = 0.38f;   // vertical squish (perspective view angle)
    private static final float BRAID_R1  = 155f;
    private static final float BRAID_R2  = 212f;
    private static final float ORBIT_R   = 148f;    // node orbital radius
    // Animation
    private float ringAngle  = 0f;   // braid ring phase (ring 1 clockwise)
    private float dustAngle  = 0f;   // dust lane rotation
    private float pulseTimer = 0f;   // general time for flicker/pulse
    private float glitchTick = 0f;   // countdown to next glitch event

    private int starW = 0, starH = 0; // track canvas size for star sprite invalidation

    // Per-node glitch: id → [dx, dy, remaining_seconds]
    private final Map<ResourceLocation, float[]> nodeGlitch = new HashMap<>();

    // Stable node ordering for orbital positions (lazily populated)
    private final List<ResourceLocation> cachedOrder = new ArrayList<>();

    @Override public @Nullable String disciplineId() { return "void"; }
    @Override public MotionClock.Signature signature() { return MotionClock.Signature.VOID; }

    @Override
    public void onActivate(MotionClock clock) {
        starW = starH = 0;
        ringAngle = pulseTimer = 0f;
        cachedOrder.clear();
        nodeGlitch.clear();
    }

    @Override
    public void tick(float dt, RenderContext ctx) {
        ringAngle  += dt * 0.19f;
        dustAngle  += dt * 0.034f;
        pulseTimer += dt;
        glitchTick -= dt;

        if (glitchTick <= 0f) {
            glitchTick = 0.7f + (float)(Math.random() * 2.8f);
            List<ResearchNode> ns = new ArrayList<>(ctx.tree().getNodes());
            if (!ns.isEmpty()) {
                ResearchNode t = ns.get((int)(Math.random() * ns.size()));
                nodeGlitch.put(t.id, new float[]{
                    (float)(Math.random() - 0.5f) * 8f,
                    (float)(Math.random() - 0.5f) * 4f,
                    0.18f
                });
            }
        }
        nodeGlitch.entrySet().removeIf(e -> (e.getValue()[2] -= dt) <= 0f);
    }

    // ── Background: star field, dust, singularity ─────────────────────────────

    @Override
    public void renderBackground(GuiGraphics g, RenderContext ctx) {
        int cw = ctx.canvasW(), ch = ctx.canvasH();

        // Deep space fill
        g.fill(0, 0, cw, ch, 0xFF000008);

        // Star field — pre-baked sprite, single blit
        blitFrame(g, VOID_STARS, 0, 0, 0, cw, ch, 0.92f);

        float cx = cw * 0.5f, cy = ch * 0.5f;

        // Rotating dust lanes — 3 bands 120° apart, drawn via pose rotation
        for (int lane = 0; lane < 3; lane++) {
            float la = dustAngle + lane * 2.094f;
            g.pose().pushPose();
            g.pose().translate(cx, cy, 0f);
            g.pose().mulPose(new Quaternionf().rotationZ(la));
            for (int band = -2; band <= 2; band++) {
                float bOff  = band * 54f;
                float fall  = 1f - Math.abs(band) / 2.6f;
                int   alpha = (int)(17 * fall);
                g.fill(-700, (int)(bOff - 15), 700, (int)(bOff + 15), (alpha << 24) | 0x552211);
            }
            g.pose().popPose();
        }

        // Accretion disk — pre-baked sprite sheet, frame driven by ringAngle
        int diskFrame = timeToFrame(ringAngle / (2f * 3.14159f), 1f, VOID_DISK.frames());
        blitFrame(g, VOID_DISK, diskFrame, 0, 0, cw, ch, 0.9f);
        // No drawVoidCenter — the sprite bakes a transparent center, dark space fill shows through

        // Two braided chain rings, counter-rotating
        drawBraidRing(g, cx, cy, BRAID_R1,  ringAngle,               DISK_SQSH + 0.14f, 0xBBCCCCDD, 0x66AABBD0);
        drawBraidRing(g, cx, cy, BRAID_R2, -ringAngle * 0.62f + 0.9f, DISK_SQSH,         0x99BBBCCC, 0x44999ABB);
    }

    private void drawBraidRing(GuiGraphics g, float cx, float cy, float R, float phase,
                                float squish, int col1, int col2) {
        final int   STEPS = 320;
        final int   FREQ  = 20;
        final float AMP   = 9f;

        // Pass 1 — draw strand 2 everywhere, strand 1 only where it's "behind"
        for (int i = 0; i < STEPS; i++) {
            float t    = (float)i / STEPS * 6.28318f;
            float a    = t + phase;
            float cosA = (float)Math.cos(a);
            float sinA = (float)Math.sin(a);
            float rx   = cx + R * cosA;
            float ry   = cy + R * sinA * squish;

            float px = -sinA, py = cosA * squish;
            float pLen = (float)Math.sqrt(px * px + py * py);
            if (pLen > 0.001f) { px /= pLen; py /= pLen; }

            float wave = AMP * (float)Math.sin(t * FREQ);

            float s2x = rx - px * wave, s2y = ry - py * wave;
            g.fill((int)s2x - 1, (int)s2y, (int)s2x + 2, (int)s2y + 2, col2);

            if (wave <= 0f) {
                float s1x = rx + px * wave, s1y = ry + py * wave;
                g.fill((int)s1x - 1, (int)s1y, (int)s1x + 2, (int)s1y + 2, col1);
            }
        }
        // Pass 2 — strand 1 on top where it's "in front"
        for (int i = 0; i < STEPS; i++) {
            float t    = (float)i / STEPS * 6.28318f;
            float a    = t + phase;
            float cosA = (float)Math.cos(a);
            float sinA = (float)Math.sin(a);
            float rx   = cx + R * cosA;
            float ry   = cy + R * sinA * squish;

            float px = -sinA, py = cosA * squish;
            float pLen = (float)Math.sqrt(px * px + py * py);
            if (pLen > 0.001f) { px /= pLen; py /= pLen; }

            float wave = AMP * (float)Math.sin(t * FREQ);
            if (wave > 0f) {
                float s1x = rx + px * wave, s1y = ry + py * wave;
                g.fill((int)s1x - 1, (int)s1y, (int)s1x + 2, (int)s1y + 2, col1);
            }
        }
    }

    // ── Edges ─────────────────────────────────────────────────────────────────

    @Override
    public void renderEdges(GuiGraphics g, RenderContext ctx) {
        for (ResearchNode node : ctx.tree().getNodes()) {
            if (!node.isVisible(ctx.unlocked())) continue;
            float[] tp = nodePos(node, ctx);
            for (ResourceLocation prereq : node.prerequisites) {
                ResearchNode src = ctx.tree().getNode(prereq).orElse(null);
                if (src == null || !src.isVisible(ctx.unlocked())) continue;
                float[] sp = nodePos(src, ctx);
                drawEdge(g, sp[0], sp[1], tp[0], tp[1], ctx.isUnlocked(src.id));
            }
        }
    }

    private void drawEdge(GuiGraphics g, float x0, float y0, float x1, float y1, boolean active) {
        float dx = x1 - x0, dy = y1 - y0;
        float len = (float)Math.sqrt(dx * dx + dy * dy);
        if (len < 0.5f) return;
        int DOTS = Math.max(4, (int)(len / 7));
        for (int i = 0; i <= DOTS; i++) {
            float t  = (float)i / DOTS;
            float px = x0 + dx * t;
            float py = y0 + dy * t;
            float perp = (float)Math.sin(t * 3.14159f + pulseTimer * 0.45f) * 3.5f;
            px += -dy / len * perp;
            py +=  dx / len * perp;
            int a   = active ? 110 : 45;
            int col = active ? ((a << 24) | 0x4444BB) : ((a << 24) | 0x222244);
            g.fill((int)px - 1, (int)py - 1, (int)px + 2, (int)py + 2, col);
        }
    }

    // ── Nodes ─────────────────────────────────────────────────────────────────

    @Override
    public void renderNodes(GuiGraphics g, RenderContext ctx) {
        for (ResearchNode node : ctx.tree().getNodes()) {
            if (!node.isVisible(ctx.unlocked())) continue;
            float[] p = nodePos(node, ctx);
            drawNode(g, node, p[0], p[1], ctx);
        }
    }

    private void drawNode(GuiGraphics g, ResearchNode node, float fx, float fy, RenderContext ctx) {
        int cx = (int)fx, cy = (int)fy;
        boolean unlocked  = ctx.isUnlocked(node.id);
        boolean lockedOut = ctx.isLockedOut(node.id);
        boolean available = ctx.isAvailable(node);
        boolean sel       = ctx.isSelected(node);

        float pulse  = MotionClock.Signature.VOID.pulse(ctx.clock().getElapsed());
        float intense = ctx.intensity().get();

        // Outer glow halos (layered additive)
        if (unlocked || available) {
            for (int ring = 3; ring >= 1; ring--) {
                int r   = ring * 10;
                int ga  = (int)(22 * pulse * intense * (1f / ring));
                int gc  = unlocked ? ((ga << 24) | 0x2222CC) : ((ga << 24) | 0x113366);
                g.fill(cx - r, cy - r, cx + r, cy + r, gc);
            }
        }

        // Hexagonal body — 3 overlapping rects at 0°, 60°, 120°
        int bg  = lockedOut ? 0xFF080012 : unlocked ? 0xFF06061E : available ? 0xFF060818 : 0xFF030308;
        int brd = lockedOut ? 0xFF330055 : unlocked ? (sel ? 0xFFCCCCFF : 0xFF6666DD) : available ? 0xFF3344AA : 0xFF111133;
        int HW = 17, HH = 7;

        g.pose().pushPose();
        g.pose().translate(cx, cy, 0f);
        for (int r = 0; r < 3; r++) {
            g.fill(-HW, -HH, HW, HH, bg);
            g.pose().mulPose(new Quaternionf().rotationZ((float)Math.PI / 3f));
        }
        g.pose().popPose();

        // Hex border: 6 vertex marks + edge highlights
        int HR = 18;
        for (int vi = 0; vi < 6; vi++) {
            float va = vi * 6.28318f / 6f;
            int vx = cx + (int)(HR * Math.cos(va));
            int vy = cy + (int)(HR * Math.sin(va));
            g.fill(vx - 1, vy - 1, vx + 2, vy + 2, brd);
        }
        // Edge lines approximated as 3 rotated fills (thin)
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0f);
        for (int r = 0; r < 3; r++) {
            g.fill(-HR, -1, HR, 1, brd);
            g.pose().mulPose(new Quaternionf().rotationZ((float)Math.PI / 3f));
        }
        g.pose().popPose();

        // Selection pulse ring
        if (sel) {
            int sa = (int)(60 * pulse);
            g.fill(cx - HR - 4, cy - HR - 4, cx + HR + 4, cy + HR + 4, (sa << 24) | 0x8888FF);
            g.fill(cx - HR - 3, cy - HR - 3, cx + HR + 3, cy + HR + 3, 0xFF000010);
        }

        // Icon
        if (!node.hidden || unlocked) renderIcon(g, node.icon, cx - 8, cy - 8);

        if (node.isCommitmentNode) {
            g.fill(cx - 2, cy - HR - 7, cx + 2, cy - HR - 2, 0xFF5555CC);
        }
    }

    // ── Positioning ───────────────────────────────────────────────────────────

    @Override
    public float[] nodePos(ResearchNode node, RenderContext ctx) {
        // Lazily build stable ordering (alphabetical by id for determinism)
        if (cachedOrder.isEmpty()) {
            ctx.tree().getNodes().stream()
                .sorted(Comparator.comparing(n -> n.id.toString()))
                .forEach(n -> cachedOrder.add(n.id));
        }
        int idx   = cachedOrder.indexOf(node.id);
        if (idx < 0) idx = 0;
        int total = Math.max(1, cachedOrder.size());

        float angle = (float)idx / total * 6.28318f + ringAngle * 0.12f;
        float[] glitch = nodeGlitch.get(node.id);
        float gx = glitch != null ? glitch[0] : 0f;
        float gy = glitch != null ? glitch[1] : 0f;

        return new float[]{
            ORBIT_R * (float)Math.cos(angle) + gx,
            ORBIT_R * (float)Math.sin(angle) * (DISK_SQSH + 0.28f) + gy
        };
    }

    @Override
    public boolean hitsNode(ResearchNode node, float mx, float my, RenderContext ctx) {
        float[] p = nodePos(node, ctx);
        return Math.abs(mx - p[0]) <= 20 && Math.abs(my - p[1]) <= 20;
    }

    @Override
    public @Nullable ResourceLocation shaderLocation() {
        return new ResourceLocation(PhoenixCore.MOD_ID, "shaders/post/axiom_void.json");
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
