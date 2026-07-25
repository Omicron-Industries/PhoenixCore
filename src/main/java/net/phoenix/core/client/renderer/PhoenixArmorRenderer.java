package net.phoenix.core.client.renderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.phoenix.core.common.data.item.PhoenixArmorItem;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class PhoenixArmorRenderer extends GeoArmorRenderer<PhoenixArmorItem> {

    public PhoenixArmorRenderer(PhoenixArmorItem phoenixItem) {
        super(new PhoenixArmorModel(phoenixItem));

        addRenderLayer(new GeoRenderLayer<PhoenixArmorItem>(this) {

            @Override
            public void render(PoseStack poseStack, PhoenixArmorItem animatable, BakedGeoModel model,
                               RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                               float partialTick, int packedLight, int packedOverlay) {
                if (getCurrentSlot() == EquipmentSlot.CHEST) {
                    ResourceLocation wingsTex = new ResourceLocation("phoenixcore", "textures/armor/phoenix_wings.png");

                    RenderType wingsRenderType = RenderType.armorCutoutNoCull(wingsTex);

                    getRenderer().reRender(model, poseStack, bufferSource, animatable, wingsRenderType,
                            bufferSource.getBuffer(wingsRenderType), partialTick, packedLight, packedOverlay,
                            1, 1, 1, 1);
                }
            }
        });
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
        setAllVisible(false);
        switch (currentSlot) {
            case CHEST -> {
                setBoneVisible(this.body, true);
                setBoneVisible(this.rightArm, true);
                setBoneVisible(this.leftArm, true);
                getGeoModel().getBone("wing_1").ifPresent(b -> b.setHidden(false));
                getGeoModel().getBone("wing_2").ifPresent(b -> b.setHidden(false));
            }
            case LEGS -> {
                setBoneVisible(this.rightLeg, true);
                setBoneVisible(this.leftLeg, true);
            }
            case FEET -> {
                setBoneVisible(this.rightBoot, true);
                setBoneVisible(this.leftBoot, true);
            }
            case HEAD -> setBoneVisible(this.head, true);
        }
    }
}
