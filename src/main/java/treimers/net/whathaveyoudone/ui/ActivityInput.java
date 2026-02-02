package treimers.net.whathaveyoudone.ui;

import java.time.LocalDateTime;
import treimers.net.whathaveyoudone.model.Customer;
import treimers.net.whathaveyoudone.model.Project;
import treimers.net.whathaveyoudone.model.Task;

public class ActivityInput {
    private final Customer customer;
    private final Project project;
    private final Task task;
    private final LocalDateTime from;
    private final LocalDateTime to;

    public ActivityInput(Customer customer, Project project, Task task, LocalDateTime from, LocalDateTime to) {
        this.customer = customer;
        this.project = project;
        this.task = task;
        this.from = from;
        this.to = to;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Project getProject() {
        return project;
    }

    public Task getTask() {
        return task;
    }

    public LocalDateTime getFrom() {
        return from;
    }

    public LocalDateTime getTo() {
        return to;
    }
}
