package net.phoenix.core.integration.phantasia.client.camera;

import net.minecraft.util.Mth;

import org.joml.Vector3f;

import javax.annotation.Nullable;

/**
 * PhantasiaCamera
 *
 * Single source of truth for all Phantasia scene camera state.
 * Contains NO OpenGL calls and holds NO renderer references — it is pure data + math.
 *
 * ── Ownership model ──────────────────────────────────────────────────────────
 *
 * playerOwned locked scriptDrive() result
 * ──────────── ──────── ────────────────────────────────────────────────────
 * false true Lerp to script target. playerOwned stays false.
 * false false Lerp to script target. playerOwned stays false.
 * true true Lerp to script target. playerOwned cleared → false.
 * true false Ignored — player has control.
 *
 * Any player input (orbit / zoom / pan) always sets playerOwned = true and
 * cancels any active lerp immediately.
 *
 * hardReset() always clears playerOwned and starts a fresh position.
 * save() / restore() round-trip the full snapshot without touching playerOwned.
 *
 * ── Threading ────────────────────────────────────────────────────────────────
 * All methods must be called from the render/main thread.
 */
public class PhantasiaCamera {

    // ── Lerp job record ───────────────────────────────────────────────────────

    /**
     * An in-progress camera interpolation.
     * Elapsed advances in {@link #tick()}; the interpolated position is computed
     * lazily in {@link #getView(float)} so partial-tick smoothness is free.
     */
    public record LerpJob(
                          float fromYaw, float fromPitch, float fromZoom,
                          float fromTX, float fromTY, float fromTZ,
                          float toYaw, float toPitch, float toZoom,
                          float toTX, float toTY, float toTZ,
                          int totalTicks,
                          int elapsed,
                          LerpType type) {

        /** Returns a copy with elapsed incremented by one tick. */
        LerpJob advance() {
            return new LerpJob(fromYaw, fromPitch, fromZoom,
                    fromTX, fromTY, fromTZ,
                    toYaw, toPitch, toZoom,
                    toTX, toTY, toTZ,
                    totalTicks, elapsed + 1, type);
        }

        boolean finished() {
            return type == LerpType.SNAP || elapsed >= totalTicks;
        }
    }

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Field-of-view in degrees. */
    public static final float FOV = 60f;

    // ── Authoritative state ───────────────────────────────────────────────────

    private float yaw;         // degrees; 0 = north, +90 = west
    private float pitch;       // degrees; 0 = horizontal, -90 = straight down
    private float zoom;        // world units: distance from target to eye
    private float targetX;
    private float targetY;
    private float targetZ;

    // Baseplate floor Y so we never clip the camera underground.
    private float floorY = Float.NEGATIVE_INFINITY;

    // ── Lerp ─────────────────────────────────────────────────────────────────

    @Nullable
    private LerpJob activeLerp;

    // ── Ownership ─────────────────────────────────────────────────────────────

    /** True once the player has dragged, scrolled, or panned. */
    private boolean playerOwned = false;

    /**
     * True = camera is "locked" (script drives it; the UI shows 🔒).
     * False = player has released the lock (UI shows 🔓).
     * Toggled by the lock button in the timeline bar.
     */
    private boolean locked = true;

    // ── Sub-screen snapshot ───────────────────────────────────────────────────

    @Nullable
    private CameraSnapshot savedSnapshot;

    // ── Constructor ───────────────────────────────────────────────────────────

    public PhantasiaCamera(float yaw, float pitch, float zoom,
                           float targetX, float targetY, float targetZ) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.zoom = zoom;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    /**
     * Called exactly once per game tick (20 Hz).
     * Advances the active lerp and commits its endpoint when finished.
     */
    public void tick() {
        if (activeLerp == null) return;

        if (activeLerp.finished()) {
            // Commit the destination as authoritative state.
            commitLerpEnd();
            activeLerp = null;
            return;
        }

        activeLerp = activeLerp.advance();

        // Also commit once the advanced job is done.
        if (activeLerp.finished()) {
            commitLerpEnd();
            activeLerp = null;
        }
    }

