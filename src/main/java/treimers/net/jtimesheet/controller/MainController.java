package treimers.net.jtimesheet.controller;

import static javafx.util.Duration.millis;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.prefs.Preferences;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.ToolBar;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import treimers.net.jtimesheet.model.Activity;
import treimers.net.jtimesheet.model.AppSettings;
import treimers.net.jtimesheet.model.Customer;
import treimers.net.jtimesheet.model.Language;
import treimers.net.jtimesheet.model.Project;
import treimers.net.jtimesheet.model.Task;
import treimers.net.jtimesheet.service.SettingsService;
import treimers.net.jtimesheet.service.StorageService;
import treimers.net.jtimesheet.service.TimesheetWriter;
import treimers.net.jtimesheet.storage.ActivityData;
import treimers.net.jtimesheet.storage.CustomerData;
import treimers.net.jtimesheet.storage.ProjectData;
import treimers.net.jtimesheet.storage.SettingsData;
import treimers.net.jtimesheet.storage.StorageData;
import treimers.net.jtimesheet.storage.TaskData;
import treimers.net.jtimesheet.ui.ActivityCallbacks;
import treimers.net.jtimesheet.ui.ActivityInput;
import treimers.net.jtimesheet.ui.ManagementDialog;
import treimers.net.jtimesheet.view.ActivityDialogView;
import treimers.net.jtimesheet.view.MainView;
import treimers.net.jtimesheet.view.SettingsDialogView;

public class MainController {
    private static final Path DATA_PATH = Paths.get(System.getProperty("user.home"), "jtimesheet.json");
    private static final DateTimeFormatter CSV_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Color TOOLBAR_ICON_COLOR = Color.web("#2563eb");
    private static final Color TOOLBAR_ICON_HOVER_COLOR = Color.web("#1d4ed8");
    private static final String TOOLBAR_BUTTON_HOVER_STYLE = "-fx-background-color: rgba(37, 99, 235, 0.12);";
    private static final String TOOLBAR_BUTTON_NORMAL_STYLE = "";
    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final AppSettings settings;
    private ResourceBundle messages;
    private Locale currentLocale = Locale.ENGLISH;
    private final ObservableList<Customer> customers = FXCollections.observableArrayList();
    private final ObservableList<Activity> activities = FXCollections.observableArrayList();
    private TableView<Activity> activityTable;
    private Stage primaryStage;
    private FilteredList<Activity> filteredActivities;
    private Activity lastActivity;
    private PauseTransition reminderDelay;
    private final Preferences preferences = Preferences.userRoot().node("net/treimers/jtimesheet");
    private final SettingsService settingsService;
    private final StorageService storageService;
    private boolean restoringPreferences;
    private final Map<LocalDate, Long> dailyTotalsByDate = new HashMap<>();
    private final Map<LocalDate, Activity> dailyTotalLastActivity = new HashMap<>();
    private MenuBar menuBar;
    private Menu fileMenu;
    private Menu manageMenu;
    private Menu activityMenu;
    private Menu settingsMenu;
    private MenuItem manageMenuItem;
    private MenuItem importCsvMenuItem;
    private MenuItem exportCsvMenuItem;
    private MenuItem writeTimesheetMenuItem;
    private MenuItem addActivityMenuItem;
    private MenuItem editActivityMenuItem;
    private MenuItem deleteActivityMenuItem;
    private MenuItem askNowMenuItem;
    private MenuItem settingsMenuItem;
    private MenuItem contextAddActivityItem;
    private MenuItem contextEditActivityItem;
    private MenuItem contextDeleteActivityItem;
    private Button writeTimesheetButton;
    private Label totalHoursLabel;
    private Label activitiesHeaderLabel;
    private ComboBox<Customer> customerFilter;
    private ComboBox<Project> projectFilter;
    private ListView<Task> taskFilter;
    private DatePicker fromFilter;
    private DatePicker toFilter;
    private ComboBox<PresetRange> presetFilter;
    private Label customerFilterLabel;
    private Label projectFilterLabel;
    private Label tasksFilterLabel;
    private Label fromFilterLabel;
    private Label toFilterLabel;
    private Label presetFilterLabel;
    private Button clearCustomerButton;
    private Button clearProjectButton;
    private Button clearTasksButton;
    private Button clearDatesButton;
    private TableColumn<Activity, String> customerColumn;
    private TableColumn<Activity, String> projectColumn;
    private TableColumn<Activity, String> taskColumn;
    private TableColumn<Activity, String> fromColumn;
    private TableColumn<Activity, String> toColumn;
    private TableColumn<Activity, String> durationColumn;
    private TableColumn<Activity, String> dailyTotalColumn;

    public MainController() {
        this(new AppSettings());
    }

    public MainController(AppSettings settings) {
        this.settings = settings;
        this.settingsService = new SettingsService(preferences);
        this.storageService = new StorageService(objectMapper);
    }

    public void start(Stage stage) {
        this.primaryStage = stage;
        loadBundle(settings.getLanguage());
        checkBundleCompleteness();
        menuBar = new MenuBar(
            fileMenu(),
            manageMenu(),
            activityMenu(),
            settingsMenu()
        );

        activityTable = createActivityTable();
        ToolBar toolBar = createMainToolBar();
        activities.addListener((ListChangeListener<Activity>) change -> applyFilters());
        totalHoursLabel = new Label(i18n("total.prefix") + "00:00");
        activitiesHeaderLabel = sectionHeader(i18n("section.activities"));
        HBox activityHeader = new HBox(10, activitiesHeaderLabel, new Region(), totalHoursLabel);
        HBox.setHgrow(activityHeader.getChildren().get(1), Priority.ALWAYS);
        VBox activityPanel = new VBox(
            10,
            activityHeader,
            createFilterPanel(),
            activityTable
        );
        activityPanel.setPadding(new Insets(12));
        VBox.setVgrow(activityTable, Priority.ALWAYS);

        MainView view = new MainView(menuBar, toolBar, activityPanel);

        Scene scene = new Scene(view.getRoot(), 900, 520);
        stage.setTitle(i18n("app.title"));
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();

        loadData();
        setLastActivityFromData();
        startReminderScheduler();
    }

    private Menu manageMenu() {
        manageMenuItem = menuItemWithIcon(
            i18n("menu.manage.open"),
            "manage",
            this::openManagementDialog
        );
        manageMenu = new Menu(i18n("menu.manage"));
        manageMenu.getItems().add(manageMenuItem);
        return manageMenu;
    }

    private Menu fileMenu() {
        importCsvMenuItem = menuItemWithIcon(i18n("menu.file.import"), "import", this::importCsv);
        exportCsvMenuItem = menuItemWithIcon(i18n("menu.file.export"), "export", this::exportCsv);
        writeTimesheetMenuItem = menuItemWithIcon(i18n("menu.file.timesheet"), "export", this::writeTimesheet);

        fileMenu = new Menu(i18n("menu.file"));
        fileMenu.getItems().addAll(importCsvMenuItem, exportCsvMenuItem, writeTimesheetMenuItem);
        return fileMenu;
    }

