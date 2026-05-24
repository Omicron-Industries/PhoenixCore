package net.phoenix.core.integration.phantasia;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;

import java.util.*;

/**
 * PhantasiaScriptData — the canonical, serialisable form of a Phantasia script.
 *
 * Written to / read from disk:
 * data/phoenixcore/phantasia/scripts/<namespace>/<path>.json
 *
 * Contains no lambdas or predicates — only plain types Gson can round-trip.
 * At runtime, PhantasiaScript.fromData(data) compiles it into the fast
 * predicate-based form used by the scene renderer.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * JSON SCHEMA (camera block extended with lerp fields)
 * ─────────────────────────────────────────────────────────────────────────────
 * {
 * "machine": "gtceu:electric_blast_furnace",
 * "startCamera": {
 * "yaw": -135.0, // starting yaw (degrees). Overrides facing-direction default.
 * "pitch": -35.0, // starting pitch (degrees).
 * "zoom": 30.0, // starting zoom (world units). ≤ 0 = auto from bounding box.
 * "targetOffsetX": 0.0, // world-space offset from bounding-box centre (optional)
 * "targetOffsetY": 2.0, // e.g. look slightly above the midpoint
 * "targetOffsetZ": 0.0
 * },
 * "steps": [
 * {
 * "tick": 0,
 * "caption": "The EBF smelts metals at extreme temperatures.",
 * "show": "all",
 * "layer": 0,
 * "layerMin": 0,
 * "layerMax": 2,
 * "positions": [[1,0,0]],
 * "hideLayer": 3,
 * "hidePositions": [],
 * "working": false,
 * "camera": {
 * "yaw": -135.0,
 * "pitch": -35.0,
 * "zoom": -1.0, // ≤ 0 = auto
 * "lerpType": "EASE_OUT", // SNAP | LINEAR | EASE_OUT | EASE_IN_OUT
 * "lerpTicks": 15 // game ticks (ignored when lerpType = SNAP)
 * }
 * }
 * ],
 * "mistakes": [
 * { "x": 1, "y": 3, "z": 1, "label": "Muffler must be on top", "color": "FFB74D" }
 * ],
 * "globalMistakes": ["Controller must face south"]
 * }
 * ─────────────────────────────────────────────────────────────────────────────
 */
@Getter
public class PhantasiaScriptData {

    // ── Top-level fields ──────────────────────────────────────────────────────

    @SerializedName("machine")
    private String machine = "";

    /**
     * Optional fixed starting camera for this machine.
     * Overrides the auto-computed default (facing direction + bounding-box zoom).
     * Unlike step-0's camera block (which drives animation), this is used ONLY for
     * the initial view — it does not create an animation step.
     * If absent, the renderer auto-frames the machine from its facing direction.
     */
    @SerializedName("startCamera")
    private StartCameraData startCamera = null;

    @SerializedName("steps")
    private List<StepData> steps = new ArrayList<>();

    /**
     * Optional total script duration in ticks.
     * When > 0, the timeline spans exactly this many ticks regardless of where the
     * last step sits. When ≤ 0 (the default), duration is computed automatically as
     * {@code lastStep.tick + 60}.
     *
     * Exposed via getter/setter so the editor can override it without affecting the
     * step tick values themselves.
     */
    @SerializedName("scriptDuration")
    private int scriptDuration = -1;

    public int getScriptDuration() {
        return scriptDuration;
    }

    public void setScriptDuration(int ticks) {
        this.scriptDuration = ticks;
    }

    @SerializedName("mistakes")
    private List<MistakeData> mistakes = new ArrayList<>();

    @SerializedName("globalMistakes")
    private List<String> globalMistakes = new ArrayList<>();

    // ── Step data ─────────────────────────────────────────────────────────────

    @Getter
    public static class StepData {

        @SerializedName("tick")
        public int tick = 0;

        @SerializedName("caption")
        public String caption = null;

