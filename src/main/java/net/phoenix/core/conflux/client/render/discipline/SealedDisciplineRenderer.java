package net.phoenix.core.conflux.client.render.discipline;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.conflux.client.render.*;
import net.phoenix.core.conflux.research.ResearchNode;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static net.phoenix.core.conflux.client.render.AxiomSprites.*;

/**
 * Sealed Discipline renderer — THE FORBIDDEN ARCHIVE.
 *
 * Identity: you are reading something you're not supposed to.
 *  - Background: procedural "classified document" — censored text bars,
 *    file headers, margin rules, case numbers
 *  - Nodes: wax seals — ring + emblem, crack-shard burst on unlock
 *  - Edges: dossier connecting lines with reference hash dots
 *  - Overlays: periodic "CLASSIFIED" stamp sweeps, red alert pulses
 *  - Locked nodes: seal is whole but opaque. Unlocked: seal cracked, content visible.
 *
 * Color palette: aged parchment dark, black redaction, crimson stamps, silver seal wax.
 */
public class SealedDisciplineRenderer implements DisciplineRenderer {

    private final String disciplineIdValue;

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final int   NODE_SEAL_R  = 22;
    private static final int   MAX_SHARDS   = 64;
    private static final int   DOC_LINES    = 32;   // background text lines
    private static final float STAMP_PERIOD = 12f;  // seconds between stamp sweeps

    // Palette
    private static final int C_BG          = 0xFF0E0C09;
    private static final int C_PAPER       = 0xFF1A1610;
    private static final int C_REDACT      = 0xFF050402;
    private static final int C_SEAL_WAX    = 0xFF8B7355;
    private static final int C_SEAL_CRACK  = 0xFFCCBB99;
    private static final int C_SEAL_DONE   = 0xFFDDBB88;
    private static final int C_SEAL_AVL    = 0xFF7A6640;
    private static final int C_SEAL_LOCK   = 0xFF3A3020;
    private static final int C_SEAL_EXCL   = 0xFF4A1010;
    private static final int C_SEAL_SEL    = 0xFFEEDDAA;
    private static final int C_STAMP       = 0xFFCC1111;
    private static final int C_LINE_HOT    = 0xFF887755;
    private static final int C_LINE_COLD   = 0xFF2A2218;
    private static final int C_ALERT_RED   = 0xFFCC0000;

    // ── Shard particles (on node unlock) ─────────────────────────────────────
    private static class Shard {
        float x, y, vx, vy, angle, angVel, life, maxLife, len;
    }
    private final Shard[] shards = new Shard[MAX_SHARDS];
    private int shardHead = 0;

    // ── Stamp overlay ─────────────────────────────────────────────────────────
    private float stampTimer = 3f;  // seconds until first stamp
    private float stampX = 0f, stampY = 0f, stampAngle = 0f;
    private float stampAge = 1f;    // 0=fresh, 1=hidden
    private int   stampType = 0;    // 0=CLASSIFIED, 1=REDACTED, 2=DENIED

    // ── Alert pulse ───────────────────────────────────────────────────────────
    private float alertPulse = 0f;

    // ── Crack state per unlocked node ─────────────────────────────────────────
    private final Map<ResourceLocation, Float> crackAge = new HashMap<>();
    // Pre-computed crack lines per node (deterministic)
    private final Map<ResourceLocation, int[][]> crackLines = new HashMap<>();

    // ── Edge growth ───────────────────────────────────────────────────────────
    private final Map<String, Float> edgeGrowth = new HashMap<>();

    private float time = 0f;
    private final Random rng = new Random();

    public SealedDisciplineRenderer(String disciplineId) {
        this.disciplineIdValue = disciplineId;
    }

    @Override public String disciplineId()             { return disciplineIdValue; }
    @Override public MotionClock.Signature signature() { return MotionClock.Signature.SEALED; }

    @Override
    public void onActivate(MotionClock clock) {
        crackAge.clear(); crackLines.clear(); edgeGrowth.clear();
        time = 0f; stampAge = 1f; stampTimer = 3f; alertPulse = 0f;
        Arrays.fill(shards, null); shardHead = 0;
    }

    @Override
    public void onUnlock(ResearchNode node, MotionClock clock, IntensityController intensity) {
        DisciplineRenderer.super.onUnlock(node, clock, intensity);
        float ox = node.posX*110f, oy = node.posY*110f;
        crackAge.put(node.id, 0f);
        buildCrackLines(node);
        // Shard burst
        for (int i = 0; i < 18; i++) spawnShard(ox, oy);
        alertPulse = 1f;
    }

