package net.phoenix.core.conflux.client.render;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.conflux.research.ResearchNode;
import org.jetbrains.annotations.Nullable;

/**
 * Owns all visual logic for one Discipline's research tree.
 *
 * The contract: shape geometry (node placement, connector style) and any
 * shader/particle state share the same {@link MotionClock} instance so they
 * are guaranteed to stay in phase. Neither system should invent its own timer.
 *
 * The coordinate system given to all render methods is canvas-space (the
 * pose-stack has already been translated by pan and scaled by zoom).
 */
public interface DisciplineRenderer {

    /** Discipline ID string this handles, or {@code null} for the default fallback. */
    @Nullable String disciplineId();

    /** The {@link MotionClock.Signature} that drives all animations for this discipline. */
    MotionClock.Signature signature();

    /** Called once when this discipline becomes the active view. */
    default void onActivate(MotionClock clock) {}

    /** Called when a node finishes unlocking. Trigger event animations here. */
    default void onUnlock(ResearchNode node, MotionClock clock, IntensityController intensity) {
        intensity.onEvent();
    }

    /** Per-frame update — advance particle systems, growth timers, glitch state, etc. */
    void tick(float delta, RenderContext ctx);

    /**
     * Render the canvas background (texture wash, particle field, ambient glow).
     * Called BEFORE edges and nodes. Pose stack is in canvas space.
     */
    void renderBackground(GuiGraphics g, RenderContext ctx);

    /**
     * Render all connector edges.
     * Call {@link #nodePos} to get the discipline-correct screen position of each node.
     */
    void renderEdges(GuiGraphics g, RenderContext ctx);

    /**
     * Render all nodes.
     */
    void renderNodes(GuiGraphics g, RenderContext ctx);

    /**
     * Called after nodes — render foreground particles, overlay glows, etc.
     * Optional.
     */
    default void renderForeground(GuiGraphics g, RenderContext ctx) {}

    /**
     * Convert a node's grid-space (posX, posY) to canvas-space pixel position.
     * Override to implement radial (Phoenix), parallax (Void), etc.
     *
     * @return float[2] = {x, y} in canvas space
     */
    float[] nodePos(ResearchNode node, RenderContext ctx);

    /**
     * Hit-test: returns true if canvas-space point (mx, my) hits this node.
     * Default uses a square radius; override for per-discipline shapes.
     */
    default boolean hitsNode(ResearchNode node, float mx, float my, RenderContext ctx) {
        float[] p = nodePos(node, ctx);
        return Math.abs(mx - p[0]) <= 22 && Math.abs(my - p[1]) <= 22;
    }

    /**
     * Resource location of the PostChain shader to load when this discipline is active.
     * Return {@code null} to skip post-processing for this discipline.
     */
    default @Nullable ResourceLocation shaderLocation() { return null; }

    // ── Shader data hooks (used by AxiomShaderManager.pushFrameData) ─────────

    /** Flat [x0,y0, x1,y1, ...] of ripple origin positions in canvas space. Sculk only. */
    default float[] rippleOriginsCanvas() { return new float[0]; }

    /** Age [0,1] per ripple entry. */
    default float[] rippleAges() { return new float[0]; }

    /** Number of active ripples. */
    default int rippleCount() { return 0; }

    /**
     * Per-node heat/intensity [0,1] for the shader, indexed to match
     * the order returned by iterating visible nodes. Phoenix only.
     * Returns {@code null} to use a default of 1.0 for all.
     */
    default @Nullable float[] nodeHeatStrength(RenderContext ctx) { return null; }
}