    private Menu activityMenu() {
        addActivityMenuItem = menuItemWithIcon(i18n("menu.activity.add"), "add", this::addActivity);
        editActivityMenuItem = menuItemWithIcon(i18n("menu.activity.edit"), "edit", this::editActivity);
        deleteActivityMenuItem = menuItemWithIcon(i18n("menu.activity.delete"), "delete", this::deleteActivity);
        askNowMenuItem = menuItemWithIcon(
            i18n("menu.activity.ask"),
            "reminder",
            this::showHourlyPrompt
        );

        activityMenu = new Menu(i18n("menu.activity"));
        activityMenu.getItems().addAll(addActivityMenuItem, editActivityMenuItem, deleteActivityMenuItem, askNowMenuItem);
        return activityMenu;
    }

    private Menu settingsMenu() {
        settingsMenuItem = menuItemWithIcon(i18n("menu.settings.open"), "manage", this::openSettingsDialog);
        settingsMenu = new Menu(i18n("menu.settings"));
        settingsMenu.getItems().add(settingsMenuItem);
        return settingsMenu;
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

    private Button toolbarButton(String text, String iconKey, Runnable action) {
        SVGPath icon = iconPath(iconKey, TOOLBAR_ICON_COLOR);
        Button button = new Button(text, icon);
        button.setOnAction(event -> action.run());
        applyToolbarHover(button, icon);
        return button;
    }

    private ToolBar createMainToolBar() {
        Button manageButton = toolbarButton(i18n("menu.manage.open"), "manage", this::openManagementDialog);
        Button addButton = toolbarButton(i18n("menu.activity.add"), "add", this::addActivity);
        Button editButton = toolbarButton(i18n("menu.activity.edit"), "edit", this::editActivity);
        Button deleteButton = toolbarButton(i18n("menu.activity.delete"), "delete", this::deleteActivity);
        Button askNowButton = toolbarButton(i18n("menu.activity.ask"), "reminder", this::showHourlyPrompt);
        Button importButton = toolbarButton(i18n("menu.file.import"), "import", this::importCsv);
        Button exportButton = toolbarButton(i18n("menu.file.export"), "export", this::exportCsv);
        writeTimesheetButton = toolbarButton(i18n("menu.file.timesheet"), "export", this::writeTimesheet);
        Button settingsButton = toolbarButton(i18n("menu.settings.open"), "manage", this::openSettingsDialog);

        editButton.disableProperty().bind(activityTable.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.disableProperty().bind(activityTable.getSelectionModel().selectedItemProperty().isNull());

        return new ToolBar(
            manageButton,
            new Separator(),
            addButton,
            editButton,
            deleteButton,
            askNowButton,
            new Separator(),
            importButton,
            exportButton,
            writeTimesheetButton,
            new Separator(),
            settingsButton
        );
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
            case "manage":
                path.setContent("M6 1h4l1 2h3v2h-2l-1 2 1 2h2v2h-3l-1 2H6l-1-2H2v-2h2l1-2-1-2H2V3h3z M8 6a2 2 0 110 4 2 2 0 010-4z");
                break;
            case "import":
                path.setContent("M8 2v7l2-2 1 1-4 4-4-4 1-1 2 2V2z M2 13h12v2H2z");
                break;
            case "export":
                path.setContent("M8 14V7l-2 2-1-1 4-4 4 4-1 1-2-2v7z M2 13h12v2H2z");
                break;
            case "reminder":
                path.setContent("M8 1a6 6 0 016 6v3l1 2H1l1-2V7a6 6 0 016-6zm1 11H7v2h2v-2z");
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

    private TableView<Activity> createActivityTable() {
        filteredActivities = new FilteredList<>(activities, activity -> true);
        TableView<Activity> table = new TableView<>(filteredActivities);

        customerColumn = new TableColumn<>(i18n("table.customer"));
        customerColumn.setCellValueFactory(cell ->
            new SimpleStringProperty(resolveCustomerName(cell.getValue().getCustomerId()))
        );

        projectColumn = new TableColumn<>(i18n("table.project"));
        projectColumn.setCellValueFactory(cell ->
            new SimpleStringProperty(resolveProjectName(cell.getValue().getProjectId()))
        );

        taskColumn = new TableColumn<>(i18n("table.task"));
        taskColumn.setCellValueFactory(cell ->
            new SimpleStringProperty(resolveTaskName(cell.getValue().getTaskId()))
        );

        fromColumn = new TableColumn<>(i18n("table.from"));
        fromColumn.setCellValueFactory(cell -> cell.getValue().fromProperty());

        toColumn = new TableColumn<>(i18n("table.to"));
        toColumn.setCellValueFactory(cell -> cell.getValue().toProperty());

        durationColumn = new TableColumn<>(i18n("table.duration"));
        durationColumn.setCellValueFactory(cell -> cell.getValue().durationProperty());

        dailyTotalColumn = new TableColumn<>(i18n("table.dailyTotal"));
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

        contextAddActivityItem = menuItemWithIcon(i18n("menu.activity.add"), "add", this::addActivity);
        contextEditActivityItem = menuItemWithIcon(i18n("menu.activity.edit"), "edit", this::editActivity);
        contextEditActivityItem.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        contextDeleteActivityItem = menuItemWithIcon(i18n("menu.activity.delete"), "delete", this::deleteActivity);
        contextDeleteActivityItem.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        ContextMenu menu = new ContextMenu(contextAddActivityItem, contextEditActivityItem, contextDeleteActivityItem);
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
        customerFilter.setPromptText(i18n("filter.customer.placeholder"));
        customerFilter.setPrefWidth(180);
        customerFilter.setButtonCell(createCustomerFilterCell(i18n("filter.customer.placeholder")));
        customerFilter.setCellFactory(listView -> createCustomerFilterCell(i18n("filter.customer.placeholder")));
        if (writeTimesheetMenuItem != null) {
            writeTimesheetMenuItem.disableProperty().bind(customerFilter.valueProperty().isNull());
        }
        if (writeTimesheetButton != null) {
            writeTimesheetButton.disableProperty().bind(customerFilter.valueProperty().isNull());
        }

        projectFilter = new ComboBox<>();
        projectFilter.setPromptText(i18n("filter.project.placeholder"));
        projectFilter.setPrefWidth(180);
        projectFilter.setButtonCell(createProjectFilterCell(i18n("filter.project.placeholder")));
        projectFilter.setCellFactory(listView -> createProjectFilterCell(i18n("filter.project.placeholder")));

        taskFilter = new ListView<>();
        taskFilter.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        taskFilter.setFixedCellSize(24);
        taskFilter.setPrefHeight(taskFilter.getFixedCellSize() * 6 + 6);
        taskFilter.setMinHeight(taskFilter.getPrefHeight());
        taskFilter.setPrefWidth(180);

        fromFilter = new DatePicker();
        toFilter = new DatePicker();

        presetFilter = new ComboBox<>();
        presetFilter.getItems().setAll(PresetRange.values());
        presetFilter.getSelectionModel().select(PresetRange.CUSTOM);
        presetFilter.setPrefWidth(140);
        presetFilter.setButtonCell(createPresetCell());
        presetFilter.setCellFactory(listView -> createPresetCell());

        clearCustomerButton = new Button(i18n("filter.clear.all"));
        clearCustomerButton.setOnAction(event -> {
            customerFilter.getSelectionModel().clearSelection();
            customerFilter.setValue(null);
            updateProjectFilter(null);
            updateTaskFilter(null, null);
            applyFilters();
            saveFilterPreferences();
        });

        clearProjectButton = new Button(i18n("filter.clear.all"));
        clearProjectButton.setOnAction(event -> {
            projectFilter.getSelectionModel().clearSelection();
            projectFilter.setValue(null);
            updateTaskFilter(customerFilter.getValue(), null);
            reapplyProjectAutoSelectIfSingle();
            applyFilters();
            saveFilterPreferences();
        });

        clearTasksButton = new Button(i18n("filter.clear.tasks"));
        clearTasksButton.setOnAction(event -> {
            taskFilter.getSelectionModel().clearSelection();
            applyFilters();
            saveFilterPreferences();
        });

        clearDatesButton = new Button(i18n("filter.clear.dates"));
        clearDatesButton.setOnAction(event -> {
            fromFilter.setValue(null);
            toFilter.setValue(null);
            presetFilter.getSelectionModel().select(PresetRange.CUSTOM);
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
            presetFilter.getSelectionModel().select(PresetRange.CUSTOM);
            applyFilters();
            saveFilterPreferences();
        });
        toFilter.valueProperty().addListener((obs, oldValue, newValue) -> {
            presetFilter.getSelectionModel().select(PresetRange.CUSTOM);
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
            customerFilterLabel = new Label(i18n("filter.customer.label")),
            customerFilter,
            clearCustomerButton,
            projectFilterLabel = new Label(i18n("filter.project.label")),
            projectFilter,
            clearProjectButton
        );
        HBox row2 = new HBox(10, tasksFilterLabel = new Label(i18n("filter.tasks.label")), taskFilter, clearTasksButton);
        HBox row3 = new HBox(
            10,
            fromFilterLabel = new Label(i18n("filter.from.label")),
            fromFilter,
            toFilterLabel = new Label(i18n("filter.to.label")),
            toFilter,
            presetFilterLabel = new Label(i18n("filter.preset.label")),
            presetFilter,
            clearDatesButton
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
        preferences.put("filter.customer", customerFilter.getValue() != null ? customerFilter.getValue().getId() : "");
        preferences.put("filter.project", projectFilter.getValue() != null ? projectFilter.getValue().getId() : "");
        preferences.put("filter.tasks", String.join(",", getSelectedTaskIds()));
        preferences.put("filter.preset", presetFilter.getValue() != null ? presetFilter.getValue().name() : PresetRange.CUSTOM.name());
        preferences.put("filter.from", fromFilter.getValue() != null ? fromFilter.getValue().toString() : "");
        preferences.put("filter.to", toFilter.getValue() != null ? toFilter.getValue().toString() : "");
    }

    private void restoreFilterPreferences() {
        restoringPreferences = true;
        String customerId = preferences.get("filter.customer", "");
        String projectId = preferences.get("filter.project", "");
        String taskIds = preferences.get("filter.tasks", "");
        String presetValue = preferences.get("filter.preset", PresetRange.CUSTOM.name());
        String fromValue = preferences.get("filter.from", "");
        String toValue = preferences.get("filter.to", "");

        Customer customer = findCustomerById(customerId);
        if (customer == null && !customerId.isBlank()) {
            customer = findCustomerByName(customerId);
        }
        if (customer != null) {
            customerFilter.getSelectionModel().select(customer);
        } else {
            customerFilter.getSelectionModel().clearSelection();
            customerFilter.setValue(null);
        }

        Project project = customer != null ? findProjectById(customer, projectId) : null;
        if (project == null && customer != null && !projectId.isBlank()) {
            project = findProjectByName(customer, projectId);
        }
        updateProjectFilter(customer);
        if (project != null && projectFilter.getItems().contains(project)) {
            projectFilter.getSelectionModel().select(project);
        } else if (customer != null && projectFilter.getItems().size() == 1) {
            projectFilter.getSelectionModel().select(0);
        }

        updateTaskFilter(customerFilter.getValue(), projectFilter.getValue());
        taskFilter.getSelectionModel().clearSelection();
        if (!taskIds.isBlank()) {
            for (String id : taskIds.split(",")) {
                Task task = findTaskById(projectFilter.getValue(), id.trim());
                if (task == null) {
                    task = findTaskByName(projectFilter.getValue(), id.trim());
                }
                if (task != null) {
                    taskFilter.getSelectionModel().select(task);
                }
            }
        }

        PresetRange preset = PresetRange.fromPreference(presetValue);
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

    private List<String> getSelectedTaskIds() {
        List<String> ids = new ArrayList<>();
        for (Task task : taskFilter.getSelectionModel().getSelectedItems()) {
            ids.add(task.getId());
        }
        return ids;
    }

    private void reapplyProjectAutoSelectIfSingle() {
        Customer currentCustomer = customerFilter.getValue();
        if (currentCustomer != null && projectFilter.getItems().size() == 1) {
            projectFilter.getSelectionModel().select(0);
        }
    }

    private void applyPreset(PresetRange preset) {
        if (preset == null || preset == PresetRange.CUSTOM) {
            return;
        }
        LocalDate today = LocalDate.now();
        switch (preset) {
            case TODAY:
                fromFilter.setValue(today);
                toFilter.setValue(today);
                break;
            case YESTERDAY:
                LocalDate yesterday = today.minusDays(1);
                fromFilter.setValue(yesterday);
                toFilter.setValue(yesterday);
                break;
            case THIS_WEEK: {
                LocalDate start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate end = start.plusDays(6);
                fromFilter.setValue(start);
                toFilter.setValue(end);
                break;
            }
            case LAST_WEEK: {
                LocalDate start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1);
                LocalDate end = start.plusDays(6);
                fromFilter.setValue(start);
                toFilter.setValue(end);
                break;
            }
            case THIS_MONTH: {
                LocalDate start = today.with(TemporalAdjusters.firstDayOfMonth());
                LocalDate end = today.with(TemporalAdjusters.lastDayOfMonth());
                fromFilter.setValue(start);
                toFilter.setValue(end);
                break;
            }
            case LAST_MONTH: {
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
            if (customer != null && !customer.getId().equals(activity.getCustomerId())) {
                return false;
            }
            if (project != null && !project.getId().equals(activity.getProjectId())) {
                return false;
            }
            if (!selectedTasks.isEmpty()) {
                boolean matchesTask = false;
                for (Task task : selectedTasks) {
                    if (task.getId().equals(activity.getTaskId())) {
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

    private void startReminderScheduler() {
        if (reminderDelay != null) {
            reminderDelay.stop();
        }
        scheduleNextReminder();
    }

    private void scheduleNextReminder() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = nextReminderTime(now);
        Duration delay = Duration.between(now, next);
        if (delay.isNegative()) {
            delay = Duration.ZERO;
        }
        reminderDelay = new PauseTransition(millis(delay.toMillis()));
        reminderDelay.setOnFinished(event -> {
            Platform.runLater(() -> {
                showHourlyPrompt();
                scheduleNextReminder();
            });
        });
        reminderDelay.play();
    }

    private LocalDateTime nextReminderTime(LocalDateTime now) {
        int interval = AppSettings.normalizeReminderIntervalMinutes(settings.getReminderIntervalMinutes());
        LocalTime start = settings.getReminderStartTime();
        LocalTime end = settings.getReminderEndTime();
        LocalDateTime candidate = alignToReminderInterval(now, interval);

        while (true) {
            LocalDate date = candidate.toLocalDate();
            LocalDateTime windowStart = LocalDateTime.of(date, start);
            LocalDateTime windowEnd = LocalDateTime.of(date, end);
            if (candidate.isBefore(windowStart)) {
                candidate = alignToReminderInterval(windowStart, interval);
            }
            if (candidate.isAfter(windowEnd)) {
                candidate = alignToReminderInterval(LocalDateTime.of(date.plusDays(1), start), interval);
                continue;
            }
            return candidate;
        }
    }

    private LocalDateTime alignToReminderInterval(LocalDateTime time, int interval) {
        LocalDateTime base = time.truncatedTo(ChronoUnit.MINUTES);
        if (time.isAfter(base)) {
            base = base.plusMinutes(1);
        }
        int minute = base.getMinute();
        int mod = minute % interval;
        if (mod != 0) {
            base = base.plusMinutes(interval - mod);
        }
        return base;
    }

    private void showHourlyPrompt() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime[] range = suggestedPromptRange(now);
        LocalDateTime from = range[0];
        LocalDateTime to = range[1];

        Customer customer = null;
        Project project = null;
        Task task = null;
        if (lastActivity != null) {
            customer = findCustomerById(lastActivity.getCustomerId());
            project = customer != null ? findProjectById(customer, lastActivity.getProjectId()) : null;
            task = project != null ? findTaskById(project, lastActivity.getTaskId()) : null;
        }

        Optional<ActivityInput> input = showActivityDialog(
            i18n("prompt.hourly.title"),
            null,
            from,
            to,
            customer,
            project,
            task
        );
        input.ifPresent(this::addOrMergeActivity);
    }

    private LocalDateTime[] suggestedPromptRange(LocalDateTime now) {
        LocalDateTime from = now.minusHours(1);
        LocalDateTime to = now;
        if (lastActivity != null) {
            LocalDateTime lastTo = Activity.parseStoredDateTime(lastActivity.getTo());
            if (lastTo != null) {
                LocalDate lastDate = lastTo.toLocalDate();
                if (lastDate.equals(now.toLocalDate())) {
                    from = lastTo;
                    to = now;
                } else if (lastDate.equals(now.toLocalDate().minusDays(1))) {
                    from = now.minusHours(1);
                    to = now;
                }
            }
        }
        if (from.isAfter(to)) {
            from = to;
        }
        return new LocalDateTime[] { from, to };
    }

    private void importCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(i18n("csv.import.title"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(i18n("csv.files.label"), "*.csv"));
        File file = chooser.showOpenDialog(primaryStage);
        if (file == null) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            if (lines.isEmpty()) {
                showInfo(i18n("csv.empty"));
                return;
            }
            List<Activity> imported = parseCsvLines(lines);
            if (imported.isEmpty()) {
                showInfo(i18n("csv.none"));
                return;
            }
            activities.addAll(imported);
            setLastActivityFromData();
            saveData();
            applyFilters();
        } catch (IOException exception) {
            showInfo(i18n("csv.read.error", exception.getMessage()));
        }
    }

    private void exportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(i18n("csv.export.title"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(i18n("csv.files.label"), "*.csv"));
        File file = chooser.showSaveDialog(primaryStage);
        if (file == null) {
            return;
        }
        List<String> lines = new ArrayList<>();
        lines.add(i18n("csv.header"));
        List<Activity> exportActivities = filteredActivities != null ? filteredActivities : activities;
        for (Activity activity : exportActivities) {
            String start = formatCsvDate(activity.getFrom());
            String end = formatCsvDate(activity.getTo());
            lines.add(String.join(
                ";",
                safeCsv(resolveCustomerName(activity.getCustomerId())),
                safeCsv(resolveProjectName(activity.getProjectId())),
                safeCsv(resolveTaskName(activity.getTaskId())),
                start,
                end
            ));
        }
        try {
            Files.write(file.toPath(), lines);
        } catch (IOException exception) {
            showInfo(i18n("csv.write.error", exception.getMessage()));
        }
    }

    private void writeTimesheet() {
        if (activities.isEmpty()) {
            showInfo(i18n("timesheet.none"));
            return;
        }
        Customer selectedCustomer = customerFilter.getValue();
        if (selectedCustomer == null) {
            showInfo(i18n("timesheet.customer.required"));
            return;
        }
        if (!hasRequiredTimesheetConfig(selectedCustomer)) {
            showInfo(i18n("timesheet.customer.missing.properties", selectedCustomer.getName()));
            return;
        }
        Path templatePath = resolveCustomerFile(selectedCustomer.getTimesheetTemplatePath());
        if (templatePath == null) {
            showInfo(i18n("timesheet.customer.missing.template", selectedCustomer.getName()));
            return;
        }
        File outputFile = chooseTimesheetOutputFile(selectedCustomer, templatePath);
        if (outputFile == null) {
            return;
        }
        List<Activity> exportActivities = filteredActivities != null
            ? new ArrayList<>(filteredActivities)
            : new ArrayList<>(activities);
        exportActivities.removeIf(activity -> !selectedCustomer.getId().equals(activity.getCustomerId()));
        TimesheetWriter writer = new TimesheetWriter();
        Properties properties = buildTimesheetProperties(selectedCustomer);
        try {
            writer.writeTimesheet(
                properties,
                templatePath,
                outputFile.toPath(),
                exportActivities,
                this::resolveCustomerName,
                this::resolveProjectName,
                this::resolveTaskName
            );
        } catch (IOException | IllegalArgumentException exception) {
            showInfo(i18n("timesheet.write.error", exception.getMessage()));
        }
    }

    private Properties buildTimesheetProperties(Customer customer) {
        Properties properties = new Properties();
        putIfNotBlank(properties, "rounding", customer.getTimesheetRounding());
        putIfNotBlank(properties, "target.sheetno", customer.getTimesheetSheetNo());
        putIfNotBlank(properties, "target.month.row", customer.getTimesheetMonthRow());
        putIfNotBlank(properties, "target.month.column", customer.getTimesheetMonthColumn());
        putIfNotBlank(properties, "target.data.row", customer.getTimesheetDataRow());
        putIfNotBlank(properties, "target.date.column", customer.getTimesheetDateColumn());
        putIfNotBlank(properties, "target.start.column", customer.getTimesheetStartColumn());
        putIfNotBlank(properties, "target.end.column", customer.getTimesheetEndColumn());
        putIfNotBlank(properties, "target.pause.column", customer.getTimesheetPauseColumn());
        putIfNotBlank(properties, "target.task.column", customer.getTimesheetTaskColumn());
        putIfNotBlank(properties, "target.date.format", customer.getTimesheetDateFormat());
        putIfNotBlank(properties, "target.evaluate.formulas", customer.getTimesheetEvaluateFormulas());
        putIfNotBlank(properties, "target.task.separator", customer.getTimesheetTaskSeparator());
        return properties;
    }

    private boolean hasRequiredTimesheetConfig(Customer customer) {
        return hasText(customer.getTimesheetSheetNo())
            && hasText(customer.getTimesheetMonthRow())
            && hasText(customer.getTimesheetMonthColumn())
            && hasText(customer.getTimesheetDataRow())
            && hasText(customer.getTimesheetDateColumn())
            && hasText(customer.getTimesheetStartColumn())
            && hasText(customer.getTimesheetEndColumn())
            && hasText(customer.getTimesheetPauseColumn())
            && hasText(customer.getTimesheetTaskColumn());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void putIfNotBlank(Properties properties, String key, String value) {
        if (value == null) {
            return;
        }
        String trimmed = value.trim();
        if (!trimmed.isEmpty()) {
            properties.setProperty(key, trimmed);
        }
    }


    private File chooseTimesheetOutputFile(Customer customer, Path templatePath) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(i18n("timesheet.output.title"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
            i18n("timesheet.output.filter"),
            "*.xls",
            "*.xlsx"
        ));
        applyInitialFile(chooser, templatePath != null ? templatePath.toString() : null);
        String extension = resolveTemplateExtension(templatePath);
        String suggestion = customer != null ? customer.getTimesheetFilenameSuggestion() : null;
        if (suggestion != null && !suggestion.isBlank()) {
            chooser.setInitialFileName(suggestion.trim() + extension);
        } else if (customer != null && customer.getName() != null && !customer.getName().isBlank()) {
            chooser.setInitialFileName(customer.getName().trim() + "-timesheet" + extension);
        }
        File file = chooser.showSaveDialog(primaryStage);
        return file;
    }

    private String resolveTemplateExtension(Path templatePath) {
        if (templatePath == null) {
            return ".xlsx";
        }
        String name = templatePath.getFileName().toString().toLowerCase();
        if (name.endsWith(".xls")) {
            return ".xls";
        }
        if (name.endsWith(".xlsx")) {
            return ".xlsx";
        }
        return ".xlsx";
    }

    private Path resolveCustomerFile(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        Path resolved = Paths.get(path.trim());
        return Files.exists(resolved) ? resolved : null;
    }

    private void applyInitialFile(FileChooser chooser, String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        File file = new File(path);
        File directory = file.isDirectory() ? file : file.getParentFile();
        if (directory != null && directory.exists()) {
            chooser.setInitialDirectory(directory);
        }
        if (file.isFile()) {
            chooser.setInitialFileName(file.getName());
        }
    }

    private List<Activity> parseCsvLines(List<String> lines) {
        List<Activity> result = new ArrayList<>();
        if (lines.isEmpty()) {
            return result;
        }
        String header = lines.get(0).trim();
        int startIndex = 0;
        if (!header.isEmpty() && isCsvHeaderLine(header)) {
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
            Customer customerObj = getOrCreateCustomerByName(customer);
            Project projectObj = getOrCreateProjectByName(customerObj, project);
            Task taskObj = getOrCreateTaskByName(projectObj, task);
            result.add(new Activity(
                customerObj.getId(),
                projectObj.getId(),
                taskObj.getId(),
                Activity.formatDateTime(startTime),
                Activity.formatDateTime(endTime)
            ));
        }
        return result;
    }

    private boolean isCsvHeaderLine(String line) {
        if (line == null || line.isBlank()) {
            return false;
        }
        String delimiter = line.contains(";") ? ";" : ",";
        String[] parts = line.toLowerCase().split(delimiter, -1);
        if (parts.length < 5) {
            return false;
        }
        int matches = 0;
        if (matchesAny(parts, "kundenname", "kunde", "customer")) {
            matches++;
        }
        if (matchesAny(parts, "projekt", "project")) {
            matches++;
        }
        if (matchesAny(parts, "tätigkeit", "taetigkeit", "task")) {
            matches++;
        }
        if (matchesAny(parts, "startzeit", "start", "from")) {
            matches++;
        }
        if (matchesAny(parts, "endzeit", "end", "to")) {
            matches++;
        }
        return matches >= 3;
    }

    private boolean matchesAny(String[] parts, String... values) {
        for (String part : parts) {
            String trimmed = part.trim();
            for (String value : values) {
                if (trimmed.contains(value)) {
                    return true;
                }
            }
        }
        return false;
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
            long minutes = Duration.between(from, to).toMinutes();
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
            totalHoursLabel.setText(i18n("total.prefix") + formatMinutes(totalMinutes));
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
                public int countActivitiesForCustomer(String customerId) {
                    return MainController.this.countActivitiesForCustomer(customerId);
                }

                @Override
                public int countActivitiesForProject(String customerId, String projectId) {
                    return MainController.this.countActivitiesForProject(customerId, projectId);
                }

                @Override
                public int countActivitiesForTask(String customerId, String projectId, String taskId) {
                    return MainController.this.countActivitiesForTask(customerId, projectId, taskId);
                }

                @Override
                public void removeActivitiesForCustomer(String customerId) {
                    MainController.this.removeActivitiesForCustomer(customerId);
                }

                @Override
                public void removeActivitiesForProject(String customerId, String projectId) {
                    MainController.this.removeActivitiesForProject(customerId, projectId);
                }

                @Override
                public void removeActivitiesForTask(String customerId, String projectId, String taskId) {
                    MainController.this.removeActivitiesForTask(customerId, projectId, taskId);
                }
            },
            messages,
            currentLocale
        );
        dialog.show(primaryStage);
        refreshFilterChoices();
    }

    private void openSettingsDialog() {
        SettingsDialogView view = new SettingsDialogView(messages, currentLocale);
        Optional<SettingsDialogView.SettingsResult> result = view.show(settings);
        if (result.isPresent()) {
            SettingsDialogView.SettingsResult values = result.get();
            settings.setTimeGridMinutes(values.getTimeGridMinutes());
            settings.setReminderIntervalMinutes(values.getReminderIntervalMinutes());
            settings.setReminderWindow(values.getReminderStart(), values.getReminderEnd());
            settings.setLanguage(values.getLanguage());
            settingsService.save(settings);
            saveData();
            applyLanguage();
            startReminderScheduler();
        }
    }

    private void addActivity() {
        Optional<ActivityInput> input = showActivityDialog(i18n("activity.add.title"), null);
        input.ifPresent(this::addOrMergeActivity);
    }

    private void addOrMergeActivity(ActivityInput input) {
        Activity activity = new Activity(
            input.getCustomer().getId(),
            input.getProject().getId(),
            input.getTask().getId(),
            formatDateTime(input.getFrom()),
            formatDateTime(input.getTo())
        );
        if (canMergeWithLast(activity)) {
            lastActivity.setTo(activity.getTo());
            if (activityTable != null) {
                activityTable.refresh();
                activityTable.getSelectionModel().select(lastActivity);
            }
        } else {
            activities.add(activity);
            lastActivity = activity;
            if (activityTable != null) {
                activityTable.getSelectionModel().select(activity);
            }
        }
        saveData();
        applyFilters();
    }

    private boolean canMergeWithLast(Activity activity) {
        if (lastActivity == null) {
            return false;
        }
        if (!lastActivity.getCustomerId().equals(activity.getCustomerId())) {
            return false;
        }
        if (!lastActivity.getProjectId().equals(activity.getProjectId())) {
            return false;
        }
        if (!lastActivity.getTaskId().equals(activity.getTaskId())) {
            return false;
        }
        LocalDateTime lastTo = Activity.parseStoredDateTime(lastActivity.getTo());
        LocalDateTime newFrom = Activity.parseStoredDateTime(activity.getFrom());
        return lastTo != null && lastTo.equals(newFrom);
    }

    private void editActivity() {
        Activity selected = getSelectedActivity();
        if (selected == null) {
            showInfo(i18n("activity.edit.select"));
            return;
        }
        Optional<ActivityInput> input = showActivityDialog(i18n("activity.edit.title"), selected);
        input.ifPresent(value -> {
            selected.setCustomerId(value.getCustomer().getId());
            selected.setProjectId(value.getProject().getId());
            selected.setTaskId(value.getTask().getId());
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
            showInfo(i18n("activity.delete.select"));
            return;
        }
        if (confirmDelete(
            i18n("activity.delete.title"),
            i18n("activity.delete.confirm")
        )) {
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
        LocalDateTime fromDateTime = defaultFrom;
        LocalDateTime toDateTime = defaultTo;
        Customer resolvedCustomer = defaultCustomer;
        Project resolvedProject = defaultProject;
        Task resolvedTask = defaultTask;
        if (existing != null) {
            resolvedCustomer = findCustomerById(existing.getCustomerId());
            if (resolvedCustomer != null) {
                resolvedProject = findProjectById(resolvedCustomer, existing.getProjectId());
                if (resolvedProject != null) {
                    resolvedTask = findTaskById(resolvedProject, existing.getTaskId());
                }
            }
            fromDateTime = Activity.parseStoredDateTime(existing.getFrom());
            toDateTime = Activity.parseStoredDateTime(existing.getTo());
        }
        ActivityDialogView view = new ActivityDialogView(messages, currentLocale);
        return view.show(
            title,
            customers,
            fromDateTime,
            toDateTime,
            resolvedCustomer,
            resolvedProject,
            resolvedTask,
            settings.getTimeGridMinutes()
        );
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

    private Customer findCustomerById(String id) {
        if (id == null) {
            return null;
        }
        for (Customer customer : customers) {
            if (id.equals(customer.getId())) {
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

    private Project findProjectById(Customer customer, String id) {
        if (customer == null || id == null) {
            return null;
        }
        for (Project project : customer.getProjects()) {
            if (id.equals(project.getId())) {
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

    private Task findTaskById(Project project, String id) {
        if (project == null || id == null) {
            return null;
        }
        for (Task task : project.getTasks()) {
            if (id.equals(task.getId())) {
                return task;
            }
        }
        return null;
    }

    private String resolveCustomerName(String id) {
        Customer customer = findCustomerById(id);
        return customer != null ? customer.getName() : "";
    }

    private String resolveProjectName(String id) {
        if (id == null) {
            return "";
        }
        for (Customer customer : customers) {
            Project project = findProjectById(customer, id);
            if (project != null) {
                return project.getName();
            }
        }
        return "";
    }

    private String resolveTaskName(String id) {
        if (id == null) {
            return "";
        }
        for (Customer customer : customers) {
            for (Project project : customer.getProjects()) {
                Task task = findTaskById(project, id);
                if (task != null) {
                    return task.getName();
                }
            }
        }
        return "";
    }

    private Customer getOrCreateCustomerByName(String name) {
        Customer customer = findCustomerByName(name);
        if (customer != null) {
            return customer;
        }
        Customer created = new Customer(name);
        customers.add(created);
        sortCustomers();
        return created;
    }

    private Project getOrCreateProjectByName(Customer customer, String name) {
        Project project = findProjectByName(customer, name);
        if (project != null) {
            return project;
        }
        Project created = new Project(name);
        customer.getProjects().add(created);
        sortProjects(customer);
        return created;
    }

    private Task getOrCreateTaskByName(Project project, String name) {
        Task task = findTaskByName(project, name);
        if (task != null) {
            return task;
        }
        Task created = new Task(name);
        project.getTasks().add(created);
        sortTasks(project);
        return created;
    }

    private int countActivitiesForCustomer(String customerId) {
        int count = 0;
        for (Activity activity : activities) {
            if (customerId.equals(activity.getCustomerId())) {
                count++;
            }
        }
        return count;
    }

    private int countActivitiesForProject(String customerId, String projectId) {
        int count = 0;
        for (Activity activity : activities) {
            if (customerId.equals(activity.getCustomerId()) && projectId.equals(activity.getProjectId())) {
                count++;
            }
        }
        return count;
    }

    private int countActivitiesForTask(String customerId, String projectId, String taskId) {
        int count = 0;
        for (Activity activity : activities) {
            if (customerId.equals(activity.getCustomerId())
                && projectId.equals(activity.getProjectId())
                && taskId.equals(activity.getTaskId())) {
                count++;
            }
        }
        return count;
    }

    private void removeActivitiesForCustomer(String customerId) {
        activities.removeIf(activity -> customerId.equals(activity.getCustomerId()));
        if (activityTable != null) {
            activityTable.refresh();
        }
    }

    private void removeActivitiesForProject(String customerId, String projectId) {
        activities.removeIf(activity -> customerId.equals(activity.getCustomerId())
            && projectId.equals(activity.getProjectId()));
        if (activityTable != null) {
            activityTable.refresh();
        }
    }

    private void removeActivitiesForTask(String customerId, String projectId, String taskId) {
        activities.removeIf(activity -> customerId.equals(activity.getCustomerId())
            && projectId.equals(activity.getProjectId())
            && taskId.equals(activity.getTaskId()));
        if (activityTable != null) {
            activityTable.refresh();
        }
    }

    private void loadData() {
        if (!Files.exists(DATA_PATH)) {
            return;
        }
        try {
            StorageData data = storageService.load(DATA_PATH);
            applySettings(data != null ? data.settings : null);
            customers.clear();
            activities.clear();
            if (data != null && data.customers != null) {
                for (CustomerData customerData : data.customers) {
                    String customerId = customerData.id != null ? customerData.id : UUID.randomUUID().toString();
                    Customer customer = new Customer(
                        customerId,
                        customerData.name,
                        customerData.address,
                        customerData.timesheetTemplatePath,
                        customerData.timesheetFilenameSuggestion,
                        customerData.timesheetRounding,
                        customerData.timesheetSheetNo,
                        customerData.timesheetMonthRow,
                        customerData.timesheetMonthColumn,
                        customerData.timesheetDataRow,
                        customerData.timesheetDateColumn,
                        customerData.timesheetStartColumn,
                        customerData.timesheetEndColumn,
                        customerData.timesheetPauseColumn,
                        customerData.timesheetTaskColumn,
                        customerData.timesheetDateFormat,
                        customerData.timesheetEvaluateFormulas,
                        customerData.timesheetTaskSeparator
                    );
                    if (customerData.projects != null) {
                        for (ProjectData projectData : customerData.projects) {
                            String projectId = projectData.id != null ? projectData.id : UUID.randomUUID().toString();
                            Project project = new Project(projectId, projectData.name);
                            if (projectData.tasks != null && !projectData.tasks.isEmpty()) {
                                for (TaskData taskData : projectData.tasks) {
                                    String taskId = taskData.id != null ? taskData.id : UUID.randomUUID().toString();
                                    String taskName = taskData.name != null ? taskData.name : "";
                                    project.getTasks().add(new Task(taskId, taskName));
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
                    String customerId = activityData.customerId;
                    String projectId = activityData.projectId;
                    String taskId = activityData.taskId;
                    if (customerId == null || projectId == null || taskId == null) {
                        continue;
                    }
                    activities.add(new Activity(
                        customerId,
                        projectId,
                        taskId,
                        activityData.from,
                        activityData.to
                    ));
                }
            }
            sortCustomers();
            applyFilters();
            refreshFilterChoices();
            restoreFilterPreferences();
            applyLanguage();
        } catch (IOException exception) {
            showInfo(i18n("data.load.error", exception.getMessage()));
        }
    }

    private void saveData() {
        StorageData data = new StorageData();
        for (Customer customer : customers) {
            CustomerData customerData = new CustomerData();
            customerData.id = customer.getId();
            customerData.name = customer.getName();
            customerData.address = customer.getAddress();
            customerData.timesheetTemplatePath = customer.getTimesheetTemplatePath();
            customerData.timesheetFilenameSuggestion = customer.getTimesheetFilenameSuggestion();
            customerData.timesheetRounding = customer.getTimesheetRounding();
            customerData.timesheetSheetNo = customer.getTimesheetSheetNo();
            customerData.timesheetMonthRow = customer.getTimesheetMonthRow();
            customerData.timesheetMonthColumn = customer.getTimesheetMonthColumn();
            customerData.timesheetDataRow = customer.getTimesheetDataRow();
            customerData.timesheetDateColumn = customer.getTimesheetDateColumn();
            customerData.timesheetStartColumn = customer.getTimesheetStartColumn();
            customerData.timesheetEndColumn = customer.getTimesheetEndColumn();
            customerData.timesheetPauseColumn = customer.getTimesheetPauseColumn();
            customerData.timesheetTaskColumn = customer.getTimesheetTaskColumn();
            customerData.timesheetDateFormat = customer.getTimesheetDateFormat();
            customerData.timesheetEvaluateFormulas = customer.getTimesheetEvaluateFormulas();
            customerData.timesheetTaskSeparator = customer.getTimesheetTaskSeparator();
            customerData.projects = new ArrayList<>();
            for (Project project : customer.getProjects()) {
                ProjectData projectData = new ProjectData();
                projectData.id = project.getId();
                projectData.name = project.getName();
                projectData.tasks = new ArrayList<>();
                for (Task task : project.getTasks()) {
                    projectData.tasks.add(new TaskData(task.getId(), task.getName()));
                }
                customerData.projects.add(projectData);
            }
            data.customers.add(customerData);
        }
        data.activities = new ArrayList<>();
        for (Activity activity : activities) {
            ActivityData activityData = new ActivityData();
            activityData.customerId = activity.getCustomerId();
            activityData.projectId = activity.getProjectId();
            activityData.taskId = activity.getTaskId();
            activityData.from = activity.getFrom();
            activityData.to = activity.getTo();
            data.activities.add(activityData);
        }
        data.settings = null;
        try {
            storageService.save(DATA_PATH, data);
        } catch (IOException exception) {
            showInfo(i18n("data.save.error", exception.getMessage()));
        }
    }

    private void applySettings(SettingsData data) {
        if (settingsService.loadIfPresent(settings)) {
            return;
        }
        if (data == null) {
            settings.setLanguage(Language.ENGLISH);
            settings.setTimeGridMinutes(AppSettings.DEFAULT_TIME_GRID_MINUTES);
            settings.setReminderIntervalMinutes(AppSettings.DEFAULT_REMINDER_INTERVAL_MINUTES);
            settings.setReminderWindow(LocalTime.of(9, 0), LocalTime.of(17, 0));
        } else {
            settings.setLanguage(Language.fromCode(data.language));
            settings.setTimeGridMinutes(data.timeGridMinutes);
            settings.setReminderIntervalMinutes(data.reminderIntervalMinutes);
            LocalTime start = parseTimeValue(data.reminderStart, LocalTime.of(9, 0));
            LocalTime end = parseTimeValue(data.reminderEnd, LocalTime.of(17, 0));
            settings.setReminderWindow(start, end);
        }
        settingsService.save(settings);
    }

    private void loadBundle(Language language) {
        Locale locale = language == Language.GERMAN ? Locale.GERMAN : Locale.ENGLISH;
        currentLocale = locale;
        try {
            messages = ResourceBundle.getBundle("i18n.messages", locale);
        } catch (MissingResourceException exception) {
            currentLocale = Locale.ENGLISH;
            messages = ResourceBundle.getBundle("i18n.messages", Locale.ENGLISH);
        }
    }

    private void checkBundleCompleteness() {
        ResourceBundle english = ResourceBundle.getBundle("i18n.messages", Locale.ENGLISH);
        ResourceBundle german = ResourceBundle.getBundle("i18n.messages", Locale.GERMAN);
        List<String> missingInGerman = new ArrayList<>();
        List<String> missingInEnglish = new ArrayList<>();
        for (String key : english.keySet()) {
            if (!german.containsKey(key)) {
                missingInGerman.add(key);
            }
        }
        for (String key : german.keySet()) {
            if (!english.containsKey(key)) {
                missingInEnglish.add(key);
            }
        }
        if (missingInGerman.isEmpty() && missingInEnglish.isEmpty()) {
            return;
        }
        StringBuilder message = new StringBuilder();
        if (!missingInGerman.isEmpty()) {
            message.append(i18n("i18n.missing.de", String.join(", ", missingInGerman))).append("\n\n");
        }
        if (!missingInEnglish.isEmpty()) {
            message.append(i18n("i18n.missing.en", String.join(", ", missingInEnglish)));
        }
        Alert alert = new Alert(AlertType.WARNING, message.toString(), ButtonType.OK);
        alert.setTitle(i18n("i18n.missing.title"));
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private String i18n(String key, Object... args) {
        if (messages == null) {
            loadBundle(Language.ENGLISH);
        }
        String value;
        try {
            value = messages.getString(key);
        } catch (MissingResourceException exception) {
            value = key;
        }
        if (args == null || args.length == 0) {
            return value;
        }
        return String.format(currentLocale, value, args);
    }

    private void applyLanguage() {
        loadBundle(settings.getLanguage());
        if (primaryStage != null) {
            primaryStage.setTitle(i18n("app.title"));
        }
        if (activitiesHeaderLabel != null) {
            activitiesHeaderLabel.setText(i18n("section.activities"));
        }
        if (fileMenu != null) {
            fileMenu.setText(i18n("menu.file"));
        }
        if (manageMenu != null) {
            manageMenu.setText(i18n("menu.manage"));
        }
        if (activityMenu != null) {
            activityMenu.setText(i18n("menu.activity"));
        }
        if (settingsMenu != null) {
            settingsMenu.setText(i18n("menu.settings"));
        }
        if (manageMenuItem != null) {
            manageMenuItem.setText(i18n("menu.manage.open"));
        }
        if (importCsvMenuItem != null) {
            importCsvMenuItem.setText(i18n("menu.file.import"));
        }
        if (exportCsvMenuItem != null) {
            exportCsvMenuItem.setText(i18n("menu.file.export"));
        }
        if (writeTimesheetMenuItem != null) {
            writeTimesheetMenuItem.setText(i18n("menu.file.timesheet"));
        }
        if (addActivityMenuItem != null) {
            addActivityMenuItem.setText(i18n("menu.activity.add"));
        }
        if (editActivityMenuItem != null) {
            editActivityMenuItem.setText(i18n("menu.activity.edit"));
        }
        if (deleteActivityMenuItem != null) {
            deleteActivityMenuItem.setText(i18n("menu.activity.delete"));
        }
        if (askNowMenuItem != null) {
            askNowMenuItem.setText(i18n("menu.activity.ask"));
        }
        if (settingsMenuItem != null) {
            settingsMenuItem.setText(i18n("menu.settings.open"));
        }
        if (contextAddActivityItem != null) {
            contextAddActivityItem.setText(i18n("menu.activity.add"));
        }
        if (contextEditActivityItem != null) {
            contextEditActivityItem.setText(i18n("menu.activity.edit"));
        }
        if (contextDeleteActivityItem != null) {
            contextDeleteActivityItem.setText(i18n("menu.activity.delete"));
        }
        if (customerColumn != null) {
            customerColumn.setText(i18n("table.customer"));
        }
        if (projectColumn != null) {
            projectColumn.setText(i18n("table.project"));
        }
        if (taskColumn != null) {
            taskColumn.setText(i18n("table.task"));
        }
        if (fromColumn != null) {
            fromColumn.setText(i18n("table.from"));
        }
        if (toColumn != null) {
            toColumn.setText(i18n("table.to"));
        }
        if (durationColumn != null) {
            durationColumn.setText(i18n("table.duration"));
        }
        if (dailyTotalColumn != null) {
            dailyTotalColumn.setText(i18n("table.dailyTotal"));
        }
        updateFilterTexts();
        if (presetFilter != null) {
            presetFilter.setButtonCell(createPresetCell());
            presetFilter.setCellFactory(listView -> createPresetCell());
        }
        applyFilters();
    }

    private void updateFilterTexts() {
        if (customerFilterLabel != null) {
            customerFilterLabel.setText(i18n("filter.customer.label"));
        }
        if (projectFilterLabel != null) {
            projectFilterLabel.setText(i18n("filter.project.label"));
        }
        if (tasksFilterLabel != null) {
            tasksFilterLabel.setText(i18n("filter.tasks.label"));
        }
        if (fromFilterLabel != null) {
            fromFilterLabel.setText(i18n("filter.from.label"));
        }
        if (toFilterLabel != null) {
            toFilterLabel.setText(i18n("filter.to.label"));
        }
        if (presetFilterLabel != null) {
            presetFilterLabel.setText(i18n("filter.preset.label"));
        }
        if (clearCustomerButton != null) {
            clearCustomerButton.setText(i18n("filter.clear.all"));
        }
        if (clearProjectButton != null) {
            clearProjectButton.setText(i18n("filter.clear.all"));
        }
        if (clearTasksButton != null) {
            clearTasksButton.setText(i18n("filter.clear.tasks"));
        }
        if (clearDatesButton != null) {
            clearDatesButton.setText(i18n("filter.clear.dates"));
        }
        if (customerFilter != null) {
            String placeholder = i18n("filter.customer.placeholder");
            customerFilter.setPromptText(placeholder);
            customerFilter.setButtonCell(createCustomerFilterCell(placeholder));
            customerFilter.setCellFactory(listView -> createCustomerFilterCell(placeholder));
        }
        if (projectFilter != null) {
            String placeholder = i18n("filter.project.placeholder");
            projectFilter.setPromptText(placeholder);
            projectFilter.setButtonCell(createProjectFilterCell(placeholder));
            projectFilter.setCellFactory(listView -> createProjectFilterCell(placeholder));
        }
    }

    private ListCell<PresetRange> createPresetCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(PresetRange item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(presetLabel(item));
            }
        };
    }

    private String presetLabel(PresetRange preset) {
        if (preset == null) {
            return "";
        }
        switch (preset) {
            case CUSTOM:
                return i18n("preset.custom");
            case TODAY:
                return i18n("preset.today");
            case YESTERDAY:
                return i18n("preset.yesterday");
            case THIS_WEEK:
                return i18n("preset.thisWeek");
            case LAST_WEEK:
                return i18n("preset.lastWeek");
            case THIS_MONTH:
                return i18n("preset.thisMonth");
            case LAST_MONTH:
                return i18n("preset.lastMonth");
            default:
                return "";
        }
    }

    private String formatDateTime(LocalDateTime value) {
        return Activity.formatDateTime(value);
    }

    private LocalTime parseTimeValue(String value, LocalTime fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return LocalTime.parse(value, TIME_FORMAT);
        } catch (DateTimeParseException exception) {
            return fallback;
        }
    }

    private enum PresetRange {
        CUSTOM,
        TODAY,
        YESTERDAY,
        THIS_WEEK,
        LAST_WEEK,
        THIS_MONTH,
        LAST_MONTH;

        static PresetRange fromPreference(String value) {
            if (value == null || value.isBlank()) {
                return CUSTOM;
            }
            try {
                return PresetRange.valueOf(value);
            } catch (IllegalArgumentException exception) {
                switch (value.trim()) {
                    case "Custom":
                        return CUSTOM;
                    case "Today":
                        return TODAY;
                    case "Yesterday":
                        return YESTERDAY;
                    case "This Week":
                        return THIS_WEEK;
                    case "Last Week":
                        return LAST_WEEK;
                    case "This Month":
                        return THIS_MONTH;
                    case "Last Month":
                        return LAST_MONTH;
                    default:
                        return CUSTOM;
                }
            }
        }
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
}