    @Override
    public void tick(float dt, RenderContext ctx) {
        time += dt;

        // Edge growth
        for (ResearchNode n : ctx.tree().getNodes())
            for (ResourceLocation p : n.prerequisites)
                if (ctx.isUnlocked(p))
                    edgeGrowth.merge(p+"→"+n.id, dt*0.7f, (o,d)->Math.min(1f,o+d));

        // Crack age
        crackAge.replaceAll((id, age) -> Math.min(1f, age + dt*1.2f));

        // Shards
        for (int i = 0; i < MAX_SHARDS; i++) {
            Shard s = shards[i]; if (s==null) continue;
            s.x += s.vx*dt; s.y += s.vy*dt;
            s.vy += 40f*dt; // gravity
            s.angle += s.angVel*dt;
            s.life -= dt;
            if (s.life <= 0f) shards[i] = null;
        }

        // Stamp sweep
        stampTimer -= dt;
        if (stampTimer <= 0f && stampAge >= 1f) {
            stampTimer = STAMP_PERIOD + (float)(Math.random()*8f);
            stampX = 80f + (float)(Math.random()*300f);
            stampY = 60f + (float)(Math.random()*200f);
            stampAngle = (float)(Math.random()*0.5f - 0.25f);
            stampType = rng.nextInt(3);
            stampAge = 0f;
        }
        if (stampAge < 1f) stampAge = Math.min(1f, stampAge + dt*0.6f);

        // Alert decay
        alertPulse = Math.max(0f, alertPulse - dt*1.5f);
    }

    private void spawnShard(float ox, float oy) {
        Shard s = new Shard();
        double a = Math.random() * 6.28318;
        float spd = 30f + (float)(Math.random()*70f);
        s.x = ox; s.y = oy;
        s.vx = (float)(Math.cos(a)*spd); s.vy = (float)(Math.sin(a)*spd) - 20f;
        s.angle = (float)(Math.random()*6.28); s.angVel = (float)(Math.random()-0.5)*4f;
        s.len = 4f + (float)(Math.random()*10f);
        s.maxLife = s.life = 0.8f + (float)(Math.random()*0.7f);
        shards[shardHead % MAX_SHARDS] = s; shardHead++;
    }

    private void buildCrackLines(ResearchNode node) {
        long seed = node.id.hashCode();
        int NUM = 8;
        int[][] lines = new int[NUM][4]; // each: dx0,dy0, dx1,dy1 (offsets from center)
        Random cr = new Random(seed);
        for (int i = 0; i < NUM; i++) {
            double a = 2*Math.PI*i/NUM + cr.nextFloat()*0.6 - 0.3;
            float r = 6f + cr.nextFloat()*(NODE_SEAL_R-6f);
            float r2 = r + cr.nextFloat()*8f;
            lines[i][0] = (int)(Math.cos(a)*r*0.3f);
            lines[i][1] = (int)(Math.sin(a)*r*0.3f);
            lines[i][2] = (int)(Math.cos(a)*r2);
            lines[i][3] = (int)(Math.sin(a)*r2);
        }
        crackLines.put(node.id, lines);
    }

    // ── Background: classified document ───────────────────────────────────────

    @Override
    public void renderBackground(GuiGraphics g, RenderContext ctx) {
        int cw = ctx.canvasW(), ch = ctx.canvasH();

        g.fill(0, 0, cw, ch, C_BG);

        // Document background — pre-baked sprite (parchment, redactions, text stubs)
        blitFrame(g, SEALED_DOC, 0, 0, 0, cw, ch, 1f);

        // Alert pulse (dynamic — fires on unlock)
        if (alertPulse > 0.01f) {
            int aa = (int)(alertPulse*alertPulse*80);
            g.fill(0, 0, cw, ch, (aa<<24)|(C_ALERT_RED&0xFFFFFF));
            int ba = (int)(alertPulse*120);
            g.fill(0, 0, cw, 3, (ba<<24)|(C_ALERT_RED&0xFFFFFF));
            g.fill(0, ch-3, cw, ch, (ba<<24)|(C_ALERT_RED&0xFFFFFF));
            g.fill(0, 0, 3, ch, (ba<<24)|(C_ALERT_RED&0xFFFFFF));
            g.fill(cw-3, 0, cw, ch, (ba<<24)|(C_ALERT_RED&0xFFFFFF));
        }

        // Stamp overlay (dynamic — random sweep)
        if (stampAge < 0.98f) renderStamp(g, cw, ch);
    }

