package treimers.net.whathaveyoudone.storage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TaskData {
    public String id;
    public String name;

    public TaskData() {
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public TaskData(String name) {
        this.name = name;
    }

    public TaskData(@JsonProperty("id") String id, @JsonProperty("name") String name) {
        this.id = id;
        this.name = name;
    }
}
