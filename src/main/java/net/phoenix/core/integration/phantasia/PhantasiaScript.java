package net.phoenix.core.integration.phantasia;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.phoenix.core.integration.phantasia.client.PhantasiaSceneScreen;

import lombok.Getter;

import java.util.*;
import java.util.function.Predicate;

/**
 * PhantasiaScript — runtime-compiled animation script.
 *
 * Always built from a {@link PhantasiaScriptData} via {@link #fromData}.
 * Never serialised directly. The data layer (PhantasiaScriptData / JSON) is the
 * source of truth; this class is the fast predicate form the renderer consumes.
 *
 * The fluent Builder is kept as a compatibility shim — it still works but
 * produces a PhantasiaScriptData under the hood so the JSON registry stays consistent.
 */
@Getter
public class PhantasiaScript {

    // ── Step record ───────────────────────────────────────────────────────────

    public record Step(
                       int tickOffset,
                       String caption,
                       Predicate<BlockPos> filter,
                       boolean working,
                       int forceShape,
                       int forceCoil,
                       float yaw,
                       float pitch,
                       boolean useCam) {

        public boolean hasCamera() {
            return useCam;
        }
    }

    // ── Warning / heatmap records ─────────────────────────────────────────────

    public record LocalWarning(BlockPos localPos, String label, int color) {

        public LocalWarning(BlockPos localPos, String label) {
            this(localPos, label, 0xFFFFB74D);
        }
    }

    public record HeatmapTier(String name, int color, Predicate<BlockPos> matcher) {}

    // ── Fields ────────────────────────────────────────────────────────────────

    /** Raw data this was compiled from — kept so the editor can round-trip edits. */
    private final PhantasiaScriptData sourceData;

    private final List<Step> steps;
    private final int totalTicks;
    private final List<LocalWarning> commonMistakes;
    private final List<String> globalMistakes;
    private final List<HeatmapTier> heatmapTiers;

