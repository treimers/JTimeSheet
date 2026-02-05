package treimers.net.jtimesheet.ui;

import treimers.net.jtimesheet.model.Customer;
import treimers.net.jtimesheet.model.Project;

class SelectedProject {
    final Customer customer;
    final Project project;

    SelectedProject(Customer customer, Project project) {
        this.customer = customer;
        this.project = project;
    }
}
