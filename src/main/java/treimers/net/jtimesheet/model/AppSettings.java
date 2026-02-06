package treimers.net.jtimesheet.model;

import java.time.LocalTime;

public class AppSettings {
    public static final int DEFAULT_TIME_GRID_MINUTES = 1;
    public static final int DEFAULT_REMINDER_INTERVAL_MINUTES = 60;
    public static final String DEFAULT_DATA_DIRECTORY = System.getProperty("user.home");

    private int timeGridMinutes = DEFAULT_TIME_GRID_MINUTES;
    private Language language = Language.ENGLISH;
    private int reminderIntervalMinutes = DEFAULT_REMINDER_INTERVAL_MINUTES;
    private LocalTime reminderStartTime = LocalTime.of(9, 0);
    private LocalTime reminderEndTime = LocalTime.of(17, 0);
    private String dataDirectory = DEFAULT_DATA_DIRECTORY;

    public int getTimeGridMinutes() {
        return timeGridMinutes;
    }

    public void setTimeGridMinutes(Integer value) {
        timeGridMinutes = normalizeTimeGridMinutes(value);
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language != null ? language : Language.ENGLISH;
    }

    public int getReminderIntervalMinutes() {
        return reminderIntervalMinutes;
    }

    public void setReminderIntervalMinutes(Integer value) {
        reminderIntervalMinutes = normalizeReminderIntervalMinutes(value);
    }

    public LocalTime getReminderStartTime() {
        return reminderStartTime;
    }

    public LocalTime getReminderEndTime() {
        return reminderEndTime;
    }

    public void setReminderWindow(LocalTime start, LocalTime end) {
        if (start == null || end == null || !start.isBefore(end)) {
            reminderStartTime = LocalTime.of(9, 0);
            reminderEndTime = LocalTime.of(17, 0);
            return;
        }
        reminderStartTime = start;
        reminderEndTime = end;
    }

    public String getDataDirectory() {
        return dataDirectory;
    }

    public void setDataDirectory(String value) {
        if (value == null) {
            dataDirectory = DEFAULT_DATA_DIRECTORY;
            return;
        }
        String normalized = value.trim();
        dataDirectory = normalized.isEmpty() ? DEFAULT_DATA_DIRECTORY : normalized;
    }

    public static int normalizeTimeGridMinutes(Integer value) {
        if (value == null) {
            return DEFAULT_TIME_GRID_MINUTES;
        }
        if (value < 1 || value > 60) {
            return DEFAULT_TIME_GRID_MINUTES;
        }
        if (60 % value != 0) {
            return DEFAULT_TIME_GRID_MINUTES;
        }
        return value;
    }

    public static int normalizeReminderIntervalMinutes(Integer value) {
        if (value == null) {
            return DEFAULT_REMINDER_INTERVAL_MINUTES;
        }
        if (value < 1 || value > 60) {
            return DEFAULT_REMINDER_INTERVAL_MINUTES;
        }
        if (60 % value != 0) {
            return DEFAULT_REMINDER_INTERVAL_MINUTES;
        }
        return value;
    }
}
