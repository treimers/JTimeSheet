package treimers.net.jtimesheet.storage;

import java.util.ArrayList;
import java.util.List;

public class CustomerData {
    public String id;
    public String name;
    public String address;
    public String timesheetTemplatePath;
    public String timesheetFilenameSuggestion;
    public String timesheetRounding;
    public String timesheetSheetNo;
    public String timesheetMonthRow;
    public String timesheetMonthColumn;
    public String timesheetDataRow;
    public String timesheetDateColumn;
    public String timesheetStartColumn;
    public String timesheetEndColumn;
    public String timesheetPauseColumn;
    public String timesheetTaskColumn;
    public String timesheetEvaluateFormulas;
    public String timesheetTaskSeparator;
    public List<ProjectData> projects = new ArrayList<>();
}
