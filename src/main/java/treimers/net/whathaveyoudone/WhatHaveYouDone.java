package treimers.net.whathaveyoudone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.prefs.Preferences;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.ListCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import treimers.net.whathaveyoudone.model.Activity;
import treimers.net.whathaveyoudone.model.Customer;
import treimers.net.whathaveyoudone.model.Project;
import treimers.net.whathaveyoudone.model.Task;
import treimers.net.whathaveyoudone.storage.ActivityData;
import treimers.net.whathaveyoudone.storage.CustomerData;
import treimers.net.whathaveyoudone.storage.ProjectData;
import treimers.net.whathaveyoudone.storage.StorageData;
import treimers.net.whathaveyoudone.ui.ActivityCallbacks;
import treimers.net.whathaveyoudone.ui.ActivityInput;
import treimers.net.whathaveyoudone.ui.ManagementDialog;

public class WhatHaveYouDone extends Application {
    private static final Path DATA_PATH = Paths.get("whathaveyoudone.json");
    private static final DateTimeFormatter CSV_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");
    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final ObservableList<Customer> customers = FXCollections.observableArrayList();
    private final ObservableList<Activity> activities = FXCollections.observableArrayList();
    private TableView<Activity> activityTable;
    private Stage primaryStage;
    private FilteredList<Activity> filteredActivities;
    private Activity lastActivity;
    private Timeline hourlyTimeline;
    private PauseTransition hourlyDelay;
    private final Preferences preferences = Preferences.userRoot().node("net/treimers/whathaveyoudone");
    private boolean restoringPreferences;
    private final Map<LocalDate, Long> dailyTotalsByDate = new HashMap<>();
    private final Map<LocalDate, Activity> dailyTotalLastActivity = new HashMap<>();
    private Label totalHoursLabel;
    private ComboBox<Customer> customerFilter;
    private ComboBox<Project> projectFilter;
    private ListView<Task> taskFilter;
    private DatePicker fromFilter;
    private DatePicker toFilter;
    private ComboBox<String> presetFilter;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        MenuBar menuBar = new MenuBar(
            fileMenu(),
            manageMenu(),
            activityMenu()
        );

        activityTable = createActivityTable();
        activities.addListener((ListChangeListener<Activity>) change -> applyFilters());
        totalHoursLabel = new Label("Total: 00:00");
        HBox activityHeader = new HBox(10, sectionHeader("Activities"), new Region(), totalHoursLabel);
        HBox.setHgrow(activityHeader.getChildren().get(1), Priority.ALWAYS);
        VBox activityPanel = new VBox(
            10,
            activityHeader,
            createFilterPanel(),
            activityTable
        );
        activityPanel.setPadding(new Insets(12));
        VBox.setVgrow(activityTable, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setTop(menuBar);
        root.setCenter(activityPanel);
        BorderPane.setMargin(activityPanel, new Insets(10));

        Scene scene = new Scene(root, 900, 520);
        stage.setTitle("Project Time Manager");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();

        loadData();
        setLastActivityFromData();
        startHourlyReminder();
    }

    private Menu manageMenu() {
        MenuItem openManager = menuItemWithIcon(
            "Manage Customers, Projects, and Tasks...",
            "manage",
            this::openManagementDialog
        );
        Menu menu = new Menu("Manage");
        menu.getItems().add(openManager);
        return menu;
    }

    private Menu fileMenu() {
        MenuItem exportCsv = menuItemWithIcon("Export CSV...", "export", this::exportCsv);
        MenuItem importCsv = menuItemWithIcon("Import CSV...", "import", this::importCsv);

        Menu menu = new Menu("File");
        menu.getItems().addAll(importCsv, exportCsv);
        return menu;
    }

    private Menu activityMenu() {
        MenuItem add = menuItemWithIcon("Add Activity", "add", this::addActivity);
        MenuItem edit = menuItemWithIcon("Edit Activity", "edit", this::editActivity);
        MenuItem delete = menuItemWithIcon("Delete Activity", "delete", this::deleteActivity);

        Menu menu = new Menu("Activity");
        menu.getItems().addAll(add, edit, delete);
        return menu;
    }

