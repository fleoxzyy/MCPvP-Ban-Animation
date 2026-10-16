package com.fleoxxzy.mcpvpBanAnimation.trigger;

/** Which detector noticed the ban. */
public enum BanSource {
    /** A ban command was run (vanilla, Essentials, or any ban plugin's command). */
    COMMAND,
    /** A kick that was recognised as a ban. */
    KICK,
    /** A ban plugin's own event API. */
    API
}
