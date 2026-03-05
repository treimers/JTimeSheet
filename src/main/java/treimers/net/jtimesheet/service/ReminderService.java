package treimers.net.jtimesheet.service;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import treimers.net.jtimesheet.model.AppSettings;

/**
 * Runs a timer every full minute and calls a callback. The callback should use
 * {@link #isReminderDue(LocalDateTime, AppSettings)} to decide whether to show the reminder dialog.
 * Uses a background scheduler (not JavaFX animation) so the timer keeps firing when the app
 * is in the background on macOS (unless the process is suspended by App Nap).
 * For tests, call {@link #tick(LocalDateTime)} with a fixed time instead of waiting for the timer.
 */
public class ReminderService {

    private static final long TICK_INTERVAL_SECONDS = 60L;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledFuture;
    private Consumer<LocalDateTime> onTick;

    /**
     * Starts a timer that calls {@code onTick} every full minute with the current time.
     * Does not call onTick immediately (no reminder on startup).
     */
    public void start(AppSettings settings, Consumer<LocalDateTime> onTick) {
        stop();
        this.onTick = onTick;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ReminderService-timer");
            t.setDaemon(true);
            return t;
        });
        scheduledFuture = scheduler.scheduleAtFixedRate(
                () -> {
                    if (this.onTick != null) {
                        this.onTick.accept(LocalDateTime.now());
                    }
                },
                TICK_INTERVAL_SECONDS,
                TICK_INTERVAL_SECONDS,
                TimeUnit.SECONDS);
    }

    /**
     * For testing: invokes the tick callback with the given time without waiting for the timer.
     * Has no effect if {@link #start(AppSettings, Consumer)} was not called.
     */
    public void tick(LocalDateTime now) {
        if (onTick != null) {
            onTick.accept(now);
        }
    }

    public void stop() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
        }
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
        onTick = null;
    }

    /**
     * Returns true if a reminder dialog should be shown at the given time: the time is within the
     * reminder window (weekday, start–end) and exactly on a reminder interval boundary (e.g. :00, :15, :30, :45 for 15 min).
     * Uses {@link ReminderWindowRules} and {@link ReminderIntervalRules}.
     */
    public boolean isReminderDue(LocalDateTime now, AppSettings settings) {
        return ReminderIntervalRules.isReminderDue(now, settings);
    }
}
