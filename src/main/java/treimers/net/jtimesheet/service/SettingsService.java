package treimers.net.jtimesheet.service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.prefs.Preferences;

import treimers.net.jtimesheet.model.AppSettings;
import treimers.net.jtimesheet.model.Language;

public class SettingsService {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final String PREF_LANGUAGE = "settings.language";
    private static final String PREF_TIME_GRID = "settings.timeGridMinutes";
    private static final String PREF_REMINDER_INTERVAL = "settings.reminder.intervalMinutes";
    private static final String PREF_REMINDER_START = "settings.reminder.start";
    private static final String PREF_REMINDER_END = "settings.reminder.end";

    private final Preferences preferences;

    public SettingsService(Preferences preferences) {
        this.preferences = preferences;
    }

    public boolean loadIfPresent(AppSettings settings) {
        String languageCode = preferences.get(PREF_LANGUAGE, null);
        Integer timeGrid = readIntPreference(PREF_TIME_GRID);
        Integer reminderInterval = readIntPreference(PREF_REMINDER_INTERVAL);
        String reminderStartValue = preferences.get(PREF_REMINDER_START, null);
        String reminderEndValue = preferences.get(PREF_REMINDER_END, null);
        if (languageCode == null && timeGrid == null && reminderInterval == null
            && reminderStartValue == null && reminderEndValue == null) {
            return false;
        }
        settings.setLanguage(Language.fromCode(languageCode));
        settings.setTimeGridMinutes(timeGrid);
        settings.setReminderIntervalMinutes(reminderInterval);
        LocalTime start = parseTimeValue(reminderStartValue, LocalTime.of(9, 0));
        LocalTime end = parseTimeValue(reminderEndValue, LocalTime.of(17, 0));
        settings.setReminderWindow(start, end);
        return true;
    }

    public void save(AppSettings settings) {
        preferences.put(PREF_LANGUAGE, settings.getLanguage().getCode());
        preferences.put(PREF_TIME_GRID, String.valueOf(settings.getTimeGridMinutes()));
        preferences.put(PREF_REMINDER_INTERVAL, String.valueOf(settings.getReminderIntervalMinutes()));
        preferences.put(PREF_REMINDER_START, TIME_FORMAT.format(settings.getReminderStartTime()));
        preferences.put(PREF_REMINDER_END, TIME_FORMAT.format(settings.getReminderEndTime()));
    }

    private Integer readIntPreference(String key) {
        String value = preferences.get(key, null);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private LocalTime parseTimeValue(String value, LocalTime fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return LocalTime.parse(value, TIME_FORMAT);
        } catch (Exception exception) {
            return fallback;
        }
    }
}
