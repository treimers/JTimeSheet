package treimers.net.jtimesheet.service;

import static javafx.util.Duration.millis;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.function.Consumer;

import javafx.animation.PauseTransition;
import treimers.net.jtimesheet.model.AppSettings;

/**
 * Runs a timer every full minute and calls a callback. The callback should use
 * {@link #isReminderDue(LocalDateTime, AppSettings)} to decide whether to show the reminder dialog.
 * For tests, call {@link #tick(LocalDateTime)} with a fixed time instead of waiting for the timer.
 */
public class ReminderService {

    private static final long TICK_INTERVAL_MS = 60_000L;

    private PauseTransition minuteTimer;
    private Consumer<LocalDateTime> onTick;

    /**
     * Starts a timer that calls {@code onTick} every full minute with the current time.
     * Does not call onTick immediately (no reminder on startup).
     */
    public void start(AppSettings settings, Consumer<LocalDateTime> onTick) {
        stop();
        this.onTick = onTick;
        minuteTimer = new PauseTransition(millis(TICK_INTERVAL_MS));
        minuteTimer.setOnFinished(event -> {
            if (this.onTick != null) {
                this.onTick.accept(LocalDateTime.now());
            }
            if (minuteTimer != null) {
                minuteTimer.playFromStart();
            }
        });
        minuteTimer.play();
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
        if (minuteTimer != null) {
            minuteTimer.stop();
            minuteTimer = null;
        }
        onTick = null;
    }

    /**
     * Returns true if a reminder dialog should be shown at the given time: the time is within the
     * reminder window (weekday, start–end) and exactly on a reminder interval boundary (e.g. :00, :15, :30, :45 for 15 min).
     */
    public boolean isReminderDue(LocalDateTime now, AppSettings settings) {
        if (!isWithinReminderWindow(now, settings)) {
            return false;
        }
        int interval = AppSettings.normalizeReminderIntervalMinutes(settings.getReminderIntervalMinutes());
        LocalDateTime truncated = now.truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime aligned = alignToReminderInterval(truncated, interval);
        return truncated.equals(aligned);
    }

    private boolean isWithinReminderWindow(LocalDateTime now, AppSettings settings) {
        Set<DayOfWeek> weekdays = settings.getReminderWeekdays();
        if (weekdays == null || weekdays.isEmpty()) {
            return false;
        }
        if (!weekdays.contains(now.getDayOfWeek())) {
            return false;
        }
        return !now.toLocalTime().isBefore(settings.getReminderStartTime())
                && !now.toLocalTime().isAfter(settings.getReminderEndTime());
    }

    private static LocalDateTime alignToReminderInterval(LocalDateTime time, int interval) {
        LocalDateTime base = time.truncatedTo(ChronoUnit.MINUTES);
        int minute = base.getMinute();
        int mod = minute % interval;
        if (mod != 0) {
            base = base.plusMinutes(interval - mod);
        }
        return base;
    }
}
