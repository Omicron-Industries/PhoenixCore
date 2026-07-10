package net.phoenix.core.conflux.client.render.discipline;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.conflux.client.render.*;
import net.phoenix.core.conflux.research.ResearchNode;
import org.jetbrains.annotations.Nullable;

import static net.phoenix.core.conflux.client.render.AxiomSprites.*;

import java.util.*;

/**
 * Sculk Discipline — The Living Network.
 *
 * Unique identity: biological horror, not cosmic horror (that's Void).
 *  - Background covered in procedural bioluminescent veins
 *  - 7 sculk-sensor "eyes" track the cursor and blink
 *  - Nodes are mushroom fruiting bodies: stem + domed cap + glowing underside
 *  - Drip particles fall downward from unlocked nodes
 *  - Spores float upward on unlock bursts
 *  - Color: cyan-green + deep purple accents (alien biology)
 */
public class SculkDisciplineRenderer implements DisciplineRenderer {

    private static final int   MAX_DRIPS   = 80;
    private static final int   MAX_SPORES  = 60;
    private static final int   MAX_RIPPLES = 4;
    private static final int   EYE_COUNT   = 7;

    // Palette
    private static final int C_BG        = 0xFF000A06;
    private static final int C_VEIN_HOT  = 0xFF00BB77;
    private static final int C_VEIN_COLD = 0xFF002A14;
    private static final int C_BORD_DONE = 0xFF00CC88;
    private static final int C_BORD_AVL  = 0xFF006644;
    private static final int C_BORD_LOCK = 0xFF002211;
    private static final int C_BORD_EXCL = 0xFF330011;
    private static final int C_BORD_SEL  = 0xFF88FFCC;
    private static final int C_PURPLE    = 0xFF440066;
    private static final int C_GLOW      = 0xFF00FF99;

    // ── Drip (falls down from nodes) ──────────────────────────────────────────
    private static class Drip {
        float x, y, vy, life, maxLife, size;
        boolean splashed;
        float splashR;
    }
    private final Drip[] drips = new Drip[MAX_DRIPS];
    private int dripHead = 0;

    // ── Spore (floats up on unlock bursts) ───────────────────────────────────
    private static class Spore {
        float x, y, vx, vy, life, maxLife, size;
    }
    private final Spore[] spores = new Spore[MAX_SPORES];
    private int sporeHead = 0;

    // ── Sensor eyes ───────────────────────────────────────────────────────────
    private static class Eye {
        float x, y;           // canvas-space position (set once from hash)
        float irisX, irisY;   // current iris position (tracks cursor)
        float blinkTimer;     // countdown to next blink
        float blinkDuration;  // how long blink lasts
        boolean blinking;
    }
    private final Eye[] eyes = new Eye[EYE_COUNT];
    private int eyeW = 0, eyeH = 0;

    // ── Ripple buffer ─────────────────────────────────────────────────────────
    private final float[] rippleX   = new float[MAX_RIPPLES];
    private final float[] rippleY   = new float[MAX_RIPPLES];
    private final float[] rippleAge = new float[MAX_RIPPLES];
    private int rippleHead    = 0;
    private int activeRipples = 0;

    // ── Edge growth ───────────────────────────────────────────────────────────
    private final Map<String, Float> edgeGrowth = new HashMap<>();

    // vein network is now a pre-baked sprite — no Vein list needed

    private float breatheTime = 0f;

    @Override public @Nullable String disciplineId() { return "sculk"; }
    @Override public MotionClock.Signature signature() { return MotionClock.Signature.SCULK; }

    @Override
    public void onActivate(MotionClock clock) {
        edgeGrowth.clear();
        Arrays.fill(rippleAge, 1f);
        activeRipples = 0;
        eyeW = eyeH = 0;
        breatheTime = 0f;
        Arrays.fill(eyes, null);
        Arrays.fill(drips, null);
        Arrays.fill(spores, null);
        dripHead = sporeHead = 0;
    }

