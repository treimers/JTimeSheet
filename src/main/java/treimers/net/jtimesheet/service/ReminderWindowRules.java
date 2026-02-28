package treimers.net.jtimesheet.service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

import treimers.net.jtimesheet.model.AppSettings;

/**
 * Rules for the reminder time window: only times on configured weekdays and between
 * window start and end are considered "within window". Used to decide if the current
 * time can trigger a reminder (together with {@link ReminderIntervalRules}).
 * <p>
 * When end time is {@link LocalTime#MIDNIGHT} (00:00), it is interpreted as end of day (24:00).
 * <p>
 * See docs/Rules.md "Reminder-Zeitfenster".
 */
public final class ReminderWindowRules {

    private ReminderWindowRules() {}

    /**
     * Returns true if {@code now} is within the reminder window: weekday is in settings'
     * reminder weekdays and local time is between window start and end (inclusive).
     * End time 00:00 means "until end of day" (24:00).
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
        LocalTime time = now.toLocalTime();
        if (time.isBefore(settings.getReminderStartTime())) {
            return false;
        }
        LocalTime end = settings.getReminderEndTime();
        if (end.equals(LocalTime.MIDNIGHT)) {
            return true;
        }
        return !time.isAfter(end);
    }
}
