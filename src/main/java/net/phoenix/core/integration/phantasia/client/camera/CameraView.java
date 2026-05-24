package net.phoenix.core.integration.phantasia.client.camera;

import org.joml.Vector3f;

/**
 * The result of {@link PhantasiaCamera#getView(float)}: the two vectors that
 * define where the camera is and what it is looking at for a given render frame.
 *
 * Produced every frame, never stored — the camera fields are the source of truth.
 */
public record CameraView(Vector3f eyePos, Vector3f lookAt) {

    public float eyeX() {
        return eyePos.x();
    }

    public float eyeY() {
        return eyePos.y();
    }

    public float eyeZ() {
        return eyePos.z();
    }

    public float lookAtX() {
        return lookAt.x();
    }

    public float lookAtY() {
        return lookAt.y();
    }

    public float lookAtZ() {
        return lookAt.z();
    }

    /** Direction vector eye → lookAt, not normalised. */
    public Vector3f direction() {
        return new Vector3f(lookAt).sub(eyePos);
    }
}