    @Override
    public void onUnlock(ResearchNode node, MotionClock clock, IntensityController intensity) {
        DisciplineRenderer.super.onUnlock(node, clock, intensity);
        float ox = node.posX * 110f, oy = node.posY * 110f;
        // Spore burst upward
        for (int i = 0; i < 16; i++) spawnSpore(ox, oy);
        // Drip burst downward
        for (int i = 0; i < 8; i++) spawnDrip(ox, oy);
        // Ripple
        int slot = rippleHead % MAX_RIPPLES;
        rippleX[slot] = ox; rippleY[slot] = oy; rippleAge[slot] = 0f;
        rippleHead++; activeRipples = Math.min(activeRipples + 1, MAX_RIPPLES);
    }

    @Override
    public void tick(float dt, RenderContext ctx) {
        breatheTime += dt;

        // Edge growth
        for (ResearchNode n : ctx.tree().getNodes())
            for (ResourceLocation prereq : n.prerequisites)
                if (ctx.isUnlocked(prereq))
                    edgeGrowth.merge(prereq + "→" + n.id, dt*0.55f, (o,d) -> Math.min(1f,o+d));

        // Passive drips from unlocked nodes
        if (Math.random() < dt * 5f)
            for (ResearchNode n : ctx.tree().getNodes())
                if (ctx.isUnlocked(n.id) && Math.random() < 0.2f) {
                    spawnDrip(n.posX*110f, n.posY*110f); break;
                }

        // Tick drips
        for (int i = 0; i < MAX_DRIPS; i++) {
            Drip d = drips[i];
            if (d == null) continue;
            if (!d.splashed) {
                d.y += d.vy * dt;
                d.vy = Math.min(d.vy + 60f*dt, 90f); // gravity
                d.life -= dt;
                if (d.life <= 0) drips[i] = null;
            } else {
                d.splashR += 20f * dt;
                d.life -= dt * 3f;
                if (d.life <= 0) drips[i] = null;
            }
        }

        // Tick spores
        for (int i = 0; i < MAX_SPORES; i++) {
            Spore s = spores[i];
            if (s == null) continue;
            s.x += s.vx*dt; s.y += s.vy*dt;
            s.vx += (float)(Math.random()-0.5)*6f*dt;
            s.life -= dt;
            if (s.life <= 0) spores[i] = null;
        }

        // Tick ripples
        int alive = 0;
        for (int i = 0; i < MAX_RIPPLES; i++) {
            if (rippleAge[i] < 1f) { rippleAge[i] = Math.min(1f, rippleAge[i]+dt*0.65f); alive++; }
        }
        activeRipples = alive;

        // Tick eyes — track cursor, blink
        float cursorCX = ctx.canvasMx(), cursorCY = ctx.canvasMy();
        for (Eye eye : eyes) {
            if (eye == null) continue;
            // Move iris toward cursor (lazy tracking)
            float toX = cursorCX - eye.x, toY = cursorCY - eye.y;
            float dist = (float)Math.sqrt(toX*toX + toY*toY);
            float maxIris = 5f;
            float targetX = dist > 0.01f ? eye.x + toX/dist * Math.min(maxIris, dist*0.3f) : eye.x;
            float targetY = dist > 0.01f ? eye.y + toY/dist * Math.min(maxIris, dist*0.3f) : eye.y;
            eye.irisX = eye.irisX * 0.85f + targetX * 0.15f;
            eye.irisY = eye.irisY * 0.85f + targetY * 0.15f;
            // Blink
            eye.blinkTimer -= dt;
            if (eye.blinkTimer <= 0f && !eye.blinking) {
                eye.blinking = true;
                eye.blinkDuration = 0.12f;
                eye.blinkTimer = 0.12f;
            }
            if (eye.blinking) {
                eye.blinkDuration -= dt;
                if (eye.blinkDuration <= 0f) { eye.blinking = false; eye.blinkTimer = 2.5f + (float)(Math.random()*4f); }
            }
        }
    }

    private void spawnDrip(float ox, float oy) {
        Drip d = new Drip();
        d.x = ox + (float)(Math.random()-0.5)*12f;
        d.y = oy;
        d.vy = 15f + (float)(Math.random()*25f);
        d.size = 1.5f + (float)(Math.random()*2f);
        d.maxLife = d.life = 1.2f + (float)(Math.random()*1.5f);
        d.splashed = false; d.splashR = 0f;
        drips[dripHead % MAX_DRIPS] = d; dripHead++;
    }

