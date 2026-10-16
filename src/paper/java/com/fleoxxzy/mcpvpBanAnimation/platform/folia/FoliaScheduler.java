package com.fleoxxzy.mcpvpBanAnimation.platform.folia;

import com.fleoxxzy.mcpvpBanAnimation.core.Reflect;
import com.fleoxxzy.mcpvpBanAnimation.scheduler.TaskHandle;
import com.fleoxxzy.mcpvpBanAnimation.scheduler.TaskScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * {@link TaskScheduler} built on the region / entity / global / async schedulers.
 * Used on Folia <b>and</b> Paper, which ships the same API, so both run identical code paths.
 *
 * <p>This class never touches {@code Bukkit.getScheduler()}.
 *
 * <p>Region and entity schedulers have no "cancel all for plugin" method, so their tasks are
 * tracked here and cancelled in {@link #cancelAll()}.
 */
public final class FoliaScheduler implements TaskScheduler {

    private static final long TICK_MS = 50L;
    /** {@code Bukkit.isGlobalTickThread()} appeared in a later 1.21.x API, so it is looked up at runtime. */
    private static final Method GLOBAL_TICK_CHECK = Reflect.findMethod(Bukkit.class, "isGlobalTickThread");

    private final Plugin plugin;
    private final boolean folia;
    private final Set<ScheduledTask> tracked = ConcurrentHashMap.newKeySet();

    public FoliaScheduler(Plugin plugin, boolean folia) {
        this.plugin = plugin;
        this.folia = folia;
    }

    // --- global ----------------------------------------------------------------------------------

    @Override
    public TaskHandle runGlobal(Runnable task) {
        try {
            return track(Bukkit.getGlobalRegionScheduler().run(plugin, st -> runTracked(st, task)));
        } catch (Throwable t) {
            return failed("schedule global task", t, task);
        }
    }

    @Override
    public TaskHandle runGlobalLater(Runnable task, long delayTicks) {
        if (delayTicks <= 0) {
            return runGlobal(task);
        }
        try {
            return track(Bukkit.getGlobalRegionScheduler().runDelayed(plugin, st -> runTracked(st, task), delayTicks));
        } catch (Throwable t) {
            return failed("schedule global task", t, task);
        }
    }

    // --- region ----------------------------------------------------------------------------------

    @Override
    public TaskHandle runAt(Location location, Runnable task) {
        try {
            return track(Bukkit.getRegionScheduler().run(plugin, location, st -> runTracked(st, task)));
        } catch (Throwable t) {
            log("schedule region task", t);
            return TaskHandle.NONE;
        }
    }

    @Override
    public TaskHandle runAtLater(Location location, Runnable task, long delayTicks) {
        if (delayTicks <= 0) {
            return runAt(location, task);
        }
        try {
            return track(Bukkit.getRegionScheduler().runDelayed(plugin, location, st -> runTracked(st, task), delayTicks));
        } catch (Throwable t) {
            log("schedule region task", t);
            return TaskHandle.NONE;
        }
    }

    @Override
    public TaskHandle runTimerAt(Location location, Consumer<TaskHandle> task, long delayTicks, long periodTicks) {
        try {
            ScheduledTask st = Bukkit.getRegionScheduler().runAtFixedRate(plugin, location,
                    s -> runTimerBody(s, task), Math.max(1, delayTicks), Math.max(1, periodTicks));
            return track(st);
        } catch (Throwable t) {
            log("schedule region timer", t);
            return TaskHandle.NONE;
        }
    }

    // --- entity ----------------------------------------------------------------------------------

    @Override
    public TaskHandle runFor(Entity entity, Runnable task, Runnable retired) {
        return runForLater(entity, task, retired, 0);
    }

    @Override
    public TaskHandle runForLater(Entity entity, Runnable task, Runnable retired, long delayTicks) {
        if (entity == null) {
            runSafely(retired);
            return TaskHandle.NONE;
        }
        RetireOnce once = new RetireOnce(retired);
        try {
            ScheduledTask st = delayTicks <= 0
                    ? entity.getScheduler().run(plugin, s -> runTracked(s, task), once)
                    : entity.getScheduler().runDelayed(plugin, s -> runTracked(s, task), once, delayTicks);
            if (st == null) {
                // null = the entity's scheduler is already retired (entity removed).
                once.run();
                return TaskHandle.NONE;
            }
            return track(st);
        } catch (Throwable t) {
            log("schedule entity task", t);
            once.run();
            return TaskHandle.NONE;
        }
    }

    @Override
    public TaskHandle runTimerFor(Entity entity, Consumer<TaskHandle> task, Runnable retired, long delayTicks, long periodTicks) {
        if (entity == null) {
            runSafely(retired);
            return TaskHandle.NONE;
        }
        RetireOnce once = new RetireOnce(retired);
        try {
            ScheduledTask st = entity.getScheduler().runAtFixedRate(plugin, s -> runTimerBody(s, task), once,
                    Math.max(1, delayTicks), Math.max(1, periodTicks));
            if (st == null) {
                once.run();
                return TaskHandle.NONE;
            }
            return track(st);
        } catch (Throwable t) {
            log("schedule entity timer", t);
            once.run();
            return TaskHandle.NONE;
        }
    }

    // --- async -----------------------------------------------------------------------------------

    @Override
    public TaskHandle runAsync(Runnable task) {
        try {
            return track(Bukkit.getAsyncScheduler().runNow(plugin, st -> runTracked(st, task)));
        } catch (Throwable t) {
            log("schedule async task", t);
            return TaskHandle.NONE;
        }
    }

    @Override
    public TaskHandle runAsyncLater(Runnable task, long delayTicks) {
        if (delayTicks <= 0) {
            return runAsync(task);
        }
        try {
            return track(Bukkit.getAsyncScheduler().runDelayed(plugin, st -> runTracked(st, task),
                    delayTicks * TICK_MS, TimeUnit.MILLISECONDS));
        } catch (Throwable t) {
            log("schedule async task", t);
            return TaskHandle.NONE;
        }
    }

    // --- thread checks ---------------------------------------------------------------------------

    @Override
    public boolean isOwnedByCurrentThread(Location location) {
        try {
            return Bukkit.isOwnedByCurrentRegion(location);
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean isOwnedByCurrentThread(Entity entity) {
        try {
            return Bukkit.isOwnedByCurrentRegion(entity);
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean isGlobalThread() {
        Method check = GLOBAL_TICK_CHECK;
        if (check != null) {
            try {
                return (Boolean) check.invoke(null);
            } catch (Throwable ignored) {
                return false;
            }
        }
        // Older 1.21 builds: on Paper the main thread is the global thread. On Folia, report
        // false so callers always schedule onto the global scheduler, which is always safe.
        return !folia && Bukkit.isPrimaryThread();
    }

    @Override
    public void cancelAll() {
        for (ScheduledTask st : tracked) {
            try {
                st.cancel();
            } catch (Throwable ignored) {
                // Already finished or cancelled.
            }
        }
        tracked.clear();
        try {
            Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
        } catch (Throwable ignored) {
            // Plugin already disabled.
        }
        try {
            Bukkit.getAsyncScheduler().cancelTasks(plugin);
        } catch (Throwable ignored) {
            // Plugin already disabled.
        }
    }

    // ---------------------------------------------------------------------------------------------

    private TaskHandle track(ScheduledTask st) {
        if (st == null) {
            return TaskHandle.NONE;
        }
        // A one-shot task may already have run (and untracked itself) on another thread
        // before we get here, so only track it if it is still pending.
        if (!isFinished(st)) {
            tracked.add(st);
            if (isFinished(st)) {
                tracked.remove(st);
            }
        }
        return new Handle(st);
    }

    private static boolean isFinished(ScheduledTask st) {
        ScheduledTask.ExecutionState state = st.getExecutionState();
        return state == ScheduledTask.ExecutionState.FINISHED
                || state == ScheduledTask.ExecutionState.CANCELLED
                || state == ScheduledTask.ExecutionState.CANCELLED_RUNNING;
    }

    private void runTracked(ScheduledTask st, Runnable task) {
        try {
            task.run();
        } catch (Throwable t) {
            log("run task", t);
        } finally {
            tracked.remove(st);
        }
    }

    private void runTimerBody(ScheduledTask st, Consumer<TaskHandle> task) {
        try {
            task.accept(new Handle(st));
        } catch (Throwable t) {
            log("run timer", t);
        }
        if (st.isCancelled()) {
            tracked.remove(st);
        }
    }

    private TaskHandle failed(String what, Throwable t, Runnable task) {
        // Global scheduling fails only while disabled. Ban releases go through the global
        // scheduler, so run inline if we are already on the global thread.
        if (isGlobalThread()) {
            runSafely(task);
        } else {
            log(what, t);
        }
        return TaskHandle.NONE;
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

    private final class Handle implements TaskHandle {
        private final ScheduledTask task;

        Handle(ScheduledTask task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            try {
                task.cancel();
            } catch (Throwable ignored) {
                // Already finished.
            } finally {
                tracked.remove(task);
            }
        }

        @Override
        public boolean isCancelled() {
            return task.isCancelled();
        }
    }

    /** Folia may call the retired callback while we also call it when scheduling fails. Run it only once. */
    private final class RetireOnce implements Runnable {
        private final Runnable delegate;
        private final AtomicBoolean done = new AtomicBoolean();

        RetireOnce(Runnable delegate) {
            this.delegate = delegate;
        }

        @Override
        public void run() {
            if (done.compareAndSet(false, true)) {
                runSafely(delegate);
            }
        }
    }
}
