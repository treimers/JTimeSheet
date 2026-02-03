package treimers.net.whathaveyoudone.ui;

import java.util.Comparator;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
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
import javafx.scene.control.Separator;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import treimers.net.whathaveyoudone.model.Customer;
import treimers.net.whathaveyoudone.model.Project;
import treimers.net.whathaveyoudone.model.Task;

public class ManagementDialog {
    private static final Color TOOLBAR_ICON_COLOR = Color.web("#2563eb");
    private static final Color TOOLBAR_ICON_HOVER_COLOR = Color.web("#1d4ed8");
    private static final String TOOLBAR_BUTTON_HOVER_STYLE = "-fx-background-color: rgba(37, 99, 235, 0.12);";
    private static final String TOOLBAR_BUTTON_NORMAL_STYLE = "";
    private final ObservableList<Customer> customers;
    private final Runnable onSave;
    private final ActivityCallbacks activityCallbacks;
    private final ResourceBundle messages;
    private final Locale locale;
    private TreeView<NodeData> treeView;
    private TreeItem<NodeData> rootItem;
    private Stage dialogStage;
    private TextField customerNameField;
    private TextArea customerAddressArea;
    private TextField propertiesPathField;
    private TextField templatePathField;
    private TextField timesheetNameField;
    private Button propertiesBrowseButton;
    private Button templateBrowseButton;
    private Customer detailsCustomer;
    private boolean updatingDetails;

    public ManagementDialog(
        ObservableList<Customer> customers,
        Runnable onSave,
        ActivityCallbacks activityCallbacks,
        ResourceBundle messages,
        Locale locale
    ) {
        this.customers = customers;
        this.onSave = onSave;
        this.activityCallbacks = activityCallbacks;
        this.messages = messages;
        this.locale = locale;
    }

    public void show(Stage owner) {
        ensureManagementTree();

        MenuBar menuBar = new MenuBar(
            customerMenu(),
            projectMenu(),
            taskMenu()
        );

        ToolBar toolBar = new ToolBar(
            toolbarButton(i18n("management.add.customer"), "add", this::addCustomer),
            toolbarButton(i18n("management.add.project"), "add", this::addProject),
            toolbarButton(i18n("management.add.task"), "add", this::addTask),
            new Separator(),
            toolbarButton(i18n("management.rename.customer"), "edit", this::renameCustomer),
            toolbarButton(i18n("management.rename.project"), "edit", this::renameProject),
            toolbarButton(i18n("management.rename.task"), "edit", this::renameTask),
            new Separator(),
            toolbarButton(i18n("management.delete.customer"), "delete", this::deleteCustomer),
            toolbarButton(i18n("management.delete.project"), "delete", this::deleteProject),
            toolbarButton(i18n("management.delete.task"), "delete", this::deleteTask)
        );

        VBox treePanel = new VBox(10, sectionHeader(i18n("management.section.title")), treeView);
        treePanel.setPadding(new Insets(12));
        VBox.setVgrow(treeView, Priority.ALWAYS);

        VBox detailsPanel = createDetailsPanel();
        HBox content = new HBox(12, treePanel, detailsPanel);
        HBox.setHgrow(treePanel, Priority.ALWAYS);
        HBox.setHgrow(detailsPanel, Priority.ALWAYS);
        detailsPanel.setPrefWidth(420);

        BorderPane root = new BorderPane();
        root.setTop(new VBox(menuBar, toolBar));
        root.setCenter(content);
        BorderPane.setMargin(content, new Insets(10));

        dialogStage = new Stage();
        dialogStage.initOwner(owner);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle(i18n("management.title"));
        dialogStage.setScene(new Scene(root, 980, 620));
        dialogStage.setOnHiding(event -> commitAllDetails());
        dialogStage.showAndWait();
    }

    ContextMenu emptyMenu() {
        MenuItem addCustomer = menuItemWithIcon(i18n("management.add.customer"), "add", this::addCustomer);
        return new ContextMenu(addCustomer);
    }

