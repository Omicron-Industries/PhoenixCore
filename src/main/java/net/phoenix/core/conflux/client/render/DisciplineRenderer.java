package net.phoenix.core.conflux.client.render;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.conflux.research.ResearchNode;

import org.jetbrains.annotations.Nullable;

public interface DisciplineRenderer {

    @Nullable
    String disciplineId();

    MotionClock.Signature signature();

    default void onActivate(MotionClock clock) {}

    default void onUnlock(ResearchNode node, MotionClock clock, IntensityController intensity) {
        intensity.onEvent();
    }

    void tick(float delta, RenderContext ctx);

    void renderBackground(GuiGraphics g, RenderContext ctx);

    void renderEdges(GuiGraphics g, RenderContext ctx);

    void renderNodes(GuiGraphics g, RenderContext ctx);

    default void renderForeground(GuiGraphics g, RenderContext ctx) {}

    float[] nodePos(ResearchNode node, RenderContext ctx);

    default boolean hitsNode(ResearchNode node, float mx, float my, RenderContext ctx) {
        float[] p = nodePos(node, ctx);
        return Math.abs(mx - p[0]) <= 22 && Math.abs(my - p[1]) <= 22;
    }

    default @Nullable ResourceLocation shaderLocation() {
        return null;
    }

    default float[] rippleOriginsCanvas() {
        return new float[0];
    }

    default float[] rippleAges() {
        return new float[0];
    }

    default int rippleCount() {
        return 0;
    }

    default @Nullable float[] nodeHeatStrength(RenderContext ctx) {
        return null;
    }
}