        /**
         * Visibility mode. One of:
         * "all" — show every block
         * "layer" — show only blocks at y = layer
         * "layers" — show blocks with layerMin ≤ y ≤ layerMax
         * "pos" — show only the listed positions
         * "parts" — hatches, buses, muffler, maintenance
         * "controller" — show only the controller
         * "functional" — machine blocks + special structural blocks
         */
        @SerializedName("show")
        public String show = "all";

        @SerializedName("layer")
        public int layer = 0;

        @SerializedName("layerMin")
        public int layerMin = 0;

        @SerializedName("layerMax")
        public int layerMax = 0;

        /** Local positions to show, each as [x, y, z]. */
        @SerializedName("positions")
        public List<int[]> positions = new ArrayList<>();

        /** If ≥ 0, hide this Y layer on top of whatever show mode is active. */
        @SerializedName("hideLayer")
        public int hideLayer = -1;

        /** Local positions to always hide, each as [x, y, z]. */
        @SerializedName("hidePositions")
        public List<int[]> hidePositions = new ArrayList<>();

        @SerializedName("working")
        public boolean working = false;

        @SerializedName("fakeRecipeId")
        public String fakeRecipeId = null;

        @SerializedName("camera")
        public CameraData camera = null;

        public StepData() {}

        public StepData(int tick, String caption) {
            this.tick = tick;
            this.caption = caption;
        }

        /** Deep-copy constructor used by the editor. */
        public StepData copy() {
            StepData c = new StepData(tick, caption);
            c.show = show;
            c.layer = layer;
            c.layerMin = layerMin;
            c.layerMax = layerMax;
            c.hideLayer = hideLayer;
            c.working = working;
            // UPDATE THE COPY METHOD TO PRESERVE THE VALUE:
            c.fakeRecipeId = fakeRecipeId;
            c.camera = camera == null ? null : new CameraData(camera.yaw, camera.pitch, camera.zoom,
                    camera.lerpType, camera.lerpTicks);
            for (int[] p : positions) c.positions.add(new int[] { p[0], p[1], p[2] });
            for (int[] p : hidePositions) c.hidePositions.add(new int[] { p[0], p[1], p[2] });
            return c;
        }
    }

    // ── Start camera data ────────────────────────────────────────────────────

    /**
     * Top-level starting camera declaration. Separate from step cameras so the
     * initial framing can be set without creating an animation step.
     *
     * All fields are optional — omit any you don't need to override.
     * targetOffset* values are in world units relative to the auto-computed
     * look-at centre (bounding-box midpoint). Positive Y moves the target up.
     */
    @Getter
    public static class StartCameraData {

        @SerializedName("yaw")
        public float yaw = Float.NaN;   // NaN = "not set, use auto"

        @SerializedName("pitch")
        public float pitch = Float.NaN;

        @SerializedName("zoom")
        public float zoom = -1f;        // ≤ 0 = auto

        @SerializedName("targetOffsetX")
        public float targetOffsetX = 0f;

        @SerializedName("targetOffsetY")
        public float targetOffsetY = 0f;

        @SerializedName("targetOffsetZ")
        public float targetOffsetZ = 0f;

        public StartCameraData() {}

        public boolean hasYaw() {
            return !Float.isNaN(yaw);
        }

        public boolean hasPitch() {
            return !Float.isNaN(pitch);
        }

        public boolean hasZoom() {
            return zoom > 0f;
        }

        public boolean hasTargetOffset() {
            return targetOffsetX != 0f || targetOffsetY != 0f || targetOffsetZ != 0f;
        }

        /** Deep copy used by PhantasiaScriptData.copy(). */
        public StartCameraData copy() {
            StartCameraData c = new StartCameraData();
            c.yaw = yaw;
            c.pitch = pitch;
            c.zoom = zoom;
            c.targetOffsetX = targetOffsetX;
            c.targetOffsetY = targetOffsetY;
            c.targetOffsetZ = targetOffsetZ;
            return c;
        }
    }

    // ── Camera data ───────────────────────────────────────────────────────────

    @Getter
    public static class CameraData {

        @SerializedName("yaw")
        public float yaw = -135f;

        @SerializedName("pitch")
        public float pitch = -35f;

