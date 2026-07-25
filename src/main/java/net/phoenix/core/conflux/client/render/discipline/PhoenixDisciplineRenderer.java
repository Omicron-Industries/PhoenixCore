package net.phoenix.core.conflux.client.render.discipline;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.conflux.client.render.*;
import net.phoenix.core.conflux.research.ResearchNode;
import net.phoenix.core.conflux.research.ResearchTree;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import static net.phoenix.core.conflux.client.render.AxiomSprites.*;

import java.util.*;

public class PhoenixDisciplineRenderer implements DisciplineRenderer {

    private static final int   MAX_ASH    = 140;
    private static final float ARM_COUNT  = 4f;
    private static final float BASE_DIST  = 75f;
    private static final float ARM_SPACE  = 88f;
    private static final float SPIRAL_W   = 0.45f;

    private static final int C_WHITE  = 0xFFFFFFDD;
    private static final int C_HOT    = 0xFFFFFF88;
    private static final int C_YELLOW = 0xFFFFCC00;
    private static final int C_ORANGE = 0xFFFF6600;
    private static final int C_RED    = 0xFFCC2200;
    private static final int C_EMBER  = 0xFF661100;

    private static class Ash {
        float x, y, vx, vy, life, maxLife, size;
    }
    private final Ash[] ash = new Ash[MAX_ASH];
    private int ashHead = 0;

    private float rayAngle   = 0f;   
    private float forgeTime  = 0f;   
    private float[] prominPhase = new float[6]; 

    private final Map<ResourceLocation, float[]> posCache = new HashMap<>();
    private String lastTreeId = null;

    @Override public @Nullable String disciplineId() { return "phoenix"; }
    @Override public MotionClock.Signature signature() { return MotionClock.Signature.PHOENIX; }

    @Override
    public void onActivate(MotionClock clock) {
        Arrays.fill(ash, null);
        ashHead = 0; rayAngle = 0f; forgeTime = 0f;
        posCache.clear(); lastTreeId = null;
        for (int i = 0; i < prominPhase.length; i++)
            prominPhase[i] = (float)(Math.random() * 6.28318f);
    }

    @Override
    public void onUnlock(ResearchNode node, MotionClock clock, IntensityController intensity) {
        DisciplineRenderer.super.onUnlock(node, clock, intensity);
        float[] p = posCache.get(node.id);
        if (p != null) for (int i = 0; i < 24; i++) spawnAsh(p[0], p[1], true);
    }

    @Override
    public void tick(float dt, RenderContext ctx) {
        rayAngle  += dt * 0.09f;
        forgeTime += dt;
        ensurePositions(ctx.tree());

        if (Math.random() < dt * 4f) {
            for (ResearchNode n : ctx.tree().getNodes()) {
                if (ctx.isUnlocked(n.id) && Math.random() < 0.2f) {
                    float[] p = posCache.get(n.id);
                    if (p != null) { spawnAsh(p[0], p[1], false); break; }
                }
            }
        }

        for (int i = 0; i < MAX_ASH; i++) {
            Ash a = ash[i];
            if (a == null) continue;
            a.x += a.vx * dt; a.y += a.vy * dt;
            
            a.vx *= (float)Math.pow(0.96, dt * 60);
            a.vy += (float)(Math.random() - 0.4) * 8f * dt; 
            a.size *= (float)Math.pow(0.94, dt * 60);
            a.life -= dt;
            if (a.life <= 0 || a.size < 0.3f) ash[i] = null;
        }
    }

    private void ensurePositions(ResearchTree tree) {
        String tid = tree.id != null ? tree.id.toString() : "?";
        if (tid.equals(lastTreeId)) return;
        lastTreeId = tid; posCache.clear();
        Map<Integer, List<ResearchNode>> byArm = new TreeMap<>();
        for (ResearchNode n : tree.getNodes())
            byArm.computeIfAbsent(n.posX, k -> new ArrayList<>()).add(n);
        for (Map.Entry<Integer, List<ResearchNode>> e : byArm.entrySet()) {
            int arm = e.getKey();
            float base = arm * (6.28318f / ARM_COUNT);
            List<ResearchNode> ns = e.getValue();
            ns.sort(Comparator.comparingInt(n -> n.posY));
            for (ResearchNode n : ns) {
                float dist = BASE_DIST + (n.posY + 1) * ARM_SPACE;
                float ang  = base + dist * SPIRAL_W / 100f;
                posCache.put(n.id, new float[]{ dist * (float)Math.cos(ang), dist * (float)Math.sin(ang) });
            }
        }
    }