    ContextMenu contextMenuFor(NodeData data) {
        MenuItem addCustomer = menuItemWithIcon(i18n("management.add.customer"), "add", this::addCustomer);
        MenuItem addProject = menuItemWithIcon(i18n("management.add.project"), "add", this::addProject);
        MenuItem addTask = menuItemWithIcon(i18n("management.add.task"), "add", this::addTask);
        MenuItem renameCustomer = menuItemWithIcon(i18n("management.rename.customer"), "edit", this::renameCustomer);
        MenuItem deleteCustomer = menuItemWithIcon(i18n("management.delete.customer"), "delete", this::deleteCustomer);
        MenuItem renameProject = menuItemWithIcon(i18n("management.rename.project"), "edit", this::renameProject);
        MenuItem deleteProject = menuItemWithIcon(i18n("management.delete.project"), "delete", this::deleteProject);
        MenuItem renameTask = menuItemWithIcon(i18n("management.rename.task"), "edit", this::renameTask);
        MenuItem deleteTask = menuItemWithIcon(i18n("management.delete.task"), "delete", this::deleteTask);

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
        MenuItem add = menuItemWithIcon(i18n("management.add.customer"), "add", this::addCustomer);
        MenuItem rename = menuItemWithIcon(i18n("management.rename.customer"), "edit", this::renameCustomer);
        MenuItem delete = menuItemWithIcon(i18n("management.delete.customer"), "delete", this::deleteCustomer);

        Menu menu = new Menu(i18n("management.menu.customer"));
        menu.getItems().addAll(add, rename, delete);
        return menu;
    }

    private Menu projectMenu() {
        MenuItem add = menuItemWithIcon(i18n("management.add.project"), "add", this::addProject);
        MenuItem rename = menuItemWithIcon(i18n("management.rename.project"), "edit", this::renameProject);
        MenuItem delete = menuItemWithIcon(i18n("management.delete.project"), "delete", this::deleteProject);

        Menu menu = new Menu(i18n("management.menu.project"));
        menu.getItems().addAll(add, rename, delete);
        return menu;
    }

