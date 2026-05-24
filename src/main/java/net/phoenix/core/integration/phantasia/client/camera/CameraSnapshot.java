package net.phoenix.core.integration.phantasia.client.camera;

/**
 * Immutable snapshot of all camera state.
 * Created by {@link PhantasiaCamera#save()} and consumed by
 * {@link PhantasiaCamera#restore()} so sub-screen navigation never loses
 * the player's view.
 */
public record CameraSnapshot(
                             float yaw,
                             float pitch,
                             float zoom,
                             float targetX,
                             float targetY,
                             float targetZ,
                             boolean playerOwned) {}