    private void spawnAsh(float ox, float oy, boolean burst) {
        Ash a = new Ash();
        a.x = ox + (float)(Math.random() - 0.5) * (burst ? 20f : 8f);
        a.y = oy + (float)(Math.random() - 0.5) * (burst ? 20f : 8f);
        
        a.vx = (float)(Math.random() - 0.5) * (burst ? 45f : 20f);
        a.vy = (float)(Math.random() - 0.5) * (burst ? 30f : 12f);
        a.size = 1.5f + (float)(Math.random() * (burst ? 3.5f : 1.5f));
        a.maxLife = a.life = 1.8f + (float)(Math.random() * 2f);
        ash[ashHead % MAX_ASH] = a;
        ashHead++;
    }

    @Override
    public void renderBackground(GuiGraphics g, RenderContext ctx) {
        int cw = ctx.canvasW(), ch = ctx.canvasH();
        float cx = cw * 0.5f, cy = ch * 0.5f;
        float intense = ctx.intensity().get();
        float pulse   = MotionClock.Signature.PHOENIX.pulse(forgeTime);

        g.fill(0, 0, cw, ch, 0xFF1A0200);
        int bgFrame = timeToFrame(rayAngle / (2f * 3.14159f), 1f, PHOENIX_BG.frames());
        blitFrame(g, PHOENIX_BG, bgFrame, 0, 0, cw, ch, intense);

        int PROMINS = 6;
        for (int pi = 0; pi < PROMINS; pi++) {
            float pa   = pi * 6.28318f / PROMINS + prominPhase[pi] + forgeTime * 0.04f;
            float anim = prominPhase[pi] + forgeTime * 0.6f;
            drawProminence(g, cx, cy, pa, anim, intense, pulse);
        }

        int coreR = (int)(20 + 6 * pulse);
        for (float dy = -coreR; dy <= coreR; dy++) {
            float hw = coreR * (float)Math.sqrt(Math.max(0, 1f - (dy/coreR)*(dy/coreR)));
            int a = (int)(230 * pulse * intense + 80);
            g.fill((int)(cx - hw), (int)(cy + dy), (int)(cx + hw), (int)(cy + dy + 1), (a << 24) | 0xFFFFEE);
        }
        
        for (int cr = 3; cr >= 1; cr--) {
            int cr2 = coreR + cr * 8;
            int ca = (int)(40 / cr * pulse);
            g.fill((int)(cx - cr2), (int)(cy - cr2), (int)(cx + cr2), (int)(cy + cr2), (ca << 24) | 0xFFEE88);
        }
    }

    private void drawProminence(GuiGraphics g, float cx, float cy,
                                 float angle, float anim, float intense, float pulse) {
        float height  = 90f  + 35f  * (float)Math.sin(anim * 0.7f);
        float width   = 22f  + 10f  * (float)Math.sin(anim * 1.1f);
        float surfR   = 28f;
        float brightness = 0.5f + 0.5f * (float)Math.sin(anim * 0.9f);

        float cosA = cos(angle), sinA = sin(angle);

        float sx = cx + cosA * surfR, sy = cy + sinA * surfR;
        
        float topX = cx + cosA * (surfR + height), topY = cy + sinA * (surfR + height);
        
        float perpX = -sinA * width, perpY = cosA * width;
        float cp1x = (sx + topX) * 0.5f + perpX;
        float cp1y = (sy + topY) * 0.5f + perpY;
        float cp2x = (sx + topX) * 0.5f - perpX;
        float cp2y = (sy + topY) * 0.5f - perpY;

        int STEPS = 36;
        
        for (int i = 0; i <= STEPS; i++) {
            float t = (float)i / STEPS, u = 1f - t;
            float px = u*u*sx + 2*u*t*cp1x + t*t*topX;
            float py = u*u*sy + 2*u*t*cp1y + t*t*topY;
            float heat = (float)Math.sin(t * 3.14159f); 
            int a = (int)(115 * intense * brightness * heat);
            int rv = 255, gv = (int)(200 * heat), bv = (int)(30 * heat);
            g.fill((int)px-1, (int)py-1, (int)px+2, (int)py+2, (a<<24)|(rv<<16)|(gv<<8)|bv);
        }
        
        for (int i = 0; i <= STEPS; i++) {
            float t = (float)i / STEPS, u = 1f - t;
            float px = u*u*topX + 2*u*t*cp2x + t*t*sx;
            float py = u*u*topY + 2*u*t*cp2y + t*t*sy;
            float heat = (float)Math.sin(t * 3.14159f);
            int a = (int)(90 * intense * brightness * heat);
            g.fill((int)px-1, (int)py-1, (int)px+2, (int)py+2, (a<<24)|0xFF8822);
        }
        
        int ta = (int)(160 * brightness * pulse);
        g.fill((int)topX-2, (int)topY-2, (int)topX+3, (int)topY+3, (ta<<24)|0xFFFFAA);
    }