    private void renderStamp(GuiGraphics g, int cw, int ch) {
        // Stamp fades in fast, stays, fades slowly
        float t = stampAge;
        float alpha = t < 0.1f ? t*10f : t < 0.6f ? 1f : 1f-(t-0.6f)/0.4f;
        int a = (int)(alpha*180);
        if (a < 2) return;

        String[] labels = {"CLASSIFIED", "REDACTED", "DENIED"};
        String label = labels[stampType];

        int sw = label.length() * 6 + 16;
        int sh = 20;
        int sx = (int)stampX - sw/2, sy = (int)stampY - sh/2;

        // Outer rectangle border
        int sc = (a<<24)|(C_STAMP&0xFFFFFF);
        g.fill(sx, sy, sx+sw, sy+2, sc);
        g.fill(sx, sy+sh-2, sx+sw, sy+sh, sc);
        g.fill(sx, sy, sx+2, sy+sh, sc);
        g.fill(sx+sw-2, sy, sx+sw, sy+sh, sc);
        // Inner border
        g.fill(sx+3, sy+3, sx+sw-3, sy+5, (a/3<<24)|(C_STAMP&0xFFFFFF));
        g.fill(sx+3, sy+sh-5, sx+sw-3, sy+sh-3, (a/3<<24)|(C_STAMP&0xFFFFFF));
        // Label
        int textColor = (a<<24)|(C_STAMP&0xFFFFFF);
        g.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font,
            label, (int)stampX, (int)stampY-4, textColor);
    }

    // ── Edges: dossier lines with case dots ───────────────────────────────────

    @Override
    public void renderEdges(GuiGraphics g, RenderContext ctx) {
        for (ResearchNode node : ctx.tree().getNodes()) {
            if (!node.isVisible(ctx.unlocked())) continue;
            float[] tp = nodePos(node, ctx);
            for (ResourceLocation p : node.prerequisites) {
                ResearchNode src = ctx.tree().getNode(p).orElse(null);
                if (src==null||!src.isVisible(ctx.unlocked())) continue;
                float[] sp = nodePos(src, ctx);
                float growth = edgeGrowth.getOrDefault(p+"→"+node.id, 0f);
                drawDossierLine(g, sp[0],sp[1], tp[0],tp[1], ctx.isUnlocked(p), growth, (long)(p.hashCode()*31L+node.id.hashCode()), false);
            }
            for (ResourceLocation p : node.prerequisitesAny) {
                ResearchNode src = ctx.tree().getNode(p).orElse(null);
                if (src==null||!src.isVisible(ctx.unlocked())) continue;
                float[] sp = nodePos(src, ctx);
                float growth = edgeGrowth.getOrDefault(p+"→"+node.id, 0f);
                drawDossierLine(g, sp[0],sp[1], tp[0],tp[1], ctx.isUnlocked(p), growth, (long)(p.hashCode()*37L+node.id.hashCode()), true);
            }
        }
    }

    private void drawDossierLine(GuiGraphics g, float x1, float y1, float x2, float y2,
                                  boolean hot, float growth, long seed, boolean any) {
        int col = hot ? C_LINE_HOT : C_LINE_COLD;
        if (any) col = MotionClock.lerpColor(col, 0xFF442222, 0.5f);
        float midY = (y1+y2)*0.5f;

        // Three-segment right-angle route: V then H then V
        float[] pts = {x1,y1, x1,midY, x2,midY, x2,y2};
        float totalLen = Math.abs(y1-midY)+Math.abs(x1-x2)+Math.abs(midY-y2);
        float drawLen = totalLen*growth;
        float walked = 0f;
        for (int seg = 0; seg < 3; seg++) {
            float sx=pts[seg*2],sy=pts[seg*2+1],ex=pts[seg*2+2],ey=pts[seg*2+3];
            float segLen = (float)Math.sqrt((ex-sx)*(ex-sx)+(ey-sy)*(ey-sy));
            float segDraw = Math.min(segLen, drawLen-walked);
            if (segDraw <= 0f) break;
            float t = segDraw/segLen;
            drawThickLine(g,(int)sx,(int)sy,(int)(sx+(ex-sx)*t),(int)(sy+(ey-sy)*t),col);
            walked += segLen;
        }

        // Reference hash dots along the midpoint
        if (growth > 0.4f) {
            int dotA = hot ? 0xCC887755 : 0x44443322;
            for (int i = 0; i < 3; i++) {
                long dh = seed ^ (i*6364136223846793005L);
                float dx = x1 + (x2-x1)*(0.25f+i*0.25f);
                float dy = y1 + (y2-y1)*(0.25f+i*0.25f);
                g.fill((int)dx-2,(int)dy-2,(int)dx+2,(int)dy+2,dotA);
            }
        }
    }

    private static void drawThickLine(GuiGraphics g, int x1, int y1, int x2, int y2, int col) {
        if (x1==x2) {
            g.fill(x1-1,Math.min(y1,y2),x1+2,Math.max(y1,y2),col);
        } else {
            g.fill(Math.min(x1,x2),y1-1,Math.max(x1,x2),y1+2,col);
        }
    }

    // ── Nodes: wax seal sigils ────────────────────────────────────────────────

    @Override
    public void renderNodes(GuiGraphics g, RenderContext ctx) {
        for (ResearchNode node : ctx.tree().getNodes()) {
            if (!node.isVisible(ctx.unlocked())) continue;
            float[] p = nodePos(node, ctx);
            drawSealNode(g, node, p[0], p[1], ctx);
        }
    }

    private void drawSealNode(GuiGraphics g, ResearchNode node, float fx, float fy, RenderContext ctx) {
        int cx = (int) fx, cy = (int) fy;
        boolean unlocked = ctx.isUnlocked(node.id);
        boolean lockedOut = ctx.isLockedOut(node.id);
        boolean available = ctx.isAvailable(node);
        boolean sel = ctx.isSelected(node);
        boolean mystery = node.hidden && !unlocked;

        int waxCol = lockedOut ? C_SEAL_EXCL : mystery ? C_SEAL_LOCK
                : unlocked ? C_SEAL_DONE : available ? C_SEAL_AVL : C_SEAL_LOCK;
        if (sel) waxCol = C_SEAL_SEL;

        float breathe = 0.5f + 0.5f * (float) Math.sin(time * 1.2f + fx * 0.01f);
        float crk = crackAge.getOrDefault(node.id, unlocked ? 1f : 0f);

        // ── Wax disk ─────────────────────────────────────────────────────────
        int r = NODE_SEAL_R;
        // Fill as scanlined circle
        for (int dy = -r; dy <= r; dy++) {
            float hw = r * (float) Math.sqrt(Math.max(0, 1f - (float) dy / r * (float) dy / r));
            g.fill((int) (cx - hw), cy + dy, (int) (cx + hw), cy + dy + 1, waxCol);
        }

        // ── Seal ring embossing (concentric rings) ────────────────────────────
        for (int ri = 1; ri <= 3; ri++) {
            float rr = r - ri * 5.5f;
            if (rr < 3) break;
            int rSteps = (int) (rr * 6.28318f / 2) + 1;
            for (int s = 0; s < rSteps; s++) {
                double a = 2 * Math.PI * s / rSteps;
                int rx = cx + (int) (rr * Math.cos(a)), ry = cy + (int) (rr * Math.sin(a));
                int ra = (int) (30 + 15 * breathe);
                g.fill(rx, ry, rx + 1, ry + 1, (ra << 24) | 0x000000);
            }
        }

        // ── Star emblem (5-pointed, reduced) ─────────────────────────────────
        float starR = r * 0.45f;
        for (int si = 0; si < 5; si++) {
            double a1 = 2 * Math.PI * si / 5 - Math.PI / 2;
            double a2 = 2 * Math.PI * (si + 0.5) / 5 - Math.PI / 2;
            int ox1 = (int) (starR * Math.cos(a1)), oy1 = (int) (starR * Math.sin(a1));
            int ox2 = (int) (starR * 0.42 * Math.cos(a2)), oy2 = (int) (starR * 0.42 * Math.sin(a2));
            drawThickLine(g, cx + ox1, cy + oy1, cx + ox2, cy + oy2, 0xFF000000);
            int ni = (si + 1) % 5;
            double a3 = 2 * Math.PI * ni / 5 - Math.PI / 2;
            int ox3 = (int) (starR * Math.cos(a3)), oy3 = (int) (starR * Math.sin(a3));
            drawThickLine(g, cx + ox2, cy + oy2, cx + ox3, cy + oy3, 0xFF000000);
        }

        // ── Crack lines (on unlock) ───────────────────────────────────────────
        if (crk > 0.01f) {
            if (!crackLines.containsKey(node.id)) buildCrackLines(node);
            int[][] cl = crackLines.get(node.id);
            {
                if (cl != null) {
                    for (int ci = 0; ci < cl.length; ci++) {
                        float progress = Math.min(1f, crk * cl.length - ci);
                        if (progress <= 0f) break;
                        int ex = cl[ci][0] + (int) ((cl[ci][2] - cl[ci][0]) * progress);
                        int ey = cl[ci][1] + (int) ((cl[ci][3] - cl[ci][1]) * progress);
                        drawThickLine(g, cx + cl[ci][0], cy + cl[ci][1], cx + ex, cy + ey, 0xFF000000);
                        // Highlight edge
                        g.fill(cx + ex - 1, cy + ey - 1, cx + ex + 2, cy + ey + 2, C_SEAL_CRACK);
                    }
                }
            }

            // ── Unlocked: icon visible through crack ──────────────────────────────
            if (unlocked && !mystery && !lockedOut && crk > 0.5f) {
                int iconA = (int) ((crk - 0.5f) * 2f * 255);
                // Dim overlay strips (scan lines showing redacted info peeking through)
                for (int row = cy - r + 4; row < cy + r - 4; row += 3) {
                    g.fill(cx - r + 4, row, cx + r - 4, row + 1, ((iconA / 8) << 24) | 0x886644);
                }
                renderIcon(g, node.icon, cx - 8, cy - 8);
            }

            // ── Mystery ───────────────────────────────────────────────────────────
            if (mystery) {
                // Redaction bar across the seal
                g.fill(cx - r + 4, cy - 3, cx + r - 4, cy + 3, C_REDACT);
                g.fill(cx - r + 4, cy + 6, cx + r / 2, cy + 9, C_REDACT);
            }

            // ── Commitment marker: wax drip notch at top ──────────────────────────
            if (node.isCommitmentNode) {
                g.fill(cx - 2, cy - r - 6, cx + 3, cy - r - 1, waxCol);
                g.fill(cx - 1, cy - r - 10, cx + 2, cy - r - 6, waxCol);
            }

            // ── Selection: dossier bracket corners ───────────────────────────────
            if (sel) {
                int br = r + 6;
                int ba = (int) (150 * (0.5f + 0.5f * (float) Math.sin(time * 4f)));
                int sc = (ba << 24) | (C_SEAL_SEL & 0xFFFFFF);
                g.fill(cx - br, cy - br, cx - br + 6, cy - br + 1, sc);
                g.fill(cx - br, cy - br, cx - br + 1, cy - br + 6, sc);
                g.fill(cx + br - 6, cy - br, cx + br, cy - br + 1, sc);
                g.fill(cx + br - 1, cy - br, cx + br, cy - br + 6, sc);
                g.fill(cx - br, cy + br - 1, cx - br + 6, cy + br, sc);
                g.fill(cx - br, cy + br - 6, cx - br + 1, cy + br, sc);
                g.fill(cx + br - 6, cy + br - 1, cx + br, cy + br, sc);
                g.fill(cx + br - 1, cy + br - 6, cx + br, cy + br, sc);
            }
        }
    }

    // ── Foreground: shards ────────────────────────────────────────────────────

    @Override
    public void renderForeground(GuiGraphics g, RenderContext ctx) {
        for (Shard s : shards) {
            if (s==null) continue;
            float frac = s.life/s.maxLife;
            int alpha = (int)(frac*200);
            int bx = (int)s.x, by = (int)s.y;
            // Draw shard as a short thick rotated line (approximate with 2 fills)
            float ca = (float)Math.cos(s.angle), sa2 = (float)Math.sin(s.angle);
            int ex = bx+(int)(ca*s.len), ey = by+(int)(sa2*s.len);
            drawThickLine(g, bx, by, ex, ey, (alpha<<24)|(C_SEAL_WAX&0xFFFFFF));
            // Highlight edge
            g.fill(ex-1,ey-1,ex+2,ey+2,((alpha/2)<<24)|(C_SEAL_CRACK&0xFFFFFF));
        }
    }

    // ── Positioning ───────────────────────────────────────────────────────────

    @Override
    public float[] nodePos(ResearchNode node, RenderContext ctx) {
        return new float[]{ node.posX*110f, node.posY*110f };
    }

    @Override
    public boolean hitsNode(ResearchNode node, float mx, float my, RenderContext ctx) {
        float[] p = nodePos(node, ctx);
        float dx = mx-p[0], dy = my-p[1];
        return dx*dx+dy*dy <= (NODE_SEAL_R+4f)*(NODE_SEAL_R+4f);
    }

    @Override
    public @Nullable ResourceLocation shaderLocation() {
        return new ResourceLocation(PhoenixCore.MOD_ID, "shaders/post/axiom_sealed.json");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void renderIcon(GuiGraphics g, String iconId, int x, int y) {
        if (iconId==null||iconId.isEmpty()) return;
        ResourceLocation rl = ResourceLocation.tryParse(iconId);
        if (rl==null) return;
        var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(rl);
        if (item!=null&&item!=net.minecraft.world.item.Items.AIR)
            g.renderItem(new net.minecraft.world.item.ItemStack(item),x,y);
    }
}
