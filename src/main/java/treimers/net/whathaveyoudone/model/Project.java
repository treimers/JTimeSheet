package treimers.net.whathaveyoudone.model;

import java.util.UUID;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Project {
    private final String id;
    private final StringProperty name;
    private final ObservableList<Task> tasks = FXCollections.observableArrayList();

    public Project(String name) {
        this(UUID.randomUUID().toString(), name);
    }

    public Project(String id, String name) {
        this.id = id;
        this.name = new SimpleStringProperty(name);
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

    public ObservableList<Task> getTasks() {
        return tasks;
    }

    @Override
    public String toString() {
        return getName();
    }
}