    @Override
    public void renderEdges(GuiGraphics g, RenderContext ctx) {
        drawPlasmaArms(g, ctx);
        for (ResearchNode node : ctx.tree().getNodes()) {
            if (!node.isVisible(ctx.unlocked())) continue;
            float[] tp = posCache.get(node.id);
            if (tp == null) continue;
            for (ResourceLocation prereq : node.prerequisites) {
                ResearchNode src = ctx.tree().getNode(prereq).orElse(null);
                if (src == null || !src.isVisible(ctx.unlocked())) continue;
                float[] sp = posCache.get(src.id);
                if (sp == null) continue;
                drawPlasmaChannel(g, sp[0], sp[1], tp[0], tp[1], ctx.isUnlocked(src.id));
            }
        }
    }

    private void drawPlasmaArms(GuiGraphics g, RenderContext ctx) {
        float intense = ctx.intensity().get();
        for (int arm = 0; arm < (int)ARM_COUNT; arm++) {
            float base = arm * (6.28318f / ARM_COUNT) + rayAngle * 0.5f;
            float maxD = BASE_DIST + ARM_SPACE * 5.5f;
            for (int si = 0; si < 110; si++) {
                float t    = (float)si / 110;
                float dist = BASE_DIST + t * (maxD - BASE_DIST);
                float ang  = base + dist * SPIRAL_W / 100f;
                float px   = dist * cos(ang), py = dist * sin(ang);
                float heat = 1f - t;
                int r2 = 255, gv = (int)(255 * heat * heat), bv = (int)(120 * heat * heat * heat);
                int a  = (int)(70 * (float)Math.pow(heat, 1.4f) * intense);
                float w = 1.5f + t * 4f;
                g.fill((int)(px - w), (int)(py - w/2), (int)(px + w), (int)(py + w/2 + 1),
                       (a<<24)|(r2<<16)|(gv<<8)|bv);
            }
        }
    }

    private void drawPlasmaChannel(GuiGraphics g, float x0, float y0, float x1, float y1, boolean active) {
        float dx = x1-x0, dy = y1-y0;
        float len = (float)Math.sqrt(dx*dx+dy*dy);
        if (len < 0.5f) return;
        int DOTS = Math.max(6, (int)(len / 4));
        for (int i = 0; i <= DOTS; i++) {
            float t = (float)i / DOTS;
            
            float w = active ? 2f + (1f - t) * 5f : 1f;
            float px = x0 + dx*t, py = y0 + dy*t;
            
            float wave = (float)Math.sin(t * 5f + forgeTime * 3f) * 2f;
            px += -dy/len * wave; py += dx/len * wave;
            int a = active ? (int)(180 * (1f - t*0.5f)) : 45;
            
            int col = active ? lerpCol(C_WHITE, C_ORANGE, t, a) : ((a<<24)|0x330800);
            g.fill((int)(px-w), (int)(py-w/2), (int)(px+w), (int)(py+w/2+1), col);
            
            if (active) {
                int oa = a >> 2;
                g.fill((int)(px-w-2), (int)(py-1), (int)(px+w+2), (int)(py+2), (oa<<24)|0xFF4400);
            }
        }
    }

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

        float pulse  = MotionClock.Signature.PHOENIX.pulse(forgeTime);
        float intense = ctx.intensity().get();

        if (unlocked || available) {
            int[] hR = { 44, 30, 18 };
            int[] hA = { 10, 22, 40 };
            int[] hC = { 0xFF6600, 0xFFAA00, 0xFFDD44 };
            for (int hi = 0; hi < 3; hi++) {
                int r  = (int)(hR[hi] * (1f + 0.18f * pulse));
                int ha = (int)(hA[hi] * pulse * intense);
                g.fill(cx-r, cy-r, cx+r, cy+r, (ha<<24)|(hC[hi]&0xFFFFFF));
            }
        }

