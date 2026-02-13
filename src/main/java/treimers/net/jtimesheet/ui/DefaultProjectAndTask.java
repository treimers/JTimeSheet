package treimers.net.jtimesheet.ui;

import treimers.net.jtimesheet.model.Project;
import treimers.net.jtimesheet.model.Task;

/**
 * Default project and task selection (e.g. last used for a customer).
 */
public class DefaultProjectAndTask {
    private final Project project;
    private final Task task;

    public DefaultProjectAndTask(Project project, Task task) {
        this.project = project;
        this.task = task;
    }

    public Project getProject() {
        return project;
    }

    public Task getTask() {
        return task;
    }
}
