package treimers.net.whathaveyoudone.storage;

import java.util.ArrayList;
import java.util.List;

public class CustomerData {
    public String id;
    public String name;
    public String address;
    public String timesheetPropertiesPath;
    public String timesheetTemplatePath;
    public String timesheetFilenameSuggestion;
    public List<ProjectData> projects = new ArrayList<>();
}
