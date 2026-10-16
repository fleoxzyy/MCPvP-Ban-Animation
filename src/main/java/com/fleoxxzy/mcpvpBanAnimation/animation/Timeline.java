package com.fleoxxzy.mcpvpBanAnimation.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A tick-based sequencer: one-shot cues at absolute ticks, plus tickers that run every tick
 * until they finish. It is not thread-safe on purpose: it is only touched from the region
 * thread that owns the animation.
 */
final class Timeline {

    private final TreeMap<Integer, List<Runnable>> cues = new TreeMap<>();
    private final List<Entry> tickers = new ArrayList<>();
    private int tick;

    /** Schedules {@code action} at absolute tick {@code at}. Ticks in the past run on the next step. */
    void at(int at, Runnable action) {
        cues.computeIfAbsent(Math.max(at, tick), k -> new ArrayList<>(2)).add(action);
    }

    /** Adds a ticker. It first runs on the current step if it is still in progress, otherwise on the next one. */
    void add(Ticker ticker) {
        tickers.add(new Entry(ticker));
    }

    int currentTick() {
        return tick;
    }

    boolean isIdle() {
        return cues.isEmpty() && tickers.isEmpty();
    }

    /** Advances one tick. Exceptions propagate to the caller, which aborts the animation. */
    void step() {
        tick++;
        Map.Entry<Integer, List<Runnable>> next;
        while ((next = cues.firstEntry()) != null && next.getKey() <= tick) {
            cues.pollFirstEntry();
            for (Runnable action : next.getValue()) {
                action.run();
            }
        }
        // Index loop: tickers added by a ticker this step are picked up straight away.
        for (int i = 0; i < tickers.size(); i++) {
            Entry entry = tickers.get(i);
            if (!entry.ticker.tick(++entry.age)) {
                tickers.remove(i--);
            }
        }
    }

    void clear() {
        cues.clear();
        tickers.clear();
    }

    private static final class Entry {
        final Ticker ticker;
        int age;

        Entry(Ticker ticker) {
            this.ticker = ticker;
        }
    }
}
