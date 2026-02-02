package treimers.net.whathaveyoudone.ui;

import treimers.net.whathaveyoudone.model.Customer;
import treimers.net.whathaveyoudone.model.Project;

class SelectedProject {
    final Customer customer;
    final Project project;

    SelectedProject(Customer customer, Project project) {
        this.customer = customer;
        this.project = project;
    }
}
