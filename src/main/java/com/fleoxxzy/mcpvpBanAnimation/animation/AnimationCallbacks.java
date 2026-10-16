package com.fleoxxzy.mcpvpBanAnimation.animation;

/**
 * Hooks into an animation's lifecycle. {@link #onSetup}, {@link #onImpact} and {@link #onCleanup}
 * run on the anchor's region thread. {@link #onRelease} may run on <b>any</b> thread.
 */
public interface AnimationCallbacks {

    /** First tick: spawn the replica, label, etc. through {@link AnimationContext#spawn}. */
    default void onSetup(AnimationContext context) {
    }

    /** The mace has just hit. */
    default void onImpact(AnimationContext context) {
    }

    /**
     * Called <b>exactly once</b> per animation, whatever happens (completion, failure, timeout,
     * shutdown). This is where the held ban is carried out. Must never throw.
     */
    void onRelease(ReleaseCause cause);

    /** All spawned entities have been removed. Not called if cleanup cannot run (e.g. Folia shutdown). */
    default void onCleanup() {
    }
}
