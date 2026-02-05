package treimers.net.jtimesheet.ui;

import treimers.net.jtimesheet.model.Customer;
import treimers.net.jtimesheet.model.Project;
import treimers.net.jtimesheet.model.Task;

class NodeData {
    final NodeType type;
    final Customer customer;
    final Project project;
    final Task task;

    private NodeData(NodeType type, Customer customer, Project project, Task task) {
        this.type = type;
        this.customer = customer;
        this.project = project;
        this.task = task;
    }

    static NodeData root() {
        return new NodeData(NodeType.ROOT, null, null, null);
    }

    static NodeData customer(Customer customer) {
        return new NodeData(NodeType.CUSTOMER, customer, null, null);
    }

    static NodeData project(Customer customer, Project project) {
        return new NodeData(NodeType.PROJECT, customer, project, null);
    }

    static NodeData task(Customer customer, Project project, Task task) {
        return new NodeData(NodeType.TASK, customer, project, task);
    }

    String displayName() {
        if (type == NodeType.CUSTOMER) {
            return customer.getName();
        }
        if (type == NodeType.PROJECT) {
            return project.getName();
        }
        if (type == NodeType.TASK) {
            return task.getName();
        }
        return "";
    }
}