    private void spawnSpore(float ox, float oy) {
        Spore s = new Spore();
        s.x = ox + (float)(Math.random()-0.5)*18f;
        s.y = oy + (float)(Math.random()-0.5)*10f;
        s.vx = (float)(Math.random()-0.5)*16f;
        s.vy = -(float)(Math.random()*22f + 8f);
        s.size = 1f + (float)(Math.random()*2f);
        s.maxLife = s.life = 1.8f + (float)(Math.random()*2f);
        spores[sporeHead % MAX_SPORES] = s; sporeHead++;
    }

    // ── Background: vein network + sensor eyes ────────────────────────────────

    @Override
    public void renderBackground(GuiGraphics g, RenderContext ctx) {
        int cw = ctx.canvasW(), ch = ctx.canvasH();
        if (eyeW != cw || eyeH != ch) buildEyes(cw, ch);

        float breathe = 0.5f + 0.5f * (float)Math.sin(breatheTime * 1.8f);
        float intense = ctx.intensity().get();

        g.fill(0, 0, cw, ch, C_BG);

        // Stencil mask
        float[] mask = net.phoenix.core.conflux.client.render.CanvasMask.sculkEdge(0, 0, cw, ch);
        net.phoenix.core.conflux.client.render.CanvasMask.begin();
        net.phoenix.core.conflux.client.render.CanvasMask.writeMask(mask);
        net.phoenix.core.conflux.client.render.CanvasMask.enableTest();

        // Breathing centre glow
        g.fill(cw/4, ch/4, cw*3/4, ch*3/4, ((int)(8*breathe*intense)<<24)|0x003322);

        // Vein network — pre-baked sprite, alpha breathes with the bioluminescent pulse
        float veinAlpha = (0.55f + 0.45f * breathe) * intense;
        blitFrame(g, SCULK_VEINS, 0, 0, 0, cw, ch, veinAlpha);

        // Sensor eyes (still procedural — they track cursor)
        for (Eye eye : eyes) {
            if (eye == null) continue;
            drawEye(g, eye, breathe, intense);
        }

        net.phoenix.core.conflux.client.render.CanvasMask.end();
    }

    private void buildEyes(int cw, int ch) {
        eyeW = cw; eyeH = ch;
        for (int i = 0; i < EYE_COUNT; i++) {
            long h = (i*6364136223846793005L+1442695040888963407L)^(i*7919L+99991L);
            Eye eye = new Eye();
            eye.x = (float)(h & 0xFFFFL)/65535f * cw;
            eye.y = (float)((h>>16) & 0xFFFFL)/65535f * ch;
            eye.irisX = eye.x; eye.irisY = eye.y;
            eye.blinkTimer = i * 0.7f + 1f;
            eye.blinkDuration = 0f;
            eye.blinking = false;
            eyes[i] = eye;
        }
    }

    private void drawEye(GuiGraphics g, Eye eye, float breathe, float intense) {
        int ex = (int)eye.x, ey = (int)eye.y;
        // Eye socket — dark oval
        int sockR = 9;
        for (float dy = -sockR; dy <= sockR; dy++) {
            float hw = sockR * (float)Math.sqrt(Math.max(0,1-(dy/sockR)*(dy/sockR)));
            g.fill((int)(ex-hw), (int)(ey+dy), (int)(ex+hw), (int)(ey+dy)+1, 0xFF000A04);
        }
        // Outer ring
        int ringA = (int)(60*breathe*intense);
        g.fill(ex-sockR-1, ey-3, ex+sockR+1, ey+3, (ringA<<24)|0x004422);
        g.fill(ex-3, ey-sockR-1, ex+3, ey+sockR+1, (ringA<<24)|0x004422);

        if (!eye.blinking) {
            // Iris (tracks cursor)
            int ix = (int)eye.irisX, iy = (int)eye.irisY;
            int irisA = (int)(220*breathe);
            g.fill(ix-2, iy-2, ix+3, iy+3, (irisA<<24)|0x00FFAA);
            // Sclera glow
            g.fill(ix-4, iy-4, ix+5, iy+5, ((irisA>>3)<<24)|0x004433);
        } else {
            // Blink: horizontal fill covers the eye
            int blinkA = (int)(180*breathe);
            g.fill(ex-sockR, ey-2, ex+sockR, ey+2, (blinkA<<24)|0x001A0C);
        }
    }

