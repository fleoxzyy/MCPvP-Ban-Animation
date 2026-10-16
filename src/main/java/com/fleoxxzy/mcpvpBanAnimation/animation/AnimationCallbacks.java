package com.fleoxxzy.mcpvpBanAnimation.animation;

/**
 * Hooks into an animation's lifecycle. All methods run on the anchor's region thread, except
 * {@link #onFinish}, which may run on any thread (for example during shutdown).
 */
public interface AnimationCallbacks {

    /** First tick: spawn the replica, label, etc. through {@link AnimationContext#spawn}. */
    default void onSetup(AnimationContext context) {
    }

    /** The mace has just hit. */
    default void onImpact(AnimationContext context) {
    }

    /** Called exactly once when the animation ends, however it ends. Must never throw. */
    default void onFinish(EndCause cause) {
    }
}
