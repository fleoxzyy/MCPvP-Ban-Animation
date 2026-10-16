package com.fleoxxzy.mcpvpBanAnimation.animation;

/**
 * A per-tick behaviour driven by the {@link Timeline}. Called on the animation's region thread.
 */
@FunctionalInterface
public interface Ticker {

    /**
     * @param age ticks since this ticker was added, starting at 1
     * @return {@code true} to keep ticking, {@code false} when finished
     */
    boolean tick(int age);
}
