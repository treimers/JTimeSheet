package treimers.net.whathaveyoudone.ui;

import java.util.Optional;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import treimers.net.whathaveyoudone.model.Customer;
import treimers.net.whathaveyoudone.model.Project;
import treimers.net.whathaveyoudone.model.Task;

public class ManagementDialog {
    private final ObservableList<Customer> customers;
    private final Runnable onSave;
    private final ActivityCallbacks activityCallbacks;
    private TreeView<NodeData> treeView;
    private TreeItem<NodeData> rootItem;

    public ManagementDialog(ObservableList<Customer> customers, Runnable onSave, ActivityCallbacks activityCallbacks) {
        this.customers = customers;
        this.onSave = onSave;
        this.activityCallbacks = activityCallbacks;
    }

    public void show(Stage owner) {
        ensureManagementTree();

        MenuBar menuBar = new MenuBar(
            customerMenu(),
            projectMenu(),
            taskMenu()
        );

        VBox treePanel = new VBox(10, sectionHeader("Customers, Projects, and Tasks"), treeView);
        treePanel.setPadding(new Insets(12));
        VBox.setVgrow(treeView, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setTop(menuBar);
        root.setCenter(treePanel);
        BorderPane.setMargin(treePanel, new Insets(10));

        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Manage Customers, Projects, and Tasks");
        dialog.setScene(new Scene(root, 520, 480));
        dialog.showAndWait();
    }

    ContextMenu emptyMenu() {
        MenuItem addCustomer = menuItemWithIcon("Add Customer", "add", this::addCustomer);
        return new ContextMenu(addCustomer);
    }

    ContextMenu contextMenuFor(NodeData data) {
        MenuItem addCustomer = menuItemWithIcon("Add Customer", "add", this::addCustomer);
        MenuItem addProject = menuItemWithIcon("Add Project", "add", this::addProject);
        MenuItem addTask = menuItemWithIcon("Add Task", "add", this::addTask);
        MenuItem renameCustomer = menuItemWithIcon("Rename Customer", "edit", this::renameCustomer);
        MenuItem deleteCustomer = menuItemWithIcon("Delete Customer", "delete", this::deleteCustomer);
        MenuItem renameProject = menuItemWithIcon("Rename Project", "edit", this::renameProject);
        MenuItem deleteProject = menuItemWithIcon("Delete Project", "delete", this::deleteProject);
        MenuItem renameTask = menuItemWithIcon("Rename Task", "edit", this::renameTask);
        MenuItem deleteTask = menuItemWithIcon("Delete Task", "delete", this::deleteTask);

        ContextMenu menu = new ContextMenu();
        if (data.type == NodeType.CUSTOMER) {
            menu.getItems().addAll(addCustomer, renameCustomer, deleteCustomer, new SeparatorMenuItem(), addProject);
        } else if (data.type == NodeType.PROJECT) {
            menu.getItems().addAll(addProject, renameProject, deleteProject, new SeparatorMenuItem(), addTask);
        } else if (data.type == NodeType.TASK) {
            menu.getItems().addAll(addTask, renameTask, deleteTask);
        } else {
            menu.getItems().add(addCustomer);
        }
        return menu;
    }

    private Menu customerMenu() {
        MenuItem add = menuItemWithIcon("Add Customer", "add", this::addCustomer);
        MenuItem rename = menuItemWithIcon("Rename Customer", "edit", this::renameCustomer);
        MenuItem delete = menuItemWithIcon("Delete Customer", "delete", this::deleteCustomer);

        Menu menu = new Menu("Customer");
        menu.getItems().addAll(add, rename, delete);
        return menu;
    }

    private Menu projectMenu() {
        MenuItem add = menuItemWithIcon("Add Project", "add", this::addProject);
        MenuItem rename = menuItemWithIcon("Rename Project", "edit", this::renameProject);
        MenuItem delete = menuItemWithIcon("Delete Project", "delete", this::deleteProject);

        Menu menu = new Menu("Project");
        menu.getItems().addAll(add, rename, delete);
        return menu;
    }

    private Menu taskMenu() {
        MenuItem add = menuItemWithIcon("Add Task", "add", this::addTask);
        MenuItem rename = menuItemWithIcon("Rename Task", "edit", this::renameTask);
        MenuItem delete = menuItemWithIcon("Delete Task", "delete", this::deleteTask);

        Menu menu = new Menu("Task");
        menu.getItems().addAll(add, rename, delete);
        return menu;
    }

    private Label sectionHeader(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        return label;
    }

    private void ensureManagementTree() {
        rootItem = new TreeItem<>(NodeData.root());
        rootItem.setExpanded(true);

        treeView = new TreeView<>(rootItem);
        treeView.setShowRoot(false);
        treeView.setCellFactory(listView -> new ProjectTreeCell(this));

        rebuildTree();
    }

    private void rebuildTree() {
        rootItem.getChildren().clear();
        for (Customer customer : customers) {
            TreeItem<NodeData> customerItem = new TreeItem<>(NodeData.customer(customer));
            customerItem.setExpanded(true);
            for (Project project : customer.getProjects()) {
                TreeItem<NodeData> projectItem = new TreeItem<>(NodeData.project(customer, project));
                projectItem.setExpanded(true);
                for (Task task : project.getTasks()) {
                    projectItem.getChildren().add(new TreeItem<>(NodeData.task(customer, project, task)));
                }
                customerItem.getChildren().add(projectItem);
            }
            rootItem.getChildren().add(customerItem);
        }
    }

    private void addCustomer() {
        Optional<String> name = promptForText("New Customer", "Customer name:");
        name.ifPresent(value -> {
            Customer customer = new Customer(value);
            customers.add(customer);
            rebuildTree();
            selectCustomer(customer);
            onSave.run();
        });
    }

    private void renameCustomer() {
        Customer customer = getSelectedCustomer();
        if (customer == null) {
            showInfo("Select a customer to rename.");
            return;
        }
        Optional<String> name = promptForText("Rename Customer", "New customer name:", customer.getName());
        name.ifPresent(value -> {
            String oldName = customer.getName();
            customer.setName(value);
            activityCallbacks.updateActivitiesForCustomerRename(oldName, value);
            rebuildTree();
            selectCustomer(customer);
            onSave.run();
        });
    }

    private void deleteCustomer() {
        Customer customer = getSelectedCustomer();
        if (customer == null) {
            showInfo("Select a customer to delete.");
            return;
        }
        int activityCount = activityCallbacks.countActivitiesForCustomer(customer.getName());
        String message = "Delete customer \"" + customer.getName() + "\"?";
        if (activityCount > 0) {
            message += " This will remove " + activityCount + " activities.";
        }
        if (confirmDelete("Delete customer", message)) {
            customers.remove(customer);
            activityCallbacks.removeActivitiesForCustomer(customer.getName());
            rebuildTree();
            onSave.run();
        }
    }

    private void addProject() {
        Customer customer = getSelectedCustomer();
        if (customer == null) {
            showInfo("Select a customer first.");
            return;
        }
        Optional<String> name = promptForText("New Project", "Project name:");
        name.ifPresent(value -> {
            Project project = new Project(value);
            customer.getProjects().add(project);
            rebuildTree();
            selectProject(customer, project);
            onSave.run();
        });
    }

    private void renameProject() {
        SelectedProject selected = getSelectedProject();
        if (selected == null) {
            showInfo("Select a project to rename.");
            return;
        }
        Optional<String> name = promptForText("Rename Project", "New project name:", selected.project.getName());
        name.ifPresent(value -> {
            String oldName = selected.project.getName();
            selected.project.setName(value);
            activityCallbacks.updateActivitiesForProjectRename(selected.customer.getName(), oldName, value);
            rebuildTree();
            selectProject(selected.customer, selected.project);
            onSave.run();
        });
    }

    private void deleteProject() {
        SelectedProject selected = getSelectedProject();
        if (selected == null) {
            showInfo("Select a project to delete.");
            return;
        }
        int activityCount = activityCallbacks.countActivitiesForProject(
            selected.customer.getName(),
            selected.project.getName()
        );
        String message = "Delete project \"" + selected.project.getName() + "\"?";
        if (activityCount > 0) {
            message += " This will remove " + activityCount + " activities.";
        }
        if (confirmDelete("Delete project", message)) {
            selected.customer.getProjects().remove(selected.project);
            activityCallbacks.removeActivitiesForProject(selected.customer.getName(), selected.project.getName());
            rebuildTree();
            selectCustomer(selected.customer);
            onSave.run();
        }
    }

    private void addTask() {
        SelectedProject selected = getSelectedProject();
        if (selected == null) {
            showInfo("Select a project first.");
            return;
        }
        Optional<String> name = promptForText("New Task", "Task name:");
        name.ifPresent(value -> {
            selected.project.getTasks().add(new Task(value));
            rebuildTree();
            selectTask(selected.customer, selected.project, value);
            onSave.run();
        });
    }

    private void renameTask() {
        SelectedTask selected = getSelectedTask();
        if (selected == null) {
            showInfo("Select a task to rename.");
            return;
        }
        Optional<String> name = promptForText("Rename Task", "New task name:", selected.task.getName());
        name.ifPresent(value -> {
            int index = selected.project.getTasks().indexOf(selected.task);
            if (index >= 0) {
                String oldName = selected.task.getName();
                selected.project.getTasks().set(index, new Task(value));
                activityCallbacks.updateActivitiesForTaskRename(
                    selected.customer.getName(),
                    selected.project.getName(),
                    oldName,
                    value
                );
                rebuildTree();
                selectTask(selected.customer, selected.project, value);
                onSave.run();
            }
        });
    }

    private void deleteTask() {
        SelectedTask selected = getSelectedTask();
        if (selected == null) {
            showInfo("Select a task to delete.");
            return;
        }
        int activityCount = activityCallbacks.countActivitiesForTask(
            selected.customer.getName(),
            selected.project.getName(),
            selected.task.getName()
        );
        String message = "Delete task \"" + selected.task.getName() + "\"?";
        if (activityCount > 0) {
            message += " This will remove " + activityCount + " activities.";
        }
        if (confirmDelete("Delete task", message)) {
            selected.project.getTasks().remove(selected.task);
            activityCallbacks.removeActivitiesForTask(
                selected.customer.getName(),
                selected.project.getName(),
                selected.task.getName()
            );
            rebuildTree();
            selectProject(selected.customer, selected.project);
            onSave.run();
        }
    }

    private Customer getSelectedCustomer() {
        TreeItem<NodeData> selectedItem = treeView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            return null;
        }
        NodeData data = selectedItem.getValue();
        if (data.type == NodeType.CUSTOMER) {
            return data.customer;
        }
        if (data.type == NodeType.PROJECT || data.type == NodeType.TASK) {
            return data.customer;
        }
        return null;
    }