    private void commitLerpEnd() {
        yaw = normYaw(activeLerp.toYaw());
        pitch = activeLerp.toPitch();
        zoom = activeLerp.toZoom();
        targetX = activeLerp.toTX();
        targetY = activeLerp.toTY();
        targetZ = activeLerp.toTZ();
    }

    // ── View computation (called every render frame) ───────────────────────────

    /**
     * Computes the interpolated eye position and look-at point for the current
     * render frame. This method is pure — it mutates nothing.
     *
     * @param partialTicks Minecraft partial tick in [0, 1).
     */
    public CameraView getView(float partialTicks) {
        float ry, rp, rz, tx, ty, tz;

        if (activeLerp != null && !activeLerp.finished()) {
            // Fractional progress within the current tick window.
            float t = (activeLerp.elapsed() + partialTicks) / (float) activeLerp.totalTicks();
            t = Mth.clamp(t, 0f, 1f);
            t = applyEasing(t, activeLerp.type());

            ry = lerpAngle(activeLerp.fromYaw(), activeLerp.toYaw(), t);
            rp = Mth.lerp(t, activeLerp.fromPitch(), activeLerp.toPitch());
            rz = Mth.lerp(t, activeLerp.fromZoom(), activeLerp.toZoom());
            tx = Mth.lerp(t, activeLerp.fromTX(), activeLerp.toTX());
            ty = Mth.lerp(t, activeLerp.fromTY(), activeLerp.toTY());
            tz = Mth.lerp(t, activeLerp.fromTZ(), activeLerp.toTZ());
        } else {
            ry = yaw;
            rp = pitch;
            rz = zoom;
            tx = targetX;
            ty = targetY;
            tz = targetZ;
        }

        return buildView(ry, rp, rz, tx, ty, tz);
    }

    /**
     * Converts spherical camera coordinates into Cartesian eye-pos / look-at vectors.
     * The eye sits at distance {@code zoom} from the target along the direction
     * described by yaw (azimuth) and pitch (elevation).
     */
    private CameraView buildView(float yawDeg, float pitchDeg, float dist,
                                 float tx, float ty, float tz) {
        double yr = Math.toRadians(yawDeg);
        double pr = Math.toRadians(pitchDeg);

        // Standard spherical → Cartesian (Y-up, Z-forward convention matching MC).
        float nx = (float) (Math.cos(pr) * Math.sin(yr));
        float ny = (float) Math.sin(pr);
        float nz = (float) (Math.cos(pr) * Math.cos(yr));

        float eyeX = tx + nx * dist;
        float eyeY = ty + ny * dist;
        float eyeZ = tz + nz * dist;

        // Never clip below the baseplate surface.
        eyeY = Math.max(eyeY, floorY);

        return new CameraView(new Vector3f(eyeX, eyeY, eyeZ), new Vector3f(tx, ty, tz));
    }

    // ── Script-driven camera ──────────────────────────────────────────────────

    /**
     * Called when a script step fires.
     *
     * Ownership rules (see class javadoc):
     * - If {@code locked} is false and {@code playerOwned} is true → ignored.
     * - Otherwise → starts a lerp. If {@code locked} is true, also clears
     * {@code playerOwned} so subsequent player input starts from the script position.
     */
    public void scriptDrive(float toYaw, float toPitch, float toZoom,
                            LerpType lerpType, int lerpTicks) {
        if (!locked && playerOwned) return; // player has control, script is muted

        if (locked) playerOwned = false;   // locked → script reclaims ownership

        startLerp(toYaw, toPitch, toZoom, targetX, targetY, targetZ, lerpType, lerpTicks);
    }

