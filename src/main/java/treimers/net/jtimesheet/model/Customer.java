package treimers.net.jtimesheet.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Customer {
    private static final int DEFAULT_CALENDAR_COLOR_INDEX = 0;

    private final String id;
    private String name;
    private String address;
    private String timesheetTemplatePath;
    private String timesheetFilenameSuggestion;
    private String timesheetSheetNo;
    private String timesheetTaskSeparator;
    private CalendarColorPalette calendarColorPalette = CalendarColorPalette.DEFAULT;
    private int calendarColorIndex = DEFAULT_CALENDAR_COLOR_INDEX;
    private final List<Project> projects = new ArrayList<>();

    public Customer(String name) {
        this(UUID.randomUUID().toString(), name, "", "", "", "0", ",");
    }

    public Customer(String id, String name) {
        this(id, name, "", "", "", "0", ",");
    }

    public Customer(
        String id,
        String name,
        String address,
        String timesheetTemplatePath,
        String timesheetFilenameSuggestion,
        String timesheetSheetNo,
        String timesheetTaskSeparator
    ) {
        this.id = id;
        this.name = safeValue(name);
        this.address = safeValue(address);
        this.timesheetTemplatePath = safeValue(timesheetTemplatePath);
        this.timesheetFilenameSuggestion = safeValue(timesheetFilenameSuggestion);
        this.timesheetSheetNo = safeValue(timesheetSheetNo);
        this.timesheetTaskSeparator = safeValue(timesheetTaskSeparator);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value != null ? value : "";
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String value) {
        this.address = value != null ? value : "";
    }

    public String getTimesheetTemplatePath() {
        return timesheetTemplatePath;
    }

    public void setTimesheetTemplatePath(String value) {
        this.timesheetTemplatePath = value != null ? value : "";
    }

    public String getTimesheetFilenameSuggestion() {
        return timesheetFilenameSuggestion;
    }

    public void setTimesheetFilenameSuggestion(String value) {
        this.timesheetFilenameSuggestion = value != null ? value : "";
    }

    public String getTimesheetSheetNo() {
        return timesheetSheetNo;
    }

    public void setTimesheetSheetNo(String value) {
        this.timesheetSheetNo = value != null ? value : "";
    }

    public String getTimesheetTaskSeparator() {
        return timesheetTaskSeparator;
    }

    public void setTimesheetTaskSeparator(String value) {
        this.timesheetTaskSeparator = value != null ? value : "";
    }

    public CalendarColorPalette getCalendarColorPalette() {
        return calendarColorPalette != null ? calendarColorPalette : CalendarColorPalette.DEFAULT;
    }

    public void setCalendarColorPalette(CalendarColorPalette value) {
        this.calendarColorPalette = value != null ? value : CalendarColorPalette.DEFAULT;
    }

    /** Index 0–6 for the palette color. */
    public int getCalendarColorIndex() {
        int i = calendarColorIndex;
        return i < 0 || i > 6 ? DEFAULT_CALENDAR_COLOR_INDEX : i;
    }

    public void setCalendarColorIndex(int value) {
        this.calendarColorIndex = value < 0 ? 0 : Math.min(value, 6);
    }

    /** Hex color for this customer's calendar (from palette + index). */
    public String getCalendarColorHex() {
        return getCalendarColorPalette().getHexColor(getCalendarColorIndex());
    }

    public List<Project> getProjects() {
        return projects;
    }

    private static String safeValue(String value) {
        return value != null ? value : "";
    }

    @Override
    public String toString() {
        return getName();
    }
}
