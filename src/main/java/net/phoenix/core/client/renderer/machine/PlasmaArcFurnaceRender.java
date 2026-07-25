package net.phoenix.core.client.renderer.machine;

import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;
import com.gregtechceu.gtceu.client.util.ModelEventHelper;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.model.data.ModelData;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.machine.multiblock.Shield;
import net.phoenix.core.common.machine.multiblock.electric.HighPressurePlasmaArcFurnaceMachine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import org.joml.Quaternionf;

import java.util.List;

@SuppressWarnings("all")
public class PlasmaArcFurnaceRender extends DynamicRender<WorkableElectricMultiblockMachine, PlasmaArcFurnaceRender> {

    public static final PlasmaArcFurnaceRender INSTANCE = new PlasmaArcFurnaceRender();
    public static final Codec<PlasmaArcFurnaceRender> CODEC = Codec.unit(PlasmaArcFurnaceRender.INSTANCE);
    public static final DynamicRenderType<WorkableElectricMultiblockMachine, PlasmaArcFurnaceRender> TYPE = new DynamicRenderType<>(
            PlasmaArcFurnaceRender.CODEC);

    public static final ResourceLocation SPHERE_MODEL_RL = PhoenixCore.id("obj/blue_star");
    public static final ResourceLocation RINGS_MODEL_RL = PhoenixCore.id("obj/rings");

    private static BakedModel sphereModel;
    private static BakedModel ringsModel;
    private static final RandomSource random = RandomSource.create();

    private static final float ROTATION_SPEED = 0.025F;
    private static final float MAX_ROTATION_ANGLE = 30.0F;
    private static final float RINGS_ROTATION_SPEED = 0.5F;

    private PlasmaArcFurnaceRender() {
        ModelEventHelper.registerBakeEventListener(true, (rl, bakedModel, rootModel, modelBakery) -> {
            if (rl.equals(SPHERE_MODEL_RL)) {
                sphereModel = bakedModel;
            } else if (rl.equals(RINGS_MODEL_RL)) {
                ringsModel = bakedModel;
            }
            return bakedModel;
        });
    }

    @Override
    public DynamicRenderType<WorkableElectricMultiblockMachine, PlasmaArcFurnaceRender> getType() {
        return TYPE;
    }

    @Override
    public void render(WorkableElectricMultiblockMachine machine, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!(machine instanceof HighPressurePlasmaArcFurnaceMachine furnace)) return;

        if (!furnace.isFormed() || furnace.getShieldType() != Shield.ShieldTypes.NORMAL) {
            return;
        }

        float tick = (machine.getOffsetTimer() + partialTick);
        double x = 0.5;
        double y = 2.5;
        double z = 0.5;

        switch (machine.getFrontFacing()) {
            case NORTH -> z -= 20.0;
            case SOUTH -> z += 20.0;
            case WEST -> x -= 20.0;
            case EAST -> x += 20.0;
        }

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        renderCelestialObject(tick, poseStack, buffer, packedOverlay);
        poseStack.popPose();
    }

    private void renderCelestialObject(float tick, PoseStack poseStack, MultiBufferSource buffer, int packedOverlay) {
        if (sphereModel == null || ringsModel == null) return;
        poseStack.pushPose();

        float angleY = MAX_ROTATION_ANGLE * (float) Math.sin(tick * ROTATION_SPEED);
        float angleX = (MAX_ROTATION_ANGLE / 2.0f) * (float) Math.sin(tick * ROTATION_SPEED * 2.0f);
        poseStack.mulPose(new Quaternionf().fromAxisAngleDeg(0.0F, 1.0F, 0.0F, angleY));
        poseStack.mulPose(new Quaternionf().fromAxisAngleDeg(1.0F, 0.0F, 0.0F, angleX));

        poseStack.pushPose();
        poseStack.scale(0.1F, 0.1F, 0.1F);
        renderModel(poseStack, buffer.getBuffer(Sheets.solidBlockSheet()), sphereModel, 1.0F, 1.0F, 1.0F, 1.0f,
                LightTexture.FULL_BRIGHT, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();

        float ringsAngle = (tick * RINGS_ROTATION_SPEED) % 360;
        poseStack.mulPose(new Quaternionf().fromAxisAngleDeg(0.0F, 1.0F, 0.0F, ringsAngle));

        poseStack.scale(6.4F, 6.4F, 6.4F);
        renderModel(poseStack, buffer.getBuffer(Sheets.solidBlockSheet()), ringsModel, 1.0F, 1.0F, 1.0F, 0.6F,
                LightTexture.FULL_BRIGHT, packedOverlay);
        poseStack.popPose();

        poseStack.popPose();
    }

    private void renderModel(PoseStack poseStack, VertexConsumer consumer, BakedModel model, float r, float g, float b,
                             float a, int light, int overlay) {
        if (model == null) return;
        PoseStack.Pose pose = poseStack.last();
        List<BakedQuad> quads = model.getQuads(null, null, random, ModelData.EMPTY, null);
        for (BakedQuad quad : quads) {
            consumer.putBulkData(pose, quad, r, g, b, a, light, overlay, false);
        }
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRenderOffScreen(WorkableElectricMultiblockMachine machine) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(WorkableElectricMultiblockMachine machine) {
        return new AABB(machine.getBlockPos()).inflate(getViewDistance());
    }
}