    private Menu taskMenu() {
        MenuItem add = menuItemWithIcon(i18n("management.add.task"), "add", this::addTask);
        MenuItem rename = menuItemWithIcon(i18n("management.rename.task"), "edit", this::renameTask);
        MenuItem delete = menuItemWithIcon(i18n("management.delete.task"), "delete", this::deleteTask);

        Menu menu = new Menu(i18n("management.menu.task"));
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
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            commitAllDetails();
            NodeData data = newValue != null ? newValue.getValue() : null;
            updateDetailsForSelection(data);
        });

        rebuildTree();
    }

    private VBox createDetailsPanel() {
        customerNameField = new TextField();
        customerAddressArea = new TextArea();
        customerAddressArea.setPrefRowCount(3);
        customerAddressArea.setWrapText(true);
        propertiesPathField = new TextField();
        templatePathField = new TextField();
        timesheetNameField = new TextField();
        propertiesBrowseButton = new Button(i18n("management.file.browse"));
        templateBrowseButton = new Button(i18n("management.file.browse"));
        propertiesBrowseButton.setOnAction(event -> chooseCustomerFile(propertiesPathField, "*.properties"));
        templateBrowseButton.setOnAction(event -> chooseCustomerFile(templatePathField, "*.xls", "*.xlsx"));

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(140);
        labelColumn.setPrefWidth(170);
        ColumnConstraints fieldColumn = new ColumnConstraints();
        fieldColumn.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelColumn, fieldColumn);
        grid.add(new Label(i18n("management.customer.field.name")), 0, 0);
        grid.add(customerNameField, 1, 0);
        grid.add(new Label(i18n("management.customer.field.address")), 0, 1);
        grid.add(customerAddressArea, 1, 1);
        grid.add(new Label(i18n("management.customer.field.properties")), 0, 2);
        grid.add(fieldWithBrowse(propertiesPathField, propertiesBrowseButton), 1, 2);
        grid.add(new Label(i18n("management.customer.field.template")), 0, 3);
        grid.add(fieldWithBrowse(templatePathField, templateBrowseButton), 1, 3);
        grid.add(new Label(i18n("management.customer.field.timesheetName")), 0, 4);
        grid.add(timesheetNameField, 1, 4);
        GridPane.setHgrow(customerNameField, Priority.ALWAYS);
        GridPane.setHgrow(customerAddressArea, Priority.ALWAYS);
        GridPane.setHgrow(propertiesPathField, Priority.ALWAYS);
        GridPane.setHgrow(templatePathField, Priority.ALWAYS);
        GridPane.setHgrow(timesheetNameField, Priority.ALWAYS);

        configureDetailsHandlers();
        setDetailsDisabled(true);

        VBox panel = new VBox(10, sectionHeader(i18n("management.details.title")), grid);
        panel.setPadding(new Insets(12));
        return panel;
    }

    private HBox fieldWithBrowse(TextField field, Button browseButton) {
        HBox box = new HBox(6, field, browseButton);
        HBox.setHgrow(field, Priority.ALWAYS);
        return box;
    }

    private void configureDetailsHandlers() {
        customerNameField.setOnAction(event -> commitCustomerName());
        customerNameField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                commitCustomerName();
            }
        });
        customerAddressArea.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                commitCustomerAddress();
            }
        });
        propertiesPathField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                commitCustomerPropertiesPath();
            }
        });
        templatePathField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                commitCustomerTemplatePath();
            }
        });
        timesheetNameField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                commitTimesheetNameSuggestion();
            }
        });
    }

    private void updateDetailsForSelection(NodeData data) {
        Customer customer = null;
        if (data != null) {
            customer = data.customer;
        }
        detailsCustomer = customer;
        updatingDetails = true;
        if (customer == null) {
            customerNameField.setText("");
            customerAddressArea.setText("");
            propertiesPathField.setText("");
            templatePathField.setText("");
            timesheetNameField.setText("");
            setDetailsDisabled(true);
        } else {
            customerNameField.setText(safeText(customer.getName()));
            customerAddressArea.setText(safeText(customer.getAddress()));
            propertiesPathField.setText(safeText(customer.getTimesheetPropertiesPath()));
            templatePathField.setText(safeText(customer.getTimesheetTemplatePath()));
            timesheetNameField.setText(safeText(customer.getTimesheetFilenameSuggestion()));
            setDetailsDisabled(false);
        }
        updatingDetails = false;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private void setDetailsDisabled(boolean disabled) {
        customerNameField.setDisable(disabled);
        customerAddressArea.setDisable(disabled);
        propertiesPathField.setDisable(disabled);
        templatePathField.setDisable(disabled);
        timesheetNameField.setDisable(disabled);
        propertiesBrowseButton.setDisable(disabled);
        templateBrowseButton.setDisable(disabled);
    }

    private void commitCustomerName() {
        if (updatingDetails || detailsCustomer == null) {
            return;
        }
        String value = customerNameField.getText() != null ? customerNameField.getText().trim() : "";
        if (value.isEmpty()) {
            customerNameField.setText(detailsCustomer.getName());
            return;
        }
        if (!value.equals(detailsCustomer.getName())) {
            detailsCustomer.setName(value);
            sortCustomers();
            rebuildTree();
            selectCustomer(detailsCustomer);
            onSave.run();
        }
    }

    private void commitCustomerAddress() {
        if (updatingDetails || detailsCustomer == null) {
            return;
        }
        String value = customerAddressArea.getText();
        if (value == null) {
            value = "";
        }
        if (!value.equals(detailsCustomer.getAddress())) {
            detailsCustomer.setAddress(value);
            onSave.run();
        }
    }

    private void commitCustomerPropertiesPath() {
        if (updatingDetails || detailsCustomer == null) {
            return;
        }
        String value = normalizePath(propertiesPathField.getText());
        if (!value.equals(safeText(detailsCustomer.getTimesheetPropertiesPath()))) {
            detailsCustomer.setTimesheetPropertiesPath(value);
            onSave.run();
        }
    }

    private void commitCustomerTemplatePath() {
        if (updatingDetails || detailsCustomer == null) {
            return;
        }
        String value = normalizePath(templatePathField.getText());
        if (!value.equals(safeText(detailsCustomer.getTimesheetTemplatePath()))) {
            detailsCustomer.setTimesheetTemplatePath(value);
            onSave.run();
        }
    }

    private void commitTimesheetNameSuggestion() {
        if (updatingDetails || detailsCustomer == null) {
            return;
        }
        String value = normalizePath(timesheetNameField.getText());
        if (!value.equals(safeText(detailsCustomer.getTimesheetFilenameSuggestion()))) {
            detailsCustomer.setTimesheetFilenameSuggestion(value);
            onSave.run();
        }
    }

    private void commitAllDetails() {
        commitCustomerName();
        commitCustomerAddress();
        commitCustomerPropertiesPath();
        commitCustomerTemplatePath();
        commitTimesheetNameSuggestion();
    }

    private String normalizePath(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private void chooseCustomerFile(TextField targetField, String... extensions) {
        if (dialogStage == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle(i18n("management.file.title"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
            i18n("management.file.filter"),
            extensions
        ));
        applyInitialFile(chooser, targetField.getText());
        java.io.File file = chooser.showOpenDialog(dialogStage);
        if (file != null) {
            targetField.setText(file.getAbsolutePath());
            if (targetField == propertiesPathField) {
                commitCustomerPropertiesPath();
            } else if (targetField == templatePathField) {
                commitCustomerTemplatePath();
            }
        }
    }

    private void applyInitialFile(FileChooser chooser, String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        java.io.File file = new java.io.File(path);
        java.io.File directory = file.isDirectory() ? file : file.getParentFile();
        if (directory != null && directory.exists()) {
            chooser.setInitialDirectory(directory);
        }
        if (file.isFile()) {
            chooser.setInitialFileName(file.getName());
        }
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
        Optional<String> name = promptForText(i18n("management.customer.new.title"), i18n("management.customer.new.label"));
        name.ifPresent(value -> {
            Customer customer = new Customer(value);
            customers.add(customer);
            sortCustomers();
            rebuildTree();
            selectCustomer(customer);
            onSave.run();
        });
    }

    private void renameCustomer() {
        Customer customer = getSelectedCustomer();
        if (customer == null) {
            showInfo(i18n("management.customer.select.rename"));
            return;
        }
        Optional<String> name = promptForText(
            i18n("management.customer.rename.title"),
            i18n("management.customer.rename.label"),
            customer.getName()
        );
        name.ifPresent(value -> {
            customer.setName(value);
            sortCustomers();
            rebuildTree();
            selectCustomer(customer);
            onSave.run();
        });
    }

    private void deleteCustomer() {
        Customer customer = getSelectedCustomer();
        if (customer == null) {
            showInfo(i18n("management.customer.select.delete"));
            return;
        }
        int activityCount = activityCallbacks.countActivitiesForCustomer(customer.getId());
        String message = i18n("management.customer.delete.message", customer.getName());
        if (activityCount > 0) {
            message += " " + i18n("management.delete.activities", activityCount);
        }
        if (confirmDelete(i18n("management.customer.delete.title"), message)) {
            customers.remove(customer);
            activityCallbacks.removeActivitiesForCustomer(customer.getId());
            rebuildTree();
            onSave.run();
        }
    }

    private void addProject() {
        Customer customer = getSelectedCustomer();
        if (customer == null) {
            showInfo(i18n("management.customer.select.first"));
            return;
        }
        Optional<String> name = promptForText(i18n("management.project.new.title"), i18n("management.project.new.label"));
        name.ifPresent(value -> {
            Project project = new Project(value);
            customer.getProjects().add(project);
            sortProjects(customer);
            rebuildTree();
            selectProject(customer, project);
            onSave.run();
        });
    }

    private void renameProject() {
        SelectedProject selected = getSelectedProject();
        if (selected == null) {
            showInfo(i18n("management.project.select.rename"));
            return;
        }
        Optional<String> name = promptForText(
            i18n("management.project.rename.title"),
            i18n("management.project.rename.label"),
            selected.project.getName()
        );
        name.ifPresent(value -> {
            selected.project.setName(value);
            sortProjects(selected.customer);
            rebuildTree();
            selectProject(selected.customer, selected.project);
            onSave.run();
        });
    }

    private void deleteProject() {
        SelectedProject selected = getSelectedProject();
        if (selected == null) {
            showInfo(i18n("management.project.select.delete"));
            return;
        }
        int activityCount = activityCallbacks.countActivitiesForProject(
            selected.customer.getId(),
            selected.project.getId()
        );
        String message = i18n("management.project.delete.message", selected.project.getName());
        if (activityCount > 0) {
            message += " " + i18n("management.delete.activities", activityCount);
        }
        if (confirmDelete(i18n("management.project.delete.title"), message)) {
            selected.customer.getProjects().remove(selected.project);
            activityCallbacks.removeActivitiesForProject(selected.customer.getId(), selected.project.getId());
            rebuildTree();
            selectCustomer(selected.customer);
            onSave.run();
        }
    }

    private void addTask() {
        SelectedProject selected = getSelectedProject();
        if (selected == null) {
            showInfo(i18n("management.project.select.first"));
            return;
        }
        Optional<String> name = promptForText(i18n("management.task.new.title"), i18n("management.task.new.label"));
        name.ifPresent(value -> {
            selected.project.getTasks().add(new Task(value));
            sortTasks(selected.project);
            rebuildTree();
            selectTask(selected.customer, selected.project, value);
            onSave.run();
        });
    }

    private void renameTask() {
        SelectedTask selected = getSelectedTask();
        if (selected == null) {
            showInfo(i18n("management.task.select.rename"));
            return;
        }
        Optional<String> name = promptForText(
            i18n("management.task.rename.title"),
            i18n("management.task.rename.label"),
            selected.task.getName()
        );
        name.ifPresent(value -> {
            int index = selected.project.getTasks().indexOf(selected.task);
            if (index >= 0) {
                selected.project.getTasks().set(index, new Task(value));
                sortTasks(selected.project);
                rebuildTree();
                selectTask(selected.customer, selected.project, value);
                onSave.run();
            }
        });
    }

    private void deleteTask() {
        SelectedTask selected = getSelectedTask();
        if (selected == null) {
            showInfo(i18n("management.task.select.delete"));
            return;
        }
        int activityCount = activityCallbacks.countActivitiesForTask(
            selected.customer.getId(),
            selected.project.getId(),
            selected.task.getId()
        );
        String message = i18n("management.task.delete.message", selected.task.getName());
        if (activityCount > 0) {
            message += " " + i18n("management.delete.activities", activityCount);
        }
        if (confirmDelete(i18n("management.task.delete.title"), message)) {
            selected.project.getTasks().remove(selected.task);
            activityCallbacks.removeActivitiesForTask(
                selected.customer.getId(),
                selected.project.getId(),
                selected.task.getId()
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
        dialog.setContentText(i18n("label.name"));
        return dialog.showAndWait().map(String::trim).filter(value -> !value.isEmpty());
    }

    private boolean confirmDelete(String title, String content) {
        ButtonType delete = new ButtonType(i18n("button.delete"), ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType(i18n("button.cancel"), ButtonData.CANCEL_CLOSE);
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

    private void sortCustomers() {
        FXCollections.sort(customers, Comparator.comparing(customer -> sortKey(customer.getName())));
        for (Customer customer : customers) {
            sortProjects(customer);
        }
    }

    private void sortProjects(Customer customer) {
        FXCollections.sort(customer.getProjects(), Comparator.comparing(project -> sortKey(project.getName())));
        for (Project project : customer.getProjects()) {
            sortTasks(project);
        }
    }

    private void sortTasks(Project project) {
        FXCollections.sort(project.getTasks(), Comparator.comparing(task -> sortKey(task.getName())));
    }

    private String sortKey(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private String i18n(String key, Object... args) {
        String value;
        try {
            value = messages.getString(key);
        } catch (MissingResourceException exception) {
            value = key;
        }
        if (args == null || args.length == 0) {
            return value;
        }
        return String.format(locale, value, args);
    }

    private MenuItem menuItemWithIcon(String text, String iconKey, Runnable action) {
        MenuItem item = new MenuItem(text);
        item.setGraphic(iconFor(iconKey));
        item.setOnAction(event -> action.run());
        return item;
    }

    private Button toolbarButton(String text, String iconKey, Runnable action) {
        SVGPath icon = iconPath(iconKey, TOOLBAR_ICON_COLOR);
        Button button = new Button(text, icon);
        button.setOnAction(event -> action.run());
        applyToolbarHover(button, icon);
        return button;
    }

    private Node iconFor(String iconKey) {
        return iconPath(iconKey, Color.web("#4b5563"));
    }

    private SVGPath iconPath(String iconKey, Color color) {
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
        path.setFill(color);
        path.setScaleX(1.1);
        path.setScaleY(1.1);
        return path;
    }

    private void applyToolbarHover(Button button, SVGPath icon) {
        button.hoverProperty().addListener((obs, wasHovered, isHovered) -> {
            if (button.isDisabled()) {
                return;
            }
            icon.setFill(isHovered ? TOOLBAR_ICON_HOVER_COLOR : TOOLBAR_ICON_COLOR);
            button.setStyle(isHovered ? TOOLBAR_BUTTON_HOVER_STYLE : TOOLBAR_BUTTON_NORMAL_STYLE);
        });
        button.disabledProperty().addListener((obs, wasDisabled, isDisabled) -> {
            if (!isDisabled) {
                icon.setFill(button.isHover() ? TOOLBAR_ICON_HOVER_COLOR : TOOLBAR_ICON_COLOR);
                button.setStyle(button.isHover() ? TOOLBAR_BUTTON_HOVER_STYLE : TOOLBAR_BUTTON_NORMAL_STYLE);
            }
        });
    }
}
