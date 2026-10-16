package com.fleoxxzy.mcpvpBanAnimation.animation;

/** Why an animation ended. */
public enum EndCause {
    /** It played to the end. */
    COMPLETED,
    /** Something failed, or the chunk/world went away. */
    ABORTED,
    /** The hard timeout fired. */
    TIMEOUT,
    /** The plugin is disabling. */
    SHUTDOWN
}
