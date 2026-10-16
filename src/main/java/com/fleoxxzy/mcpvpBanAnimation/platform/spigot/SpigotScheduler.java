package com.fleoxxzy.mcpvpBanAnimation.platform.spigot;

import com.fleoxxzy.mcpvpBanAnimation.scheduler.TaskHandle;
import com.fleoxxzy.mcpvpBanAnimation.scheduler.TaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * {@link TaskScheduler} for Spigot. Everything runs on the single main thread through the
 * {@link BukkitScheduler}. Entity tasks copy Folia's "retired" behaviour: if the entity is gone by
 * the time the task runs (offline player, removed entity), {@code retired} runs instead.
 */
public final class SpigotScheduler implements TaskScheduler {

    private final Plugin plugin;
    private final BukkitScheduler bukkit;

    public SpigotScheduler(Plugin plugin) {
        this.plugin = plugin;
        this.bukkit = Bukkit.getScheduler();
    }

    // --- global / region (all the same on Spigot) ------------------------------------------------

    @Override
    public TaskHandle runGlobal(Runnable task) {
        return submit(task, 0);
    }

    @Override
    public TaskHandle runGlobalLater(Runnable task, long delayTicks) {
        return submit(task, delayTicks);
    }

    @Override
    public TaskHandle runAt(Location location, Runnable task) {
        return submit(task, 0);
    }

    @Override
    public TaskHandle runAtLater(Location location, Runnable task, long delayTicks) {
        return submit(task, delayTicks);
    }

    @Override
    public TaskHandle runTimerAt(Location location, Consumer<TaskHandle> task, long delayTicks, long periodTicks) {
        return timer(task, null, null, delayTicks, periodTicks);
    }

    // --- entity ----------------------------------------------------------------------------------

    @Override
    public TaskHandle runFor(Entity entity, Runnable task, Runnable retired) {
        return runForLater(entity, task, retired, 0);
    }

    @Override
    public TaskHandle runForLater(Entity entity, Runnable task, Runnable retired, long delayTicks) {
        if (!isAlive(entity)) {
            runSafely(retired);
            return TaskHandle.NONE;
        }
        return submit(() -> {
            if (isAlive(entity)) {
                task.run();
            } else {
                runSafely(retired);
            }
        }, delayTicks);
    }

    @Override
    public TaskHandle runTimerFor(Entity entity, Consumer<TaskHandle> task, Runnable retired, long delayTicks, long periodTicks) {
        if (!isAlive(entity)) {
            runSafely(retired);
            return TaskHandle.NONE;
        }
        return timer(task, entity, retired, delayTicks, periodTicks);
    }

    // --- async -----------------------------------------------------------------------------------

    @Override
    public TaskHandle runAsync(Runnable task) {
        try {
            return wrap(bukkit.runTaskAsynchronously(plugin, guarded(task)));
        } catch (Throwable t) {
            log("schedule async task", t);
            return TaskHandle.NONE;
        }
    }

    @Override
    public TaskHandle runAsyncLater(Runnable task, long delayTicks) {
        try {
            return wrap(bukkit.runTaskLaterAsynchronously(plugin, guarded(task), Math.max(0, delayTicks)));
        } catch (Throwable t) {
            log("schedule async task", t);
            return TaskHandle.NONE;
        }
    }

    // --- thread checks ---------------------------------------------------------------------------

    @Override
    public boolean isOwnedByCurrentThread(Location location) {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public boolean isOwnedByCurrentThread(Entity entity) {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public boolean isGlobalThread() {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public void cancelAll() {
        try {
            bukkit.cancelTasks(plugin);
        } catch (Throwable t) {
            log("cancel tasks", t);
        }
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Players count as alive while online (they stay valid while dead or respawning).
     * Other entities count as alive while valid.
     */
    private static boolean isAlive(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (entity instanceof Player player) {
            return player.isOnline();
        }
        return entity.isValid();
    }

    private TaskHandle submit(Runnable task, long delayTicks) {
        try {
            Runnable safe = guarded(task);
            BukkitTask bt = delayTicks <= 0 ? bukkit.runTask(plugin, safe) : bukkit.runTaskLater(plugin, safe, delayTicks);
            return wrap(bt);
        } catch (Throwable t) {
            // Fails only while the plugin is disabled. Run inline so ban releases are not lost.
            if (Bukkit.isPrimaryThread()) {
                runSafely(task);
            } else {
                log("schedule task", t);
            }
            return TaskHandle.NONE;
        }
    }

    private TaskHandle timer(Consumer<TaskHandle> task, Entity entity, Runnable retired, long delayTicks, long periodTicks) {
        SpigotTimer timer = new SpigotTimer(task, entity, retired);
        try {
            timer.bukkitTask = bukkit.runTaskTimer(plugin, timer, Math.max(0, delayTicks), Math.max(1, periodTicks));
            return timer;
        } catch (Throwable t) {
            log("schedule timer", t);
            runSafely(retired);
            return TaskHandle.NONE;
        }
    }

    private Runnable guarded(Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (Throwable t) {
                log("run task", t);
            }
        };
    }

    private void runSafely(Runnable r) {
        if (r == null) {
            return;
        }
        try {
            r.run();
        } catch (Throwable t) {
            log("run task", t);
        }
    }

    private void log(String what, Throwable t) {
        plugin.getLogger().log(Level.SEVERE, "Failed to " + what, t);
    }

    private static TaskHandle wrap(BukkitTask task) {
        return new TaskHandle() {
            @Override
            public void cancel() {
                task.cancel();
            }

            @Override
            public boolean isCancelled() {
                return task.isCancelled();
            }
        };
    }

    private final class SpigotTimer implements Runnable, TaskHandle {
        private final Consumer<TaskHandle> body;
        private final Entity entity;
        private final Runnable retired;
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private volatile BukkitTask bukkitTask;

        SpigotTimer(Consumer<TaskHandle> body, Entity entity, Runnable retired) {
            this.body = body;
            this.entity = entity;
            this.retired = retired;
        }

        @Override
        public void run() {
            if (cancelled.get()) {
                return;
            }
            if (entity != null && !isAlive(entity)) {
                cancel();
                runSafely(retired);
                return;
            }
            try {
                body.accept(this);
            } catch (Throwable t) {
                log("run timer", t);
            }
        }

        @Override
        public void cancel() {
            if (cancelled.compareAndSet(false, true)) {
                BukkitTask bt = bukkitTask;
                if (bt != null) {
                    bt.cancel();
                }
            }
        }

        @Override
        public boolean isCancelled() {
            return cancelled.get();
        }
    }
}