    // ── Edges: growing sculk veins ────────────────────────────────────────────

    @Override
    public void renderEdges(GuiGraphics g, RenderContext ctx) {
        for (ResearchNode node : ctx.tree().getNodes()) {
            if (!node.isVisible(ctx.unlocked())) continue;
            float[] tp = nodePos(node, ctx);
            for (ResourceLocation prereq : node.prerequisites) {
                ResearchNode src = ctx.tree().getNode(prereq).orElse(null);
                if (src==null||!src.isVisible(ctx.unlocked())) continue;
                float[] sp = nodePos(src, ctx);
                float growth = edgeGrowth.getOrDefault(prereq+"→"+node.id, 0f);
                drawEdgeVein(g, sp[0],sp[1], tp[0],tp[1], growth, node.id.hashCode()^prereq.hashCode());
            }
            for (ResourceLocation anyId : node.prerequisitesAny) {
                ResearchNode src = ctx.tree().getNode(anyId).orElse(null);
                if (src==null||!src.isVisible(ctx.unlocked())) continue;
                float[] sp = nodePos(src, ctx);
                float growth = edgeGrowth.getOrDefault(anyId+"→"+node.id, 0f);
                drawEdgeVein(g, sp[0],sp[1], tp[0],tp[1], growth, node.id.hashCode()^anyId.hashCode());
            }
        }
    }

    private void drawEdgeVein(GuiGraphics g, float x0,float y0, float x1,float y1, float growth, int seed) {
        long h = ((long)seed * 6364136223846793005L + 1442695040888963407L);
        float span = Math.max(0.01f, (float)Math.sqrt((x1-x0)*(x1-x0)+(y1-y0)*(y1-y0)));
        float cpOff = 30f + (h&0x1FL)*1.5f;
        float perpX = -(y1-y0)/span*cpOff, perpY = (x1-x0)/span*cpOff;
        float cx0=x0+(x1-x0)*0.35f+perpX, cy0=y0+(y1-y0)*0.35f+perpY;
        float cx1=x0+(x1-x0)*0.65f-perpX*0.5f, cy1=y0+(y1-y0)*0.65f-perpY*0.5f;
        float breathe = 0.5f+0.5f*(float)Math.sin(breatheTime*1.8f+x0*0.02f);
        int STEPS = Math.max(8,(int)(span/6));
        int drawTo = (int)(STEPS*growth);
        for (int i = 0; i <= drawTo; i++) {
            float t=i/(float)STEPS, u=1-t;
            float px=u*u*u*x0+3*u*u*t*cx0+3*u*t*t*cx1+t*t*t*x1;
            float py=u*u*u*y0+3*u*u*t*cy0+3*u*t*t*cy1+t*t*t*y1;
            int a = (int)((50+45*breathe)*(1-t));
            g.fill((int)px-1,(int)py-1,(int)px+2,(int)py+2, lerpCol(C_VEIN_HOT,C_VEIN_COLD,t,a));
        }
        // Dead-end tendrils
        for (int ti = 1; ti <= 2; ti++) {
            float tb = ti*0.33f;
            if (tb > growth) break;
            float u=1-tb;
            float px=u*u*u*x0+3*u*u*tb*cx0+3*u*tb*tb*cx1+tb*tb*tb*x1;
            float py=u*u*u*y0+3*u*u*tb*cy0+3*u*tb*tb*cy1+tb*tb*tb*y1;
            long th=h^(ti*31337L);
            float ta=(th&0xFFL)/255f*6.28318f, tl=10f+(th>>8&0xFL)*1.5f;
            for (float ft=0; ft<tl; ft+=2f) {
                int fa=(int)(22*(1-ft/tl));
                g.fill((int)(px+ft*Math.cos(ta))-1,(int)(py+ft*Math.sin(ta))-1,
                       (int)(px+ft*Math.cos(ta))+2,(int)(py+ft*Math.sin(ta))+2,(fa<<24)|0x002A1A);
            }
        }
    }