    /**
     * Variant that also moves the look-at target (useful for "focus on this block" steps).
     */
    public void scriptDrive(float toYaw, float toPitch, float toZoom,
                            float toTX, float toTY, float toTZ,
                            LerpType lerpType, int lerpTicks) {
        if (!locked && playerOwned) return;
        if (locked) playerOwned = false;
        startLerp(toYaw, toPitch, toZoom, toTX, toTY, toTZ, lerpType, lerpTicks);
    }

    // ── Player input ──────────────────────────────────────────────────────────

    public void orbit(float dx, float dy) {
        cancelLerp();
        this.yaw -= dx;
        // FIX: Clamp to -89.99f and 89.99f instead of -90f and 90f
        // to prevent the direction matrix from collapsing into NaN
        this.pitch = Mth.clamp(this.pitch + dy, -89.99f, 89.99f);
        this.playerOwned = true;
    }

    /**
     * Zoom in or out by a multiplicative factor.
     * factor < 1 zooms in; factor > 1 zooms out.
     * Clamped to [{@code minZoom}, {@code maxZoom}].
     */
    public void zoom(float factor, float minZoom, float maxZoom) {
        cancelLerp();
        zoom = Mth.clamp(zoom * factor, minZoom, maxZoom);
        playerOwned = true;
    }

    /**
     * Pan the look-at target in world space (middle-mouse drag).
     *
     * @param worldDX Delta along the camera's right axis.
     * @param worldDY Delta along the camera's up axis.
     * @param worldDZ Delta along the camera's forward axis (rarely used).
     */
    public void pan(float worldDX, float worldDY, float worldDZ) {
        cancelLerp();
        targetX += worldDX;
        targetY += worldDY;
        targetZ += worldDZ;
        playerOwned = true;
    }

    // ── System resets ─────────────────────────────────────────────────────────

    /**
     * Hard reset: snap (or lerp) to an entirely new position.
     * Clears playerOwned so the script can drive after this.
     * Called when the viewed multiblock changes entirely, or when the player
     * clicks "Center Camera".
     *
     * @param lerpType  Use {@link LerpType#SNAP} for an instant reset.
     * @param lerpTicks Duration if not SNAP (ignored for SNAP).
     */
    public void hardReset(float toYaw, float toPitch, float toZoom,
                          float toTX, float toTY, float toTZ,
                          LerpType lerpType, int lerpTicks) {
        playerOwned = false;
        startLerp(toYaw, toPitch, toZoom, toTX, toTY, toTZ, lerpType, lerpTicks);
    }

    /** Convenience overload: instant snap. */
    public void hardReset(float toYaw, float toPitch, float toZoom,
                          float toTX, float toTY, float toTZ) {
        hardReset(toYaw, toPitch, toZoom, toTX, toTY, toTZ, LerpType.SNAP, 0);
    }

    // ── Sub-screen save / restore ─────────────────────────────────────────────

    /**
     * Snapshot the current camera before navigating to a sub-screen.
     * Does NOT touch playerOwned — that is restored together with the position.
     */
    public void save() {
        savedSnapshot = new CameraSnapshot(yaw, pitch, zoom,
                targetX, targetY, targetZ, playerOwned);
    }

    /**
     * Restore a previously saved snapshot.
     * Cancels any active lerp.
     * playerOwned is restored from the snapshot.
     *
     * @return true if a snapshot existed and was restored, false if nothing was saved.
     */
    public boolean restore() {
        if (savedSnapshot == null) return false;
        cancelLerp();
        yaw = savedSnapshot.yaw();
        pitch = savedSnapshot.pitch();
        zoom = savedSnapshot.zoom();
        targetX = savedSnapshot.targetX();
        targetY = savedSnapshot.targetY();
        targetZ = savedSnapshot.targetZ();
        playerOwned = savedSnapshot.playerOwned();
        savedSnapshot = null;
        return true;
    }

    public boolean hasSavedSnapshot() {
        return savedSnapshot != null;
    }

    /**
     * Discard any saved snapshot without restoring it.
     * Call before triggering an init() that should NOT restore the old camera
     * position (e.g. switching structure size — the new pattern has a different
     * centroid and the old position would point at empty space).
     */
    public void clearSnapshot() {
        savedSnapshot = null;
    }

