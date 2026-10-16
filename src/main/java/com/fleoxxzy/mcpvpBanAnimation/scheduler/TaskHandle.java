package com.fleoxxzy.mcpvpBanAnimation.scheduler;

/**
 * Platform-neutral handle to a scheduled task.
 */
public interface TaskHandle {

    /** Returned when a task could not be scheduled at all (e.g. the entity was already gone). */
    TaskHandle NONE = new TaskHandle() {
        @Override
        public void cancel() {
        }

        @Override
        public boolean isCancelled() {
            return true;
        }
    };

    void cancel();

    boolean isCancelled();
}
