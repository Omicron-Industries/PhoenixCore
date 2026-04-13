package net.phoenix.core.client.renderer.machine;

import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.FusionReactorMachine;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.phoenix.core.client.renderer.PhoenixRenderTypes;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class HelicalFusionRenderer extends DynamicRender<FusionReactorMachine, HelicalFusionRenderer> {

    public static final HelicalFusionRenderer INSTANCE = new HelicalFusionRenderer();
    public static final Codec<HelicalFusionRenderer> CODEC = Codec.unit(INSTANCE);
    public static final DynamicRenderType<FusionReactorMachine, HelicalFusionRenderer> TYPE = new DynamicRenderType<>(
            CODEC);

    private static final float FADEOUT = 60f;
    private static final double LOD_NEAR = 48.0;
    private static final double LOD_MID = 96.0;

    private static final int RING_SEGMENTS = 16;
    private static final int RING_VERTS = RING_SEGMENTS + 1;

    private static final float OUTER_RADIUS = 0.45f * 0.6f; // 0.27f
    private static final float INNER_RADIUS = 0.60f * 0.6f; // 0.36f

    private static final float TWIST_SPEED = 10.0f;

    private static final Vec3[] BASE_RING = new Vec3[RING_VERTS];

    static {
        for (int i = 0; i <= RING_SEGMENTS; i++) {
            float a = (float) (2.0 * Math.PI * i / RING_SEGMENTS);
            BASE_RING[i] = new Vec3(Mth.cos(a), Mth.sin(a), 0);
        }
    }

    private float delta = 0f;
    private int lastColor = -1;

    private HelicalFusionRenderer() {}

    @Override
    public @NotNull DynamicRenderType<FusionReactorMachine, HelicalFusionRenderer> getType() {
        return TYPE;
    }

    @Override
    public boolean shouldRender(FusionReactorMachine machine, @NotNull Vec3 cameraPos) {
        return machine.isFormed() && (machine.getRecipeLogic().isWorking() || delta > 0);
    }

    @Override
    public void render(FusionReactorMachine machine, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        if (!machine.isFormed()) return;

        RecipeLogic logic = machine.getRecipeLogic();
        int recipeColor = machine.getColor();

        if (logic.isWorking()) {
            lastColor = recipeColor;
            delta = FADEOUT;
        } else if (delta > 0 && lastColor != -1) {
            delta -= Minecraft.getInstance().getDeltaFrameTime();
        } else return;

        float alpha = Mth.clamp(delta / FADEOUT, 0f, 1f);

        float rf = FastColor.ARGB32.red(lastColor) / 255f;
        float gf = FastColor.ARGB32.green(lastColor) / 255f;
        float bf = FastColor.ARGB32.blue(lastColor) / 255f;

        float lum = 0.2126f * rf + 0.7152f * gf + 0.0722f * bf;

        final float MAX_LUM = 0.65f;
        if (lum > MAX_LUM) {
            float scale = MAX_LUM / lum;
            rf *= scale;
            gf *= scale;
            bf *= scale;
        }

        if (bf > rf && bf > gf && lum > 0.55f) {
            bf *= 0.90f;
            gf *= 0.95f;
        }

        int rBase = (int) (rf * 255f);
        int gBase = (int) (gf * 255f);
        int bBase = (int) (bf * 255f);

        float time = (machine.getOffsetTimer() + partialTick) * 0.02f;

        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 center = Vec3.atCenterOf(machine.getPos());
        double distSq = cam.distanceToSqr(center);

        int segments, crossSections;
        if (distSq < LOD_NEAR * LOD_NEAR) {
            segments = 400;
            crossSections = 12;
        } else if (distSq < LOD_MID * LOD_MID) {
            segments = 260;
            crossSections = 10;
        } else {
            segments = 160;
            crossSections = 8;
        }

        Vec3 frontStep = new Vec3(
                machine.getFrontFacing().step().x(),
                machine.getFrontFacing().step().y(),
                machine.getFrontFacing().step().z()).normalize().scale(20);

        poseStack.pushPose();
        poseStack.translate(frontStep.x + 0.5, frontStep.y + 0.5, frontStep.z + 0.5);

        VertexConsumer vc = buffer.getBuffer(PhoenixRenderTypes.LIGHT_RING());

        renderHelix(poseStack, vc, time, 0f,
                rBase, gBase, bBase, alpha,
                segments, crossSections);

        renderHelix(poseStack, vc, time, Mth.PI,
                rBase, gBase, bBase, alpha,
                segments, crossSections);

        poseStack.popPose();
    }

    private void renderHelix(PoseStack stack, VertexConsumer vc,
                             float time, float phase,
                             int rBase, int gBase, int bBase, float alpha,
                             int segments, int crossSections) {
        final float OUTER_ALPHA = 0.35f;
        final float INNER_ALPHA = 0.15f;
        final float OUTER_DEPTH_NUDGE = 0.015f;
        final float INNER_DEPTH_NUDGE = 0.0f;
        final float INNER_RADIUS_SCALE = 0.85f;

        int ringCount = segments + 1;

        Vec3[] centers = new Vec3[ringCount];
        Vec3[] tangents = new Vec3[ringCount];
        Vec3[] normals = new Vec3[ringCount];
        Vec3[] binorms = new Vec3[ringCount];

        computeCurve(time, phase, segments, centers, tangents);
        computeBishopFrame(segments, tangents, normals, binorms);

        centers[segments] = centers[0];
        tangents[segments] = tangents[0];
        normals[segments] = normals[0];
        binorms[segments] = binorms[0];

        Vec3[][] outer = new Vec3[ringCount][crossSections + 1];
        Vec3[][] inner = new Vec3[ringCount][crossSections + 1];

        float innerRadiusVal = INNER_RADIUS * INNER_RADIUS_SCALE;

        buildRings(
                time,
                ringCount,
                crossSections,
                centers,
                tangents,
                normals,
                binorms,
                outer,
                inner,
                innerRadiusVal);

        /* -------- Rendering -------- */

        renderTube(
                stack, vc,
                outer,
                segments, crossSections,
                rBase, gBase, bBase,
                alpha * OUTER_ALPHA,
                OUTER_DEPTH_NUDGE);

        renderTube(
                stack, vc,
                inner,
                segments, crossSections,
                rBase, gBase, bBase,
                alpha * INNER_ALPHA,
                INNER_DEPTH_NUDGE);
    }

    private void buildRings(float time, int ringCount, int crossSections,
                            Vec3[] c, Vec3[] t,
                            Vec3[] n, Vec3[] b,
                            Vec3[][] o, Vec3[][] in,
                            float innerRadius) {
        for (int i = 0; i < ringCount; i++) {
            float twist = time * TWIST_SPEED + i * 0.12f;

            Vec3 nt = rotate(n[i], t[i], twist);
            Vec3 bt = t[i].cross(nt).normalize();

            for (int v = 0; v <= crossSections; v++) {
                float angle = v * Mth.TWO_PI / crossSections;
                float cos = Mth.cos(angle);
                float sin = Mth.sin(angle);

                o[i][v] = c[i].add(nt.scale(cos * OUTER_RADIUS))
                        .add(bt.scale(sin * OUTER_RADIUS));

                float pulse = 0.96f + 0.04f * Mth.sin(i * 0.1f + time * 3f);
                in[i][v] = c[i].add(nt.scale(cos * innerRadius * pulse))
                        .add(bt.scale(sin * innerRadius * pulse));
            }
        }
    }

    private void renderTube(PoseStack stack, VertexConsumer vc,
                            Vec3[][] rings, int segments, int crossSections,
                            int rBase, int gBase, int bBase, float alpha, float depthNudge) {
        Matrix4f pose = stack.last().pose();

        for (int i = 0; i < segments; i++) {
            int nextI = i + 1;

            float finalAlpha = alpha;

            for (int v = 0; v < crossSections; v++) {
                int nextV = v + 1;

                float s = 1.0f + depthNudge;

                Vec3 v1 = rings[i][v].scale(s);
                Vec3 v2 = rings[i][nextV].scale(s);
                Vec3 v3 = rings[nextI][nextV].scale(s);
                Vec3 v4 = rings[nextI][v].scale(s);

                quad(vc, pose, v1, v2, v3, v4, rBase, gBase, bBase, finalAlpha);
            }
        }
    }

    /* ---------------- Math ---------------- */

    private void computeCurve(float time, float phase, int segments,
                              Vec3[] c, Vec3[] t) {
        float dt = Mth.TWO_PI / segments;
        for (int i = 0; i < segments; i++) {
            Vec3 p0 = lissajous(i * dt + phase, time);
            Vec3 p1 = lissajous((i + 1) * dt + phase, time);
            c[i] = p0;
            t[i] = p1.subtract(p0).normalize();
        }
    }

    private void computeBishopFrame(int segments, Vec3[] t,
                                    Vec3[] n, Vec3[] b) {
        Vec3 up = Math.abs(t[0].y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);

        n[0] = up.subtract(t[0].scale(up.dot(t[0]))).normalize();
        b[0] = t[0].cross(n[0]).normalize();

        for (int i = 1; i < segments; i++) {
            Vec3 ni = n[i - 1].subtract(t[i].scale(n[i - 1].dot(t[i]))).normalize();
            n[i] = ni;
            b[i] = t[i].cross(ni).normalize();
        }
    }

    private Vec3 lissajous(float t, float time) {
        float scale = 0.6f;
        float x = 2f * scale * Mth.cos(4 * t);
        float y = 2f * scale * Mth.sin(4 * t);
        float z = 17f * scale * Mth.cos(t);

        float c = Mth.cos(time);
        float s = Mth.sin(time);

        return new Vec3(
                x * c - y * s,
                x * s + y * c,
                z * 0.4f);
    }

    private Vec3 rotate(Vec3 v, Vec3 axis, float ang) {
        double c = Math.cos(ang);
        double s = Math.sin(ang);
        return v.scale(c).add(axis.cross(v).scale(s)).add(axis.scale(axis.dot(v) * (1 - c)));
    }

    private void quad(VertexConsumer vc, Matrix4f pose,
                      Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                      int r, int g, int bl, float al) {
        vertex(vc, pose, a, r, g, bl, al);
        vertex(vc, pose, b, r, g, bl, al);
        vertex(vc, pose, c, r, g, bl, al);
        vertex(vc, pose, d, r, g, bl, al);
    }

    private void vertex(VertexConsumer vc, Matrix4f pose,
                        Vec3 p, int r, int g, int b, float a) {
        vc.vertex(pose, (float) p.x, (float) p.y, (float) p.z)
                .color(r / 255f, g / 255f, b / 255f, a)
                .endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(FusionReactorMachine m) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public @NotNull AABB getRenderBoundingBox(FusionReactorMachine m) {
        return new AABB(m.getPos()).inflate(40);
    }
}

/*
 * MONI ONE
 * package net.neganote.phoenix.client.render;
 * 
 * import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
 * import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
 * import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;
 * import com.gregtechceu.gtceu.common.machine.multiblock.electric.FusionReactorMachine;
 * 
 * import net.minecraft.client.Minecraft;
 * import net.minecraft.client.renderer.MultiBufferSource;
 * import net.minecraft.core.Direction;
 * import net.minecraft.util.FastColor;
 * import net.minecraft.util.Mth;
 * import net.minecraft.world.phys.AABB;
 * import net.minecraft.world.phys.Vec3;
 * 
 * import com.mojang.blaze3d.vertex.PoseStack;
 * import com.mojang.blaze3d.vertex.VertexConsumer;
 * import com.mojang.math.Axis;
 * import com.mojang.serialization.Codec;
 * import org.jetbrains.annotations.NotNull;
 * import org.joml.Matrix4f;
 * 
 * public class HelicalFusionRenderer extends DynamicRender<FusionReactorMachine, HelicalFusionRenderer> {
 * 
 * public static final HelicalFusionRenderer INSTANCE = new HelicalFusionRenderer(
 * 2.0f,
 * new Vec3(-2.0, -1, 0),
 * 0.09f,
 * 0.12f,
 * 10f);
 * 
 * private static final float FADEOUT = 60f;
 * private static final double LOD_NEAR = 64.0;
 * private static final double LOD_MID = 128.0;
 * 
 * private final float scale;
 * private final Vec3 offset;
 * private final float outerRadius;
 * private final float innerRadius;
 * private final float twistSpeed;
 * 
 * private float delta = 0f;
 * private int lastColor = -1;
 * 
 * public HelicalFusionRenderer(float scale, Vec3 offset, float outerRadius, float innerRadius, float twistSpeed) {
 * this.scale = scale;
 * this.offset = offset;
 * this.outerRadius = outerRadius;
 * this.innerRadius = innerRadius;
 * this.twistSpeed = twistSpeed;
 * }
 * 
 * public static final Codec<HelicalFusionRenderer> CODEC = Codec.unit(() -> INSTANCE);
 * public static final DynamicRenderType<FusionReactorMachine, HelicalFusionRenderer> TYPE = new DynamicRenderType<>(
 * CODEC);
 * 
 * @Override
 * public @NotNull DynamicRenderType<FusionReactorMachine, HelicalFusionRenderer> getType() {
 * return TYPE;
 * }
 * 
 * @Override
 * public boolean shouldRender(FusionReactorMachine machine, @NotNull Vec3 cameraPos) {
 * return machine.isFormed() && (machine.getRecipeLogic().isWorking() || delta > 0);
 * }
 * 
 * @Override
 * public void render(FusionReactorMachine machine, float partialTick,
 * 
 * @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer,
 * int packedLight, int packedOverlay) {
 * if (!machine.isFormed()) return;
 * 
 * RecipeLogic logic = machine.getRecipeLogic();
 * if (logic.isWorking()) {
 * lastColor = machine.getColor();
 * delta = FADEOUT;
 * } else if (delta > 0 && lastColor != -1) {
 * delta -= Minecraft.getInstance().getDeltaFrameTime();
 * } else return;
 * 
 * float alpha = Mth.clamp(delta / FADEOUT, 0f, 1f);
 * 
 * float rf = FastColor.ARGB32.red(lastColor) / 255f;
 * float gf = FastColor.ARGB32.green(lastColor) / 255f;
 * float bf = FastColor.ARGB32.blue(lastColor) / 255f;
 * float lum = 0.2126f * rf + 0.7152f * gf + 0.0722f * bf;
 * if (lum > 0.65f) {
 * float s = 0.65f / lum;
 * rf *= s;
 * gf *= s;
 * bf *= s;
 * }
 * int rBase = (int) (rf * 255f);
 * int gBase = (int) (gf * 255f);
 * int bBase = (int) (bf * 255f);
 * 
 * float time = (machine.getOffsetTimer() + partialTick) * 0.02f;
 * 
 * Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
 * double distSq = cam.distanceToSqr(Vec3.atCenterOf(machine.getPos()));
 * int segments = distSq < LOD_NEAR * LOD_NEAR ? 400 : (distSq < LOD_MID * LOD_MID ? 260 : 160);
 * int crossSections = distSq < LOD_NEAR * LOD_NEAR ? 12 : 8;
 * 
 * poseStack.pushPose();
 * 
 * poseStack.translate(0.5, 0.5, 0.5);
 * 
 * Direction facing = machine.getFrontFacing();
 * poseStack.mulPose(facing.getRotation());
 * 
 * poseStack.mulPose(Axis.XP.rotationDegrees(90));
 * poseStack.mulPose(Axis.YP.rotationDegrees(90));
 * 
 * poseStack.translate(offset.x, offset.y, offset.z);
 * poseStack.scale(scale, scale, scale);
 * 
 * VertexConsumer vc = buffer.getBuffer(HelicalRenderHelpers.LIGHT_RING());
 * 
 * renderHelix(poseStack, vc, time, 0f, rBase, gBase, bBase, alpha, segments, crossSections);
 * renderHelix(poseStack, vc, time, Mth.PI, rBase, gBase, bBase, alpha, segments, crossSections);
 * 
 * poseStack.popPose();
 * }
 * 
 * private void renderHelix(PoseStack stack, VertexConsumer vc, float time, float phase,
 * int rBase, int gBase, int bBase, float alpha,
 * int segments, int crossSections) {
 * final float OUTER_ALPHA = 0.35f;
 * final float INNER_ALPHA = 0.15f;
 * final float OUTER_DEPTH_NUDGE = 0.015f;
 * final float INNER_RADIUS_SCALE = 0.85f;
 * 
 * int ringCount = segments + 1;
 * Vec3[] centers = new Vec3[ringCount];
 * Vec3[] tangents = new Vec3[ringCount];
 * Vec3[] normals = new Vec3[ringCount];
 * Vec3[] binorms = new Vec3[ringCount];
 * 
 * computeCurve(time, phase, segments, centers, tangents);
 * computeBishopFrame(segments, tangents, normals, binorms);
 * 
 * centers[segments] = centers[0];
 * tangents[segments] = tangents[0];
 * normals[segments] = normals[0];
 * binorms[segments] = binorms[0];
 * 
 * Vec3[][] outer = new Vec3[ringCount][crossSections + 1];
 * Vec3[][] inner = new Vec3[ringCount][crossSections + 1];
 * 
 * buildRings(time, ringCount, crossSections, centers, tangents, normals, binorms, outer, inner,
 * innerRadius * INNER_RADIUS_SCALE);
 * 
 * renderTube(stack, vc, outer, segments, crossSections, rBase, gBase, bBase, alpha * OUTER_ALPHA,
 * OUTER_DEPTH_NUDGE);
 * renderTube(stack, vc, inner, segments, crossSections, rBase, gBase, bBase, alpha * INNER_ALPHA, 0.0f);
 * }
 * 
 * private void buildRings(float time, int ringCount, int crossSections, Vec3[] c, Vec3[] t, Vec3[] n, Vec3[] b,
 * Vec3[][] o, Vec3[][] in, float inRad) {
 * for (int i = 0; i < ringCount; i++) {
 * float twist = time * twistSpeed + i * 0.12f;
 * Vec3 nt = rotate(n[i], t[i], twist);
 * Vec3 bt = t[i].cross(nt).normalize();
 * 
 * for (int v = 0; v <= crossSections; v++) {
 * float angle = v * Mth.TWO_PI / crossSections;
 * float cos = Mth.cos(angle);
 * float sin = Mth.sin(angle);
 * 
 * o[i][v] = c[i].add(nt.scale(cos * outerRadius)).add(bt.scale(sin * outerRadius));
 * float pulse = 0.96f + 0.04f * Mth.sin(i * 0.1f + time * 3f);
 * in[i][v] = c[i].add(nt.scale(cos * inRad * pulse)).add(bt.scale(sin * inRad * pulse));
 * }
 * }
 * }
 * 
 * private void renderTube(PoseStack stack, VertexConsumer vc, Vec3[][] rings, int segments, int crossSections, int r,
 * int g, int b, float a, float depthNudge) {
 * Matrix4f pose = stack.last().pose();
 * for (int i = 0; i < segments; i++) {
 * for (int v = 0; v < crossSections; v++) {
 * float s = 1.0f + depthNudge;
 * quad(vc, pose, rings[i][v].scale(s), rings[i][v + 1].scale(s), rings[i + 1][v + 1].scale(s),
 * rings[i + 1][v].scale(s), r, g, b, a);
 * }
 * }
 * }
 * 
 * private void computeCurve(float time, float phase, int segments, Vec3[] c, Vec3[] t) {
 * float dt = Mth.TWO_PI / segments;
 * for (int i = 0; i < segments; i++) {
 * c[i] = lissajous(i * dt + phase, time);
 * }
 * for (int i = 0; i < segments; i++) {
 * int next = (i + 1) % segments;
 * t[i] = lissajous((next * dt) + phase, time).subtract(c[i]).normalize();
 * }
 * }
 * 
 * private void computeBishopFrame(int segments, Vec3[] t, Vec3[] n, Vec3[] b) {
 * Vec3 up = Math.abs(t[0].y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
 * n[0] = up.subtract(t[0].scale(up.dot(t[0]))).normalize();
 * b[0] = t[0].cross(n[0]).normalize();
 * for (int i = 1; i < segments; i++) {
 * n[i] = n[i - 1].subtract(t[i].scale(n[i - 1].dot(t[i]))).normalize();
 * b[i] = t[i].cross(n[i]).normalize();
 * }
 * }
 * 
 * private Vec3 lissajous(float t, float time) {
 * float bx = 0.5f * Mth.cos(4 * t);
 * float by = 0.5f * Mth.sin(4 * t);
 * float bz = 4.0f * Mth.cos(t);
 * 
 * float cosT = Mth.cos(time);
 * float sinT = Mth.sin(time);
 * 
 * return new Vec3(bx * cosT - by * sinT, bx * sinT + by * cosT, bz * 0.4f);
 * }
 * 
 * private Vec3 rotate(Vec3 v, Vec3 axis, float ang) {
 * double c = Math.cos(ang);
 * double s = Math.sin(ang);
 * return v.scale(c).add(axis.cross(v).scale(s)).add(axis.scale(axis.dot(v) * (1 - c)));
 * }
 * 
 * private void quad(VertexConsumer vc, Matrix4f pose, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int r, int g, int bl,
 * float al) {
 * vertex(vc, pose, a, r, g, bl, al);
 * vertex(vc, pose, b, r, g, bl, al);
 * vertex(vc, pose, c, r, g, bl, al);
 * vertex(vc, pose, d, r, g, bl, al);
 * }
 * 
 * private void vertex(VertexConsumer vc, Matrix4f pose, Vec3 p, int r, int g, int b, float a) {
 * vc.vertex(pose, (float) p.x, (float) p.y, (float) p.z).color(r / 255f, g / 255f, b / 255f, a).endVertex();
 * }
 * 
 * @Override
 * public boolean shouldRenderOffScreen(FusionReactorMachine m) {
 * return true;
 * }
 * 
 * @Override
 * public int getViewDistance() {
 * return 128;
 * }
 * 
 * @Override
 * public @NotNull AABB getRenderBoundingBox(FusionReactorMachine m) {
 * return new AABB(m.getPos()).inflate(60);
 * }
 * }
 * 
 */
/*
 * public class HelicalFusionRenderer extends DynamicRender<FusionReactorMachine, HelicalFusionRenderer> {
 * 
 * public static final HelicalFusionRenderer INSTANCE = new HelicalFusionRenderer();
 * public static final Codec<HelicalFusionRenderer> CODEC = Codec.unit(INSTANCE);
 * public static final DynamicRenderType<FusionReactorMachine, HelicalFusionRenderer> TYPE =
 * new DynamicRenderType<>(CODEC);
 * 
 * private static final float FADEOUT = 60f;
 * 
 * // Distance-based LOD thresholds
 * private static final double LOD_NEAR = 48.0;
 * private static final double LOD_MID = 96.0;
 * 
 * private float delta = 0f;
 * private int lastColor = -1;
 * 
 * private HelicalFusionRenderer() {}
 * 
 * @Override
 * public @NotNull DynamicRenderType<FusionReactorMachine, HelicalFusionRenderer> getType() {
 * return TYPE;
 * }
 * 
 * @Override
 * public boolean shouldRender(FusionReactorMachine machine, Vec3 cameraPos) {
 * return machine.isFormed() && (machine.getRecipeLogic().isWorking() || delta > 0);
 * }
 * 
 * @Override
 * public void render(FusionReactorMachine machine, float partialTick,
 * PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
 * 
 * if (!machine.isFormed()) return;
 * 
 * RecipeLogic logic = machine.getRecipeLogic();
 * int recipeColor = machine.getColor();
 * 
 * // --- Fadeout / color carryover ---
 * if (logic.isWorking()) {
 * lastColor = recipeColor;
 * delta = FADEOUT;
 * } else if (delta > 0 && lastColor != -1) {
 * float alphaFactor = delta / FADEOUT;
 * int a = Mth.clamp(Mth.floor(alphaFactor * 255f), 0, 255);
 * lastColor = FastColor.ARGB32.color(
 * a,
 * FastColor.ARGB32.red(lastColor),
 * FastColor.ARGB32.green(lastColor),
 * FastColor.ARGB32.blue(lastColor)
 * );
 * delta -= Minecraft.getInstance().getDeltaFrameTime();
 * } else {
 * if (lastColor == -1) return;
 * }
 * 
 * // --- Time & LOD selection ---
 * float time = (machine.getOffsetTimer() + partialTick) * 0.02f;
 * 
 * Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
 * Vec3 center = Vec3.atCenterOf(machine.getPos());
 * double distSq = cameraPos.distanceToSqr(center);
 * 
 * int segments;
 * int crossSections;
 * 
 * if (distSq < LOD_NEAR * LOD_NEAR) {
 * segments = 400;
 * crossSections = 12;
 * } else if (distSq < LOD_MID * LOD_MID) {
 * segments = 260;
 * crossSections = 10;
 * } else {
 * segments = 160;
 * crossSections = 8;
 * }
 * 
 * // --- Push the render 20 blocks in front of the multiblock ---
 * Vec3 frontStep = new Vec3(
 * machine.getFrontFacing().step().x(),
 * machine.getFrontFacing().step().y(),
 * machine.getFrontFacing().step().z()
 * ).normalize().scale(20.0);
 * 
 * poseStack.pushPose();
 * poseStack.translate(frontStep.x, frontStep.y, frontStep.z);
 * 
 * // Multiple consumers, but one per tube layer (Ghosti-style)
 * VertexConsumer vcOuterA = buffer.getBuffer(GTRenderTypes.getLightRing());
 * VertexConsumer vcInnerA = buffer.getBuffer(GTRenderTypes.getLightRing());
 * VertexConsumer vcOuterB = buffer.getBuffer(GTRenderTypes.getLightRing());
 * VertexConsumer vcInnerB = buffer.getBuffer(GTRenderTypes.getLightRing());
 * 
 * // Dual interleaved Lissajous curves with full tube layers
 * renderCurve(poseStack, vcOuterA, vcInnerA, time, 0f, lastColor, segments, crossSections);
 * renderCurve(poseStack, vcOuterB, vcInnerB, time, Mth.PI, lastColor, segments, crossSections);
 * 
 * poseStack.popPose();
 * }
 * 
 * private void renderCurve(PoseStack stack,
 * VertexConsumer vcOuter,
 * VertexConsumer vcInner,
 * float time,
 * float phase,
 * int baseColor,
 * int segments,
 * int crossSections) {
 * 
 * final float baseRadius = 0.35f;
 * 
 * Vec3[][] rings = new Vec3[segments][crossSections];
 * Vec3[] centers = new Vec3[segments];
 * 
 * // --- Parallel Transport Frame setup ---
 * Vec3 pInitial = lissajousPoint(phase, time);
 * Vec3 pNextInit = lissajousPoint(phase + 0.0001f, time);
 * Vec3 T = pNextInit.subtract(pInitial).normalize();
 * Vec3 V = new Vec3(0, 1, 0);
 * if (Math.abs(T.dot(V)) > 0.9f) V = new Vec3(0, 0, 1);
 * Vec3 B = T.cross(V).normalize();
 * Vec3 N = B.cross(T).normalize();
 * 
 * // Ensure initial frame is perfectly orthonormal
 * B = T.cross(N).normalize();
 * N = B.cross(T).normalize();
 * 
 * for (int i = 0; i < segments; i++) {
 * float t = (float) i / segments * Mth.TWO_PI;
 * Vec3 p0 = lissajousPoint(t + phase, time);
 * centers[i] = p0;
 * 
 * Vec3 pNext = lissajousPoint(t + phase + 0.001f, time);
 * Vec3 nextT = pNext.subtract(p0).normalize();
 * 
 * // Rotate frame for smooth twist
 * Vec3 axis = T.cross(nextT);
 * if (axis.length() > 0.0001f) {
 * axis = axis.normalize();
 * float angle = (float) Math.acos(Mth.clamp(T.dot(nextT), -1f, 1f));
 * N = rotateAroundAxis(N, axis, angle);
 * // Re-orthonormalize frame to avoid drift
 * B = nextT.cross(N).normalize();
 * N = B.cross(nextT).normalize();
 * }
 * T = nextT;
 * 
 * // Build ring cross-sections
 * for (int j = 0; j < crossSections; j++) {
 * float angle = j * Mth.TWO_PI / crossSections;
 * float pulse = 0.8f + 0.2f * Mth.sin(i * 0.1f + time * 5f);
 * float r = baseRadius * pulse;
 * 
 * Vec3 offset = N.scale(Mth.cos(angle) * r).add(B.scale(Mth.sin(angle) * r));
 * 
 * // Slow axial twist around tangent
 * rings[i][j] = p0.add(rotateAroundAxis(offset, T, time * 0.5f));
 * }
 * }
 * 
 * // Color setup
 * int rBase = FastColor.ARGB32.red(baseColor);
 * int gBase = FastColor.ARGB32.green(baseColor);
 * int bBase = FastColor.ARGB32.blue(baseColor);
 * float alpha = FastColor.ARGB32.alpha(baseColor) / 255f;
 * 
 * // Outer and inner tubes for depth and glow
 * renderTube(stack, vcOuter, rings, centers, segments, crossSections,
 * rBase, gBase, bBase, alpha, 1.0f);
 * 
 * // Slightly adjusted scale to reduce z-fighting and visual seams
 * renderTube(stack, vcInner, rings, centers, segments, crossSections,
 * rBase, gBase, bBase, alpha * 0.35f, 1.38f);
 * }
 * 
 * private void renderTube(PoseStack stack,
 * VertexConsumer vc,
 * Vec3[][] rings,
 * Vec3[] centers,
 * int segments,
 * int crossSections,
 * int rBase,
 * int gBase,
 * int bBase,
 * float alpha,
 * float scale) {
 * 
 * Matrix4f pose = stack.last().pose();
 * 
 * // Use segments-1 in color interpolation to avoid a visible seam
 * int colorDenom = Math.max(1, segments - 1);
 * 
 * for (int i = 0; i < segments; i++) {
 * int nextI = (i + 1) % segments;
 * 
 * float color Interp = (Mth.sin((float) i / colorDenom * Mth.TWO_PI) + 1f) / 2f;
 * int r = Mth.floor(Mth.lerp(colorInterp, rBase, Math.min(255, rBase + 60)));
 * int g = Mth.floor(Mth.lerp(colorInterp, gBase, Math.min(255, gBase + 60)));
 * int b = Mth.floor(Mth.lerp(colorInterp, bBase, Math.min(255, bBase + 60)));
 * 
 * for (int j = 0; j < crossSections; j++) {
 * int nextJ = (j + 1) % crossSections;
 * 
 * Vec3 v1 = scalePoint(rings[i][j], centers[i], scale);
 * Vec3 v2 = scalePoint(rings[i][nextJ], centers[i], scale);
 * Vec3 v3 = scalePoint(rings[nextI][nextJ], centers[nextI], scale);
 * Vec3 v4 = scalePoint(rings[nextI][j], centers[nextI], scale);
 * 
 * // Front face
 * vertex(vc, pose, v1, r, g, b, alpha);
 * vertex(vc, pose, v2, r, g, b, alpha);
 * vertex(vc, pose, v3, r, g, b, alpha);
 * vertex(vc, pose, v4, r, g, b, alpha);
 * 
 * // Backface to reduce transparency holes
 * vertex(vc, pose, v4, r, g, b, alpha);
 * vertex(vc, pose, v3, r, g, b, alpha);
 * vertex(vc, pose, v2, r, g, b, alpha);
 * vertex(vc, pose, v1, r, g, b, alpha);
 * }
 * }
 * }
 * 
 * private Vec3 scalePoint(Vec3 point, Vec3 center, float scale) {
 * if (scale == 1.0f) return point;
 * return center.add(point.subtract(center).scale(scale));
 * }
 * 
 * private void vertex(VertexConsumer vc, Matrix4f pose, Vec3 p, int r, int g, int b, float a) {
 * vc.vertex(pose, (float) p.x, (float) p.y, (float) p.z)
 * .color(r / 255f, g / 255f, b / 255f, a)
 * .endVertex();
 * }
 * 
 * private Vec3 lissajousPoint(float t, float time) {
 * float x = 2f * Mth.cos(4f * t);
 * float y = 2f * Mth.sin(4f * t);
 * float z = 17f * Mth.cos(t);
 * 
 * float cos = Mth.cos(time);
 * float sin = Mth.sin(time);
 * 
 * return new Vec3(
 * x * cos - y * sin,
 * x * sin + y * cos,
 * z * 0.4f
 * );
 * }
 * 
 * private Vec3 rotateAroundAxis(Vec3 v, Vec3 axis, float angle) {
 * double cos = Math.cos(angle);
 * double sin = Math.sin(angle);
 * Vec3 a = axis.normalize();
 * return new Vec3(
 * v.x * cos + (a.y * v.z - a.z * v.y) * sin + a.x * (a.x * v.x + a.y * v.y + a.z * v.z) * (1 - cos),
 * v.y * cos + (a.z * v.x - a.x * v.z) * sin + a.y * (a.x * v.x + a.y * v.y + a.z * v.z) * (1 - cos),
 * v.z * cos + (a.x * v.y - a.y * v.x) * sin + a.z * (a.x * v.x + a.y * v.y + a.z * v.z) * (1 - cos)
 * );
 * }
 * 
 * @Override
 * public boolean shouldRenderOffScreen(FusionReactorMachine machine) {
 * return true;
 * }
 * 
 * @Override
 * public int getViewDistance() {
 * return 128;
 * }
 * 
 * @Override
 * public @NotNull AABB getRenderBoundingBox(FusionReactorMachine machine) {
 * return new AABB(machine.getPos()).inflate(40, 40, 40);
 * }
 * }
 * 
 * 
 * 
 * 
 * 
 * /*
 * public class HelicalFusionRenderer
 * extends DynamicRender<FusionReactorMachine, HelicalFusionRenderer> {
 * 
 * public static final HelicalFusionRenderer INSTANCE = new HelicalFusionRenderer();
 * public static final Codec<HelicalFusionRenderer> CODEC = Codec.unit(INSTANCE);
 * public static final DynamicRenderType<FusionReactorMachine, HelicalFusionRenderer> TYPE =
 * new DynamicRenderType<>(CODEC);
 * 
 * @Override
 * public @NotNull DynamicRenderType<FusionReactorMachine, HelicalFusionRenderer> getType() {
 * return TYPE;
 * }
 * 
 * @Override
 * public boolean shouldRender(FusionReactorMachine machine, @NotNull Vec3 cameraPos) {
 * return machine.isFormed() && machine.getRecipeLogic() != null && machine.getRecipeLogic().isWorking();
 * }
 * 
 * @Override
 * public void render(
 * FusionReactorMachine machine,
 * float partialTick,
 * 
 * @NotNull PoseStack poseStack,
 * MultiBufferSource buffer,
 * int packedLight,
 * int packedOverlay
 * ) {
 * VertexConsumer vc = buffer.getBuffer(GTRenderTypes.getLightRing());
 * 
 * float time = machine.getOffsetTimer() + partialTick;
 * 
 *
 * float pulse = Mth.sin(time * 0.1f) * 0.15f + 0.85f;
 * float r = 1.0f * pulse;
 * float g = 0.55f * pulse;
 * float b = 0.15f * pulse;
 * float a = 0.85f;
 * 
 * poseStack.pushPose();
 * PoseStack.Pose pose = poseStack.last();
 * 
 * int segments = 64;
 * float size = 4.0f;
 * float thickness = 0.35f;
 * 
 * for (int i = 0; i < segments - 1; i++) {
 * float t0 = (float) i / segments;
 * float t1 = (float) (i + 1) / segments;
 * 
 * Vec3[] s0 = mobiusSection(t0, size, thickness);
 * Vec3[] s1 = mobiusSection(t1, size, thickness);
 * 
 *
 * emitQuad(vc, pose, s0[0], s0[1], s1[1], s1[0], r, g, b, a);
 * 
 *
 * emitQuad(vc, pose, s1[2], s1[3], s0[3], s0[2], r, g, b, a);
 * }
 * 
 * poseStack.popPose();
 * }
 * 
 * 
 * private static Vec3[] mobiusSection(float t, float size, float halfThickness) {
 * float angle = t * Mth.TWO_PI;
 * float twist = angle * 0.5f;
 * 
 * float x = Mth.cos(angle) * size;
 * float z = Mth.sin(angle) * size;
 * 
 * float dx = -Mth.sin(angle);
 * float dz = Mth.cos(angle);
 * 
 * float nx = Mth.cos(twist);
 * float ny = Mth.sin(twist);
 * 
 * Vec3 offset = new Vec3(dx * nx, ny, dz * nx).normalize().scale(halfThickness);
 * 
 * return new Vec3[] {
 * new Vec3(x, 0, z).add(offset),
 * new Vec3(x, 0, z).subtract(offset),
 * new Vec3(x, 0, z).subtract(offset.scale(0.6)),
 * new Vec3(x, 0, z).add(offset.scale(0.6))
 * };
 * }
 * 
 * 
 * private static void emitQuad(
 * VertexConsumer vc,
 * PoseStack.Pose pose,
 * Vec3 a, Vec3 b, Vec3 c, Vec3 d,
 * float r, float g, float bCol, float aCol
 * ) {
 * Vec3 normal = b.subtract(a).cross(d.subtract(a)).normalize();
 * 
 * vertex(vc, pose, a, normal, r, g, bCol, aCol);
 * vertex(vc, pose, b, normal, r, g, bCol, aCol);
 * vertex(vc, pose, c, normal, r, g, bCol, aCol);
 * vertex(vc, pose, d, normal, r, g, bCol, aCol);
 * }
 * 
 * private static void vertex(
 * VertexConsumer vc,
 * PoseStack.Pose pose,
 * Vec3 v,
 * Vec3 n,
 * float r, float g, float b, float a
 * ) {
 * vc.vertex(pose.pose(), (float) v.x, (float) v.y, (float) v.z)
 * .color(r, g, b, a)
 * .normal(pose.normal(), (float) n.x, (float) n.y, (float) n.z)
 * .endVertex();
 * }
 * }
 */
