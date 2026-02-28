package treimers.net.jtimesheet.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import treimers.net.jtimesheet.model.AppSettings;

/**
 * Rules for reminder interval: a reminder is due only when the current minute is exactly
 * on an interval boundary (e.g. :00, :15, :30, :45 for 15 min). Used together with
 * {@link ReminderWindowRules} to decide if a reminder should be shown.
 * <p>
 * See docs/Rules.md "Reminder-Intervall".
 */
public final class ReminderIntervalRules {

    private ReminderIntervalRules() {}

    /**
     * Returns true if {@code now} (truncated to minutes) is exactly on a reminder-interval
     * boundary for the given settings (e.g. :00, :15, :30, :45 for 15 min).
     */
    public static boolean isOnIntervalBoundary(LocalDateTime now, AppSettings settings) {
        int interval = AppSettings.normalizeReminderIntervalMinutes(settings.getReminderIntervalMinutes());
        LocalDateTime truncated = now.truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime aligned = alignToInterval(truncated, interval);
        return truncated.equals(aligned);
    }

    /**
     * Returns true if a reminder is due at {@code now}: time is within the reminder window
     * and on an interval boundary.
     */
    public static boolean isReminderDue(LocalDateTime now, AppSettings settings) {
        return ReminderWindowRules.isWithinWindow(now, settings)
                && isOnIntervalBoundary(now, settings);
    }

    static LocalDateTime alignToInterval(LocalDateTime time, int intervalMinutes) {
        LocalDateTime base = time.truncatedTo(ChronoUnit.MINUTES);
        int minute = base.getMinute();
        int mod = minute % intervalMinutes;
        if (mod != 0) {
            base = base.plusMinutes(intervalMinutes - mod);
        }
        return base;
    }
}
