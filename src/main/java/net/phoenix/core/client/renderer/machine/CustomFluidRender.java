package net.phoenix.core.client.renderer.machine;

import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.common.machine.trait.multiblock.MultiblockFluidRendererTrait;
import com.gregtechceu.gtceu.client.renderer.block.FluidBlockRenderer;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;
import com.gregtechceu.gtceu.client.util.RenderUtil;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.RenderTypeHelper;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Custom renderer that draws the current recipe output fluid on chosen faces.
 */
@SuppressWarnings("all")
public class CustomFluidRender extends DynamicRender<WorkableMultiblockMachine, CustomFluidRender> {

    public static final CustomFluidRender INSTANCE = new CustomFluidRender();
    public static final Codec<CustomFluidRender> CODEC = Codec.unit(CustomFluidRender::new);
    public static final DynamicRenderType<WorkableMultiblockMachine, CustomFluidRender> TYPE = new DynamicRenderType<>(CODEC);

    private final FluidBlockRenderer fluidRenderer;
    private final List<RelativeDirection> drawFaces;

    private @Nullable Fluid cachedFluid = null;

    public CustomFluidRender() {
        this.fluidRenderer = FluidBlockRenderer.Builder.create()
                .setFaceOffset(-0.125f)
                .setForcedLight(LightTexture.FULL_BRIGHT)
                .getRenderer();

        this.drawFaces = List.of(
                RelativeDirection.DOWN,  // bottom
                RelativeDirection.UP     // top
        );
    }

    @Override
    public @NotNull DynamicRenderType<WorkableMultiblockMachine, CustomFluidRender> getType() {
        return TYPE;
    }

    @Override
    public void render(@NotNull WorkableMultiblockMachine machine, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        if (!ConfigHolder.INSTANCE.client.renderer.renderFluids) return;

        // FIX 1: Passed .TYPE instead of .class
        var fluidTrait = machine.getTrait(MultiblockFluidRendererTrait.TYPE);
        if (fluidTrait == null || !machine.isFormed()) return;

        Set<BlockPos> fluidOffsets = fluidTrait.getFluidOffsets();
        if (fluidOffsets == null || fluidOffsets.isEmpty()) return;

        var recipeLogic = machine.getRecipeLogic();
        if (recipeLogic == null) return;

        var lastRecipe = recipeLogic.getLastRecipe();
        if (lastRecipe == null) return;

        if (recipeLogic.isActive()) {
            if (cachedFluid == null || machine.getOffsetTimer() % 20 == 0) {
                cachedFluid = RenderUtil.getRecipeFluidToRender(lastRecipe);
            }
        } else {
            cachedFluid = null;
        }

        if (cachedFluid == null) return;

        var fluidRenderType = ItemBlockRenderTypes.getRenderLayer(cachedFluid.defaultFluidState());
        var consumer = buffer.getBuffer(RenderTypeHelper.getEntityRenderType(fluidRenderType, false));

        for (RelativeDirection face : drawFaces) {
            poseStack.pushPose();

            Direction dir = face.getRelativeFacing(
                    machine.getFrontFacing(),
                    machine.getUpwardsFacing(),
                    false
            );

            if (dir.getAxis() != Direction.Axis.Y) {
                dir = dir.getOpposite();
            }

            // FIX 2: Let drawPlane handle the Collection natively instead of looping manually.
            // Passed fluidOffsets, poseStack, and machine.getBlockPos() directly matching the signature.
            fluidRenderer.drawPlane(
                    dir,
                    fluidOffsets,
                    poseStack,
                    consumer,
                    cachedFluid,
                    RenderUtil.FluidTextureType.STILL,
                    packedOverlay, // 7. combinedOverlay
                    packedLight    // 8. combinedLight
            );

            poseStack.popPose();
        }
    }

    @Override
    public boolean shouldRenderOffScreen(@NotNull WorkableMultiblockMachine machine) {
        return true;
    }

    @Override
    public @NotNull AABB getRenderBoundingBox(WorkableMultiblockMachine machine) {
        return new AABB(machine.getBlockPos()).inflate(32);
    }
}