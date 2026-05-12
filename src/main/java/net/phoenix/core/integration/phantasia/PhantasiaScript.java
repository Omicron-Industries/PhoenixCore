package net.phoenix.core.integration.phantasia;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.common.block.CoilBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import lombok.Getter;
import net.phoenix.core.integration.phantasia.client.PhantasiaSceneScreen;

import java.util.*;
import java.util.function.Predicate;

/**
 * PhantasiaScript — timed animation script for a multiblock's Phantasia scene.
 */
@Getter
public class PhantasiaScript {

    // ── Step ─────────────────────────────────────────────────────────────────
    public record Step(
            int tickOffset,
            String caption,
            Predicate<BlockPos> filter,
            boolean working,
            int forceShape,
            int forceCoil,
            float yaw,    // NEW: Camera Yaw
            float pitch,  // NEW: Camera Pitch
            boolean useCam // NEW: Does this step actually want to move the camera?
    ) {
        public boolean hasCamera() { return useCam; }
    }

    // ── Common-mistake warning marker ─────────────────────────────────────────
    public record LocalWarning(BlockPos localPos, String label, int color) {

        public LocalWarning(BlockPos localPos, String label) {
            this(localPos, label, 0xFFFFB74D); // default amber
        }
    }

    // ── Heatmap tier ─────────────────────────────────────────────────────────
    public record HeatmapTier(String name, int color, Predicate<BlockPos> matcher) {}

    // ── Fields ────────────────────────────────────────────────────────────────
    private final List<Step> steps;
    private final int totalTicks;
    private final List<LocalWarning> commonMistakes;
    private final List<String> globalMistakes; // FIXED: Added missing field declaration
    private final List<HeatmapTier> heatmapTiers;

    private PhantasiaScript(List<Step> steps,
                            List<LocalWarning> commonMistakes,
                            List<String> globalMistakes,
                            List<HeatmapTier> heatmapTiers) {
        this.steps = Collections.unmodifiableList(steps);
        this.commonMistakes = Collections.unmodifiableList(commonMistakes);
        this.globalMistakes = Collections.unmodifiableList(globalMistakes);
        this.heatmapTiers = Collections.unmodifiableList(heatmapTiers);
        this.totalTicks = steps.isEmpty() ? 60 :
                steps.get(steps.size() - 1).tickOffset() + 60;
    }

    public boolean hasMistakes() {
        return !commonMistakes.isEmpty() || !globalMistakes.isEmpty();
    }

    public boolean hasCommonMistakes() {
        return !commonMistakes.isEmpty();
    }

    public boolean hasHeatmap() {
        return !heatmapTiers.isEmpty();
    }

