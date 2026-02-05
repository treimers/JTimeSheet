package treimers.net.jtimesheet.ui;

import treimers.net.jtimesheet.model.Customer;
import treimers.net.jtimesheet.model.Project;
import treimers.net.jtimesheet.model.Task;

class SelectedTask {
    final Customer customer;
    final Project project;
    final Task task;

    SelectedTask(Customer customer, Project project, Task task) {
        this.customer = customer;
        this.project = project;
        this.task = task;
    }
}