    // ── Nodes: mushroom fruiting bodies ───────────────────────────────────────

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
        boolean mystery   = node.hidden && !unlocked;

        float breathe = 0.5f+0.5f*(float)Math.sin(breatheTime*1.8f+fx*0.015f);
        float intense  = ctx.intensity().get();

        int brd = lockedOut ? C_BORD_EXCL : mystery ? C_BORD_LOCK : unlocked ? C_BORD_DONE : available ? C_BORD_AVL : C_BORD_LOCK;
        if (sel) brd = C_BORD_SEL;

        // Outer ambient glow
        if ((unlocked||available)&&!lockedOut) {
            for (int hr = 3; hr >= 1; hr--) {
                int hrad = hr*16, ha=(int)(12*breathe*intense/hr);
                g.fill(cx-hrad, cy-hrad, cx+hrad, cy+hrad, (ha<<24)|(C_BORD_DONE&0xFFFFFF));
            }
        }

        // ── Stem (below cap centre) ───────────────────────────────────────────
        int stemW = 7, stemH = 14;
        int stemBg = lockedOut ? 0xFF060202 : unlocked ? 0xFF04120A : 0xFF030A06;
        g.fill(cx-stemW, cy, cx+stemW, cy+stemH, stemBg);
        g.fill(cx-stemW, cy, cx-stemW+1, cy+stemH, brd);
        g.fill(cx+stemW-1, cy, cx+stemW, cy+stemH, brd);
        g.fill(cx-stemW, cy+stemH, cx+stemW, cy+stemH+1, brd);

        // ── Cap (semicircle above cx,cy) ──────────────────────────────────────
        int capH = 22, capW = 28;
        int capBg = lockedOut ? 0xFF0A0404 : unlocked ? 0xFF063020 : available ? 0xFF042818 : 0xFF021508;
        // Draw cap as horizontal scanlines (semicircle)
        for (int dy = 0; dy >= -capH; dy--) {
            float t = (float)(-dy) / capH; // 0=base of cap, 1=top
            float hw = capW * (float)Math.sqrt(1f - t*t); // semicircle width
            g.fill((int)(cx-hw), cy+dy, (int)(cx+hw), cy+dy+1, capBg);
        }
        // Cap border ring
        int capBorderSteps = 28;
        for (int i = 0; i <= capBorderSteps; i++) {
            float a = (float)i/capBorderSteps * 3.14159f; // semicircle (top half only)
            int bx = cx+(int)(capW*Math.cos(a));
            int by = cy-(int)(capH*Math.sin(a));
            g.fill(bx-1,by-1,bx+2,by+2,brd);
        }
        // Base of cap (flat underside)
        g.fill(cx-capW, cy, cx+capW, cy+1, brd);

        // ── Bioluminescent underside glow ─────────────────────────────────────
        if ((unlocked||available)&&!lockedOut) {
            for (int ug = 1; ug <= 6; ug++) {
                float ut = ug/6f;
                float hw = capW*(1-ut*0.7f);
                int ua = (int)(45*breathe*intense*(1-ut));
                g.fill((int)(cx-hw), cy+ug, (int)(cx+hw), cy+ug+1, (ua<<24)|(C_GLOW&0xFFFFFF));
            }
        }

        // ── Spots on cap ──────────────────────────────────────────────────────
        for (int si = 0; si < 4; si++) {
            long sh = (si*6364136223846793005L) ^ (long)node.id.hashCode();
            float sa = ((sh&0xFFL)/255f)*2.8f + 0.2f; // angle on cap (not full 180°)
            float sr = 0.4f + ((sh>>8&0xFL)/15f)*0.45f; // radial pos (0=centre, 0.9=edge)
            int sx = cx+(int)(capW*sr*Math.cos(sa));
            int sy = cy-(int)(capH*sr*Math.sin(sa));
            int sA = (int)(100*breathe);
            g.fill(sx-2, sy-2, sx+3, sy+3, (sA<<24)|(brd&0xFFFFFF));
        }