    public Step getActiveStep(int currentTick) {
        Step active = null;
        for (Step s : steps) {
            if (s.tickOffset() <= currentTick) active = s;
            else break;
        }
        return active;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Builder
    // ─────────────────────────────────────────────────────────────────────────
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private boolean pendingWorking = false;

        public Builder setWorking(boolean working) {
            this.pendingWorking = working;
            return this;
        }


        public Builder working(boolean working) {
            this.pendingWorking = working;
            return this;
        }


        private int pendingCoil = -1; // NEW: Track coil tier

        /**
         * Sets the coil tier for this step.
         * @param index The index in your COIL_TIERS list (0=Cupronickel, 1=Kanthal, etc.)
         */
        public Builder coil(int index) {
            this.pendingCoil = index;
            return this;
        }

        private final List<Step> steps = new ArrayList<>();
        private final List<LocalWarning> commonMistakes = new ArrayList<>();
        private final List<String> globalMistakes = new ArrayList<>();
        private final List<HeatmapTier> heatmapTiers = new ArrayList<>();

        private int pendingTick = -1;
        private String pendingCaption = null;
        private Predicate<BlockPos> allowPred = null;
        private Predicate<BlockPos> denyPred = null;

        // NEW: Global string-based mistakes (no position needed)
        public Builder mistake(String message) {
            this.globalMistakes.add(message);
            return this;
        }

        public Builder step(int tickOffset, String caption) {
            commitPending();
            pendingTick = tickOffset;
            pendingCaption = caption;
            allowPred = null;
            denyPred = null;
            return this;
        }

        private BlockPos controllerWorldPos = BlockPos.ZERO;

        // Inside PhantasiaScript.java -> Builder
        // Inside the Builder class in PhantasiaScript.java
        public Builder tierState(String name, int color, Predicate<BlockState> statePredicate) {
            return tier(name, color, lp -> {
                if (PhantasiaSceneScreen.SHARED_LEVEL == null) return false;
                // Translate local pos (e.g. 0,1,0) to world pos (e.g. 512,51,0)
                BlockPos wp = lp.offset(PhantasiaSceneScreen.getOriginForCurrentPattern());
                BlockState state = PhantasiaSceneScreen.SHARED_LEVEL.getBlockState(wp);
                return statePredicate.test(state);
            });
        }

        // Inside PhantasiaScript.java -> Builder
        public Builder coilHeatmap(String name) {
            // We pass a dummy color (0) because we will override it inside the matcher
            // logic if your Script system supports dynamic colors, OR we just use a
            // static mapping if it doesn't.

            return tier(name, 0xFFFF0000, lp -> { // Default Red if material fails
                if (PhantasiaSceneScreen.SHARED_LEVEL == null) return false;

                BlockPos wp = lp.offset(PhantasiaSceneScreen.getOriginForCurrentPattern());
                BlockState state = PhantasiaSceneScreen.SHARED_LEVEL.getBlockState(wp);

                if (state.getBlock() instanceof CoilBlock coilBlock) {
                    // This is the magic line: it gets the actual color of the Cupronickel/Kanthal/etc.
                    int materialColor = coilBlock.coilType.getMaterial().getMaterialRGB();

                    // To make it look like a "Heatmap", we usually want it semi-transparent
                    // or forced to full alpha.
                    // You could store this color in a temporary map or use it to
                    // return true for a specific tier check.
                    return true;
                }
                return false;
            });
        }

        public Builder showController() {
            return filter(state -> {
                if (state.getBlock() instanceof MetaMachineBlock mmb) {
                    MachineDefinition def = mmb.getDefinition();
                    // In GTCEu, MultiblockMachineDefinition is a specific class for controllers
                    return def instanceof MultiblockMachineDefinition;
                }
                return false;
            });
        }

        public Builder showIdContains(String text) {
            return filter(state -> state.getBlock().getDescriptionId().contains(text));
        }

        /**
         * Shows all Input/Output hatches and busses (Parts).
         */
        public Builder showParts() {
            return filter(state -> {
                if (state.getBlock() instanceof com.gregtechceu.gtceu.api.block.MetaMachineBlock mmb) {
                    var def = mmb.getDefinition();
                    // 1. Check if it's NOT a controller
                    if (def instanceof MultiblockMachineDefinition) return false;

                    // 2. Check for common Part keywords in the ID
                    String path = def.getId().getPath();
                    if (path.contains("hatch") || path.contains("bus") || path.contains("access") ||
                            path.contains("port") || path.contains("storage") || path.contains("input") || path.contains("output")) {
                        return true;
                    }

                    // 3. Fallback: If it's a machine but not a controller, it's likely a part
                    return true;
                }
                return false;
            });
        }

        /**
         * Shows blocks matching a specific class (e.g., CoilBlock.class).
         */
        public Builder showType(Class<?> blockClass) {
            return filter(state -> blockClass.isInstance(state.getBlock()));
        }

        /**
         * Shows all blocks that ARE NOT air and ARE NOT standard casings.
         * Useful for highlighting the "interesting" parts of a machine.
         */
        public Builder showFunctional() {
            return filter(state -> {
                if (state.isAir()) return false;
                // If it's a machine part or a special block (like a coil or frame), show it
                boolean isMachine = state.getBlock() instanceof MetaMachineBlock;
                boolean isSpecial = state.getBlock().getDescriptionId().contains("frame") ||
                        state.getBlock().getDescriptionId().contains("gearbox");
                return isMachine || isSpecial;
            });
        }

        public Builder filter(Predicate<BlockState> statePredicate) {
            return addAllow(pos -> {
                if (PhantasiaSceneScreen.SHARED_LEVEL == null) return false;
                BlockPos wp = pos.offset(PhantasiaSceneScreen.getOriginForCurrentPattern());
                BlockState state = PhantasiaSceneScreen.SHARED_LEVEL.getBlockState(wp);
                return statePredicate.test(state);
            });
        }

        // show* methods
        public Builder showAll() {
            allowPred = pos -> true;
            return this;
        }

        public Builder showLayer(int y) {
            return addAllow(pos -> pos.getY() == y);
        }

        public Builder showLayers(int minY, int maxY) {
            return addAllow(pos -> pos.getY() >= minY && pos.getY() <= maxY);
        }

        public Builder showPos(BlockPos... positions) {
            Set<BlockPos> s = new HashSet<>(Arrays.asList(positions));
            return addAllow(s::contains);
        }

        public Builder showWhere(Predicate<BlockPos> p) {
            return addAllow(p);
        }

        // hide* methods
        public Builder hidePos(BlockPos... positions) {
            Set<BlockPos> s = new HashSet<>(Arrays.asList(positions));
            return addDeny(s::contains);
        }

        public Builder hideLayer(int y) {
            return addDeny(pos -> pos.getY() == y);
        }

        public Builder hideWhere(Predicate<BlockPos> p) {
            return addDeny(p);
        }

        // Positional Mistakes
        public Builder mistake(int x, int y, int z, String label) {
            commonMistakes.add(new LocalWarning(new BlockPos(x, y, z), label));
            return this;
        }

        public Builder mistake(int x, int y, int z, String label, int color) {
            commonMistakes.add(new LocalWarning(new BlockPos(x, y, z), label, color));
            return this;
        }

        public Builder mistake(BlockPos pos, String label) {
            commonMistakes.add(new LocalWarning(pos, label));
            return this;
        }

        public Builder mistake(BlockPos pos, String label, int color) {
            commonMistakes.add(new LocalWarning(pos, label, color));
            return this;
        }

        // Heatmap tiers
        public Builder tier(String name, int color, Predicate<BlockPos> matcher) {
            heatmapTiers.add(new HeatmapTier(name, color, matcher));
            return this;
        }

        public Builder tier(String name, int color, BlockPos... positions) {
            Set<BlockPos> s = new HashSet<>(Arrays.asList(positions));
            return tier(name, color, s::contains);
        }

        private int pendingShape = -1;

        public Builder shape(int index) {
            this.pendingShape = index;
            return this;
        }

        public PhantasiaScript build() {
            commitPending();
            return new PhantasiaScript(
                    new ArrayList<>(steps),
                    new ArrayList<>(commonMistakes),
                    new ArrayList<>(globalMistakes),
                    new ArrayList<>(heatmapTiers));
        }

        private Builder addAllow(Predicate<BlockPos> p) {
            allowPred = allowPred == null ? p : allowPred.or(p);
            return this;
        }

        private Builder addDeny(Predicate<BlockPos> p) {
            denyPred = denyPred == null ? p : denyPred.or(p);
            return this;
        }

        private float pendingYaw = 0;
        private float pendingPitch = 0;
        private boolean pendingUseCam = false;

        /**
         * Set the camera orientation for this step.
         * @param yaw Rotation around the Y-axis (0-360)
         * @param pitch Look up/down angle (-90 to 90)
         */
        public Builder camera(float yaw, float pitch) {
            this.pendingYaw = yaw;
            this.pendingPitch = pitch;
            this.pendingUseCam = true;
            return this;
        }

        // ... other methods ...

        private void commitPending() {
            if (pendingTick < 0) return;

            Predicate<BlockPos> allow = allowPred != null ? allowPred : pos -> false;
            Predicate<BlockPos> deny = denyPred != null ? denyPred : pos -> false;

            // Create the step with all forced values including Camera
            steps.add(new Step(
                    pendingTick,
                    pendingCaption,
                    pos -> allow.test(pos) && !deny.test(pos),
                    pendingWorking,
                    pendingShape,
                    pendingCoil,
                    pendingYaw,
                    pendingPitch,
                    pendingUseCam
            ));

            // Reset all pending state
            pendingTick = -1;
            pendingCaption = null;
            allowPred = null;
            denyPred = null;
            pendingWorking = false;
            pendingShape = -1;
            pendingCoil = -1;

            // Reset camera state so it doesn't "stick" to the next step
            pendingYaw = 0;
            pendingPitch = 0;
            pendingUseCam = false;
        }
    }

    public static PhantasiaScript showAll() {
        return builder().step(0, null).showAll().build();
    }

    public static PhantasiaScript simple(String text) {
        return builder().step(0, text).showAll().build();
    }
}