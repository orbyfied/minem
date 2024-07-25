package com.github.orbyfied.minem.scheduler;

import com.github.orbyfied.minem.MinecraftClient;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import slatepowered.veru.misc.Throwables;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A scheduler.
 */
@RequiredArgsConstructor
public class ClientScheduler {

    final MinecraftClient client;

    /**
     * The real-time scheduled executor.
     */
    @Getter
    final ScheduledExecutorService realTimeExecutor = Executors.newSingleThreadScheduledExecutor();

    public void stop() {
        // force stop real-time exec
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
