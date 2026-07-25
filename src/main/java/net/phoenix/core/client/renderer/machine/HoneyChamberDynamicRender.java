package net.phoenix.core.client.renderer.machine;

import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.client.renderer.block.FluidBlockRenderer;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;
import com.gregtechceu.gtceu.client.util.RenderUtil;
import com.gregtechceu.gtceu.config.ConfigHolder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.RenderTypeHelper;
import net.phoenix.core.client.renderer.PhoenixRenderTypes;
import net.phoenix.core.common.machine.multiblock.electric.HoneyCrystallizationChamberMachine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HoneyChamberDynamicRender extends
                                       DynamicRender<HoneyCrystallizationChamberMachine, HoneyChamberDynamicRender> {

    public static final HoneyChamberDynamicRender INSTANCE = new HoneyChamberDynamicRender();
    public static final Codec<HoneyChamberDynamicRender> CODEC = Codec.unit(HoneyChamberDynamicRender.INSTANCE);
    public static final DynamicRenderType<HoneyCrystallizationChamberMachine, HoneyChamberDynamicRender> TYPE = new DynamicRenderType<>(
            CODEC);

    private static final int STRAND_COUNT = 8;
    private static final float STRAND_RADIUS = 0.35f;
    private static final int STRAND_SEGMENTS = 24;
    private static final int BEE_COUNT = 64;
    private static final int MOTE_COUNT = 150;
    private static final float FIELD_RADIUS = 30.0f;
    private static final int FOG_PARTICLE_COUNT = 75;

    private final FluidBlockRenderer fluidRenderer = FluidBlockRenderer.Builder.create()
            .setFaceOffset(-0.125f)
            .setForcedLight(LightTexture.FULL_BRIGHT)
            .getRenderer();

    private final List<RelativeDirection> fluidFaces = List.of(RelativeDirection.DOWN, RelativeDirection.UP);
    private final List<BeeParticle> bees = new ArrayList<>();
    private final List<MoteParticle> motes = new ArrayList<>();
    private final List<FogParticle> fogParticles = new ArrayList<>();
    private final Random random = new Random();
    private @Nullable Fluid cachedFluid = null;

    private HoneyChamberDynamicRender() {
        for (int i = 0; i < BEE_COUNT; i++) bees.add(new BeeParticle(random));
        for (int i = 0; i < MOTE_COUNT; i++) motes.add(new MoteParticle(random));
        for (int i = 0; i < FOG_PARTICLE_COUNT; i++) fogParticles.add(new FogParticle(random));
    }

    @Override
    public @NotNull DynamicRenderType<HoneyCrystallizationChamberMachine, HoneyChamberDynamicRender> getType() {
        return TYPE;
    }

    @Override
    public void render(@NotNull HoneyCrystallizationChamberMachine machine, float partialTick,
                       @NotNull PoseStack stack, @NotNull MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        if (!machine.isFormed()) return;

        float time = (machine.getOffsetTimer() + partialTick) * 0.02f;
        RecipeLogic logic = machine.getRecipeLogic();
        float progress = (logic != null && logic.isWorking()) ? (float) logic.getProgressPercent() : 0f;

        stack.pushPose();

        renderFluid(machine, stack, buffer, packedLight, packedOverlay);

        stack.translate(0.5, 0.5, 0.5);

        renderHoneyFog(stack, buffer.getBuffer(PhoenixRenderTypes.HONEY_FOG()), time, progress);

        VertexConsumer additiveVc = buffer.getBuffer(PhoenixRenderTypes.LIGHT_RING());
        renderHoneyStrands(stack, additiveVc, time, progress);
        renderMotes(stack, additiveVc, time, progress);
        renderBees(stack, additiveVc, time, progress);

        stack.popPose();
    }

    private void renderFluid(HoneyCrystallizationChamberMachine machine, PoseStack stack, MultiBufferSource buffer,
                             int packedLight, int packedOverlay) {
        if (!ConfigHolder.INSTANCE.client.renderer.renderFluids) return;
        var offsets = machine.getFluidOffsets();
        if (offsets == null) return;

        var recipeLogic = machine.getRecipeLogic();
        if (recipeLogic == null || recipeLogic.getLastRecipe() == null) return;

        if (recipeLogic.isActive()) {
            if (cachedFluid == null || machine.getOffsetTimer() % 20 == 0) {
                cachedFluid = RenderUtil.getRecipeFluidToRender(recipeLogic.getLastRecipe());
            }
        } else {
            cachedFluid = null;
        }

        if (cachedFluid == null) return;

        var fluidLayer = ItemBlockRenderTypes.getRenderLayer(cachedFluid.defaultFluidState());
        var consumer = buffer.getBuffer(RenderTypeHelper.getEntityRenderType(fluidLayer, false));

        for (RelativeDirection face : fluidFaces) {
            stack.pushPose();

            var dir = face.getRelativeFacing(machine.getFrontFacing(), machine.getUpwardsFacing(), machine.isFlipped());

            if (dir.getAxis() != Direction.Axis.Y) dir = dir.getOpposite();

            fluidRenderer.drawPlane(
                    dir,
                    offsets,
                    stack,
                    consumer,
                    cachedFluid,
                    RenderUtil.FluidTextureType.STILL,
                    packedOverlay,
                    packedLight);
            stack.popPose();
        }
    }

    private void renderHoneyStrands(PoseStack stack, VertexConsumer vc, float time, float progress) {
        Matrix4f pose = stack.last().pose();
        float r1 = Mth.lerp(progress, 0.90f, 1.00f);
        float g1 = Mth.lerp(progress, 0.65f, 0.85f);
        float b1 = Mth.lerp(progress, 0.18f, 0.40f);

        for (int s = 0; s < STRAND_COUNT; s++) {
            float phase = (float) s / STRAND_COUNT * Mth.TWO_PI;
            Vec3 prev = getStrandPoint(phase, 0f, time);

            for (int i = 1; i <= STRAND_SEGMENTS; i++) {
                float t = (float) i / STRAND_SEGMENTS;
                Vec3 curr = getStrandPoint(phase, t, time);
                float width = (0.04f + (0.03f * progress)) * (0.8f + 0.2f * Mth.sin(time * 1.5f + s));
                float alpha = Mth.clamp((0.2f + 0.6f * progress) + 0.1f * Mth.sin(time * 2f + t * 5f), 0.1f, 0.9f);
                quadLine(vc, pose, prev, curr, width, r1, g1, b1, alpha);
                quadLine(vc, pose, prev, curr, width * 2.5f, r1, g1, b1, alpha * 0.2f);
                prev = curr;
            }
        }
    }

    private Vec3 getStrandPoint(float phase, float t, float time) {
        float angle = t * Mth.TWO_PI + time * 0.3f + phase;
        float radius = STRAND_RADIUS + 0.08f * Mth.sin(time * 0.7f + t * 4f + phase);
        float y = (t * 2.0f - 1.0f) + 0.05f * Mth.sin(time * 1.1f + phase + t * 3f);
        return new Vec3(radius * Mth.cos(angle), y, radius * Mth.sin(angle));
    }

    private void quadLine(VertexConsumer vc, Matrix4f pose, Vec3 start, Vec3 end, float width, float r, float g,
                          float b, float a) {
        Vec3 dir = end.subtract(start).normalize();
        Vec3 up = Math.abs(dir.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 side = dir.cross(up).normalize().scale(width);

        vertex(vc, pose, start.add(side), r, g, b, a);
        vertex(vc, pose, start.subtract(side), r, g, b, a);
        vertex(vc, pose, end.subtract(side), r, g, b, a);
        vertex(vc, pose, end.add(side), r, g, b, a);
    }

    private void vertex(VertexConsumer vc, Matrix4f pose, Vec3 p, float r, float g, float b, float a) {
        vc.vertex(pose, (float) p.x, (float) p.y, (float) p.z).color(r, g, b, a).endVertex();
    }

    private void renderBees(PoseStack stack, VertexConsumer vc, float time, float progress) {
        Matrix4f pose = stack.last().pose();
        for (BeeParticle bee : bees) {
            bee.update(time, progress);
            float distFactor = (float) Mth.clamp(1.0 - (bee.pos.length() / FIELD_RADIUS), 0.0, 1.0);
            float alpha = (0.3f + 0.7f * progress) * distFactor;
            quadLine(vc, pose, bee.prevPos, bee.pos, 0.05f, 1.0f, 0.8f, 0.1f, alpha);
        }
    }

    private void renderMotes(PoseStack stack, VertexConsumer vc, float time, float progress) {
        Matrix4f pose = stack.last().pose();
        for (MoteParticle mote : motes) {
            mote.update(time);
            float distFactor = (float) Mth.clamp(1.0 - (mote.pos.length() / FIELD_RADIUS), 0.0, 1.0);
            float alpha = (0.1f + 0.3f * progress) * distFactor *
                    (0.6f + 0.4f * Mth.sin(time * 2f + mote.flickerPhase));
            Vec3 p = mote.pos;
            quadLine(vc, pose, p.subtract(0, 0.03, 0), p.add(0, 0.03, 0), 0.015f, 1.0f, 0.9f, 0.4f, alpha);
            quadLine(vc, pose, p.subtract(0.03, 0, 0), p.add(0.03, 0, 0), 0.015f, 1.0f, 0.9f, 0.4f, alpha);
        }
    }

    private void renderHoneyFog(PoseStack stack, VertexConsumer vc, float time, float progress) {
        float baseAlpha = 0.05f + 0.1f * progress;
        float r = 0.95f, g = 0.75f, b = 0.3f;

        for (FogParticle particle : fogParticles) {
            particle.update(time);

            float distFactor = (float) Mth.clamp(1.0 - (particle.pos.length() / (FIELD_RADIUS * 0.8)), 0.0, 1.0);
            float alpha = baseAlpha * distFactor * particle.alpha;

            Vec3 p = particle.pos;
            float size = particle.size;

            stack.pushPose();
            stack.translate(p.x, p.y, p.z);
            stack.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
            Matrix4f pose = stack.last().pose();

            vc.vertex(pose, -size, -size, 0).color(r, g, b, alpha).endVertex();
            vc.vertex(pose, -size, size, 0).color(r, g, b, alpha).endVertex();
            vc.vertex(pose, size, size, 0).color(r, g, b, alpha).endVertex();
            vc.vertex(pose, size, -size, 0).color(r, g, b, alpha).endVertex();

            stack.popPose();
        }
    }

    @Override
    public boolean shouldRenderOffScreen(HoneyCrystallizationChamberMachine m) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public @NotNull AABB getRenderBoundingBox(HoneyCrystallizationChamberMachine m) {
        return new AABB(m.getBlockPos()).inflate(FIELD_RADIUS + 5);
    }

    private static class BeeParticle {

        Vec3 pos, prevPos;
        float radius, speed, phase, height, nX, nZ;

        BeeParticle(Random r) {
            radius = r.nextFloat() * FIELD_RADIUS;
            speed = 0.2f + r.nextFloat() * 0.5f;
            phase = r.nextFloat() * 6.28f;
            height = -10.0f + r.nextFloat() * 20.0f;
            nX = r.nextFloat() * 100f;
            nZ = r.nextFloat() * 100f;
            pos = prevPos = Vec3.ZERO;
        }

        void update(float t, float progress) {
            prevPos = pos;
            float curSpeed = speed * (1.0f + progress);
            float curRadius = radius * (1.0f - (progress * 0.5f));
            float a = t * curSpeed + phase;
            pos = new Vec3(
                    curRadius * Mth.cos(a) + Mth.sin(t * 0.5f + nX) * 2.0f,
                    height + Mth.sin(t * 0.3f) * 3.0f,
                    curRadius * Mth.sin(a) + Mth.cos(t * 0.5f + nZ) * 2.0f);
        }
    }

    private static class MoteParticle {

        Vec3 pos;
        float angle, radius, height, speed, flickerPhase;

        MoteParticle(Random r) {
            angle = r.nextFloat() * 6.28f;
            radius = r.nextFloat() * FIELD_RADIUS;
            height = -15.0f + r.nextFloat() * 30.0f;
            speed = 0.05f + r.nextFloat() * 0.15f;
            flickerPhase = r.nextFloat() * 6.28f;
            pos = Vec3.ZERO;
        }

        void update(float t) {
            float a = angle + t * speed;
            pos = new Vec3(radius * Mth.cos(a), height + Mth.sin(t * 0.2f + flickerPhase), radius * Mth.sin(a));
        }
    }

    private static class FogParticle {

        Vec3 pos;
        float size;
        float alpha;
        float speed;
        float life;
        float maxLife;

        FogParticle(Random r) {
            maxLife = 200.0f + r.nextFloat() * 200.0f;
            life = r.nextFloat() * maxLife;
            speed = 0.01f + r.nextFloat() * 0.02f;
            reset(r);
        }

        void reset(Random r) {
            pos = new Vec3(
                    (r.nextFloat() - 0.5f) * FIELD_RADIUS,
                    (r.nextFloat() - 0.5f) * 20.0f,
                    (r.nextFloat() - 0.5f) * FIELD_RADIUS);
            size = 1.5f + r.nextFloat() * 2.5f;
            alpha = 0.0f;
        }

        void update(float time) {
            life++;
            if (life > maxLife) {
                life = 0;
            }

            float lifeRatio = life / maxLife;
            alpha = Mth.sin(lifeRatio * Mth.PI);

            pos = pos.add(
                    Mth.sin(time * 0.2f + maxLife) * 0.01f,
                    speed,
                    Mth.cos(time * 0.2f + maxLife) * 0.01f);

            if (pos.y > 10.0f) {
                pos = new Vec3(pos.x, -10.0f, pos.z);
            }
        }
    }
}
