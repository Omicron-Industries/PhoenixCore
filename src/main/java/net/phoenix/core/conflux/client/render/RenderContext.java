package net.phoenix.core.conflux.client.render;

import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.conflux.research.ResearchNode;
import net.phoenix.core.conflux.research.ResearchTree;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Immutable per-frame snapshot passed to every {@link DisciplineRenderer} method.
 * Keeps method signatures sane and makes it easy to add data without breaking
 * every renderer.
 */
public record RenderContext(
        ResearchTree tree,
        Set<ResourceLocation> unlocked,
        Set<ResourceLocation> lockedOut,
        @Nullable ResearchNode selected,
        /** Mouse position in canvas space (after pan/zoom transform). */
        float canvasMx,
        float canvasMy,
        int canvasW,
        int canvasH,
        MotionClock clock,
        IntensityController intensity
) {
    public boolean isUnlocked(ResourceLocation id)  { return unlocked.contains(id); }
    public boolean isLockedOut(ResourceLocation id) { return lockedOut.contains(id); }
    public boolean isSelected(ResearchNode n) { return selected != null && selected.id.equals(n.id); }

    public boolean isAvailable(ResearchNode n) {
        return !isUnlocked(n.id) && !isLockedOut(n.id)
                && n.canUnlock(unlocked, lockedOut);
    }
}
