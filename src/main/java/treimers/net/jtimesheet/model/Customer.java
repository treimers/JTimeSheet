package treimers.net.jtimesheet.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Customer {
    private final String id;
    private String name;
    private String address;
    private String timesheetTemplatePath;
    private String timesheetFilenameSuggestion;
    private String timesheetRounding;
    private String timesheetSheetNo;
    private String timesheetTaskSeparator;
    private final List<Project> projects = new ArrayList<>();

    public Customer(String name) {
        this(UUID.randomUUID().toString(), name, "", "", "", "", "0", ",");
    }

    public Customer(String id, String name) {
        this(id, name, "", "", "", "", "0", ",");
    }

    public Customer(
        String id,
        String name,
        String address,
        String timesheetTemplatePath,
        String timesheetFilenameSuggestion,
        String timesheetRounding,
        String timesheetSheetNo,
        String timesheetTaskSeparator
    ) {
        this.id = id;
        this.name = safeValue(name);
        this.address = safeValue(address);
        this.timesheetTemplatePath = safeValue(timesheetTemplatePath);
        this.timesheetFilenameSuggestion = safeValue(timesheetFilenameSuggestion);
        this.timesheetRounding = safeValue(timesheetRounding);
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

    public String getTimesheetRounding() {
        return timesheetRounding;
    }

    public void setTimesheetRounding(String value) {
        this.timesheetRounding = value != null ? value : "";
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
