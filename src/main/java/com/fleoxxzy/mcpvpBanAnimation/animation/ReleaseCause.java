package com.fleoxxzy.mcpvpBanAnimation.animation;

/** Why the held ban is being released. */
public enum ReleaseCause {
    /** The animation reached its release tick. */
    COMPLETED,
    /** Something failed, or the world/chunk/entity went away. */
    ABORTED,
    /** The hard timeout fired before the animation finished. */
    TIMEOUT,
    /** The plugin is disabling. The release must happen right now, on the calling thread if possible. */
    SHUTDOWN
}
