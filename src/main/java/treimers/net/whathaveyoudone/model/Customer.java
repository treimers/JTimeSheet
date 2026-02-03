package treimers.net.whathaveyoudone.model;

import java.util.UUID;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Customer {
    private final String id;
    private final StringProperty name;
    private final ObservableList<Project> projects = FXCollections.observableArrayList();

    public Customer(String name) {
        this(UUID.randomUUID().toString(), name);
    }

    public Customer(String id, String name) {
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

    public ObservableList<Project> getProjects() {
        return projects;
    }

    @Override
    public String toString() {
        return getName();
    }
}
