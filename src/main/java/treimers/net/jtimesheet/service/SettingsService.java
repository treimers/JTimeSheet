package treimers.net.jtimesheet.service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Set;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import treimers.net.jtimesheet.model.AppSettings;
import treimers.net.jtimesheet.model.Language;

public class SettingsService {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final String PREF_LANGUAGE = "settings.language";
    private static final String PREF_TIME_GRID = "settings.timeGridMinutes";
    private static final String PREF_REMINDER_INTERVAL = "settings.reminder.intervalMinutes";
    private static final String PREF_REMINDER_START = "settings.reminder.start";
    private static final String PREF_REMINDER_END = "settings.reminder.end";
    private static final String PREF_REMINDER_WEEKDAYS = "settings.reminder.weekdays";
    private static final String PREF_FIRST_DAY_OF_WEEK = "settings.firstDayOfWeek";
    private static final String PREF_DATA_DIRECTORY = "settings.dataDirectory";

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
        String reminderWeekdaysValue = preferences.get(PREF_REMINDER_WEEKDAYS, null);
        String firstDayOfWeekValue = preferences.get(PREF_FIRST_DAY_OF_WEEK, null);
        String dataDirectory = preferences.get(PREF_DATA_DIRECTORY, null);
        if (languageCode == null && timeGrid == null && reminderInterval == null
            && reminderStartValue == null && reminderEndValue == null
            && reminderWeekdaysValue == null && firstDayOfWeekValue == null && dataDirectory == null) {
            return false;
        }
        settings.setLanguage(Language.fromCode(languageCode));
        settings.setTimeGridMinutes(timeGrid);
        settings.setReminderIntervalMinutes(reminderInterval);
        LocalTime start = parseTimeValue(reminderStartValue, LocalTime.of(9, 0));
        LocalTime end = parseTimeValue(reminderEndValue, LocalTime.of(17, 0));
        settings.setReminderWindow(start, end);
        Set<DayOfWeek> weekdays = parseWeekdaysValue(reminderWeekdaysValue);
        if (weekdays != null) {
            settings.setReminderWeekdays(weekdays);
        }
        DayOfWeek firstDayOfWeek = parseDayOfWeek(firstDayOfWeekValue);
        if (firstDayOfWeek != null) {
            settings.setFirstDayOfWeek(firstDayOfWeek);
        }
        settings.setDataDirectory(dataDirectory);
        return true;
    }

    public void save(AppSettings settings) {
        preferences.put(PREF_LANGUAGE, settings.getLanguage().getCode());
        preferences.put(PREF_TIME_GRID, String.valueOf(settings.getTimeGridMinutes()));
        preferences.put(PREF_REMINDER_INTERVAL, String.valueOf(settings.getReminderIntervalMinutes()));
        preferences.put(PREF_REMINDER_START, TIME_FORMAT.format(settings.getReminderStartTime()));
        preferences.put(PREF_REMINDER_END, TIME_FORMAT.format(settings.getReminderEndTime()));
        String weekdaysStr = settings.getReminderWeekdays().stream()
            .map(DayOfWeek::name)
            .collect(Collectors.joining(","));
        preferences.put(PREF_REMINDER_WEEKDAYS, weekdaysStr);
        preferences.put(PREF_FIRST_DAY_OF_WEEK, settings.getFirstDayOfWeek().name());
        preferences.put(PREF_DATA_DIRECTORY, settings.getDataDirectory());
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

    private DayOfWeek parseDayOfWeek(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return DayOfWeek.valueOf(value.trim());
        } catch (Exception exception) {
            return null;
        }
    }

    private Set<DayOfWeek> parseWeekdaysValue(String value) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            return EnumSet.noneOf(DayOfWeek.class);
        }
        try {
            Set<DayOfWeek> result = EnumSet.noneOf(DayOfWeek.class);
            for (String part : value.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    result.add(DayOfWeek.valueOf(trimmed));
                }
            }
            return result.isEmpty() ? null : result;
        } catch (Exception exception) {
            return null;
        }
    }
}