        int bg  = lockedOut ? 0xFF100200 : unlocked ? 0xFF201000 : available ? 0xFF180800 : 0xFF0A0300;
        int brd = lockedOut ? 0xFF441100 : unlocked ? (sel ? 0xFFFFFFAA : 0xFFFFAA00) : available ? 0xFFDD6600 : 0xFF551100;
        int OC = 16; 

        g.pose().pushPose();
        g.pose().translate(cx, cy, 0f);
        g.fill(-OC, -OC, OC, OC, bg);                         
        g.pose().mulPose(new Quaternionf().rotationZ((float)Math.PI / 4f));
        g.fill(-OC, -OC, OC, OC, bg);                         
        g.pose().popPose();

        for (int vi = 0; vi < 8; vi++) {
            float va = vi * 6.28318f / 8f;
            int vx = cx + (int)((OC + 1) * Math.cos(va));
            int vy = cy + (int)((OC + 1) * Math.sin(va));
            g.fill(vx-1, vy-1, vx+2, vy+2, brd);
        }
        
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0f);
        for (int ti = 0; ti < 8; ti++) {
            g.fill(OC + 2, -1, OC + 7, 1, brd);
            g.pose().mulPose(new Quaternionf().rotationZ((float)Math.PI / 4f));
        }
        g.pose().popPose();

        if (!lockedOut) {
            int glowA = unlocked ? (int)(80 * pulse) : available ? (int)(40 * pulse) : 15;
            g.fill(cx - 6, cy - 6, cx + 6, cy + 6, (glowA<<24)|0xFFEE88);
        }

        if (sel) {
            int sa = (int)(90 * pulse);
            g.fill(cx-OC-7, cy-OC-7, cx+OC+7, cy+OC+7, (sa<<24)|0xFFCC00);
            g.fill(cx-OC-6, cy-OC-6, cx+OC+6, cy+OC+6, 0xFF120300);
        }

        if (!node.hidden || unlocked) renderIcon(g, node.icon, cx-8, cy-8);

        if (node.isCommitmentNode) {
            int da = (int)(255 * pulse);
            g.fill(cx-3, cy-OC-9, cx+3, cy-OC-3, (da<<24)|0xFFAA00);
        }
    }

    @Override
    public void renderForeground(GuiGraphics g, RenderContext ctx) {
        for (Ash a : ash) {
            if (a == null) continue;
            float frac = a.life / a.maxLife;
            int alpha  = (int)(frac * 185);
            int sz     = Math.max(1, (int)(a.size * frac));
            
            int col = lerpCol(C_HOT, C_EMBER, 1f - frac, alpha);
            g.fill((int)a.x-sz, (int)a.y-sz, (int)a.x+sz, (int)a.y+sz, col);
        }
    }

    @Override
    public float[] nodePos(ResearchNode node, RenderContext ctx) {
        ensurePositions(ctx.tree());
        float[] c = posCache.get(node.id);
        if (c == null) return new float[]{ node.posX * 110f, node.posY * 110f };
        float drift = rayAngle * 0.06f;
        float cos = cos(drift), sin = sin(drift);
        return new float[]{ c[0]*cos - c[1]*sin, c[0]*sin + c[1]*cos };
    }

    @Override
    public boolean hitsNode(ResearchNode node, float mx, float my, RenderContext ctx) {
        float[] p = nodePos(node, ctx);
        return Math.abs(mx-p[0]) <= 22 && Math.abs(my-p[1]) <= 22;
    }

    @Override
    public @Nullable ResourceLocation shaderLocation() {
        return new ResourceLocation(PhoenixCore.MOD_ID, "shaders/post/axiom_phoenix.json");
    }

    private static float cos(float a) { return (float)Math.cos(a); }
    private static float sin(float a) { return (float)Math.sin(a); }

    private static int lerpCol(int a, int b, float t, int alpha) {
        int ar=(a>>16)&0xFF, ag=(a>>8)&0xFF, ab2=a&0xFF;
        int br=(b>>16)&0xFF, bg=(b>>8)&0xFF, bb=b&0xFF;
        return (alpha<<24)|((ar+(int)((br-ar)*t))<<16)|((ag+(int)((bg-ag)*t))<<8)|(ab2+(int)((bb-ab2)*t));
    }

    private static void renderIcon(GuiGraphics g, String iconId, int x, int y) {
        if (iconId==null||iconId.isEmpty()) return;
        ResourceLocation rl = ResourceLocation.tryParse(iconId);
        if (rl==null) return;
        var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(rl);
        if (item!=null && item!=net.minecraft.world.item.Items.AIR)
            g.renderItem(new net.minecraft.world.item.ItemStack(item), x, y);
    }
}
