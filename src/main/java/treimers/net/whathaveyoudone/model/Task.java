package treimers.net.whathaveyoudone.model;

import java.util.UUID;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Task {
    private final String id;
    private final StringProperty name;

    public Task(String name) {
        this(UUID.randomUUID().toString(), name);
    }

    public Task(String id, String name) {
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

    @Override
    public String toString() {
        return getName();
    }
}
