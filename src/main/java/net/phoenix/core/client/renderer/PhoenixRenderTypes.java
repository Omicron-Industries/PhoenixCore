package net.phoenix.core.client.renderer;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

public class PhoenixRenderTypes extends RenderType {

    private static final RenderType BLACK_HOLE_TEST_SOLID = RenderType.create(
            "phoenix_black_hole_test_solid",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .createCompositeState(false));

    private static final RenderType RADIAL_LINES = RenderType.create(
            "phoenix_radial_lines",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES,
            256,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
                    .setLineState(new RenderStateShard.LineStateShard(java.util.OptionalDouble.of(2.5D)))

                    .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));

    private static final RenderType BLACK_HOLE_TEST_TRANSLUCENT = RenderType.create(
            "phoenix_black_hole_test_translucent",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(new WriteMaskStateShard(true, false))
                    .createCompositeState(false));

    private static final RenderType HONEY_FOG = RenderType.create(
            "phoenix_honey_fog",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256,
            true,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setLayeringState(RenderStateShard.NO_LAYERING)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    private static final RenderType LIGHT_RING = RenderType.create(
            "phoenix_light_ring",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                    .setLayeringState(RenderStateShard.NO_LAYERING)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));

    private PhoenixRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode,
                               int bufferSize, boolean affectsCrumbling, boolean sortOnUpload,
                               Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static RenderType HONEY_FOG() {
        return HONEY_FOG;
    }

    public static RenderType LIGHT_RING() {
        return LIGHT_RING;
    }

    public static RenderType BLACK_HOLE_TEST_SOLID() {
        return BLACK_HOLE_TEST_SOLID;
    }

    public static RenderType BLACK_HOLE_TEST_TRANSLUCENT() {
        return BLACK_HOLE_TEST_TRANSLUCENT;
    }

    public static RenderType RADIAL_LINES() {
        return RADIAL_LINES;
    }
}