    private Label sectionHeader(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        return label;
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
            case "manage":
                path.setContent("M6 1h4l1 2h3v2h-2l-1 2 1 2h2v2h-3l-1 2H6l-1-2H2v-2h2l1-2-1-2H2V3h3z M8 6a2 2 0 110 4 2 2 0 010-4z");
                break;
            case "import":
                path.setContent("M8 2v7l2-2 1 1-4 4-4-4 1-1 2 2V2z M2 13h12v2H2z");
                break;
            case "export":
                path.setContent("M8 14V7l-2 2-1-1 4-4 4 4-1 1-2-2v7z M2 13h12v2H2z");
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

    private TableView<Activity> createActivityTable() {
        filteredActivities = new FilteredList<>(activities, activity -> true);
        TableView<Activity> table = new TableView<>(filteredActivities);

        TableColumn<Activity, String> customerColumn = new TableColumn<>("Customer");
        customerColumn.setCellValueFactory(cell -> cell.getValue().customerNameProperty());

        TableColumn<Activity, String> projectColumn = new TableColumn<>("Project");
        projectColumn.setCellValueFactory(cell -> cell.getValue().projectNameProperty());

        TableColumn<Activity, String> taskColumn = new TableColumn<>("Task");
        taskColumn.setCellValueFactory(cell -> cell.getValue().taskNameProperty());

        TableColumn<Activity, String> fromColumn = new TableColumn<>("From");
        fromColumn.setCellValueFactory(cell -> cell.getValue().fromProperty());

        TableColumn<Activity, String> toColumn = new TableColumn<>("To");
        toColumn.setCellValueFactory(cell -> cell.getValue().toProperty());

        TableColumn<Activity, String> durationColumn = new TableColumn<>("Duration");
        durationColumn.setCellValueFactory(cell -> cell.getValue().durationProperty());

        TableColumn<Activity, String> dailyTotalColumn = new TableColumn<>("Daily Total");
        dailyTotalColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null) {
                    setText(null);
                    return;
                }
                Activity activity = (Activity) getTableRow().getItem();
                if (activity == null) {
                    setText(null);
                    return;
                }
                setText(getDailyTotalText(activity));
            }
        });

        table.getColumns().add(customerColumn);
        table.getColumns().add(projectColumn);
        table.getColumns().add(taskColumn);
        table.getColumns().add(fromColumn);
        table.getColumns().add(toColumn);
        table.getColumns().add(durationColumn);
        table.getColumns().add(dailyTotalColumn);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        MenuItem add = menuItemWithIcon("Add Activity", "add", this::addActivity);
        MenuItem edit = menuItemWithIcon("Edit Activity", "edit", this::editActivity);
        edit.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        MenuItem delete = menuItemWithIcon("Delete Activity", "delete", this::deleteActivity);
        delete.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        ContextMenu menu = new ContextMenu(add, edit, delete);
        table.setContextMenu(menu);

        table.setRowFactory(tv -> {
            TableRow<Activity> row = new TableRow<>();
            row.setContextMenu(menu);
            row.setOnMousePressed(event -> {
                if (event.getButton() == MouseButton.SECONDARY && !row.isEmpty()) {
                    table.getSelectionModel().select(row.getIndex());
                }
            });
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && !row.isEmpty()) {
                    editActivity();
                }
            });
            return row;
        });
        return table;
    }

    private VBox createFilterPanel() {
        customerFilter = new ComboBox<>(customers);
        customerFilter.setPromptText("All Customers");
        customerFilter.setPrefWidth(180);
        customerFilter.setButtonCell(createCustomerFilterCell("All Customers"));
        customerFilter.setCellFactory(listView -> createCustomerFilterCell("All Customers"));

        projectFilter = new ComboBox<>();
        projectFilter.setPromptText("All Projects");
        projectFilter.setPrefWidth(180);
        projectFilter.setButtonCell(createProjectFilterCell("All Projects"));
        projectFilter.setCellFactory(listView -> createProjectFilterCell("All Projects"));

        taskFilter = new ListView<>();
        taskFilter.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        taskFilter.setFixedCellSize(24);
        taskFilter.setPrefHeight(taskFilter.getFixedCellSize() * 6 + 6);
        taskFilter.setMinHeight(taskFilter.getPrefHeight());
        taskFilter.setPrefWidth(180);

        fromFilter = new DatePicker();
        toFilter = new DatePicker();

        presetFilter = new ComboBox<>();
        presetFilter.getItems().addAll(
            "Custom",
            "Today",
            "Yesterday",
            "This Week",
            "Last Week",
            "This Month",
            "Last Month"
        );
        presetFilter.getSelectionModel().select("Custom");
        presetFilter.setPrefWidth(140);

        Button clearCustomer = new Button("All");
        clearCustomer.setOnAction(event -> {
            customerFilter.getSelectionModel().clearSelection();
            customerFilter.setValue(null);
            updateProjectFilter(null);
            updateTaskFilter(null, null);
            applyFilters();
            saveFilterPreferences();
        });

        Button clearProject = new Button("All");
        clearProject.setOnAction(event -> {
            projectFilter.getSelectionModel().clearSelection();
            projectFilter.setValue(null);
            updateTaskFilter(customerFilter.getValue(), null);
            reapplyProjectAutoSelectIfSingle();
            applyFilters();
            saveFilterPreferences();
        });

        Button clearTasks = new Button("Clear");
        clearTasks.setOnAction(event -> {
            taskFilter.getSelectionModel().clearSelection();
            applyFilters();
            saveFilterPreferences();
        });

        Button clearDates = new Button("Clear Dates");
        clearDates.setOnAction(event -> {
            fromFilter.setValue(null);
            toFilter.setValue(null);
            presetFilter.getSelectionModel().select("Custom");
            applyFilters();
            saveFilterPreferences();
        });

        customerFilter.valueProperty().addListener((obs, oldValue, newValue) -> {
            updateProjectFilter(newValue);
            updateTaskFilter(newValue, projectFilter.getValue());
            applyFilters();
            saveFilterPreferences();
        });

        projectFilter.valueProperty().addListener((obs, oldValue, newValue) -> {
            updateTaskFilter(customerFilter.getValue(), newValue);
            applyFilters();
            saveFilterPreferences();
        });

        taskFilter.getSelectionModel().getSelectedItems().addListener((ListChangeListener<Task>) change -> {
            applyFilters();
            saveFilterPreferences();
        });

        fromFilter.valueProperty().addListener((obs, oldValue, newValue) -> {
            presetFilter.getSelectionModel().select("Custom");
            applyFilters();
            saveFilterPreferences();
        });
        toFilter.valueProperty().addListener((obs, oldValue, newValue) -> {
            presetFilter.getSelectionModel().select("Custom");
            applyFilters();
            saveFilterPreferences();
        });

        presetFilter.valueProperty().addListener((obs, oldValue, newValue) -> {
            applyPreset(newValue);
            applyFilters();
            saveFilterPreferences();
        });

        updateProjectFilter(null);
        updateTaskFilter(null, null);

        HBox row1 = new HBox(
            10,
            new Label("Customer:"),
            customerFilter,
            clearCustomer,
            new Label("Project:"),
            projectFilter,
            clearProject
        );
        HBox row2 = new HBox(10, new Label("Tasks:"), taskFilter, clearTasks);
        HBox row3 = new HBox(
            10,
            new Label("From:"),
            fromFilter,
            new Label("To:"),
            toFilter,
            new Label("Preset:"),
            presetFilter,
            clearDates
        );

        VBox panel = new VBox(8, row1, row2, row3);
        panel.setPadding(new Insets(6, 0, 6, 0));
        return panel;
    }

    private ListCell<Customer> createCustomerFilterCell(String placeholder) {
        return new ListCell<>() {
            @Override
            protected void updateItem(Customer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(placeholder);
                } else {
                    setText(item.getName());
                }
            }
        };
    }

    private ListCell<Project> createProjectFilterCell(String placeholder) {
        return new ListCell<>() {
            @Override
            protected void updateItem(Project item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(placeholder);
                } else {
                    setText(item.getName());
                }
            }
        };
    }

    private void updateProjectFilter(Customer customer) {
        ObservableList<Project> projects = FXCollections.observableArrayList();
        if (customer != null) {
            projects.addAll(customer.getProjects());
        } else {
            for (Customer existing : customers) {
                projects.addAll(existing.getProjects());
            }
        }
        projectFilter.setItems(projects);
        projectFilter.getSelectionModel().clearSelection();
        if (customer != null && projects.size() == 1) {
            projectFilter.getSelectionModel().select(0);
        }
    }

    private void updateTaskFilter(Customer customer, Project project) {
        ObservableList<Task> tasks = FXCollections.observableArrayList();
        if (project != null) {
            tasks.addAll(project.getTasks());
        } else if (customer != null) {
            for (Project existing : customer.getProjects()) {
                tasks.addAll(existing.getTasks());
            }
        } else {
            for (Customer existingCustomer : customers) {
                for (Project existingProject : existingCustomer.getProjects()) {
                    tasks.addAll(existingProject.getTasks());
                }
            }
        }
        taskFilter.setItems(tasks);
        taskFilter.getSelectionModel().clearSelection();
    }

    private void refreshFilterChoices() {
        Customer selectedCustomer = customerFilter.getValue();
        if (selectedCustomer != null && !customers.contains(selectedCustomer)) {
            selectedCustomer = null;
        }
        customerFilter.setValue(selectedCustomer);

        Project selectedProject = projectFilter.getValue();
        updateProjectFilter(selectedCustomer);
        if (selectedProject != null && projectFilter.getItems().contains(selectedProject)) {
            projectFilter.setValue(selectedProject);
        } else if (selectedCustomer != null && projectFilter.getItems().size() == 1) {
            projectFilter.getSelectionModel().select(0);
        }

        List<String> selectedTaskNames = new ArrayList<>();
        for (Task task : taskFilter.getSelectionModel().getSelectedItems()) {
            selectedTaskNames.add(task.getName());
        }
        updateTaskFilter(selectedCustomer, projectFilter.getValue());
        for (Task task : taskFilter.getItems()) {
            if (selectedTaskNames.contains(task.getName())) {
                taskFilter.getSelectionModel().select(task);
            }
        }
        applyFilters();
    }

    private void saveFilterPreferences() {
        if (restoringPreferences) {
            return;
        }
        preferences.put("filter.customer", customerFilter.getValue() != null ? customerFilter.getValue().getName() : "");
        preferences.put("filter.project", projectFilter.getValue() != null ? projectFilter.getValue().getName() : "");
        preferences.put("filter.tasks", String.join(",", getSelectedTaskNames()));
        preferences.put("filter.preset", presetFilter.getValue() != null ? presetFilter.getValue() : "Custom");
        preferences.put("filter.from", fromFilter.getValue() != null ? fromFilter.getValue().toString() : "");
        preferences.put("filter.to", toFilter.getValue() != null ? toFilter.getValue().toString() : "");
    }

    private void restoreFilterPreferences() {
        restoringPreferences = true;
        String customerName = preferences.get("filter.customer", "");
        String projectName = preferences.get("filter.project", "");
        String taskNames = preferences.get("filter.tasks", "");
        String preset = preferences.get("filter.preset", "Custom");
        String fromValue = preferences.get("filter.from", "");
        String toValue = preferences.get("filter.to", "");

        Customer customer = findCustomerByName(customerName);
        if (customer != null) {
            customerFilter.getSelectionModel().select(customer);
        } else {
            customerFilter.getSelectionModel().clearSelection();
            customerFilter.setValue(null);
        }

        Project project = customer != null ? findProjectByName(customer, projectName) : null;
        updateProjectFilter(customer);
        if (project != null && projectFilter.getItems().contains(project)) {
            projectFilter.getSelectionModel().select(project);
        } else if (customer != null && projectFilter.getItems().size() == 1) {
            projectFilter.getSelectionModel().select(0);
        }

        updateTaskFilter(customerFilter.getValue(), projectFilter.getValue());
        taskFilter.getSelectionModel().clearSelection();
        if (!taskNames.isBlank()) {
            for (String name : taskNames.split(",")) {
                Task task = findTaskByName(projectFilter.getValue(), name.trim());
                if (task != null) {
                    taskFilter.getSelectionModel().select(task);
                }
            }
        }

        presetFilter.getSelectionModel().select(preset);
        if (fromValue != null && !fromValue.isBlank()) {
            fromFilter.setValue(LocalDate.parse(fromValue));
        } else {
            fromFilter.setValue(null);
        }
        if (toValue != null && !toValue.isBlank()) {
            toFilter.setValue(LocalDate.parse(toValue));
        } else {
            toFilter.setValue(null);
        }

        applyFilters();
        restoringPreferences = false;
    }

    private List<String> getSelectedTaskNames() {
        List<String> names = new ArrayList<>();
        for (Task task : taskFilter.getSelectionModel().getSelectedItems()) {
            names.add(task.getName());
        }
        return names;
    }

    private void reapplyProjectAutoSelectIfSingle() {
        Customer currentCustomer = customerFilter.getValue();
        if (currentCustomer != null && projectFilter.getItems().size() == 1) {
            projectFilter.getSelectionModel().select(0);
        }
    }

    private void applyPreset(String preset) {
        if (preset == null || "Custom".equals(preset)) {
            return;
        }
        LocalDate today = LocalDate.now();
        switch (preset) {
            case "Today":
                fromFilter.setValue(today);
                toFilter.setValue(today);
                break;
            case "Yesterday":
                LocalDate yesterday = today.minusDays(1);
                fromFilter.setValue(yesterday);
                toFilter.setValue(yesterday);
                break;
            case "This Week": {
                LocalDate start = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                LocalDate end = start.plusDays(6);
                fromFilter.setValue(start);
                toFilter.setValue(end);
                break;
            }
            case "Last Week": {
                LocalDate start = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).minusWeeks(1);
                LocalDate end = start.plusDays(6);
                fromFilter.setValue(start);
                toFilter.setValue(end);
                break;
            }
            case "This Month": {
                LocalDate start = today.with(TemporalAdjusters.firstDayOfMonth());
                LocalDate end = today.with(TemporalAdjusters.lastDayOfMonth());
                fromFilter.setValue(start);
                toFilter.setValue(end);
                break;
            }
            case "Last Month": {
                LocalDate start = today.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth());
                LocalDate end = today.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
                fromFilter.setValue(start);
                toFilter.setValue(end);
                break;
            }
            default:
                break;
        }
    }

    private void applyFilters() {
        if (filteredActivities == null) {
            return;
        }
        Customer customer = customerFilter.getValue();
        Project project = projectFilter.getValue();
        List<Task> selectedTasks = new ArrayList<>(taskFilter.getSelectionModel().getSelectedItems());
        LocalDate from = fromFilter.getValue();
        LocalDate to = toFilter.getValue();

        filteredActivities.setPredicate(activity -> {
            if (customer != null && !customer.getName().equals(activity.getCustomerName())) {
                return false;
            }
            if (project != null && !project.getName().equals(activity.getProjectName())) {
                return false;
            }
            if (!selectedTasks.isEmpty()) {
                boolean matchesTask = false;
                for (Task task : selectedTasks) {
                    if (task.getName().equals(activity.getTaskName())) {
                        matchesTask = true;
                        break;
                    }
                }
                if (!matchesTask) {
                    return false;
                }
            }
            if (from != null || to != null) {
                LocalDateTime activityFrom = Activity.parseStoredDateTime(activity.getFrom());
                if (activityFrom == null) {
                    return false;
                }
                LocalDate activityDate = activityFrom.toLocalDate();
                if (from != null && activityDate.isBefore(from)) {
                    return false;
                }
                if (to != null && activityDate.isAfter(to)) {
                    return false;
                }
            }
            return true;
        });
        recomputeDailyTotals();
        if (activityTable != null) {
            activityTable.refresh();
        }
    }

    private void setLastActivityFromData() {
        Activity latest = null;
        LocalDateTime latestTo = null;
        for (Activity activity : activities) {
            LocalDateTime to = Activity.parseStoredDateTime(activity.getTo());
            if (to == null) {
                continue;
            }
            if (latestTo == null || to.isAfter(latestTo)) {
                latestTo = to;
                latest = activity;
            }
        }
        lastActivity = latest;
    }

    private void startHourlyReminder() {
        if (hourlyTimeline != null) {
            hourlyTimeline.stop();
        }
        if (hourlyDelay != null) {
            hourlyDelay.stop();
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextHour = now.truncatedTo(ChronoUnit.HOURS).plusHours(1);
        Duration delay = Duration.between(now, nextHour);

        hourlyDelay = new PauseTransition(javafx.util.Duration.millis(delay.toMillis()));
        hourlyDelay.setOnFinished(event -> {
            showHourlyPrompt();
            hourlyTimeline = new Timeline(new KeyFrame(javafx.util.Duration.hours(1), e -> showHourlyPrompt()));
            hourlyTimeline.setCycleCount(Timeline.INDEFINITE);
            hourlyTimeline.play();
        });
        hourlyDelay.play();
    }

    private void showHourlyPrompt() {
        ButtonType continueButton = new ButtonType("Continue previous");
        ButtonType newButton = new ButtonType("New activity");
        ButtonType nothingButton = new ButtonType("Nothing");
        ButtonType dismissButton = new ButtonType("Dismiss", ButtonData.CANCEL_CLOSE);

        Alert alert;
        if (lastActivity != null) {
            String info = String.format(
                "Continue previous activity?\n%s / %s / %s",
                lastActivity.getCustomerName(),
                lastActivity.getProjectName(),
                lastActivity.getTaskName()
            );
            alert = new Alert(AlertType.CONFIRMATION, info, continueButton, newButton, nothingButton, dismissButton);
        } else {
            alert = new Alert(AlertType.CONFIRMATION, "Create a new activity?", newButton, nothingButton, dismissButton);
        }
        alert.setTitle("What have you done?");
        alert.setHeaderText(null);

        ButtonType result = alert.showAndWait().orElse(dismissButton);
        if (result == continueButton && lastActivity != null) {
            promptContinuePrevious();
        } else if (result == newButton) {
            promptNewActivity();
        } else if (result == nothingButton) {
            // intentionally no activity created
        }
    }

    private void promptContinuePrevious() {
        LocalDateTime[] range = suggestedHourRange();
        LocalDateTime from = range[0];
        LocalDateTime to = range[1];

        Customer customer = findCustomerByName(lastActivity.getCustomerName());
        Project project = customer != null ? findProjectByName(customer, lastActivity.getProjectName()) : null;
        Task task = project != null ? findTaskByName(project, lastActivity.getTaskName()) : null;

        Optional<ActivityInput> input = showActivityDialog(
            "What have you done?",
            null,
            from,
            to,
            customer,
            project,
            task
        );
        input.ifPresent(value -> {
            Activity activity = new Activity(
                value.getCustomer().getName(),
                value.getProject().getName(),
                value.getTask().getName(),
                formatDateTime(value.getFrom()),
                formatDateTime(value.getTo())
            );
            activities.add(activity);
            lastActivity = activity;
            activityTable.getSelectionModel().select(activity);
            saveData();
            applyFilters();
        });
    }

    private void promptNewActivity() {
        LocalDateTime[] range = suggestedHourRange();
        LocalDateTime from = range[0];
        LocalDateTime to = range[1];
        Optional<ActivityInput> input = showActivityDialog("What have you done?", null, from, to, null, null, null);
        input.ifPresent(value -> {
            Activity activity = new Activity(
                value.getCustomer().getName(),
                value.getProject().getName(),
                value.getTask().getName(),
                formatDateTime(value.getFrom()),
                formatDateTime(value.getTo())
            );
            activities.add(activity);
            lastActivity = activity;
            activityTable.getSelectionModel().select(activity);
            saveData();
            applyFilters();
        });
    }

    private void importCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        java.io.File file = chooser.showOpenDialog(primaryStage);
        if (file == null) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            if (lines.isEmpty()) {
                showInfo("The file is empty.");
                return;
            }
            List<Activity> imported = parseCsvLines(lines);
            if (imported.isEmpty()) {
                showInfo("No activities found in CSV.");
                return;
            }
            activities.addAll(imported);
            setLastActivityFromData();
            saveData();
            applyFilters();
        } catch (IOException exception) {
            showInfo("Could not read CSV: " + exception.getMessage());
        }
    }

    private void exportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        java.io.File file = chooser.showSaveDialog(primaryStage);
        if (file == null) {
            return;
        }
        List<String> lines = new ArrayList<>();
        lines.add("Kundenname;Projekt;Tätigkeit;Startzeit;Endzeit");
        List<Activity> exportActivities = filteredActivities != null ? filteredActivities : activities;
        for (Activity activity : exportActivities) {
            String start = formatCsvDate(activity.getFrom());
            String end = formatCsvDate(activity.getTo());
            lines.add(String.join(
                ";",
                safeCsv(activity.getCustomerName()),
                safeCsv(activity.getProjectName()),
                safeCsv(activity.getTaskName()),
                start,
                end
            ));
        }
        try {
            Files.write(file.toPath(), lines);
        } catch (IOException exception) {
            showInfo("Could not write CSV: " + exception.getMessage());
        }
    }

    private List<Activity> parseCsvLines(List<String> lines) {
        List<Activity> result = new ArrayList<>();
        if (lines.isEmpty()) {
            return result;
        }
        String header = lines.get(0).trim();
        int startIndex = 0;
        if (!header.isEmpty() && header.toLowerCase().contains("kundenname")) {
            startIndex = 1;
        }
        for (int i = startIndex; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            String delimiter = line.contains(";") ? ";" : ",";
            String[] parts = line.split(delimiter, -1);
            if (parts.length < 5) {
                continue;
            }
            String customer = parts[0].trim();
            String project = parts[1].trim();
            String task = parts[2].trim();
            String start = parts[3].trim();
            String end = parts[4].trim();

            LocalDateTime startTime = parseCsvDate(start);
            LocalDateTime endTime = parseCsvDate(end);
            if (startTime == null || endTime == null) {
                continue;
            }
            result.add(new Activity(
                customer,
                project,
                task,
                Activity.formatDateTime(startTime),
                Activity.formatDateTime(endTime)
            ));
        }
        return result;
    }

    private LocalDateTime parseCsvDate(String value) {
        try {
            return LocalDateTime.parse(value, CSV_DATE_FORMAT);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private String formatCsvDate(String value) {
        LocalDateTime parsed = Activity.parseStoredDateTime(value);
        if (parsed == null) {
            return value;
        }
        return CSV_DATE_FORMAT.format(parsed);
    }

    private String safeCsv(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(";", ",");
    }

    private LocalDateTime[] suggestedHourRange() {
        LocalDateTime end = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        LocalDateTime start = null;
        if (lastActivity != null) {
            start = Activity.parseStoredDateTime(lastActivity.getTo());
        }
        if (start == null) {
            start = end.minusHours(1);
        }
        return new LocalDateTime[] { start, end };
    }

    private void recomputeDailyTotals() {
        dailyTotalsByDate.clear();
        dailyTotalLastActivity.clear();
        Map<LocalDate, LocalDateTime> latestToByDate = new HashMap<>();
        Map<LocalDate, LocalDateTime> latestFromByDate = new HashMap<>();
        Map<LocalDate, Integer> latestIndexByDate = new HashMap<>();
        long totalMinutes = 0;
        int index = 0;
        for (Activity activity : filteredActivities) {
            LocalDateTime from = Activity.parseStoredDateTime(activity.getFrom());
            LocalDateTime to = Activity.parseStoredDateTime(activity.getTo());
            if (from == null || to == null || to.isBefore(from)) {
                index++;
                continue;
            }
            long minutes = java.time.Duration.between(from, to).toMinutes();
            totalMinutes += minutes;
            LocalDate date = from.toLocalDate();
            dailyTotalsByDate.put(date, dailyTotalsByDate.getOrDefault(date, 0L) + minutes);
            LocalDateTime latestTo = latestToByDate.get(date);
            LocalDateTime latestFrom = latestFromByDate.get(date);
            Integer latestIndex = latestIndexByDate.get(date);
            boolean isLater = latestTo == null
                || to.isAfter(latestTo)
                || (to.equals(latestTo) && (latestFrom == null || from.isAfter(latestFrom)))
                || (to.equals(latestTo)
                    && latestFrom != null
                    && from.equals(latestFrom)
                    && (latestIndex == null || index > latestIndex));
            if (isLater) {
                latestToByDate.put(date, to);
                latestFromByDate.put(date, from);
                latestIndexByDate.put(date, index);
                dailyTotalLastActivity.put(date, activity);
            }
            index++;
        }
        if (totalHoursLabel != null) {
            totalHoursLabel.setText("Total: " + formatMinutes(totalMinutes));
        }
    }

    private String getDailyTotalText(Activity activity) {
        LocalDateTime from = Activity.parseStoredDateTime(activity.getFrom());
        if (from == null) {
            return "";
        }
        LocalDate date = from.toLocalDate();
        Activity lastForDay = dailyTotalLastActivity.get(date);
        if (lastForDay != activity) {
            return "";
        }
        Long minutes = dailyTotalsByDate.get(date);
        if (minutes == null) {
            return "";
        }
        return formatMinutes(minutes);
    }

    private String formatMinutes(long minutes) {
        long hours = minutes / 60;
        long remainder = minutes % 60;
        return String.format("%02d:%02d", hours, remainder);
    }

    private void openManagementDialog() {
        ManagementDialog dialog = new ManagementDialog(
            customers,
            this::saveData,
            new ActivityCallbacks() {
                @Override
                public int countActivitiesForCustomer(String customerName) {
                    return countActivitiesForCustomer(customerName);
                }

                @Override
                public int countActivitiesForProject(String customerName, String projectName) {
                    return countActivitiesForProject(customerName, projectName);
                }

                @Override
                public int countActivitiesForTask(String customerName, String projectName, String taskName) {
                    return countActivitiesForTask(customerName, projectName, taskName);
                }

                @Override
                public void updateActivitiesForCustomerRename(String oldName, String newName) {
                    updateActivitiesForCustomerRename(oldName, newName);
                }

                @Override
                public void updateActivitiesForProjectRename(String customerName, String oldName, String newName) {
                    updateActivitiesForProjectRename(customerName, oldName, newName);
                }

                @Override
                public void updateActivitiesForTaskRename(
                    String customerName,
                    String projectName,
                    String oldName,
                    String newName
                ) {
                    updateActivitiesForTaskRename(customerName, projectName, oldName, newName);
                }

                @Override
                public void removeActivitiesForCustomer(String customerName) {
                    removeActivitiesForCustomer(customerName);
                }

                @Override
                public void removeActivitiesForProject(String customerName, String projectName) {
                    removeActivitiesForProject(customerName, projectName);
                }

                @Override
                public void removeActivitiesForTask(String customerName, String projectName, String taskName) {
                    removeActivitiesForTask(customerName, projectName, taskName);
                }
            }
        );
        dialog.show(primaryStage);
        refreshFilterChoices();
    }

    private void addActivity() {
        Optional<ActivityInput> input = showActivityDialog("Add Activity", null);
        input.ifPresent(value -> {
            Activity activity = new Activity(
                value.getCustomer().getName(),
                value.getProject().getName(),
                value.getTask().getName(),
                formatDateTime(value.getFrom()),
                formatDateTime(value.getTo())
            );
            activities.add(activity);
            lastActivity = activity;
            activityTable.getSelectionModel().select(activity);
            saveData();
        });
    }

    private void editActivity() {
        Activity selected = getSelectedActivity();
        if (selected == null) {
            showInfo("Select an activity to edit.");
            return;
        }
        Optional<ActivityInput> input = showActivityDialog("Edit Activity", selected);
        input.ifPresent(value -> {
            selected.setCustomerName(value.getCustomer().getName());
            selected.setProjectName(value.getProject().getName());
            selected.setTaskName(value.getTask().getName());
            selected.setFrom(formatDateTime(value.getFrom()));
            selected.setTo(formatDateTime(value.getTo()));
            lastActivity = selected;
            activityTable.refresh();
            saveData();
            applyFilters();
        });
    }

    private void deleteActivity() {
        Activity selected = getSelectedActivity();
        if (selected == null) {
            showInfo("Select an activity to delete.");
            return;
        }
        if (confirmDelete("Delete activity", "Delete selected activity?")) {
            activities.remove(selected);
            if (selected == lastActivity) {
                setLastActivityFromData();
            }
            saveData();
            applyFilters();
        }
    }

    private Activity getSelectedActivity() {
        if (activityTable == null) {
            return null;
        }
        return activityTable.getSelectionModel().getSelectedItem();
    }

    private Optional<ActivityInput> showActivityDialog(String title, Activity existing) {
        return showActivityDialog(title, existing, null, null, null, null, null);
    }

    private Optional<ActivityInput> showActivityDialog(
        String title,
        Activity existing,
        LocalDateTime defaultFrom,
        LocalDateTime defaultTo,
        Customer defaultCustomer,
        Project defaultProject,
        Task defaultTask
    ) {
        Dialog<ActivityInput> dialog = new Dialog<>();
        dialog.setTitle(title);
        ButtonType saveButton = new ButtonType("Save", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        ChoiceBox<Customer> customerChoice = new ChoiceBox<>(customers);
        ChoiceBox<Project> projectChoice = new ChoiceBox<>();
        ChoiceBox<Task> taskChoice = new ChoiceBox<>();

        DatePicker fromDatePicker = new DatePicker();
        DatePicker toDatePicker = new DatePicker();
        ComboBox<Integer> fromHourChoice = createHourChoiceBox();
        ComboBox<Integer> fromMinuteChoice = createMinuteChoiceBox();
        ComboBox<Integer> toHourChoice = createHourChoiceBox();
        ComboBox<Integer> toMinuteChoice = createMinuteChoiceBox();

        Label durationValue = new Label("--:--");

        customerChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null) {
                projectChoice.setItems(FXCollections.observableArrayList());
                taskChoice.setItems(FXCollections.observableArrayList());
            } else {
                projectChoice.setItems(newValue.getProjects());
                projectChoice.getSelectionModel().clearSelection();
                taskChoice.setItems(FXCollections.observableArrayList());
            }
        });

        projectChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null) {
                taskChoice.setItems(FXCollections.observableArrayList());
            } else {
                taskChoice.setItems(newValue.getTasks());
            }
            taskChoice.getSelectionModel().clearSelection();
        });

        if (existing != null) {
            Customer selectedCustomer = findCustomerByName(existing.getCustomerName());
            if (selectedCustomer != null) {
                customerChoice.getSelectionModel().select(selectedCustomer);
                Project selectedProject = findProjectByName(selectedCustomer, existing.getProjectName());
                if (selectedProject != null) {
                    projectChoice.getSelectionModel().select(selectedProject);
                    Task selectedTask = findTaskByName(selectedProject, existing.getTaskName());
                    if (selectedTask != null) {
                        taskChoice.getSelectionModel().select(selectedTask);
                    }
                }
            }
            LocalDateTime fromDateTime = Activity.parseStoredDateTime(existing.getFrom());
            LocalDateTime toDateTime = Activity.parseStoredDateTime(existing.getTo());
            if (fromDateTime != null) {
                fromDatePicker.setValue(fromDateTime.toLocalDate());
                fromHourChoice.getSelectionModel().select(Integer.valueOf(fromDateTime.getHour()));
                fromMinuteChoice.getSelectionModel().select(Integer.valueOf(fromDateTime.getMinute()));
            }
            if (toDateTime != null) {
                toDatePicker.setValue(toDateTime.toLocalDate());
                toHourChoice.getSelectionModel().select(Integer.valueOf(toDateTime.getHour()));
                toMinuteChoice.getSelectionModel().select(Integer.valueOf(toDateTime.getMinute()));
            }
        } else {
            LocalDateTime fromDateTime = defaultFrom;
            LocalDateTime toDateTime = defaultTo;
            if (fromDateTime == null || toDateTime == null) {
                LocalDateTime now = LocalDateTime.now();
                fromDateTime = now;
                toDateTime = now.plusHours(1);
            }

            if (defaultCustomer != null) {
                customerChoice.getSelectionModel().select(defaultCustomer);
                if (defaultProject != null && defaultCustomer.getProjects().contains(defaultProject)) {
                    projectChoice.getSelectionModel().select(defaultProject);
                    if (defaultTask != null && defaultProject.getTasks().contains(defaultTask)) {
                        taskChoice.getSelectionModel().select(defaultTask);
                    }
                }
            }

            fromDatePicker.setValue(fromDateTime.toLocalDate());
            fromHourChoice.getSelectionModel().select(Integer.valueOf(fromDateTime.getHour()));
            fromMinuteChoice.getSelectionModel().select(Integer.valueOf(fromDateTime.getMinute()));

            toDatePicker.setValue(toDateTime.toLocalDate());
            toHourChoice.getSelectionModel().select(Integer.valueOf(toDateTime.getHour()));
            toMinuteChoice.getSelectionModel().select(Integer.valueOf(toDateTime.getMinute()));
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Customer:"), 0, 0);
        grid.add(customerChoice, 1, 0);
        grid.add(new Label("Project:"), 0, 1);
        grid.add(projectChoice, 1, 1);
        grid.add(new Label("Task:"), 0, 2);
        grid.add(taskChoice, 1, 2);
        grid.add(new Label("From:"), 0, 3);
        grid.add(createDateTimeRow(fromDatePicker, fromHourChoice, fromMinuteChoice), 1, 3);
        grid.add(new Label("To:"), 0, 4);
        grid.add(createDateTimeRow(toDatePicker, toHourChoice, toMinuteChoice), 1, 4);
        grid.add(new Label("Duration:"), 0, 5);
        grid.add(durationValue, 1, 5);

        dialog.getDialogPane().setContent(grid);

        Runnable updateDuration = () -> {
            LocalDateTime from = buildDateTimeOrNull(fromDatePicker.getValue(), fromHourChoice.getValue(), fromMinuteChoice.getValue());
            LocalDateTime to = buildDateTimeOrNull(toDatePicker.getValue(), toHourChoice.getValue(), toMinuteChoice.getValue());
            if (from == null || to == null) {
                durationValue.setText("--:--");
                return;
            }
            durationValue.setText(formatDuration(from, to));
        };

        fromDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> updateDuration.run());
        toDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> updateDuration.run());
        fromHourChoice.valueProperty().addListener((obs, oldValue, newValue) -> updateDuration.run());
        fromMinuteChoice.valueProperty().addListener((obs, oldValue, newValue) -> updateDuration.run());
        toHourChoice.valueProperty().addListener((obs, oldValue, newValue) -> updateDuration.run());
        toMinuteChoice.valueProperty().addListener((obs, oldValue, newValue) -> updateDuration.run());

        dialog.getDialogPane().lookupButton(saveButton).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            ActivityInput input = validateActivityInput(
                customerChoice.getSelectionModel().getSelectedItem(),
                projectChoice.getSelectionModel().getSelectedItem(),
                taskChoice.getSelectionModel().getSelectedItem(),
                fromDatePicker.getValue(),
                fromHourChoice.getSelectionModel().getSelectedItem(),
                fromMinuteChoice.getSelectionModel().getSelectedItem(),
                toDatePicker.getValue(),
                toHourChoice.getSelectionModel().getSelectedItem(),
                toMinuteChoice.getSelectionModel().getSelectedItem()
            );
            if (input == null) {
                event.consume();
            } else if (input.getFrom().isAfter(input.getTo())) {
                showInfo("The start time must be before the end time.");
                event.consume();
            }
        });

        dialog.setResultConverter(button -> {
            if (button != saveButton) {
                return null;
            }
            return validateActivityInput(
                customerChoice.getSelectionModel().getSelectedItem(),
                projectChoice.getSelectionModel().getSelectedItem(),
                taskChoice.getSelectionModel().getSelectedItem(),
                fromDatePicker.getValue(),
                fromHourChoice.getSelectionModel().getSelectedItem(),
                fromMinuteChoice.getSelectionModel().getSelectedItem(),
                toDatePicker.getValue(),
                toHourChoice.getSelectionModel().getSelectedItem(),
                toMinuteChoice.getSelectionModel().getSelectedItem()
            );
        });

        updateDuration.run();
        return dialog.showAndWait();
    }

    private ActivityInput validateActivityInput(
        Customer customer,
        Project project,
        Task task,
        LocalDate fromDate,
        Integer fromHour,
        Integer fromMinute,
        LocalDate toDate,
        Integer toHour,
        Integer toMinute
    ) {
        if (customer == null) {
            showInfo("Please select a customer.");
            return null;
        }
        if (project == null) {
            showInfo("Please select a project.");
            return null;
        }
        if (task == null || task.getName() == null || task.getName().isBlank()) {
            showInfo("Please select a task.");
            return null;
        }
        LocalDateTime from = buildDateTime("From", fromDate, fromHour, fromMinute);
        if (from == null) {
            return null;
        }
        LocalDateTime to = buildDateTime("To", toDate, toHour, toMinute);
        if (to == null) {
            return null;
        }
        return new ActivityInput(customer, project, task, from, to);
    }

    private LocalDateTime buildDateTime(String label, LocalDate date, Integer hour, Integer minute) {
        if (date == null) {
            showInfo("Please select a date for " + label + ".");
            return null;
        }
        if (hour == null || minute == null) {
            showInfo("Please select a time for " + label + ".");
            return null;
        }
        return LocalDateTime.of(date, LocalTime.of(hour, minute));
    }

    private LocalDateTime buildDateTimeOrNull(LocalDate date, Integer hour, Integer minute) {
        if (date == null || hour == null || minute == null) {
            return null;
        }
        return LocalDateTime.of(date, LocalTime.of(hour, minute));
    }

    private String formatDateTime(LocalDateTime value) {
        return Activity.formatDateTime(value);
    }

    private static String formatDuration(LocalDateTime from, LocalDateTime to) {
        return Activity.formatDuration(from, to);
    }

    private ComboBox<Integer> createHourChoiceBox() {
        List<Integer> hours = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            hours.add(i);
        }
        ComboBox<Integer> box = new ComboBox<>(FXCollections.observableArrayList(hours));
        box.setPrefWidth(70);
        return box;
    }

    private ComboBox<Integer> createMinuteChoiceBox() {
        List<Integer> minutes = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            minutes.add(i);
        }
        ComboBox<Integer> box = new ComboBox<>(FXCollections.observableArrayList(minutes));
        box.setPrefWidth(70);
        return box;
    }

    private HBox createDateTimeRow(DatePicker datePicker, ComboBox<Integer> hourChoice, ComboBox<Integer> minuteChoice) {
        Label colon = new Label(":");
        colon.setStyle("-fx-padding: 0 4 0 4;");
        HBox box = new HBox(6, datePicker, hourChoice, colon, minuteChoice);
        box.setFillHeight(true);
        return box;
    }

    private Customer findCustomerByName(String name) {
        if (name == null) {
            return null;
        }
        for (Customer customer : customers) {
            if (name.equals(customer.getName())) {
                return customer;
            }
        }
        return null;
    }

    private Project findProjectByName(Customer customer, String name) {
        if (customer == null || name == null) {
            return null;
        }
        for (Project project : customer.getProjects()) {
            if (name.equals(project.getName())) {
                return project;
            }
        }
        return null;
    }

    private Task findTaskByName(Project project, String name) {
        if (project == null || name == null) {
            return null;
        }
        for (Task task : project.getTasks()) {
            if (name.equals(task.getName())) {
                return task;
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    private int countActivitiesForCustomer(String customerName) {
        int count = 0;
        for (Activity activity : activities) {
            if (customerName.equals(activity.getCustomerName())) {
                count++;
            }
        }
        return count;
    }

    @SuppressWarnings("unused")
    private int countActivitiesForProject(String customerName, String projectName) {
        int count = 0;
        for (Activity activity : activities) {
            if (customerName.equals(activity.getCustomerName()) && projectName.equals(activity.getProjectName())) {
                count++;
            }
        }
        return count;
    }

    @SuppressWarnings("unused")
    private int countActivitiesForTask(String customerName, String projectName, String taskName) {
        int count = 0;
        for (Activity activity : activities) {
            if (customerName.equals(activity.getCustomerName())
                && projectName.equals(activity.getProjectName())
                && taskName.equals(activity.getTaskName())) {
                count++;
            }
        }
        return count;
    }

    @SuppressWarnings("unused")
    private void updateActivitiesForCustomerRename(String oldName, String newName) {
        for (Activity activity : activities) {
            if (oldName.equals(activity.getCustomerName())) {
                activity.setCustomerName(newName);
            }
        }
        if (activityTable != null) {
            activityTable.refresh();
        }
    }

    @SuppressWarnings("unused")
    private void updateActivitiesForProjectRename(String customerName, String oldName, String newName) {
        for (Activity activity : activities) {
            if (customerName.equals(activity.getCustomerName()) && oldName.equals(activity.getProjectName())) {
                activity.setProjectName(newName);
            }
        }
        if (activityTable != null) {
            activityTable.refresh();
        }
    }

    @SuppressWarnings("unused")
    private void updateActivitiesForTaskRename(String customerName, String projectName, String oldName, String newName) {
        for (Activity activity : activities) {
            if (customerName.equals(activity.getCustomerName())
                && projectName.equals(activity.getProjectName())
                && oldName.equals(activity.getTaskName())) {
                activity.setTaskName(newName);
            }
        }
        if (activityTable != null) {
            activityTable.refresh();
        }
    }

    @SuppressWarnings("unused")
    private void removeActivitiesForCustomer(String customerName) {
        activities.removeIf(activity -> customerName.equals(activity.getCustomerName()));
        if (activityTable != null) {
            activityTable.refresh();
        }
    }

    @SuppressWarnings("unused")
    private void removeActivitiesForProject(String customerName, String projectName) {
        activities.removeIf(activity -> customerName.equals(activity.getCustomerName())
            && projectName.equals(activity.getProjectName()));
        if (activityTable != null) {
            activityTable.refresh();
        }
    }

    @SuppressWarnings("unused")
    private void removeActivitiesForTask(String customerName, String projectName, String taskName) {
        activities.removeIf(activity -> customerName.equals(activity.getCustomerName())
            && projectName.equals(activity.getProjectName())
            && taskName.equals(activity.getTaskName()));
        if (activityTable != null) {
            activityTable.refresh();
        }
    }

    private void loadData() {
        if (!Files.exists(DATA_PATH)) {
            return;
        }
        try {
            StorageData data = objectMapper.readValue(DATA_PATH.toFile(), StorageData.class);
            customers.clear();
            activities.clear();
            if (data != null && data.customers != null) {
                for (CustomerData customerData : data.customers) {
                    Customer customer = new Customer(customerData.name);
                    if (customerData.projects != null) {
                        for (ProjectData projectData : customerData.projects) {
                            Project project = new Project(projectData.name);
                            List<String> tasks = projectData.tasks;
                            if ((tasks == null || tasks.isEmpty()) && projectData.activities != null) {
                                tasks = projectData.activities;
                            }
                            if (tasks != null) {
                                for (String taskName : tasks) {
                                    project.getTasks().add(new Task(taskName));
                                }
                            }
                            customer.getProjects().add(project);
                        }
                    }
                    customers.add(customer);
                }
            }
            if (data != null && data.activities != null) {
                for (ActivityData activityData : data.activities) {
                    activities.add(new Activity(
                        activityData.customerName,
                        activityData.projectName,
                        activityData.taskName,
                        activityData.from,
                        activityData.to
                    ));
                }
            }
            applyFilters();
            refreshFilterChoices();
            restoreFilterPreferences();
        } catch (IOException exception) {
            showInfo("Could not load data file: " + exception.getMessage());
        }
    }

    private void saveData() {
        StorageData data = new StorageData();
        for (Customer customer : customers) {
            CustomerData customerData = new CustomerData();
            customerData.name = customer.getName();
            customerData.projects = new ArrayList<>();
            for (Project project : customer.getProjects()) {
                ProjectData projectData = new ProjectData();
                projectData.name = project.getName();
                projectData.tasks = new ArrayList<>();
                for (Task task : project.getTasks()) {
                    projectData.tasks.add(task.getName());
                }
                customerData.projects.add(projectData);
            }
            data.customers.add(customerData);
        }
        data.activities = new ArrayList<>();
        for (Activity activity : activities) {
            ActivityData activityData = new ActivityData();
            activityData.customerName = activity.getCustomerName();
            activityData.projectName = activity.getProjectName();
            activityData.taskName = activity.getTaskName();
            activityData.from = activity.getFrom();
            activityData.to = activity.getTo();
            data.activities.add(activityData);
        }
        try {
            objectMapper.writeValue(DATA_PATH.toFile(), data);
        } catch (IOException exception) {
            showInfo("Could not save data file: " + exception.getMessage());
        }
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

}
