package com.github.orbyfied.minem.scheduler;

import lombok.Getter;
import slatepowered.veru.misc.Throwables;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * A scheduler.
 */
public class ClientScheduler {

    /**
     * The real-time scheduled executor.
     */
    @Getter
    final ScheduledExecutorService realTimeExecutor = Executors.newSingleThreadScheduledExecutor();

    public void stop() {
        realTimeExecutor.shutdownNow();
    }

    public ScheduledFuture<?> scheduleRealDelayed(Runnable runnable, Duration delay) {
        return realTimeExecutor.schedule(errorHandled(runnable), delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    public <T> ScheduledFuture<T> scheduleRealDelayed(Callable<T> runnable, Duration delay) {
        return realTimeExecutor.schedule(errorHandled(runnable), delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    // (50ms) execute tick
    public void tick() {

    }

    // (60fps) execute update
    public void update() {

    }

    /* ------- Error Handling ------- */

    private void handleError(Throwable throwable) {
        System.err.println("An error occurred in a scheduled task");
        throwable.printStackTrace();
        Throwables.sneakyThrow(throwable);
    }

    private Runnable errorHandled(Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Throwable throwable) {
                handleError(throwable);
            }
        };
    }

    private <T> Callable<T> errorHandled(Callable<T> callable) {
        return () -> {
            try {
                return callable.call();
            } catch (Throwable throwable) {
                handleError(throwable);
                return null;
            }
        };
    }

}
