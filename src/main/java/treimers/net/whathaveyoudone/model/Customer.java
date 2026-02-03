package treimers.net.whathaveyoudone.model;

import java.util.UUID;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Customer {
    private final String id;
    private final StringProperty name;
    private final StringProperty address;
    private final StringProperty timesheetPropertiesPath;
    private final StringProperty timesheetTemplatePath;
    private final StringProperty timesheetFilenameSuggestion;
    private final ObservableList<Project> projects = FXCollections.observableArrayList();

    public Customer(String name) {
        this(UUID.randomUUID().toString(), name, "", "", "", "");
    }

    public Customer(String id, String name) {
        this(id, name, "", "", "", "");
    }

    public Customer(
        String id,
        String name,
        String address,
        String timesheetPropertiesPath,
        String timesheetTemplatePath,
        String timesheetFilenameSuggestion
    ) {
        this.id = id;
        this.name = new SimpleStringProperty(name != null ? name : "");
        this.address = new SimpleStringProperty(address != null ? address : "");
        this.timesheetPropertiesPath = new SimpleStringProperty(timesheetPropertiesPath != null ? timesheetPropertiesPath : "");
        this.timesheetTemplatePath = new SimpleStringProperty(timesheetTemplatePath != null ? timesheetTemplatePath : "");
        this.timesheetFilenameSuggestion = new SimpleStringProperty(timesheetFilenameSuggestion != null ? timesheetFilenameSuggestion : "");
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

    public String getTimesheetPropertiesPath() {
        return timesheetPropertiesPath.get();
    }

    public void setTimesheetPropertiesPath(String value) {
        timesheetPropertiesPath.set(value);
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

    public ObservableList<Project> getProjects() {
        return projects;
    }

    @Override
    public String toString() {
        return getName();
    }
}
