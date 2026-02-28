package treimers.net.jtimesheet.storage;

import java.util.ArrayList;
import java.util.List;

public class CustomerData {
    public String id;
    public String name;
    public String address;
    public String timesheetTemplatePath;
    public String timesheetFilenameSuggestion;
    public String timesheetSheetNo;
    public String timesheetTaskSeparator;
    /** Calendar color: palette name (DEFAULT, PASTEL, HIGH_CONTRAST). */
    public String calendarColorPalette;
    /** Calendar color index 0–6. */
    public Integer calendarColorIndex;
    public List<ProjectData> projects = new ArrayList<>();
}
