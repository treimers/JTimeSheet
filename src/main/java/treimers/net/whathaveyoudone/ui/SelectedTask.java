package treimers.net.whathaveyoudone.ui;

import treimers.net.whathaveyoudone.model.Customer;
import treimers.net.whathaveyoudone.model.Project;
import treimers.net.whathaveyoudone.model.Task;

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
