package treimers.net.jtimesheet.service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Set;

import treimers.net.jtimesheet.model.AppSettings;

/**
 * Rules for the reminder time window: only times on configured weekdays and between
 * window start and end are considered "within window". Used to decide if the current
 * time can trigger a reminder (together with {@link ReminderIntervalRules}).
 * <p>
 * See docs/Rules.md "Reminder-Zeitfenster".
 */
public final class ReminderWindowRules {

    private ReminderWindowRules() {}

    /**
     * Returns true if {@code now} is within the reminder window: weekday is in settings'
     * reminder weekdays and local time is between window start and end (inclusive).
     * <p>
     * Szenario 2: Vor Fenster-Start → false; Nach Fenster-Ende → false;
     * Kein Reminder-Wochentag (z. B. Samstag) → false; Innerhalb Fenster und Wochentag → true.
     */
    public static boolean isWithinWindow(LocalDateTime now, AppSettings settings) {
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
}
