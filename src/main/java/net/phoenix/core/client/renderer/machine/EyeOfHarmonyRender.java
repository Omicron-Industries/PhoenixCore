package net.phoenix.core.client.renderer.machine;

import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;
import com.gregtechceu.gtceu.client.util.ModelEventHelper;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.model.data.ModelData;
import net.phoenix.core.PhoenixCore;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import org.joml.Quaternionf;

import java.util.List;

public class EyeOfHarmonyRender extends DynamicRender<WorkableElectricMultiblockMachine, EyeOfHarmonyRender> {

    public static final EyeOfHarmonyRender INSTANCE = new EyeOfHarmonyRender();
    public static final Codec<EyeOfHarmonyRender> CODEC = Codec.unit(EyeOfHarmonyRender.INSTANCE);
    public static final DynamicRenderType<WorkableElectricMultiblockMachine, EyeOfHarmonyRender> TYPE = new DynamicRenderType<>(
            EyeOfHarmonyRender.CODEC);

    public static final ResourceLocation SPACE_SHELL_MODEL_RL = PhoenixCore.id("obj/space");
    public static final ResourceLocation STAR_MODEL_RL = PhoenixCore.id("obj/star");
    public static final List<ResourceLocation> ORBIT_OBJECTS_RL = List.of(
            PhoenixCore.id("obj/the_nether"),
            PhoenixCore.id("obj/overworld"),
            PhoenixCore.id("obj/the_end"));

    private static BakedModel spaceShellModel, starModel, netherModel, overworldModel, endModel;
    private static final List<BakedModel> orbitModels = new java.util.ArrayList<>();
    static final RandomSource random = RandomSource.create();

    private EyeOfHarmonyRender() {
        ModelEventHelper.registerBakeEventListener(true, (rl, bakedModel, rootModel, modelBakery) -> {
            if (rl.equals(SPACE_SHELL_MODEL_RL)) {
                spaceShellModel = bakedModel;
            } else if (rl.equals(STAR_MODEL_RL)) {
                starModel = bakedModel;
            } else if (rl.equals(ORBIT_OBJECTS_RL.get(0))) {
                netherModel = bakedModel;
                updateOrbitList();
            } else if (rl.equals(ORBIT_OBJECTS_RL.get(1))) {
                overworldModel = bakedModel;
                updateOrbitList();
            } else if (rl.equals(ORBIT_OBJECTS_RL.get(2))) {
                endModel = bakedModel;
                updateOrbitList();
            }
            return bakedModel;
        });
    }

    private static void updateOrbitList() {
        orbitModels.clear();
        if (netherModel != null) orbitModels.add(netherModel);
        if (overworldModel != null) orbitModels.add(overworldModel);
        if (endModel != null) orbitModels.add(endModel);
    }

    @Override
    public DynamicRenderType<WorkableElectricMultiblockMachine, EyeOfHarmonyRender> getType() {
        return TYPE;
    }

    @Override
    public void render(WorkableElectricMultiblockMachine machine, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!machine.isFormed() || !machine.getRecipeLogic().isActive()) {
            return;
        }

        float tick = (machine.getOffsetTimer() + partialTick);
        VertexConsumer consumer = buffer.getBuffer(Sheets.translucentCullBlockSheet());

        double x = 0.5, y = 0.5, z = 0.5;
        switch (machine.getFrontFacing()) {
            case NORTH -> z = 16.5;
            case SOUTH -> z = -15.5;
            case WEST -> x = 16.5;
            case EAST -> x = -15.5;
        }

        poseStack.pushPose();
        poseStack.translate(x, y, z);

        renderStar(tick, poseStack, consumer, packedLight, packedOverlay);
        renderOrbitObjects(tick, poseStack, consumer, packedLight, packedOverlay);
        renderOuterSpaceShell(poseStack, consumer, packedLight, packedOverlay);

        poseStack.popPose();
    }

    private void renderStar(float tick, PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        if (starModel == null) return;
        poseStack.pushPose();
        poseStack.scale(0.05F, 0.05F, 0.05F);
        poseStack.mulPose(new Quaternionf().rotateYXZ((tick / 2.5f) * 0.017453292F, (tick / 2f) * 0.017453292F, 0));
        renderModel(poseStack, consumer, starModel, 1.0F, 1.0F, 1.0F, 1.0f, light, overlay);
        poseStack.popPose();
    }

    private void renderOrbitObjects(float tick, PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        for (int a = 0; a < orbitModels.size(); a++) {
            BakedModel model = orbitModels.get(a);
            if (model == null) continue;

            float scale = 0.01F + 0.006F * a;

            float orbitRadius;
            switch (a) {
                case 0:
                    orbitRadius = 3.99f;
                    break;
                case 1:
                    orbitRadius = 3.99f + (1 * 2.99f);
                    break;
                case 2:
                    orbitRadius = 3.99f + (2 * 2.99f);
                    break;
                default:
                    orbitRadius = 0.0f;
            }

            float orbitSpeed = 1.5F / (a + 1);
            float rotationSpeed = tick * orbitSpeed;

            poseStack.pushPose();

            float orbitX = (float) (orbitRadius * Math.sin(rotationSpeed * 0.05f));
            float orbitZ = (float) (orbitRadius * Math.cos(rotationSpeed * 0.05f));
            poseStack.translate(orbitX, 0, orbitZ);

            poseStack.mulPose(new Quaternionf().rotateY((tick * 1.5f / (a + 1)) * 0.017453292F));
            poseStack.scale(scale, scale, scale);

            float r = 1.0f, g = 1.0f, b = 1.0f;
            switch (a) {
                case 0:
                    r = 1.0f;
                    g = 0.6f;
                    b = 0.4f;
                    break;
                case 2:
                    r = 0.8f;
                    g = 0.6f;
                    b = 1.0f;
                    break;
            }

            renderModel(poseStack, consumer, model, r, g, b, 1.0f, light, overlay);

            poseStack.popPose();
        }
    }

    private void renderOuterSpaceShell(PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        if (spaceShellModel == null) return;
        poseStack.pushPose();
        float scale = 0.173F;
        poseStack.scale(scale, scale, scale);
        renderModel(poseStack, consumer, spaceShellModel, 1.0F, 1.0F, 1.0F, 0.8f, light, overlay);
        poseStack.popPose();
    }

    private void renderModel(PoseStack poseStack, VertexConsumer consumer, BakedModel model, float r, float g, float b,
                             float a, int light, int overlay) {
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