        // Commitment marker
        if (node.isCommitmentNode) g.fill(cx-3, cy-capH-8, cx+3, cy-capH-2, C_BORD_DONE);

        // Icon (in stem area)
        if (!mystery&&!lockedOut) renderIcon(g, node.icon, cx-8, cy+2);

        // Mystery label
        if (mystery) g.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, "?", cx, cy-capH/2-4, 0x88006644);

        // Selection ring
        if (sel) {
            int sa = (int)(80*breathe);
            g.fill(cx-capW-5, cy-capH-5, cx+capW+5, cy+stemH+5, (sa<<24)|0x44FFCC);
            g.fill(cx-capW-4, cy-capH-4, cx+capW+4, cy+stemH+4, 0xFF000A06);
        }
    }

    // ── Foreground: drips + spores ────────────────────────────────────────────

    @Override
    public void renderForeground(GuiGraphics g, RenderContext ctx) {
        // Drips (fall down)
        for (Drip d : drips) {
            if (d==null) continue;
            float frac = d.life/d.maxLife;
            int alpha = (int)(frac*200);
            if (!d.splashed) {
                int sz = Math.max(1,(int)(d.size*frac));
                // Teardrop: taller than wide
                g.fill((int)d.x-1,(int)d.y-sz*2,(int)d.x+2,(int)d.y+sz,(alpha<<24)|0x00AA66);
            } else {
                // Splash ring
                int sr = (int)d.splashR;
                int sa = (int)(frac*80);
                g.fill((int)d.x-sr,(int)d.y-2,(int)d.x+sr,(int)d.y+2,(sa<<24)|0x00CC77);
            }
        }
        // Spores (float up)
        for (Spore s : spores) {
            if (s==null) continue;
            float frac = s.life/s.maxLife;
            int alpha = (int)(frac*180);
            int sz = Math.max(1,(int)(s.size*frac));
            g.fill((int)s.x-sz,(int)s.y-sz,(int)s.x+sz,(int)s.y+sz,(alpha<<24)|0x44FFAA);
            g.fill((int)s.x-sz-1,(int)s.y-sz-1,(int)s.x+sz+1,(int)s.y+sz+1,((alpha>>3)<<24)|0x00CC66);
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
        // Hit test covers both cap and stem
        return Math.abs(mx-p[0]) <= 30 && my >= p[1]-24 && my <= p[1]+16;
    }

    @Override
    public @Nullable ResourceLocation shaderLocation() {
        return new ResourceLocation(PhoenixCore.MOD_ID, "shaders/post/axiom_sculk.json");
    }

    // ── Shader hooks ─────────────────────────────────────────────────────────

    @Override public float[] rippleOriginsCanvas() {
        float[] out = new float[MAX_RIPPLES*2];
        for (int i=0;i<MAX_RIPPLES;i++){out[i*2]=rippleX[i];out[i*2+1]=rippleY[i];}
        return out;
    }
    @Override public float[] rippleAges()  { return rippleAge.clone(); }
    @Override public int     rippleCount() { return activeRipples; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static int lerpCol(int a, int b, float t, int alpha) {
        int ar=(a>>16)&0xFF,ag=(a>>8)&0xFF,ab2=a&0xFF;
        int br=(b>>16)&0xFF,bg=(b>>8)&0xFF,bb=b&0xFF;
        return (Math.max(0,alpha)<<24)|((ar+(int)((br-ar)*t))<<16)|((ag+(int)((bg-ag)*t))<<8)|(ab2+(int)((bb-ab2)*t));
    }

    private static void renderIcon(GuiGraphics g, String iconId, int x, int y) {
        if (iconId==null||iconId.isEmpty()) return;
        ResourceLocation rl = ResourceLocation.tryParse(iconId);
        if (rl==null) return;
        var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(rl);
        if (item!=null&&item!=net.minecraft.world.item.Items.AIR)
            g.renderItem(new net.minecraft.world.item.ItemStack(item),x,y);
    }
}