    private SelectedProject getSelectedProject() {
        TreeItem<NodeData> selectedItem = treeView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            return null;
        }
        NodeData data = selectedItem.getValue();
        if (data.type == NodeType.PROJECT) {
            return new SelectedProject(data.customer, data.project);
        }
        if (data.type == NodeType.TASK) {
            return new SelectedProject(data.customer, data.project);
        }
        return null;
    }

    private SelectedTask getSelectedTask() {
        TreeItem<NodeData> selectedItem = treeView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            return null;
        }
        NodeData data = selectedItem.getValue();
        if (data.type == NodeType.TASK) {
            return new SelectedTask(data.customer, data.project, data.task);
        }
        return null;
    }

    private void selectCustomer(Customer customer) {
        for (TreeItem<NodeData> customerItem : rootItem.getChildren()) {
            if (customerItem.getValue().customer == customer) {
                treeView.getSelectionModel().select(customerItem);
                return;
            }
        }
    }

    private void selectProject(Customer customer, Project project) {
        for (TreeItem<NodeData> customerItem : rootItem.getChildren()) {
            if (customerItem.getValue().customer == customer) {
                for (TreeItem<NodeData> projectItem : customerItem.getChildren()) {
                    if (projectItem.getValue().project == project) {
                        treeView.getSelectionModel().select(projectItem);
                        return;
                    }
                }
            }
        }
    }

    private void selectTask(Customer customer, Project project, String taskName) {
        for (TreeItem<NodeData> customerItem : rootItem.getChildren()) {
            if (customerItem.getValue().customer == customer) {
                for (TreeItem<NodeData> projectItem : customerItem.getChildren()) {
                    if (projectItem.getValue().project == project) {
                        for (TreeItem<NodeData> taskItem : projectItem.getChildren()) {
                            if (taskName.equals(taskItem.getValue().task.getName())) {
                                treeView.getSelectionModel().select(taskItem);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private Optional<String> promptForText(String title, String header) {
        return promptForText(title, header, "");
    }

    private Optional<String> promptForText(String title, String header, String initialValue) {
        TextInputDialog dialog = new TextInputDialog(initialValue);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText("Name:");
        return dialog.showAndWait().map(String::trim).filter(value -> !value.isEmpty());
    }

    private boolean confirmDelete(String title, String content) {
        ButtonType delete = new ButtonType("Delete", ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(AlertType.CONFIRMATION, content, delete, cancel);
        alert.setTitle(title);
        alert.setHeaderText(null);
        return alert.showAndWait().orElse(cancel) == delete;
    }

    private void showInfo(String message) {
        Alert alert = new Alert(AlertType.INFORMATION, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private MenuItem menuItemWithIcon(String text, String iconKey, Runnable action) {
        MenuItem item = new MenuItem(text);
        item.setGraphic(iconFor(iconKey));
        item.setOnAction(event -> action.run());
        return item;
    }

    private Node iconFor(String iconKey) {
        SVGPath path = new SVGPath();
        switch (iconKey) {
            case "add":
                path.setContent("M7 1h2v6h6v2H9v6H7V9H1V7h6z");
                break;
            case "edit":
                path.setContent("M2 12.5V14h1.5L12 5.5 10.5 4 2 12.5z M13.5 4l-1.5-1.5 1-1L14.5 2 13.5 4z");
                break;
            case "delete":
                path.setContent("M3 4h10v1H3z M6 2h4l1 1H5z M5 5h1v8H5z M10 5h1v8h-1z M4 4h8l-1 10H5z");
                break;
            default:
                path.setContent("M2 2h12v12H2z");
                break;
        }
        path.setFill(Color.web("#4b5563"));
        path.setScaleX(1.1);
        path.setScaleY(1.1);
        return path;
    }
}
