package treimers.net.jtimesheet.model;

import java.util.UUID;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Customer {
    private final String id;
    private final StringProperty name;
    private final StringProperty address;
    private final StringProperty timesheetTemplatePath;
    private final StringProperty timesheetFilenameSuggestion;
    private final StringProperty timesheetRounding;
    private final StringProperty timesheetSheetNo;
    private final StringProperty timesheetMonthRow;
    private final StringProperty timesheetMonthColumn;
    private final StringProperty timesheetDataRow;
    private final StringProperty timesheetDateColumn;
    private final StringProperty timesheetStartColumn;
    private final StringProperty timesheetEndColumn;
    private final StringProperty timesheetPauseColumn;
    private final StringProperty timesheetTaskColumn;
    private final StringProperty timesheetDateFormat;
    private final StringProperty timesheetEvaluateFormulas;
    private final StringProperty timesheetTaskSeparator;
    private final ObservableList<Project> projects = FXCollections.observableArrayList();

    public Customer(String name) {
        this(UUID.randomUUID().toString(), name, "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "");
    }

    public Customer(String id, String name) {
        this(id, name, "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "");
    }

    public Customer(
        String id,
        String name,
        String address,
        String timesheetTemplatePath,
        String timesheetFilenameSuggestion,
        String timesheetRounding,
        String timesheetSheetNo,
        String timesheetMonthRow,
        String timesheetMonthColumn,
        String timesheetDataRow,
        String timesheetDateColumn,
        String timesheetStartColumn,
        String timesheetEndColumn,
        String timesheetPauseColumn,
        String timesheetTaskColumn,
        String timesheetDateFormat,
        String timesheetEvaluateFormulas,
        String timesheetTaskSeparator
    ) {
        this.id = id;
        this.name = new SimpleStringProperty(safeValue(name));
        this.address = new SimpleStringProperty(safeValue(address));
        this.timesheetTemplatePath = new SimpleStringProperty(safeValue(timesheetTemplatePath));
        this.timesheetFilenameSuggestion = new SimpleStringProperty(safeValue(timesheetFilenameSuggestion));
        this.timesheetRounding = new SimpleStringProperty(safeValue(timesheetRounding));
        this.timesheetSheetNo = new SimpleStringProperty(safeValue(timesheetSheetNo));
        this.timesheetMonthRow = new SimpleStringProperty(safeValue(timesheetMonthRow));
        this.timesheetMonthColumn = new SimpleStringProperty(safeValue(timesheetMonthColumn));
        this.timesheetDataRow = new SimpleStringProperty(safeValue(timesheetDataRow));
        this.timesheetDateColumn = new SimpleStringProperty(safeValue(timesheetDateColumn));
        this.timesheetStartColumn = new SimpleStringProperty(safeValue(timesheetStartColumn));
        this.timesheetEndColumn = new SimpleStringProperty(safeValue(timesheetEndColumn));
        this.timesheetPauseColumn = new SimpleStringProperty(safeValue(timesheetPauseColumn));
        this.timesheetTaskColumn = new SimpleStringProperty(safeValue(timesheetTaskColumn));
        this.timesheetDateFormat = new SimpleStringProperty(safeValue(timesheetDateFormat));
        this.timesheetEvaluateFormulas = new SimpleStringProperty(safeValue(timesheetEvaluateFormulas));
        this.timesheetTaskSeparator = new SimpleStringProperty(safeValue(timesheetTaskSeparator));
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String value) {
        name.set(value);
    }

    public String getAddress() {
        return address.get();
    }

    public void setAddress(String value) {
        address.set(value);
    }

    public String getTimesheetTemplatePath() {
        return timesheetTemplatePath.get();
    }

    public void setTimesheetTemplatePath(String value) {
        timesheetTemplatePath.set(value);
    }

    public String getTimesheetFilenameSuggestion() {
        return timesheetFilenameSuggestion.get();
    }

    public void setTimesheetFilenameSuggestion(String value) {
        timesheetFilenameSuggestion.set(value);
    }

    public String getTimesheetRounding() {
        return timesheetRounding.get();
    }

    public void setTimesheetRounding(String value) {
        timesheetRounding.set(value);
    }

    public String getTimesheetSheetNo() {
        return timesheetSheetNo.get();
    }

    public void setTimesheetSheetNo(String value) {
        timesheetSheetNo.set(value);
    }

    public String getTimesheetMonthRow() {
        return timesheetMonthRow.get();
    }

    public void setTimesheetMonthRow(String value) {
        timesheetMonthRow.set(value);
    }

    public String getTimesheetMonthColumn() {
        return timesheetMonthColumn.get();
    }

    public void setTimesheetMonthColumn(String value) {
        timesheetMonthColumn.set(value);
    }

    public String getTimesheetDataRow() {
        return timesheetDataRow.get();
    }

    public void setTimesheetDataRow(String value) {
        timesheetDataRow.set(value);
    }

    public String getTimesheetDateColumn() {
        return timesheetDateColumn.get();
    }

    public void setTimesheetDateColumn(String value) {
        timesheetDateColumn.set(value);
    }

    public String getTimesheetStartColumn() {
        return timesheetStartColumn.get();
    }

    public void setTimesheetStartColumn(String value) {
        timesheetStartColumn.set(value);
    }

    public String getTimesheetEndColumn() {
        return timesheetEndColumn.get();
    }

    public void setTimesheetEndColumn(String value) {
        timesheetEndColumn.set(value);
    }

    public String getTimesheetPauseColumn() {
        return timesheetPauseColumn.get();
    }

    public void setTimesheetPauseColumn(String value) {
        timesheetPauseColumn.set(value);
    }

    public String getTimesheetTaskColumn() {
        return timesheetTaskColumn.get();
    }

    public void setTimesheetTaskColumn(String value) {
        timesheetTaskColumn.set(value);
    }

    public String getTimesheetDateFormat() {
        return timesheetDateFormat.get();
    }

    public void setTimesheetDateFormat(String value) {
        timesheetDateFormat.set(value);
    }

    public String getTimesheetEvaluateFormulas() {
        return timesheetEvaluateFormulas.get();
    }

    public void setTimesheetEvaluateFormulas(String value) {
        timesheetEvaluateFormulas.set(value);
    }

    public String getTimesheetTaskSeparator() {
        return timesheetTaskSeparator.get();
    }

    public void setTimesheetTaskSeparator(String value) {
        timesheetTaskSeparator.set(value);
    }

    public ObservableList<Project> getProjects() {
        return projects;
    }

    private String safeValue(String value) {
        return value != null ? value : "";
    }

    @Override
    public String toString() {
        return getName();
    }
}