        /**
         * Optional zoom distance override (world units).
         * ≤ 0 means "auto" — let the screen compute it from the machine's bounding box.
         */
        @SerializedName("zoom")
        public float zoom = -1f;

        /**
         * Interpolation curve for this camera transition.
         * One of: "SNAP" (default), "LINEAR", "EASE_OUT", "EASE_IN_OUT".
         * "SNAP" ignores lerpTicks and moves instantly.
         */
        @SerializedName("lerpType")
        public String lerpType = "SNAP";

        /**
         * Duration of the camera transition in game ticks (20 ticks = 1 second).
         * Ignored when lerpType is "SNAP".
         */
        @SerializedName("lerpTicks")
        public int lerpTicks = 0;

        public CameraData() {}

        /** Backwards-compatible constructor — snap, no lerp. */
        public CameraData(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
        }

        /** Backwards-compatible constructor — snap, no lerp, explicit zoom. */
        public CameraData(float yaw, float pitch, float zoom) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.zoom = zoom;
        }

        /** Full constructor. */
        public CameraData(float yaw, float pitch, float zoom, String lerpType, int lerpTicks) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.zoom = zoom;
            this.lerpType = lerpType;
            this.lerpTicks = lerpTicks;
        }
    }

    // ── Mistake data ──────────────────────────────────────────────────────────

    @Getter
    public static class MistakeData {

        @SerializedName("x")
        public int x = 0;
        @SerializedName("y")
        public int y = 0;
        @SerializedName("z")
        public int z = 0;
        @SerializedName("label")
        public String label = "";

        /** RRGGBB hex string, no # prefix, e.g. "FFB74D". */
        @SerializedName("color")
        public String color = "FFB74D";

        public MistakeData() {}

        public MistakeData(int x, int y, int z, String label) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.label = label;
        }

        public MistakeData(int x, int y, int z, String label, String color) {
            this(x, y, z, label);
            this.color = color;
        }

        public int colorArgb() {
            try {
                return (int) (Long.parseLong(color, 16) | 0xFF000000L);
            } catch (NumberFormatException e) {
                return 0xFFFFB74D;
            }
        }
    }

    // ── Constructors ──────────────────────────────────────────────────────────

    public PhantasiaScriptData() {}

    public PhantasiaScriptData(String machine) {
        this.machine = machine;
    }

    // ── Factories ─────────────────────────────────────────────────────────────

    /** Default script: one step at tick=0, show=all, no caption. */
    public static PhantasiaScriptData defaultFor(String machine) {
        PhantasiaScriptData d = new PhantasiaScriptData(machine);
        StepData s = new StepData(0, null);
        s.show = "all";
        d.steps.add(s);
        return d;
    }

    /** Simple script: one step with a caption. */
    public static PhantasiaScriptData simpleFor(String machine, String caption) {
        PhantasiaScriptData d = new PhantasiaScriptData(machine);
        StepData s = new StepData(0, caption);
        s.show = "all";
        d.steps.add(s);
        return d;
    }

    // ── Deep copy ─────────────────────────────────────────────────────────────

    public PhantasiaScriptData copy() {
        PhantasiaScriptData c = new PhantasiaScriptData(machine);
        c.startCamera = startCamera == null ? null : startCamera.copy();
        c.scriptDuration = scriptDuration;
        for (StepData s : steps) c.steps.add(s.copy());
        for (MistakeData m : mistakes) {
            c.mistakes.add(new MistakeData(m.x, m.y, m.z, m.label, m.color));
        }
        c.globalMistakes.addAll(globalMistakes);
        return c;
    }

    // ── Gson codec ────────────────────────────────────────────────────────────

    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .serializeNulls()
            .create();

    public String toJson() {
        return GSON.toJson(this);
    }

    public static PhantasiaScriptData fromJson(String json) {
        return GSON.fromJson(json, PhantasiaScriptData.class);
    }

    public static PhantasiaScriptData fromJson(java.io.Reader reader) {
        return GSON.fromJson(reader, PhantasiaScriptData.class);
    }
}