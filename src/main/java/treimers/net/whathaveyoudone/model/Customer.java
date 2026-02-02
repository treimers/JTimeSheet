package treimers.net.whathaveyoudone.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Customer {
    private final StringProperty name;
    private final ObservableList<Project> projects = FXCollections.observableArrayList();

    public Customer(String name) {
        this.name = new SimpleStringProperty(name);
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
