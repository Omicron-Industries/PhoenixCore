package net.phoenix.core.conflux.client.render;

public final class IntensityController {

    public enum State {

        AMBIENT(0.15f),
        INTERACTION(0.40f),
        EVENT(1.00f);

        public final float target;

        State(float t) {
            this.target = t;
        }
    }

    private State state = State.AMBIENT;
    private float intensity = 0.15f;
    private float eventTimer = 0f;

    private static final float EVENT_HOLD = 1.4f;
    private static final float RAMP_SPEED = 3.5f;
    private static final float DECAY_SPEED = 1.2f;

    public void tick(float delta) {
        if (state == State.EVENT) {
            eventTimer -= delta;
            if (eventTimer <= 0f) state = State.AMBIENT;
        }
        float speed = intensity < state.target ? RAMP_SPEED : DECAY_SPEED;
        intensity = approach(intensity, state.target, speed * delta);
    }

    public void onHover() {
        if (state == State.AMBIENT) state = State.INTERACTION;
    }

    public void onHoverEnd() {
        if (state == State.INTERACTION) state = State.AMBIENT;
    }

    public void onEvent() {
        spike();
    }

    private void spike() {
        state = State.EVENT;
        eventTimer = EVENT_HOLD;
        intensity = 1.0f;
    }

    public float get() {
        return intensity;
    }

    public State getState() {
        return state;
    }

    private static float approach(float cur, float target, float max) {
        float d = target - cur;
        return Math.abs(d) <= max ? target : cur + Math.signum(d) * max;
    }
}