    /**
     * Clear the playerOwned flag so init()'s camera branch will call
     * resetCameraToDefault() instead of keeping the player's current view.
     *
     * Use when the scene has changed so fundamentally (e.g. structure size switch)
     * that the player's previous camera position is no longer meaningful — it
     * would be pointing at the wrong region of the shared dummy world.
     */
    public void clearPlayerOwned() {
        playerOwned = false;
    }

    // ── Floor clamp ───────────────────────────────────────────────────────────

    /**
     * Set the minimum Y the eye position may occupy.
     * Prevents the camera from clipping through the baseplate.
     *
     * @param y World Y of the top surface of the baseplate (origin.Y + 0.5 typically).
     */
    public void setFloorY(float y) {
        this.floorY = y;
    }

    // ── Lock toggle ───────────────────────────────────────────────────────────

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean l) {
        this.locked = l;
    }

    public void toggleLocked() {
        this.locked = !this.locked;
    }

    // ── Accessors (for the script editor "Capture Camera" feature) ────────────

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public float getZoom() {
        return zoom;
    }

    public float getTargetX() {
        return targetX;
    }

    public float getTargetY() {
        return targetY;
    }

    public float getTargetZ() {
        return targetZ;
    }

    public boolean isPlayerOwned() {
        return playerOwned;
    }

    /** Direct setters used when restoring from a snapshot or building programmatically. */
    public void setPosition(float yaw, float pitch, float zoom) {
        this.yaw = normYaw(yaw);
        this.pitch = pitch;
        this.zoom = zoom;
    }

    public void setTarget(float tx, float ty, float tz) {
        this.targetX = tx;
        this.targetY = ty;
        this.targetZ = tz;
    }

    // ── Pan helpers ───────────────────────────────────────────────────────────

    /**
     * Computes the camera's right and up axes from the current yaw/pitch.
     * Used by the screen's middle-mouse pan handler to convert pixel delta → world delta.
     *
     * @param outRight receives the right axis (normalised)
     * @param outUp    receives the up axis (normalised)
     */
    public void getRightAndUp(Vector3f outRight, Vector3f outUp) {
        // Forward direction (eye → target, same as buildView's n vector reversed)
        double yr = Math.toRadians(yaw);
        double pr = Math.toRadians(pitch);
        float fx = -(float) (Math.cos(pr) * Math.sin(yr));
        float fy = -(float) Math.sin(pr);
        float fz = -(float) (Math.cos(pr) * Math.cos(yr));
        Vector3f fwd = new Vector3f(fx, fy, fz).normalize();
        Vector3f worldUp = new Vector3f(0, 1, 0);
        fwd.cross(worldUp, outRight);
        outRight.normalize();
        outRight.cross(fwd, outUp);
        outUp.normalize();
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private void startLerp(float toYaw, float toPitch, float toZoom,
                           float toTX, float toTY, float toTZ,
                           LerpType type, int durationTicks) {
        // Snap: commit immediately, no lerp job needed.
        if (type == LerpType.SNAP || durationTicks <= 0) {
            activeLerp = null;
            yaw = normYaw(toYaw);
            pitch = toPitch;
            zoom = toZoom;
            targetX = toTX;
            targetY = toTY;
            targetZ = toTZ;
            return;
        }

        // Determine current visual state (may already be mid-lerp — use interpolated values
        // so chained lerps don't visually jump).
        float curYaw, curPitch, curZoom, curTX, curTY, curTZ;
        if (activeLerp != null) {
            // Sample at current elapsed + 0 partial to get the "right now" position.
            CameraView now = getView(0f);
            curYaw = yaw;
            curPitch = pitch;
            curZoom = zoom;
            curTX = targetX;
            curTY = targetY;
            curTZ = targetZ;
            // Advance the previous lerp's committed state first.
            float t = Mth.clamp((float) activeLerp.elapsed() / activeLerp.totalTicks(), 0f, 1f);
            t = applyEasing(t, activeLerp.type());
            curYaw = lerpAngle(activeLerp.fromYaw(), activeLerp.toYaw(), t);
            curPitch = Mth.lerp(t, activeLerp.fromPitch(), activeLerp.toPitch());
            curZoom = Mth.lerp(t, activeLerp.fromZoom(), activeLerp.toZoom());
            curTX = Mth.lerp(t, activeLerp.fromTX(), activeLerp.toTX());
            curTY = Mth.lerp(t, activeLerp.fromTY(), activeLerp.toTY());
            curTZ = Mth.lerp(t, activeLerp.fromTZ(), activeLerp.toTZ());
        } else {
            curYaw = yaw;
            curPitch = pitch;
            curZoom = zoom;
            curTX = targetX;
            curTY = targetY;
            curTZ = targetZ;
        }

        // Normalise the destination yaw to take the short arc.
        float normTo = shortArcYaw(curYaw, toYaw);

        activeLerp = new LerpJob(
                curYaw, curPitch, curZoom, curTX, curTY, curTZ,
                normTo, toPitch, toZoom, toTX, toTY, toTZ,
                durationTicks, 0, type);
    }

    private void cancelLerp() {
        if (activeLerp == null) return;
        // Commit the current interpolated position so the view doesn't jump.
        float t = Mth.clamp((float) activeLerp.elapsed() / activeLerp.totalTicks(), 0f, 1f);
        t = applyEasing(t, activeLerp.type());
        yaw = lerpAngle(activeLerp.fromYaw(), activeLerp.toYaw(), t);
        pitch = Mth.lerp(t, activeLerp.fromPitch(), activeLerp.toPitch());
        zoom = Mth.lerp(t, activeLerp.fromZoom(), activeLerp.toZoom());
        targetX = Mth.lerp(t, activeLerp.fromTX(), activeLerp.toTX());
        targetY = Mth.lerp(t, activeLerp.fromTY(), activeLerp.toTY());
        targetZ = Mth.lerp(t, activeLerp.fromTZ(), activeLerp.toTZ());
        activeLerp = null;
    }

    // ── Math utilities ────────────────────────────────────────────────────────

    private static float applyEasing(float t, LerpType type) {
        return switch (type) {
            case SNAP, LINEAR -> t;
            case EASE_IN -> t * t;
            case EASE_OUT -> 1f - (1f - t) * (1f - t);
            case EASE_IN_OUT -> t < 0.5f ? 2f * t * t : 1f - (-2f * t + 2f) * (-2f * t + 2f) / 2f;
            case SMOOTHSTEP -> t * t * (3.0f - 2.0f * t);

            // --- Sine math translations using java.lang.Math ---
            case SINE_IN -> 1.0f - (float) Math.cos(t * Math.PI / 2.0);
            case SINE_OUT -> (float) Math.sin(t * Math.PI / 2.0);
            case SINE_IN_OUT -> -((float) Math.cos(Math.PI * t) - 1.0f) / 2.0f;
        };
    }

    /**
     * Interpolates between two angles (degrees) taking the shortest arc.
     * Always stays in [-360, 360].
     */
    private static float lerpAngle(float from, float to, float t) {
        float delta = ((to - from + 540f) % 360f) - 180f; // shortest arc delta
        return from + delta * t;
    }

    /**
     * Returns the equivalent of {@code to} shifted so that lerpAngle travels
     * the short arc from {@code from}. Used when building a LerpJob to normalise
     * the destination yaw once (rather than recomputing each frame).
     */
    private static float shortArcYaw(float from, float to) {
        float delta = ((to - from + 540f) % 360f) - 180f;
        return from + delta;
    }

    private static float normYaw(float yaw) {
        yaw = yaw % 360f;
        if (yaw > 180f) yaw -= 360f;
        if (yaw < -180f) yaw += 360f;
        return yaw;
    }
}
