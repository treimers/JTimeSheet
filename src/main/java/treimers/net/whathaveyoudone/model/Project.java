package treimers.net.whathaveyoudone.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Project {
    private final StringProperty name;
    private final ObservableList<Task> tasks = FXCollections.observableArrayList();

    public Project(String name) {
        this.name = new SimpleStringProperty(name);
    }

    public String getName() {
        return name.get();
    }

    public void setName(String value) {
        name.set(value);
    }

    public ObservableList<Task> getTasks() {
        return tasks;
    }

    @Override
    public String toString() {
        return getName();
    }
}
