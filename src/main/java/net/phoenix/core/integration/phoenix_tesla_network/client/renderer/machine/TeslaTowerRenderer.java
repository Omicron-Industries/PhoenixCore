package net.phoenix.core.integration.phoenix_tesla_network.client.renderer.machine;

import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.phoenix.core.client.particle.PhoenixParticles;
import net.phoenix.core.client.renderer.PhoenixRenderTypes;
import net.phoenix.core.integration.phoenix_tesla_network.common.machine.multiblock.electric.TeslaTowerMachine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.List;

public class TeslaTowerRenderer extends DynamicRender<TeslaTowerMachine, TeslaTowerRenderer> {

    public static final TeslaTowerRenderer INSTANCE = new TeslaTowerRenderer();
    public static final Codec<TeslaTowerRenderer> CODEC = Codec.unit(INSTANCE);
    public static final DynamicRenderType<TeslaTowerMachine, TeslaTowerRenderer> TYPE = new DynamicRenderType<>(CODEC);

    private static final float[] Y_POSITIONS = new float[] { 5.5f, 14.5f, 22.5f };
    private static final float[] X_POSITIONS = new float[] { 1.0f, 1.0f, 1.0f };
    private static final float[] Z_POSITIONS = new float[] { 6.0f, 6.0f, 6.0f };

    private static final Vec3[] POINT_CACHE = new Vec3[64];
    static {
        for (int i = 0; i < 64; i++) POINT_CACHE[i] = Vec3.ZERO;
    }

    private TeslaTowerRenderer() {}

    @Override
    public @NotNull DynamicRenderType<TeslaTowerMachine, TeslaTowerRenderer> getType() {
        return TYPE;
    }

    @Override
    public boolean shouldRenderOffScreen(TeslaTowerMachine m) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public @NotNull AABB getRenderBoundingBox(TeslaTowerMachine m) {
        return new AABB(m.getBlockPos()).inflate(40);
    }

    @Override
    public boolean shouldRender(TeslaTowerMachine machine, @NotNull Vec3 cameraPos) {
        return machine.isFormed() && machine.isActive();
    }

    @Override
    public void render(TeslaTowerMachine machine, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        long time = machine.getLevel().getGameTime();
        VertexConsumer vc = buffer.getBuffer(PhoenixRenderTypes.LIGHT_RING());

        float RING_RADIUS = 6.5f;
        float TEEPEE_DROP = 2.5f;
        int ARC_POINTS = 30;

        BlockPos machinePos = machine.getBlockPos();
        double baseParticleX = machinePos.getX() + 0.5;
        double baseParticleY = machinePos.getY() + 0.5;
        double baseParticleZ = machinePos.getZ() + 0.5;

        poseStack.pushPose();

        for (int ringIndex = 0; ringIndex < Y_POSITIONS.length; ringIndex++) {
            float xBase = X_POSITIONS[ringIndex];
            float yBase = Y_POSITIONS[ringIndex];
            float zBase = Z_POSITIONS[ringIndex];
            Vec3 currentTopCenter = new Vec3(xBase, yBase, zBase);

            for (int i = 0; i < ARC_POINTS; i++) {
                double rotationSpeed = time * (0.02 + (ringIndex * 0.01));
                double angle = (2 * Math.PI * i / ARC_POINTS) + rotationSpeed;

                double x = xBase + Math.cos(angle) * RING_RADIUS;
                double z = zBase + Math.sin(angle) * RING_RADIUS;
                double pulse = Math.sin((time + ringIndex * 10 + i) * 0.1) * 0.2;
                double y = yBase - TEEPEE_DROP + pulse;

                Vec3 targetPos = new Vec3(x, y, z);

                if ((time + i * 7 + ringIndex * 13) % 12 < 5) {
                    renderArc(poseStack, vc, currentTopCenter, targetPos, time);

                    if (machine.getLevel().isClientSide && machine.getLevel().random.nextFloat() < 0.05f) {
                        machine.getLevel().addParticle(
                                PhoenixParticles.TESLA_SPARK.get(),
                                targetPos.x + baseParticleX,
                                targetPos.y + baseParticleY,
                                targetPos.z + baseParticleZ,
                                0.0, 0.0, 0.0);
                    }
                }
            }
        }
        poseStack.popPose();
    }

    private void generateLightningPath(List<Vec3> points, Vec3 start, Vec3 end, int depth, float jitter, long time) {
        if (depth == 0) {
            points.add(end);
            return;
        }

        Vec3 mid = start.add(end).scale(0.5);
        float seed = (float) (time + start.x + start.y + start.z);

        mid = mid.add(
                (sinNoise(seed) - 0.5) * jitter * depth,
                (sinNoise(seed + 1) - 0.5) * jitter * depth,
                (sinNoise(seed + 2) - 0.5) * jitter * depth);

        generateLightningPath(points, start, mid, depth - 1, jitter, time);
        generateLightningPath(points, mid, end, depth - 1, jitter, time);
    }

