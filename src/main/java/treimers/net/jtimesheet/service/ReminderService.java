package treimers.net.jtimesheet.service;

import static javafx.util.Duration.millis;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import javafx.animation.PauseTransition;
import treimers.net.jtimesheet.model.AppSettings;

public class ReminderService {
    private PauseTransition reminderDelay;

    public void start(AppSettings settings, Runnable onReminder) {
        stop();
        scheduleNextReminder(settings, onReminder);
    }

    public void stop() {
        if (reminderDelay != null) {
            reminderDelay.stop();
        }
    }

    private void scheduleNextReminder(AppSettings settings, Runnable onReminder) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = nextReminderTime(now, settings);
        Duration delay = Duration.between(now, next);
        if (delay.isNegative()) {
            delay = Duration.ZERO;
        }
        reminderDelay = new PauseTransition(millis(delay.toMillis()));
        reminderDelay.setOnFinished(event -> {
            onReminder.run();
            scheduleNextReminder(settings, onReminder);
        });
        reminderDelay.play();
    }

    private LocalDateTime nextReminderTime(LocalDateTime now, AppSettings settings) {
        int interval = AppSettings.normalizeReminderIntervalMinutes(settings.getReminderIntervalMinutes());
        LocalTime start = settings.getReminderStartTime();
        LocalTime end = settings.getReminderEndTime();
        LocalDateTime candidate = alignToReminderInterval(now, interval);

        while (true) {
            LocalDate date = candidate.toLocalDate();
            LocalDateTime windowStart = LocalDateTime.of(date, start);
            LocalDateTime windowEnd = LocalDateTime.of(date, end);
            if (candidate.isBefore(windowStart)) {
                candidate = alignToReminderInterval(windowStart, interval);
            }
            if (candidate.isAfter(windowEnd)) {
                candidate = alignToReminderInterval(LocalDateTime.of(date.plusDays(1), start), interval);
                continue;
            }
            return candidate;
        }
    }

    private LocalDateTime alignToReminderInterval(LocalDateTime time, int interval) {
        LocalDateTime base = time.truncatedTo(ChronoUnit.MINUTES);
        if (time.isAfter(base)) {
            base = base.plusMinutes(1);
        }
        int minute = base.getMinute();
        int mod = minute % interval;
        if (mod != 0) {
            base = base.plusMinutes(interval - mod);
        }
        return base;
    }
}
