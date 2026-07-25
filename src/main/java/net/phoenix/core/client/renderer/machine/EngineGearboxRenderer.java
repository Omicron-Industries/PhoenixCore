package net.phoenix.core.client.renderer.machine;

import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.phoenix.core.client.renderer.PhoenixRenderTypes;
import net.phoenix.core.integration.ars_nouveau.common.data.multiblock.source.BioAethericEngineMachine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.mojang.math.Axis.YP;

public class EngineGearboxRenderer extends DynamicRender<BioAethericEngineMachine, EngineGearboxRenderer> {

    public static final EngineGearboxRenderer INSTANCE = new EngineGearboxRenderer();
    public static final Codec<EngineGearboxRenderer> CODEC = Codec.unit(INSTANCE);
    public static final DynamicRenderType<BioAethericEngineMachine, EngineGearboxRenderer> TYPE = new DynamicRenderType<>(
            CODEC);

    private static final int PARTICLE_COUNT = 60;
    private final List<GearParticle> particles = new ArrayList<>();
    private final Random random = new Random();

    private EngineGearboxRenderer() {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particles.add(new GearParticle(random));
        }
    }

    @Override
    public @NotNull DynamicRenderType<BioAethericEngineMachine, EngineGearboxRenderer> getType() {
        return TYPE;
    }

    @Override
    public void render(@NotNull BioAethericEngineMachine machine, float partialTick,
                       @NotNull PoseStack stack, @NotNull MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        RecipeLogic logic = machine.getRecipeLogic();
        if (logic == null || !logic.isWorking()) return;

        net.minecraft.core.Direction facing = machine.getFrontFacing();
        float progress = (float) logic.getProgressPercent();
        VertexConsumer vc = buffer.getBuffer(PhoenixRenderTypes.LIGHT_RING());

        stack.pushPose();

        stack.translate(0.5, 0.5, 0.5);

        float angle = -facing.toYRot();
        stack.mulPose(YP.rotationDegrees(angle));

        stack.mulPose(YP.rotationDegrees(90f));

        stack.translate(3, 0, 0.0);

        renderEnergyStream(stack, vc, progress);

        stack.popPose();
    }

    private void renderEnergyStream(PoseStack stack, VertexConsumer vc, float progress) {
        Vec3 start = new Vec3(-2.0, 0.0, 0.0);
        Vec3 end = new Vec3(2.0, 0.0, 0.0);

        float r = 0.0f, g = 1.0f, b = 1.0f;

        for (GearParticle p : particles) {
            p.update(random);

            Vec3 pos = start.lerp(end, p.progress).add(p.jitter);

            float alpha = Mth.sin(p.progress * Mth.PI) * 0.8f;
            float size = 0.04f + (p.randomScale * 0.03f);

            stack.pushPose();
            stack.translate(pos.x, pos.y, pos.z);
            stack.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());

            Matrix4f matrix = stack.last().pose();
            drawParticleQuad(vc, matrix, size, r, g, b, alpha);

            stack.popPose();
        }
    }

    private void drawParticleQuad(VertexConsumer vc, Matrix4f matrix, float s, float r, float g, float b, float a) {
        vc.vertex(matrix, -s, -s, 0).color(r, g, b, a).endVertex();
        vc.vertex(matrix, -s, s, 0).color(r, g, b, a).endVertex();
        vc.vertex(matrix, s, s, 0).color(r, g, b, a).endVertex();
        vc.vertex(matrix, s, -s, 0).color(r, g, b, a).endVertex();
    }

    @Override
    public @NotNull AABB getRenderBoundingBox(BioAethericEngineMachine m) {
        return new AABB(m.getBlockPos()).inflate(5.0);
    }

    private static class GearParticle {

        float progress;
        float speed;
        float randomScale;
        Vec3 jitter;

        GearParticle(Random r) {
            reset(r);
            progress = r.nextFloat();
        }

        void reset(Random r) {
            progress = 0;
            speed = 0.015f + r.nextFloat() * 0.025f;
            randomScale = r.nextFloat();

            jitter = new Vec3(0, (r.nextFloat() - 0.5) * 0.3, (r.nextFloat() - 0.5) * 0.3);
        }

        void update(Random r) {
            progress += speed;
            if (progress > 1.0f) reset(r);
        }
    }
}
