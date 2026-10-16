package com.fleoxxzy.mcpvpBanAnimation.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.function.Consumer;

/**
 * Scheduler abstraction over Bukkit (Spigot) and the region/entity/global/async schedulers
 * (Paper + Folia).
 *
 * <ul>
 *   <li>{@code Global}: server-wide state, command dispatch, ban lists.</li>
 *   <li>{@code At}: the region that owns a location (spawning/moving displays, particles, sounds).</li>
 *   <li>{@code For}: the thread that owns an entity (players: kill, gamemode, teleport, kick).
 *       The {@code retired} callback runs instead of the task if the entity is removed first
 *       (including when it is already gone at scheduling time).</li>
 *   <li>{@code Async}: work that never touches the world.</li>
 * </ul>
 *
 * Delays and periods are in ticks. Delays below 1 run the task as soon as possible.
 * Implementations never throw from scheduling methods; on failure they return {@link TaskHandle#NONE}
 * (and run {@code retired} for entity tasks), so callers can always fall back to banning.
 */
public interface TaskScheduler {

    TaskHandle runGlobal(Runnable task);

    TaskHandle runGlobalLater(Runnable task, long delayTicks);

    TaskHandle runAt(Location location, Runnable task);

    TaskHandle runAtLater(Location location, Runnable task, long delayTicks);

    /** Repeating region task. The consumer receives its own handle so it can cancel itself. */
    TaskHandle runTimerAt(Location location, Consumer<TaskHandle> task, long delayTicks, long periodTicks);

    TaskHandle runFor(Entity entity, Runnable task, Runnable retired);

    TaskHandle runForLater(Entity entity, Runnable task, Runnable retired, long delayTicks);

    TaskHandle runTimerFor(Entity entity, Consumer<TaskHandle> task, Runnable retired, long delayTicks, long periodTicks);

    TaskHandle runAsync(Runnable task);

    TaskHandle runAsyncLater(Runnable task, long delayTicks);

    /** True if the current thread may touch the world at {@code location} right now. */
    boolean isOwnedByCurrentThread(Location location);

    /** True if the current thread may touch {@code entity} right now. */
    boolean isOwnedByCurrentThread(Entity entity);

    /** True if the current thread may touch global server state (ban lists, command dispatch). */
    boolean isGlobalThread();

    /** Cancels every task this plugin scheduled through this scheduler. */
    void cancelAll();

    /** Runs immediately if the current thread owns {@code location}, otherwise schedules it there. */
    default void executeAt(Location location, Runnable task) {
        if (isOwnedByCurrentThread(location)) {
            task.run();
        } else {
            runAt(location, task);
        }
    }

    /** Runs immediately if the current thread owns {@code entity}, otherwise schedules it on the entity. */
    default void executeFor(Entity entity, Runnable task, Runnable retired) {
        if (isOwnedByCurrentThread(entity)) {
            task.run();
        } else {
            runFor(entity, task, retired);
        }
    }

    /** Runs immediately on the global thread, otherwise schedules it there. */
    default void executeGlobal(Runnable task) {
        if (isGlobalThread()) {
            task.run();
        } else {
            runGlobal(task);
        }
    }
}