    private void drawVolumetricArc(VertexConsumer vc, Matrix4f pose, List<Vec3> path, float width, float r, float g,
                                   float b, float a) {
        for (int i = 0; i < path.size() - 1; i++) {
            Vec3 s = path.get(i);
            Vec3 e = path.get(i + 1);

            float dx = (float) (e.x - s.x);
            float dy = (float) (e.y - s.y);
            float dz = (float) (e.z - s.z);

            float nx = -dz * width;
            float nz = dx * width;

            vc.vertex(pose, (float) s.x - nx, (float) s.y, (float) s.z - nz).color(r, g, b, a).endVertex();
            vc.vertex(pose, (float) s.x + nx, (float) s.y, (float) s.z + nz).color(r, g, b, a).endVertex();
            vc.vertex(pose, (float) e.x + nx, (float) e.y, (float) e.z + nz).color(r, g, b, a).endVertex();
            vc.vertex(pose, (float) e.x - nx, (float) e.y, (float) e.z - nz).color(r, g, b, a).endVertex();
        }
    }

    private void drawTeslaLine(VertexConsumer vc, Matrix4f pose, Vec3 s, Vec3 e,
                               int depth, boolean isCore, long time) {
        if (depth == 0) {
            float r, g, b, a;
            if (isCore) {
                r = 160 / 255f;
                g = 32 / 255f;
                b = 240 / 255f;
                a = 1.0f;
            } else {
                r = 255 / 255f;
                g = 183 / 255f;
                b = 197 / 255f;
                a = 0.5f;
            }
            vc.vertex(pose, (float) s.x, (float) s.y, (float) s.z).color(r, g, b, a).endVertex();
            vc.vertex(pose, (float) e.x, (float) e.y, (float) e.z).color(r, g, b, a).endVertex();
            return;
        }

        Vec3 mid = s.add(e).scale(0.5);

        float seed = (float) (time + s.x * 31 + s.y * 17 + s.z);
        float jitter = 0.25f * depth;

        double jX = (sinNoise(seed) - 0.5) * jitter;
        double jY = (sinNoise(seed * 1.2f) - 0.5) * jitter;
        double jZ = (sinNoise(seed * 1.5f) - 0.5) * jitter;

        mid = mid.add(jX, jY, jZ);

        drawTeslaLine(vc, pose, s, mid, depth - 1, isCore, time);
        drawTeslaLine(vc, pose, mid, e, depth - 1, isCore, time);
    }

    private void renderArc(PoseStack stack, VertexConsumer vc, Vec3 start, Vec3 end, long time) {
        Matrix4f pose = stack.last().pose();

        float smoothTime = time * 0.05f;
        long snapTime = time / 4;

        POINT_CACHE[0] = start;
        int totalPoints = generatePath(start, end, 1, 5, 0.18f, snapTime, smoothTime);
        POINT_CACHE[totalPoints - 1] = end;

        drawRibbon(vc, pose, totalPoints, 0.08f, 1.0f, 0.7f, 0.8f, 0.3f, time);
        drawRibbon(vc, pose, totalPoints, 0.02f, 0.6f, 0.1f, 0.9f, 1.0f, time);
    }

    private int generatePath(Vec3 s, Vec3 e, int index, int depth, float jitter, long snapTime, float smoothTime) {
        if (depth == 0) {
            POINT_CACHE[index] = e;
            return index + 1;
        }

        Vec3 mid = s.add(e).scale(0.5);
        float seed = (float) (snapTime + (index * 1.5f) + smoothTime);
        float currentJitter = jitter * (float) Math.sqrt(depth);

        mid = mid.add(
                (sinNoise(seed) - 0.5) * currentJitter,
                (sinNoise(seed + 12) - 0.5) * currentJitter,
                (sinNoise(seed + 24) - 0.5) * currentJitter);

        int nextIndex = generatePath(s, mid, index, depth - 1, jitter, snapTime, smoothTime);
        return generatePath(mid, e, nextIndex, depth - 1, jitter, snapTime, smoothTime);
    }

    private void drawRibbon(VertexConsumer vc, Matrix4f pose, int count, float width, float r, float g, float b,
                            float a, long time) {
        for (int i = 0; i < count - 1; i++) {
            Vec3 s = POINT_CACHE[i];
            Vec3 e = POINT_CACHE[i + 1];

            float dx = (float) (e.x - s.x);
            float dy = (float) (e.y - s.y);
            float dz = (float) (e.z - s.z);

            float twistSpeed = time * 0.1f;
            float roll = (i * 0.5f) + twistSpeed;

            float angleX = (float) Math.cos(roll) * width;
            float angleY = (float) Math.sin(roll) * width;

            float nx = -dz * angleX;
            float ny = angleY;
            float nz = dx * angleX;

            vc.vertex(pose, (float) s.x - nx, (float) s.y - ny, (float) s.z - nz).color(r, g, b, a).endVertex();
            vc.vertex(pose, (float) s.x + nx, (float) s.y + ny, (float) s.z + nz).color(r, g, b, a).endVertex();

            float nextRoll = ((i + 1) * 0.5f) + twistSpeed;
            float nnx = -dz * (float) Math.cos(nextRoll) * width;
            float nny = (float) Math.sin(nextRoll) * width;
            float nnz = dx * (float) Math.cos(nextRoll) * width;

            vc.vertex(pose, (float) e.x + nnx, (float) e.y + nny, (float) e.z + nnz).color(r, g, b, a).endVertex();
            vc.vertex(pose, (float) e.x - nnx, (float) e.y - nny, (float) e.z - nnz).color(r, g, b, a).endVertex();
        }
    }

    private float sinNoise(float n) {
        return (float) (Math.sin(n * 2137.123) * 43758.5453) % 1.0f;
    }
}