    private PhantasiaScript(PhantasiaScriptData data,
                            List<Step> steps,
                            List<LocalWarning> commonMistakes,
                            List<String> globalMistakes,
                            List<HeatmapTier> heatmapTiers) {
        this.sourceData = data;
        this.steps = Collections.unmodifiableList(steps);
        this.commonMistakes = Collections.unmodifiableList(commonMistakes);
        this.globalMistakes = Collections.unmodifiableList(globalMistakes);
        this.heatmapTiers = Collections.unmodifiableList(heatmapTiers);
        this.totalTicks = steps.isEmpty() ? 60 : steps.get(steps.size() - 1).tickOffset() + 60;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public boolean hasMistakes() {
        return !commonMistakes.isEmpty() || !globalMistakes.isEmpty();
    }

    public boolean hasCommonMistakes() {
        return !commonMistakes.isEmpty();
    }

    public boolean hasHeatmap() {
        return !heatmapTiers.isEmpty();
    }

    /** Returns the most recent step whose tickOffset ≤ currentTick, or null. */
    public Step getActiveStep(int currentTick) {
        Step active = null;
        for (Step s : steps) {
            if (s.tickOffset() <= currentTick) active = s;
            else break;
        }
        return active;
    }

    // ── Compilation ───────────────────────────────────────────────────────────

    /** Compile a {@link PhantasiaScriptData} into a runtime script. Single entry point. */
    public static PhantasiaScript fromData(PhantasiaScriptData data) {
        List<Step> steps = new ArrayList<>();
        for (PhantasiaScriptData.StepData sd : data.getSteps())
            steps.add(compileStep(sd));

        List<LocalWarning> mistakes = new ArrayList<>();
        for (PhantasiaScriptData.MistakeData md : data.getMistakes())
            mistakes.add(new LocalWarning(new BlockPos(md.x, md.y, md.z), md.label, md.colorArgb()));

        List<String> globalMistakes = new ArrayList<>(data.getGlobalMistakes());
        List<HeatmapTier> tiers = new ArrayList<>(); // reserved for future JSON extension

        return new PhantasiaScript(data, steps, mistakes, globalMistakes, tiers);
    }

    private static Step compileStep(PhantasiaScriptData.StepData sd) {
        Predicate<BlockPos> allow = buildShowPredicate(sd);
        Predicate<BlockPos> deny = buildHidePredicate(sd);
        Predicate<BlockPos> filter = pos -> allow.test(pos) && !deny.test(pos);

        float yaw = 0f, pitch = 0f;
        boolean useCam = false;
        if (sd.camera != null) {
            yaw = sd.camera.yaw;
            pitch = sd.camera.pitch;
            useCam = true;
        }

        return new Step(sd.tick, sd.caption, filter,
                sd.working, /* forceShape */ -1, /* forceCoil */ -1,
                yaw, pitch, useCam);
    }

    private static Predicate<BlockPos> buildShowPredicate(PhantasiaScriptData.StepData sd) {
        String show = sd.show == null ? "all" : sd.show.toLowerCase(Locale.ROOT);
        return switch (show) {
            case "all" -> pos -> true;

            case "layer" -> {
                int y = sd.layer;
                yield pos -> pos.getY() == y;
            }

            case "layers" -> {
                int lo = sd.layerMin, hi = sd.layerMax;
                yield pos -> pos.getY() >= lo && pos.getY() <= hi;
            }

            case "pos" -> {
                Set<BlockPos> set = new HashSet<>();
                for (int[] xyz : sd.positions) if (xyz.length >= 3) set.add(new BlockPos(xyz[0], xyz[1], xyz[2]));
                yield set::contains;
            }

            case "parts" -> localPred(state -> {
                if (!(state.getBlock() instanceof MetaMachineBlock mmb)) return false;
                if (mmb.getDefinition() instanceof MultiblockMachineDefinition) return false;
                String p = mmb.getDefinition().getId().getPath();
                return p.contains("hatch") || p.contains("bus") || p.contains("port") || p.contains("storage") ||
                        p.contains("input") || p.contains("output") || p.contains("muffler") ||
                        p.contains("maintenance");
            });

            case "controller" -> localPred(state -> state.getBlock() instanceof MetaMachineBlock mmb &&
                    mmb.getDefinition() instanceof MultiblockMachineDefinition);

            case "functional" -> localPred(state -> {
                if (state.isAir()) return false;
                return state.getBlock() instanceof MetaMachineBlock ||
                        state.getBlock().getDescriptionId().contains("frame") ||
                        state.getBlock().getDescriptionId().contains("gearbox");
            });

            default -> pos -> true;
        };
    }

    private static Predicate<BlockPos> buildHidePredicate(PhantasiaScriptData.StepData sd) {
        Predicate<BlockPos> deny = pos -> false;

        if (sd.hideLayer >= 0) {
            int hy = sd.hideLayer;
            deny = deny.or(pos -> pos.getY() == hy);
        }

        if (!sd.hidePositions.isEmpty()) {
            Set<BlockPos> hidden = new HashSet<>();
            for (int[] xyz : sd.hidePositions) if (xyz.length >= 3)
                hidden.add(new BlockPos(xyz[0], xyz[1], xyz[2]));
            deny = deny.or(hidden::contains);
        }

        return deny;
    }

    private static Predicate<BlockPos> localPred(Predicate<BlockState> statePred) {
        return localPos -> {
            if (PhantasiaSceneScreen.SHARED_LEVEL == null) return false;
            BlockPos wp = localPos.offset(PhantasiaSceneScreen.getOriginForCurrentPattern());
            try {
                return statePred.test(PhantasiaSceneScreen.SHARED_LEVEL.getBlockState(wp));
            } catch (Exception e) {
                return false;
            }
        };
    }

    // ── Convenience factories ─────────────────────────────────────────────────

    public static PhantasiaScript showAll() {
        return fromData(PhantasiaScriptData.defaultFor(""));
    }

    public static PhantasiaScript simple(String caption) {
        return fromData(PhantasiaScriptData.simpleFor("", caption));
    }

    // ── Legacy fluent Builder (compatibility shim) ────────────────────────────
    // Existing Java callsites still compile. Internally produces PhantasiaScriptData.

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final PhantasiaScriptData data = new PhantasiaScriptData();
        private PhantasiaScriptData.StepData pending = null;

        public Builder step(int tick, String caption) {
            commit();
            pending = new PhantasiaScriptData.StepData(tick, caption);
            return this;
        }

        public Builder showAll() {
            step().show = "all";
            return this;
        }

        public Builder showLayer(int y) {
            step().show = "layer";
            step().layer = y;
            return this;
        }

        public Builder showLayers(int lo, int hi) {
            step().show = "layers";
            step().layerMin = lo;
            step().layerMax = hi;
            return this;
        }

        public Builder showPos(BlockPos... positions) {
            step().show = "pos";
            for (BlockPos p : positions) step().positions.add(new int[] { p.getX(), p.getY(), p.getZ() });
            return this;
        }

        public Builder showParts() {
            step().show = "parts";
            return this;
        }

        public Builder showController() {
            step().show = "controller";
            return this;
        }

        public Builder showFunctional() {
            step().show = "functional";
            return this;
        }

        public Builder hideLayer(int y) {
            step().hideLayer = y;
            return this;
        }

        public Builder hidePos(BlockPos... ps) {
            for (BlockPos p : ps) step().hidePositions.add(new int[] { p.getX(), p.getY(), p.getZ() });
            return this;
        }

        public Builder working(boolean w) {
            step().working = w;
            return this;
        }

        public Builder camera(float yaw, float pitch) {
            step().camera = new PhantasiaScriptData.CameraData(yaw, pitch);
            return this;
        }

        public Builder mistake(int x, int y, int z, String label) {
            data.getMistakes().add(new PhantasiaScriptData.MistakeData(x, y, z, label));
            return this;
        }

        public Builder mistake(int x, int y, int z, String label, int argb) {
            data.getMistakes()
                    .add(new PhantasiaScriptData.MistakeData(x, y, z, label, String.format("%06X", argb & 0xFFFFFF)));
            return this;
        }

        public Builder mistake(String global) {
            data.getGlobalMistakes().add(global);
            return this;
        }

        public PhantasiaScript build() {
            commit();
            return PhantasiaScript.fromData(data);
        }

        public PhantasiaScriptData buildData() {
            commit();
            return data;
        }

        private PhantasiaScriptData.StepData step() {
            if (pending == null) pending = new PhantasiaScriptData.StepData(0, null);
            return pending;
        }

        private void commit() {
            if (pending != null) {
                data.getSteps().add(pending);
                pending = null;
            }
        }
    }
}
