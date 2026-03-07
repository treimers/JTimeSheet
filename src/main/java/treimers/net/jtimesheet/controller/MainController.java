package treimers.net.jtimesheet.controller;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.function.Function;
import java.util.prefs.Preferences;

import com.calendarfx.model.Calendar;
import com.calendarfx.model.CalendarSource;
import com.calendarfx.model.Entry;
import com.calendarfx.model.Interval;
import com.calendarfx.view.CalendarView;
import com.calendarfx.view.CalendarView.Page;
import com.calendarfx.view.DateControl;
import com.calendarfx.view.DayView;
import com.calendarfx.view.MonthView;
import com.calendarfx.view.WeekDayView;
import com.calendarfx.view.WeekView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Dialog;
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
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.controlsfx.control.PopOver;
import treimers.net.jtimesheet.model.Activity;
import treimers.net.jtimesheet.view.TooltipDayEntryView;
import treimers.net.jtimesheet.view.TooltipMonthEntryView;
import treimers.net.jtimesheet.model.AppSettings;
import treimers.net.jtimesheet.model.CalendarColorPalette;
import treimers.net.jtimesheet.model.Customer;
import treimers.net.jtimesheet.model.Language;
import treimers.net.jtimesheet.model.Project;
import treimers.net.jtimesheet.model.Task;
import treimers.net.jtimesheet.model.ViewLevel;
import treimers.net.jtimesheet.model.ViewTabState;
import treimers.net.jtimesheet.service.AddActivityDialogRules;
import treimers.net.jtimesheet.service.ReminderExceptionRules;
import treimers.net.jtimesheet.service.ReminderService;
import treimers.net.jtimesheet.service.ReminderSuggestion;
import treimers.net.jtimesheet.service.ReminderSuggestionLogic;
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
import treimers.net.jtimesheet.ui.DefaultProjectAndTask;
import treimers.net.jtimesheet.ui.ManagementDialog;
import treimers.net.jtimesheet.view.ActivityDialogView;
import treimers.net.jtimesheet.view.PauseDialogView;
import treimers.net.jtimesheet.view.MainView;
import treimers.net.jtimesheet.view.SettingsDialogView;

public class MainController {
    private static final String DATA_FILENAME = "jtimesheet.json";
    private static final DateTimeFormatter CSV_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Color TOOLBAR_ICON_COLOR = Color.web("#2563eb");
    private static final Color TOOLBAR_ICON_HOVER_COLOR = Color.web("#1d4ed8");
    private static final String TOOLBAR_BUTTON_HOVER_STYLE = "-fx-background-color: rgba(37, 99, 235, 0.12);";
    private static final String REMINDER_DEBUG_PREFIX = "[ReminderDebug] ";
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
    private SortedList<Activity> sortedActivities;
    private Activity lastActivity;
    private final ReminderService reminderService = new ReminderService();
    private final ReminderSuggestionLogic reminderSuggestionLogic = new ReminderSuggestionLogic();
    /** When the application was started (for Zeitvorschläge: Start = Programmstart gerundet auf Zeitraster). */
    private LocalDateTime programStartTime;
    /** When reminder showed a gap and user didn't add, re-show same range (don't extend end). Cleared when user adds. */
    private LocalDateTime[] lastShownGapRange;
    private boolean activityDialogOpen;
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
    private Menu viewMenu;
    private Menu settingsMenu;
    private Menu helpMenu;
    private MenuItem helpMenuItem;
    private MenuItem manageMenuItem;
    private MenuItem newViewMenuItem;
    private MenuItem importCsvMenuItem;
    private MenuItem exportCsvMenuItem;
    private MenuItem writeTimesheetMenuItem;
    private MenuItem exitMenuItem;
    private MenuItem addActivityMenuItem;
    private MenuItem editActivityMenuItem;
    private MenuItem deleteActivityMenuItem;
    private MenuItem consolidateActivityMenuItem;
    private MenuItem settingsMenuItem;
    private MenuItem contextAddActivityItem;
    private MenuItem contextEditActivityItem;
    private MenuItem contextAddPauseItem;
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
    private boolean applyingPreset;
    private Label customerFilterLabel;
    private Label projectFilterLabel;
    private Label tasksFilterLabel;
    private Label fromFilterLabel;
    private Label toFilterLabel;
    private Label presetFilterLabel;
    private Label periodNavLabel;
    private Button periodPrevButton;
    private Button periodNextButton;
    private HBox periodNavBox;
    private Button clearCustomerButton;
    private Button clearProjectButton;
    private Button clearTasksButton;
    private Button clearDatesButton;
    private Button consolidateButton;
    private Button settingsButton;
    private Button manageButton;
    private Button addActivityToolbarButton;
    private Button addViewButton;
    private TableColumn<Activity, String> customerColumn;
    private TableColumn<Activity, String> projectColumn;
    private TableColumn<Activity, String> taskColumn;
    private TableColumn<Activity, String> fromColumn;
    private TableColumn<Activity, String> toColumn;
    private TableColumn<Activity, String> durationColumn;
    private TableColumn<Activity, String> dailyTotalColumn;

    private TabPane viewTabPane;
    private VBox activityPanel;
    private Node calendarViewNode;
    private Tab calendarTab;
    private boolean calendarContentCreated;
    private static final int CALENDAR_TAB_INDEX = 1;
    /** URL of the user calendar color override stylesheet, or null if none. */
    private String calendarOverrideStylesheetUrl;
    /** Reference to the CalendarView for applying color stylesheet to list/search (they may use this node's stylesheets). */
    private CalendarView calendarViewRef;
    private CalendarSource calendarSource;
    private Map<String, Calendar<Activity>> calendarsByCustomerProject = new HashMap<>();
    private final List<Entry<Activity>> calendarEntries = new ArrayList<>();
    private final List<ViewTabState> viewTabStates = new ArrayList<>();
    private boolean restoringViewTabs;
    private Stage helpDialog;
    /** Holder for dynamic view tab: state + table so the tab shows filter panel and activity table. */
    private static class ViewTabData {
        final ViewTabState state;
        final TableView<Activity> table;
        ViewTabData(ViewTabState state, TableView<Activity> table) {
            this.state = state;
            this.table = table;
        }
    }
    private HostServices hostServices;
    private boolean reminderDebug;
    private final java.util.List<String> reminderDebugLog = new java.util.ArrayList<>();
    private Dialog<Void> reminderDebugDialog;
    private TextArea reminderDebugTextArea;

    public MainController() {
        this(new AppSettings());
    }

    public MainController(AppSettings settings) {
        this.settings = settings;
        this.settingsService = new SettingsService(preferences);
        this.storageService = new StorageService(objectMapper);
    }

    public void start(Stage stage, HostServices hostServices) {
        this.primaryStage = stage;
        this.hostServices = hostServices;
        this.programStartTime = LocalDateTime.now();
        settingsService.loadIfPresent(settings);
        loadBundle(settings.getLanguage());
        checkBundleCompleteness();
        menuBar = new MenuBar(
            fileMenu(),
            manageMenu(),
            activityMenu(),
            viewMenu(),
            settingsMenu(),
            helpMenu()
        );
        menuBar.setUseSystemMenuBar(true);

        activityTable = createActivityTable();
        ToolBar toolBar = createMainToolBar();
        activities.addListener((ListChangeListener<Activity>) change -> applyFilters());
        totalHoursLabel = new Label(i18n("total.prefix") + "00:00");
        activitiesHeaderLabel = sectionHeader(i18n("section.activities"));
        HBox activityHeader = new HBox(10, activitiesHeaderLabel, new Region(), totalHoursLabel);
        HBox.setHgrow(activityHeader.getChildren().get(1), Priority.ALWAYS);
        viewTabPane = new TabPane();
        Tab mainTab = new Tab(i18n("view.main.tab"));
        mainTab.setClosable(false);
        VBox mainTabContent = new VBox(10, activityHeader, createFilterPanel(), activityTable);
        VBox.setVgrow(activityTable, Priority.ALWAYS);
        mainTab.setContent(mainTabContent);
        viewTabPane.getTabs().add(mainTab);

        calendarViewNode = createCalendarPlaceholder();
        calendarTab = new Tab(i18n("view.tab.calendar"));
        calendarTab.setClosable(false);
        calendarTab.setContent(calendarViewNode);
        viewTabPane.getTabs().add(calendarTab);

        viewTabPane.getSelectionModel().selectedIndexProperty().addListener((obs, oldIdx, newIdx) -> {
            ensureCalendarContentCreated(newIdx == null ? -1 : newIdx.intValue());
            updateTableOrCalendarVisibility();
            applyFilters();
            updateWriteTimesheetButtonForTab();
            saveViewTabsPreferences();
        });
        viewTabPane.getTabs().addListener((ListChangeListener<Tab>) c -> {
            while (c.next()) {
                if (c.wasRemoved()) {
                    for (Tab tab : c.getRemoved()) {
                        Object ud = tab.getUserData();
                        if (ud instanceof ViewTabData) {
                            viewTabStates.remove(((ViewTabData) ud).state);
                        }
                    }
                    if (!restoringViewTabs) {
                        saveViewTabsPreferences();
                    }
                }
            }
        });

        activityPanel = new VBox(10, viewTabPane);
        activityPanel.setPadding(new Insets(12));
        VBox.setVgrow(viewTabPane, Priority.ALWAYS);

        MainView view = new MainView(menuBar, toolBar, activityPanel);

        Scene scene = new Scene(view.getRoot(), 900, 520);
        scene.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (new KeyCodeCombination(KeyCode.F12).match(e)) {
                setReminderDebug(!reminderDebug);
                e.consume();
            } else if (new KeyCodeCombination(KeyCode.F12, KeyCombination.SHIFT_DOWN).match(e)) {
                toggleReminderDebugDialog();
                e.consume();
            }
        });
        // Ensure CalendarFX library styles (calendar.css with -style1-color … -style7-color) are applied.
        URL calendarFxCss = CalendarView.class.getResource("calendar.css");
        if (calendarFxCss != null) {
            scene.getStylesheets().add(calendarFxCss.toExternalForm());
        }
        stage.setTitle(i18n("app.title"));
        stage.setScene(scene);
        setStageIcon(stage);
        stage.setMaximized(true);
        stage.show();

        loadData();
        setLastActivityFromData();
        updateTableOrCalendarVisibility();
        updateWriteTimesheetButtonForTab();
        startReminderScheduler();
    }

    private void setStageIcon(Stage stage) {
        var iconUrl = MainController.class.getResource("/treimers/net/jtimesheet/JTimeSheet_128x128.png");
        if (iconUrl != null) {
            stage.getIcons().add(new Image(iconUrl.toExternalForm()));
        }
    }

    private Menu manageMenu() {
        manageMenuItem = menuItemWithIcon(
            i18n("menu.manage.open"),
            "manage",
            this::openManagementDialog
        );
        manageMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.M, KeyCombination.SHORTCUT_DOWN));
        manageMenu = new Menu(i18n("menu.manage"));
        manageMenu.getItems().add(manageMenuItem);
        return manageMenu;
    }

    private Menu fileMenu() {
        importCsvMenuItem = menuItemWithIcon(i18n("menu.file.import"), "import", this::importCsv);
        exportCsvMenuItem = menuItemWithIcon(i18n("menu.file.export"), "export", this::exportCsv);
        writeTimesheetMenuItem = menuItemWithIcon(i18n("menu.file.timesheet"), "export", this::writeTimesheet);
        writeTimesheetMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN));
        exitMenuItem = menuItemWithIcon(i18n("menu.file.exit"), "manage", Platform::exit);
        exitMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN));

        fileMenu = new Menu(i18n("menu.file"));
        fileMenu.getItems().addAll(
            importCsvMenuItem, exportCsvMenuItem, writeTimesheetMenuItem,
            new SeparatorMenuItem(),
            exitMenuItem
        );
        return fileMenu;
    }

    private Menu activityMenu() {
        addActivityMenuItem = menuItemWithIcon(i18n("menu.activity.add"), "add", this::addActivity);
        addActivityMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_DOWN));
        editActivityMenuItem = menuItemWithIcon(i18n("menu.activity.edit"), "edit", this::editActivity);
        deleteActivityMenuItem = menuItemWithIcon(i18n("menu.activity.delete"), "delete", this::deleteActivity);
        consolidateActivityMenuItem = menuItemWithIcon(
            i18n("menu.activity.consolidate"),
            "consolidate",
            this::consolidateFilteredActivities
        );
        consolidateActivityMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN));
        activityMenu = new Menu(i18n("menu.activity"));
        activityMenu.getItems().addAll(
            addActivityMenuItem,
            editActivityMenuItem,
            deleteActivityMenuItem,
            new SeparatorMenuItem(),
            consolidateActivityMenuItem
        );
        return activityMenu;
    }

    private Menu viewMenu() {
        viewMenu = new Menu(i18n("menu.view"));
        newViewMenuItem = menuItemWithIcon(i18n("menu.view.new"), "add", this::openNewViewDialog);
        newViewMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN));
        viewMenu.getItems().add(newViewMenuItem);
        return viewMenu;
    }

    private Menu settingsMenu() {
        settingsMenuItem = menuItemWithIcon(i18n("menu.settings.open"), "settings", this::openSettingsDialog);
        settingsMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));
        settingsMenu = new Menu(i18n("menu.settings"));
        settingsMenu.getItems().add(settingsMenuItem);
        return settingsMenu;
    }

    private Menu helpMenu() {
        helpMenuItem = menuItemWithIcon(i18n("menu.help"), "manage", this::doHelp);
        helpMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F1));
        helpMenu = new Menu(i18n("menu.help"));
        helpMenu.getItems().add(helpMenuItem);
        return helpMenu;
    }

    private void doHelp() {
        if (helpDialog == null) {
            helpDialog = createHelpDialog();
        }
        if (helpDialog != null) {
            if (helpDialog.isShowing()) {
                helpDialog.toFront();
                helpDialog.requestFocus();
            } else {
                helpDialog.showAndWait();
            }
        }
    }

    private Stage createHelpDialog() {
        try {
            Stage helpStage = new Stage();
            helpStage.setTitle(i18n("help.dialog.title"));
            helpStage.initOwner(primaryStage);
            helpStage.initModality(Modality.WINDOW_MODAL);
            helpStage.setResizable(true);
            URL resource = getClass().getResource("/treimers/net/jtimesheet/helppanel.fxml");
            if (resource == null) {
                return null;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();
            HelpController controller = loader.getController();
            controller.setLanguage(settings.getLanguage());
            controller.setMessages(messages);
            controller.setHostServices(hostServices);
            controller.initContent();
            Scene scene = new Scene(root, 900, 600);
            helpStage.setScene(scene);
            scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ESCAPE) {
                    helpStage.close();
                    event.consume();
                }
            });
            return helpStage;
        } catch (IOException e) {
            showAlert(AlertType.ERROR, i18n("help.dialog.title"), e.getMessage());
            return null;
        }
    }

    private static void addEscapeToClose(Dialog<?> dialog) {
        dialog.setOnShown(e -> {
            dialog.getDialogPane().getScene().addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
                if (ev.getCode() == KeyCode.ESCAPE) {
                    dialog.close();
                    ev.consume();
                }
            });
        });
    }

    private void showAlert(AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.initOwner(primaryStage);
        addEscapeToClose(alert);
        alert.showAndWait();
    }

    private void updateWriteTimesheetButtonForTab() {
        // Stundenzettel-Export ist in allen Ansichten möglich; Auswahl erfolgt im Abfragedialog.
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
        addActivityToolbarButton = toolbarButton(i18n("menu.activity.add"), "add", this::addActivity);
        addViewButton = toolbarButton(i18n("menu.view.new"), "add", this::openNewViewDialog);
        manageButton = toolbarButton(i18n("menu.manage.open"), "manage", this::openManagementDialog);
        consolidateButton = toolbarButton(i18n("menu.activity.consolidate"), "consolidate", this::consolidateFilteredActivities);
        writeTimesheetButton = toolbarButton(i18n("menu.file.timesheet"), "export", this::writeTimesheet);
        settingsButton = toolbarButton(i18n("menu.settings.open"), "settings", this::openSettingsDialog);

        return new ToolBar(
            addActivityToolbarButton,
            addViewButton,
            manageButton,
            consolidateButton,
            new Separator(),
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
                path.setContent("M7 1h2v2H7V1zM7 3h2v2H7V3zM4 5h8v1H4V5zM5 6h2v2H5V6zM9 6h2v2H9V6z");
                break;
            case "consolidate":
                path.setContent("M6 1h4l1 2h3v2h-2l-1 2 1 2h2v2h-3l-1 2H6l-1-2H2v-2h2l1-2-1-2H2V3h3zM8 6a2 2 0 110 4 2 2 0 010-4z");
                break;
            case "settings":
                path.setContent("M9.405 1.05c-.413-1.4-2.397-1.4-2.81 0l-.1.34a1.464 1.464 0 01-2.105.872l-.31-.17c-1.283-.698-2.686.705-1.987 1.987l.169.311c.446.82.023 1.841-.872 2.105l-.34.1c-1.4.413-1.4 2.397 0 2.81l.34.1a1.464 1.464 0 01.872 2.105l-.17.31c-.698 1.283.705 2.686 1.987 1.987l.311-.169a1.464 1.464 0 012.105.872l.1.34c.413 1.4 2.397 1.4 2.81 0l.1-.34a1.464 1.464 0 012.105-.872l.31.17c1.283.698 2.686-.705 1.987-1.987l-.169-.311a1.464 1.464 0 01.872-2.105l.34-.1c1.4-.413 1.4-2.397 0-2.81l-.34-.1a1.464 1.464 0 01-.872-2.105l.17-.31c.698-1.283-.705-2.686-1.987-1.987l-.311.169a1.464 1.464 0 01-2.105-.872l-.1-.34zM6 8a2 2 0 1 1 4 0 2 2 0 0 1-4 0z");
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
        if ("manage".equals(iconKey)) {
            path.setScaleX(1.8);
            path.setScaleY(1.8);
        } else {
            path.setScaleX(1.1);
            path.setScaleY(1.1);
        }
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
        sortedActivities = new SortedList<>(filteredActivities);
        sortedActivities.setComparator(Comparator.comparing(a -> Activity.parseStoredDateTime(a.getFrom()), Comparator.nullsLast(Comparator.naturalOrder())));
        TableView<Activity> table = new TableView<>(sortedActivities);

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
        table.setSortPolicy(null);

        contextAddActivityItem = menuItemWithIcon(i18n("menu.activity.add"), "add", this::addActivity);
        contextEditActivityItem = menuItemWithIcon(i18n("menu.activity.edit"), "edit", this::editActivity);
        contextEditActivityItem.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        contextAddPauseItem = menuItemWithIcon(i18n("menu.activity.addPause"), "add", this::addPause);
        contextAddPauseItem.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        contextDeleteActivityItem = menuItemWithIcon(i18n("menu.activity.delete"), "delete", this::deleteActivity);
        contextDeleteActivityItem.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        ContextMenu menu = new ContextMenu(contextAddActivityItem, contextEditActivityItem, contextAddPauseItem, contextDeleteActivityItem);
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

    /** Creates a TableView for a dynamic view tab: same columns and data as main table, own context menu. */
    private TableView<Activity> createViewActivityTable() {
        TableView<Activity> table = new TableView<>(sortedActivities);
        TableColumn<Activity, String> colCustomer = new TableColumn<>(i18n("table.customer"));
        colCustomer.setCellValueFactory(cell ->
            new SimpleStringProperty(resolveCustomerName(cell.getValue().getCustomerId())));
        TableColumn<Activity, String> colProject = new TableColumn<>(i18n("table.project"));
        colProject.setCellValueFactory(cell ->
            new SimpleStringProperty(resolveProjectName(cell.getValue().getProjectId())));
        TableColumn<Activity, String> colTask = new TableColumn<>(i18n("table.task"));
        colTask.setCellValueFactory(cell ->
            new SimpleStringProperty(resolveTaskName(cell.getValue().getTaskId())));
        TableColumn<Activity, String> colFrom = new TableColumn<>(i18n("table.from"));
        colFrom.setCellValueFactory(cell -> cell.getValue().fromProperty());
        TableColumn<Activity, String> colTo = new TableColumn<>(i18n("table.to"));
        colTo.setCellValueFactory(cell -> cell.getValue().toProperty());
        TableColumn<Activity, String> colDuration = new TableColumn<>(i18n("table.duration"));
        colDuration.setCellValueFactory(cell -> cell.getValue().durationProperty());
        TableColumn<Activity, String> colDailyTotal = new TableColumn<>(i18n("table.dailyTotal"));
        colDailyTotal.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null) {
                    setText(null);
                    return;
                }
                Activity activity = getTableRow().getItem();
                if (activity == null) {
                    setText(null);
                    return;
                }
                setText(getDailyTotalText(activity));
            }
        });
        table.getColumns().add(colCustomer);
        table.getColumns().add(colProject);
        table.getColumns().add(colTask);
        table.getColumns().add(colFrom);
        table.getColumns().add(colTo);
        table.getColumns().add(colDuration);
        table.getColumns().add(colDailyTotal);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setSortPolicy(null);

        MenuItem addItem = menuItemWithIcon(i18n("menu.activity.add"), "add", this::addActivity);
        MenuItem editItem = menuItemWithIcon(i18n("menu.activity.edit"), "edit", this::editActivity);
        editItem.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        MenuItem pauseItem = menuItemWithIcon(i18n("menu.activity.addPause"), "add", this::addPause);
        pauseItem.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        MenuItem deleteItem = menuItemWithIcon(i18n("menu.activity.delete"), "delete", this::deleteActivity);
        deleteItem.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        ContextMenu menu = new ContextMenu(addItem, editItem, pauseItem, deleteItem);
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

    private void updateTableOrCalendarVisibility() {
        // Tab content (main = header+filter+table, calendar = calendar) is switched by TabPane; nothing else to do here.
    }

    /** Creates a lightweight placeholder for the calendar tab. The real CalendarView is built on first tab switch (lazy loading). */
    private Node createCalendarPlaceholder() {
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(48, 48);
        Label label = new Label(i18n("calendar.loading"));
        VBox box = new VBox(12, spinner, label);
        box.setAlignment(Pos.CENTER);
        return new StackPane(box);
    }

    /** If the user switched to the calendar tab and the calendar was not yet built, create it asynchronously to keep UI responsive. */
    private void ensureCalendarContentCreated(int selectedIndex) {
        if (selectedIndex != CALENDAR_TAB_INDEX || calendarContentCreated) {
            return;
        }
        calendarContentCreated = true;
        Platform.runLater(() -> {
            Node cal = createCalendarContent();
            calendarViewNode = cal;
            calendarViewRef = cal instanceof CalendarView cv ? cv : null;
            if (calendarTab != null) {
                calendarTab.setContent(cal);
            }
        });
    }

    private Node createCalendarContent() {
        calendarSource = new CalendarSource(i18n("section.activities"));
        CalendarView calendarView = new CalendarView(Page.WEEK, Page.DAY, Page.MONTH, Page.YEAR);
        calendarView.getCalendarSources().clear();
        calendarView.getCalendarSources().add(calendarSource);
        calendarView.setShowAddCalendarButton(false);
        calendarView.setRequestedTime(LocalTime.now());
        calendarView.setWeekFields(WeekFields.of(settings.getFirstDayOfWeek(), 1));
        restoreCalendarPreferences(calendarView);
        calendarView.selectedPageProperty().addListener((obs, oldPage, newPage) -> saveCalendarPagePreference(newPage));
        wrapEntryDetailsPopOverToApplyCalendarColors(calendarView);
        Thread updateTimeThread = new Thread("Calendar: Update Time") {
            @Override
            public void run() {
                while (true) {
                    Platform.runLater(() -> {
                        calendarView.setToday(LocalDate.now());
                        calendarView.setTime(LocalTime.now());
                    });
                    try {
                        Thread.sleep(10_000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        };
        updateTimeThread.setPriority(Thread.MIN_PRIORITY);
        updateTimeThread.setDaemon(true);
        updateTimeThread.start();
        activities.addListener((ListChangeListener<Activity>) c -> syncAllCalendarsFromActivities());
        syncAllCalendarsFromActivities();

        // Entry view factories with tooltips (see https://github.com/dlsc-software-consulting-gmbh/CalendarFX/issues/133)
        DayView dayView = calendarView.getDayPage().getDetailedDayView().getDayView();
        dayView.setEntryViewFactory(entry -> new TooltipDayEntryView(entry, this::buildTooltipForEntry));

        WeekView weekView = calendarView.getWeekPage().getDetailedWeekView().getWeekView();
        weekView.setWeekDayViewFactory(param -> {
            WeekDayView wdv = new WeekDayView();
            wdv.setEntryViewFactory(entry -> new TooltipDayEntryView(entry, this::buildTooltipForEntry));
            return wdv;
        });

        MonthView monthView = calendarView.getMonthPage().getMonthView();
        monthView.setEntryViewFactory(entry -> new TooltipMonthEntryView(entry, this::buildTooltipForEntry));

        return calendarView;
    }

    /** One CalendarFX calendar per (customer, project); styles and colors from customer calendar color. */
    private void syncAllCalendarsFromActivities() {
        if (calendarSource == null || calendarsByCustomerProject == null) {
            return;
        }
        for (Calendar<Activity> cal : calendarsByCustomerProject.values()) {
            cal.startBatchUpdates();
            cal.clear();
        }
        calendarEntries.clear();
        Calendar.Style[] styles = Calendar.Style.values();
        ZoneId zone = ZoneId.systemDefault();
        Map<String, List<Activity>> slotGroupsByKey = new HashMap<>();
        for (Activity activity : activities) {
            LocalDateTime from = Activity.parseStoredDateTime(activity.getFrom());
            LocalDateTime to = Activity.parseStoredDateTime(activity.getTo());
            if (from == null || to == null) continue;
            String slotKey = activity.getCustomerId() + "|" + activity.getProjectId() + "|" + from + "|" + to;
            slotGroupsByKey.computeIfAbsent(slotKey, k -> new ArrayList<>()).add(activity);
        }
        Set<String> cpKeySet = new HashSet<>();
        for (Map.Entry<String, List<Activity>> e : slotGroupsByKey.entrySet()) {
            Activity first = e.getValue().get(0);
            cpKeySet.add(first.getCustomerId() + "|" + first.getProjectId());
        }
        List<String> distinctCpKeys = new ArrayList<>(cpKeySet);
        Collections.sort(distinctCpKeys);
        Map<String, Calendar.Style> styleByCpKey = new HashMap<>();
        Map<String, Integer> styleIndexByCpKey = new HashMap<>();
        List<String> hexColorsForOverride = new ArrayList<>(7);
        for (int i = 0; i < distinctCpKeys.size(); i++) {
            String cpKey = distinctCpKeys.get(i);
            styleByCpKey.put(cpKey, styles[i % styles.length]);
            styleIndexByCpKey.put(cpKey, i % 7);
            if (i < 7) {
                String customerId = cpKey.substring(0, cpKey.indexOf('|'));
                Customer customer = findCustomerById(customerId);
                String hex = customer != null ? customer.getCalendarColorHex() : CalendarColorPalette.DEFAULT.getHexColor(i);
                hexColorsForOverride.add(hex);
            }
        }
        for (int i = hexColorsForOverride.size(); i < 7; i++) {
            hexColorsForOverride.add(CalendarColorPalette.DEFAULT.getHexColor(i));
        }
        Scene scene = primaryStage != null ? primaryStage.getScene() : null;
        if (scene != null) {
            applyCalendarColorPalette(scene, hexColorsForOverride);
        }
        List<Map.Entry<String, List<Activity>>> slotOrder = new ArrayList<>(slotGroupsByKey.entrySet());
        slotOrder.sort(Comparator.comparing(e -> e.getValue().get(0).getCustomerId() + "|" + e.getValue().get(0).getProjectId()));
        for (Map.Entry<String, List<Activity>> slotEntry : slotOrder) {
            List<Activity> group = slotEntry.getValue();
            if (group.isEmpty()) continue;
            Activity first = group.get(0);
            LocalDateTime from = Activity.parseStoredDateTime(first.getFrom());
            LocalDateTime to = Activity.parseStoredDateTime(first.getTo());
            if (from == null || to == null) continue;
            String cpKey = first.getCustomerId() + "|" + first.getProjectId();
            Calendar<Activity> cal = calendarsByCustomerProject.get(cpKey);
            if (cal == null) {
                String name = buildCustomerProjectLabel(first);
                cal = new Calendar<>(name);
                cal.setStyle(styleByCpKey.getOrDefault(cpKey, styles[0]));
                cal.setReadOnly(true);
                calendarsByCustomerProject.put(cpKey, cal);
            }
            String title = group.size() > 1
                ? buildGroupedActivityTitleForCalendar(first, group.size())
                : buildActivityTitleForCalendar(first);
            Entry<Activity> entry = new Entry<>(title);
            entry.setInterval(new Interval(from, to, zone));
            entry.setUserObject(first);
            int styleIdx = styleIndexByCpKey.getOrDefault(cpKey, 0);
            entry.getStyleClass().add("jtimesheet-entry-color-" + styleIdx);
            cal.addEntry(entry);
            calendarEntries.add(entry);
        }
        for (Calendar<Activity> cal : calendarsByCustomerProject.values()) {
            cal.stopBatchUpdates();
        }
        Platform.runLater(this::attachCalendarsToView);
    }

    private String buildCustomerProjectLabel(Activity activity) {
        String c = resolveCustomerName(activity.getCustomerId());
        String p = resolveProjectName(activity.getProjectId());
        if (c != null && p != null) return c + " / " + p;
        if (c != null) return c;
        return p != null ? p : i18n("section.activities");
    }

    private void attachCalendarsToView() {
        if (calendarSource == null) return;
        calendarSource.getCalendars().clear();
        List<String> orderedKeys = new ArrayList<>(calendarsByCustomerProject.keySet());
        Collections.sort(orderedKeys);
        for (String key : orderedKeys) {
            calendarSource.getCalendars().add(calendarsByCustomerProject.get(key));
        }
        tryRestoreCalendarVisibilityPreference();
    }

    private static final String PREF_CALENDAR_PAGE = "calendar.page";
    private static final String PREF_CALENDAR_VISIBLE = "calendar.visible";

    private void saveCalendarPagePreference(Page page) {
        if (page != null) {
            preferences.put(PREF_CALENDAR_PAGE, page.name());
        }
    }

    private void restoreCalendarPreferences(CalendarView calendarView) {
        String pageName = preferences.get(PREF_CALENDAR_PAGE, Page.WEEK.name());
        try {
            Page page = Page.valueOf(pageName);
            if (calendarView.getAvailablePages().contains(page)) {
                switch (page) {
                    case DAY: calendarView.showDayPage(); break;
                    case WEEK: calendarView.showWeekPage(); break;
                    case MONTH: calendarView.showMonthPage(); break;
                    case YEAR: calendarView.showYearPage(); break;
                    default: break;
                }
            }
        } catch (Exception ignored) {
            calendarView.showWeekPage();
        }
    }

    /** Calendar visibility (which calendars are shown in the tray) is not exposed by CalendarFX public API; placeholder for future. */
    private void tryRestoreCalendarVisibilityPreference() {
        String saved = preferences.get(PREF_CALENDAR_VISIBLE, "");
        if (saved.isEmpty()) return;
    }

    /** Wraps the entry details popover content callback so the popover (opened on double-click) gets our calendar color stylesheet. */
    private void wrapEntryDetailsPopOverToApplyCalendarColors(CalendarView calendarView) {
        Callback<DateControl.EntryDetailsPopOverContentParameter, Node> defaultCallback =
            calendarView.getEntryDetailsPopOverContentCallback();
        calendarView.setEntryDetailsPopOverContentCallback(param -> {
            Node content = defaultCallback != null ? defaultCallback.call(param) : null;
            if (param != null && calendarOverrideStylesheetUrl != null) {
                PopOver popOver = param.getPopOver();
                if (popOver != null && popOver.getRoot() != null) {
                    popOver.getRoot().getStylesheets().add(calendarOverrideStylesheetUrl);
                }
            }
            return content;
        });
    }

    /**
     * Applies calendar colors via a stylesheet override.
     * CalendarFX uses CSS variables -style1-color … -style7-color for entries, the calendar list
     * (button top left), and search/popover. We override these variables on .root so that the
     * list and search use our customer colors. We also add per-entry classes (jtimesheet-entry-color-0 … 6)
     * so entry appearance is fully controlled regardless of CalendarFX defaults.
     * @param hexColors list of 7 hex colors (style 1–7), or empty to remove override
     */
    private void applyCalendarColorPalette(Scene scene, List<String> hexColors) {
        if (scene == null) {
            return;
        }
        var stylesheets = scene.getStylesheets();
        if (calendarOverrideStylesheetUrl != null) {
            stylesheets.remove(calendarOverrideStylesheetUrl);
            if (calendarViewRef != null) {
                calendarViewRef.getStylesheets().remove(calendarOverrideStylesheetUrl);
            }
            calendarOverrideStylesheetUrl = null;
        }
        if (hexColors == null || hexColors.size() < 7) {
            return;
        }
        StringBuilder css = new StringBuilder();
        css.append("/* Override CalendarFX -style1-color … -style7-color so list, search and entries use customer colors */\n");
        css.append(".root, * {\n");
        for (int i = 0; i < 7; i++) {
            css.append("  -style").append(i + 1).append("-color: ").append(hexColors.get(i)).append(";\n");
        }
        css.append("}\n");
        css.append(".calendar-view, .calendar-view * {\n");
        for (int i = 0; i < 7; i++) {
            css.append("  -style").append(i + 1).append("-color: ").append(hexColors.get(i)).append(";\n");
        }
        css.append("}\n\n");
        /* Search result view adds calendar.getStyle() + "-icon" to the color circle (e.g. STYLE1-icon); set fill/stroke so the circle uses our palette */
        for (int i = 0; i < 7; i++) {
            String hex = hexColors.get(i);
            int n = i + 1;
            css.append(".STYLE").append(n).append("-icon, .style").append(n).append("-icon { -fx-fill: ").append(hex).append("; -fx-stroke: ").append(hex).append("; }\n");
        }
        css.append("\n");
        for (int i = 0; i < 7; i++) {
            String hex = hexColors.get(i);
            String cls = "jtimesheet-entry-color-" + i;
            css.append(".").append(cls).append(" {\n");
            css.append("  -fx-background-color: ").append(hex).append(" !important;\n");
            css.append("  -fx-border-color: derive(").append(hex).append(", -25%) !important;\n");
            css.append("}\n");
            css.append(".").append(cls).append(" .start-time-label, .").append(cls).append(" .title-label {\n");
            css.append("  -fx-text-fill: derive(").append(hex).append(", -45%) !important;\n");
            css.append("}\n");
        }
        try {
            Path tmp = Files.createTempFile("jtimesheet-calendar-colors-", ".css");
            Files.writeString(tmp, css.toString());
            String url = tmp.toUri().toASCIIString();
            stylesheets.add(url);
            if (calendarViewRef != null) {
                calendarViewRef.getStylesheets().add(url);
            }
            calendarOverrideStylesheetUrl = url;
        } catch (IOException ignored) {
            // Keep previous or default colors
        }
    }

    private String buildGroupedActivityTitleForCalendar(Activity representative, int count) {
        String customer = resolveCustomerName(representative.getCustomerId());
        String project = resolveProjectName(representative.getProjectId());
        if (customer != null && project != null) {
            return customer + " / " + project + " " + i18n("calendar.entry.count", count);
        }
        if (customer != null) {
            return customer + " " + i18n("calendar.entry.count", count);
        }
        return i18n("section.activities") + " " + i18n("calendar.entry.count", count);
    }

    private String buildActivityTitleForCalendar(Activity activity) {
        String customer = resolveCustomerName(activity.getCustomerId());
        String project = resolveProjectName(activity.getProjectId());
        String task = resolveTaskName(activity.getTaskId());
        if (customer != null && project != null && task != null) {
            return customer + " / " + project + " / " + task;
        }
        if (customer != null && project != null) {
            return customer + " / " + project;
        }
        if (customer != null) {
            return customer;
        }
        return task != null ? task : (project != null ? project : i18n("section.activities"));
    }

    private long totalMinutesForDay(LocalDate date) {
        long sum = 0;
        for (Activity a : activities) {
            LocalDateTime from = Activity.parseStoredDateTime(a.getFrom());
            LocalDateTime to = Activity.parseStoredDateTime(a.getTo());
            if (from != null && to != null && !to.isBefore(from) && from.toLocalDate().equals(date)) {
                sum += Duration.between(from, to).toMinutes();
            }
        }
        return sum;
    }

    private long totalMinutesForDayByCustomer(LocalDate date, String customerId) {
        long sum = 0;
        for (Activity a : activities) {
            if (!java.util.Objects.equals(a.getCustomerId(), customerId)) continue;
            LocalDateTime from = Activity.parseStoredDateTime(a.getFrom());
            LocalDateTime to = Activity.parseStoredDateTime(a.getTo());
            if (from != null && to != null && !to.isBefore(from) && from.toLocalDate().equals(date)) {
                sum += Duration.between(from, to).toMinutes();
            }
        }
        return sum;
    }

    private long totalMinutesForDayByCustomerProject(LocalDate date, String customerId, String projectId) {
        long sum = 0;
        for (Activity a : activities) {
            if (!java.util.Objects.equals(a.getCustomerId(), customerId) || !java.util.Objects.equals(a.getProjectId(), projectId)) continue;
            LocalDateTime from = Activity.parseStoredDateTime(a.getFrom());
            LocalDateTime to = Activity.parseStoredDateTime(a.getTo());
            if (from != null && to != null && !to.isBefore(from) && from.toLocalDate().equals(date)) {
                sum += Duration.between(from, to).toMinutes();
            }
        }
        return sum;
    }

    private String buildCalendarEntryTooltipText(Entry<Activity> entry) {
        Activity first = entry.getUserObject();
        Interval interval = entry.getInterval();
        StringBuilder sb = new StringBuilder();
        sb.append(buildActivityTitleForCalendar(first));
        if (interval != null) {
            sb.append("\n");
            DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
            sb.append(interval.getStartDateTime().format(timeFmt))
                .append(" – ")
                .append(interval.getEndDateTime().format(timeFmt));
            long min = Duration.between(interval.getStartDateTime(), interval.getEndDateTime()).toMinutes();
            sb.append("  (").append(formatMinutes(min)).append(")");
            LocalDate day = interval.getStartDateTime().toLocalDate();
            sb.append("\n").append(i18n("calendar.tooltip.projectTotal", formatMinutes(totalMinutesForDayByCustomerProject(day, first.getCustomerId(), first.getProjectId()))));
            sb.append("\n").append(i18n("calendar.tooltip.customerTotal", formatMinutes(totalMinutesForDayByCustomer(day, first.getCustomerId()))));
            sb.append("\n").append(i18n("calendar.tooltip.dayTotal", formatMinutes(totalMinutesForDay(day))));
        }
        return sb.toString();
    }

    /** Builds tooltip text for any entry; used by calendar entry view factories. */
    private String buildTooltipForEntry(Entry<?> entry) {
        if (entry.getUserObject() instanceof Activity) {
            @SuppressWarnings("unchecked")
            Entry<Activity> actEntry = (Entry<Activity>) entry;
            return buildCalendarEntryTooltipText(actEntry);
        }
        String loc = entry.getLocation();
        return entry.getTitle() + (loc != null && !loc.isEmpty() ? "\n" + loc : "");
    }

    private VBox createFilterPanel() {
        customerFilter = new ComboBox<>(customers);
        customerFilter.setPromptText(i18n("filter.customer.placeholder"));
        customerFilter.setPrefWidth(180);
        customerFilter.setButtonCell(createCustomerFilterCell(i18n("filter.customer.placeholder")));
        customerFilter.setCellFactory(listView -> createCustomerFilterCell(i18n("filter.customer.placeholder")));

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
            if (!applyingPreset) {
                presetFilter.getSelectionModel().select(PresetRange.CUSTOM);
            }
            applyFilters();
            saveFilterPreferences();
        });
        toFilter.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!applyingPreset) {
                presetFilter.getSelectionModel().select(PresetRange.CUSTOM);
            }
            applyFilters();
            saveFilterPreferences();
        });

        presetFilter.valueProperty().addListener((obs, oldValue, newValue) -> {
            applyingPreset = true;
            try {
                applyPreset(newValue);
                updateMainPeriodNav();
                applyFilters();
                saveFilterPreferences();
            } finally {
                applyingPreset = false;
            }
        });

        periodNavLabel = new Label();
        periodPrevButton = new Button(i18n("filter.period.prev"));
        periodPrevButton.setOnAction(e -> shiftMainPeriod(-1));
        periodNextButton = new Button(i18n("filter.period.next"));
        periodNextButton.setOnAction(e -> shiftMainPeriod(1));
        periodNavBox = new HBox(6, periodPrevButton, periodNavLabel, periodNextButton);
        periodNavBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        updateProjectFilter(null);
        updateTaskFilter(null, null);

        fromFilter.valueProperty().addListener((obs, o, n) -> updateMainPeriodNav());
        toFilter.valueProperty().addListener((obs, o, n) -> updateMainPeriodNav());

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
            periodNavBox,
            clearDatesButton
        );

        VBox panel = new VBox(8, row1, row2, row3);
        panel.setPadding(new Insets(6, 0, 6, 0));
        return panel;
    }

    private VBox createViewFilterPanel(ViewTabState state) {
        Label fixedFilterLabel = new Label(viewTabStateDescription(state));
        fixedFilterLabel.setStyle("-fx-font-weight: bold;");

        ListView<Task> viewTaskFilter = new ListView<>();
        viewTaskFilter.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        viewTaskFilter.setFixedCellSize(24);
        viewTaskFilter.setPrefHeight(viewTaskFilter.getFixedCellSize() * 6 + 6);
        viewTaskFilter.setPrefWidth(180);
        ObservableList<Task> tasksForView = tasksForViewState(state);
        viewTaskFilter.setItems(tasksForView);
        viewTaskFilter.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Task item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        for (String id : state.getSelectedTaskIds()) {
            for (Task t : tasksForView) {
                if (t.getId().equals(id)) {
                    viewTaskFilter.getSelectionModel().select(t);
                    break;
                }
            }
        }
        viewTaskFilter.getSelectionModel().getSelectedItems().addListener((ListChangeListener<Task>) c -> {
            List<String> ids = new ArrayList<>();
            for (Task t : viewTaskFilter.getSelectionModel().getSelectedItems()) {
                ids.add(t.getId());
            }
            state.setSelectedTaskIds(ids);
            applyFilters();
            saveViewTabsPreferences();
        });

        DatePicker viewFrom = new DatePicker(state.getFromDate());
        DatePicker viewTo = new DatePicker(state.getToDate());
        ComboBox<PresetRange> viewPreset = new ComboBox<>();
        viewPreset.getItems().setAll(PresetRange.values());
        viewPreset.setValue(PresetRange.fromPreference(state.getPresetKey()));
        viewPreset.setPrefWidth(140);
        viewPreset.setButtonCell(createPresetCell());
        viewPreset.setCellFactory(listView -> createPresetCell());

        Button viewClearTasks = new Button(i18n("filter.clear.tasks"));
        viewClearTasks.setOnAction(e -> {
            viewTaskFilter.getSelectionModel().clearSelection();
            state.setSelectedTaskIds(new ArrayList<>());
            applyFilters();
            saveViewTabsPreferences();
        });
        Button viewClearDates = new Button(i18n("filter.clear.dates"));
        viewClearDates.setOnAction(e -> {
            viewFrom.setValue(null);
            viewTo.setValue(null);
            viewPreset.getSelectionModel().select(PresetRange.CUSTOM);
            state.setFromDate(null);
            state.setToDate(null);
            state.setPresetKey(PresetRange.CUSTOM.name());
            applyFilters();
            saveViewTabsPreferences();
        });

        final boolean[] applyingViewPreset = { false };
        viewFrom.valueProperty().addListener((obs, o, n) -> {
            state.setFromDate(n);
            if (!applyingViewPreset[0]) {
                viewPreset.getSelectionModel().select(PresetRange.CUSTOM);
                state.setPresetKey(PresetRange.CUSTOM.name());
            }
            applyFilters();
            saveViewTabsPreferences();
        });
        viewTo.valueProperty().addListener((obs, o, n) -> {
            state.setToDate(n);
            if (!applyingViewPreset[0]) {
                viewPreset.getSelectionModel().select(PresetRange.CUSTOM);
                state.setPresetKey(PresetRange.CUSTOM.name());
            }
            applyFilters();
            saveViewTabsPreferences();
        });

        Label viewPeriodLabel = new Label();
        Button viewPeriodPrev = new Button(i18n("filter.period.prev"));
        Button viewPeriodNext = new Button(i18n("filter.period.next"));
        HBox viewPeriodNavBox = new HBox(6, viewPeriodPrev, viewPeriodLabel, viewPeriodNext);
        viewPeriodNavBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        viewPeriodPrev.setOnAction(e -> {
            PresetRange p = viewPreset.getValue();
            LocalDate from = viewFrom.getValue();
            LocalDate to = viewTo.getValue();
            if (p == PresetRange.DAY && from != null && to != null) {
                applyingViewPreset[0] = true;
                viewFrom.setValue(from.plusDays(-1));
                viewTo.setValue(to.plusDays(-1));
                state.setFromDate(viewFrom.getValue());
                state.setToDate(viewTo.getValue());
                applyingViewPreset[0] = false;
                updateViewPeriodNavLabel(viewPeriodLabel, viewPeriodNavBox, p, viewFrom.getValue(), viewTo.getValue());
                applyFilters();
                saveViewTabsPreferences();
            } else if (p == PresetRange.WEEK && from != null && to != null) {
                applyingViewPreset[0] = true;
                viewFrom.setValue(from.plusWeeks(-1));
                viewTo.setValue(to.plusWeeks(-1));
                state.setFromDate(viewFrom.getValue());
                state.setToDate(viewTo.getValue());
                applyingViewPreset[0] = false;
                updateViewPeriodNavLabel(viewPeriodLabel, viewPeriodNavBox, p, viewFrom.getValue(), viewTo.getValue());
                applyFilters();
                saveViewTabsPreferences();
            } else if (p == PresetRange.MONTH && from != null && to != null) {
                applyingViewPreset[0] = true;
                viewFrom.setValue(from.plusMonths(-1));
                viewTo.setValue(to.plusMonths(-1));
                state.setFromDate(viewFrom.getValue());
                state.setToDate(viewTo.getValue());
                applyingViewPreset[0] = false;
                updateViewPeriodNavLabel(viewPeriodLabel, viewPeriodNavBox, p, viewFrom.getValue(), viewTo.getValue());
                applyFilters();
                saveViewTabsPreferences();
            }
        });
        viewPeriodNext.setOnAction(e -> {
            PresetRange p = viewPreset.getValue();
            LocalDate from = viewFrom.getValue();
            LocalDate to = viewTo.getValue();
            if (p == PresetRange.DAY && from != null && to != null) {
                applyingViewPreset[0] = true;
                viewFrom.setValue(from.plusDays(1));
                viewTo.setValue(to.plusDays(1));
                state.setFromDate(viewFrom.getValue());
                state.setToDate(viewTo.getValue());
                applyingViewPreset[0] = false;
                updateViewPeriodNavLabel(viewPeriodLabel, viewPeriodNavBox, p, viewFrom.getValue(), viewTo.getValue());
                applyFilters();
                saveViewTabsPreferences();
            } else if (p == PresetRange.WEEK && from != null && to != null) {
                applyingViewPreset[0] = true;
                viewFrom.setValue(from.plusWeeks(1));
                viewTo.setValue(to.plusWeeks(1));
                state.setFromDate(viewFrom.getValue());
                state.setToDate(viewTo.getValue());
                applyingViewPreset[0] = false;
                updateViewPeriodNavLabel(viewPeriodLabel, viewPeriodNavBox, p, viewFrom.getValue(), viewTo.getValue());
                applyFilters();
                saveViewTabsPreferences();
            } else if (p == PresetRange.MONTH && from != null && to != null) {
                applyingViewPreset[0] = true;
                viewFrom.setValue(from.plusMonths(1));
                viewTo.setValue(to.plusMonths(1));
                state.setFromDate(viewFrom.getValue());
                state.setToDate(viewTo.getValue());
                applyingViewPreset[0] = false;
                updateViewPeriodNavLabel(viewPeriodLabel, viewPeriodNavBox, p, viewFrom.getValue(), viewTo.getValue());
                applyFilters();
                saveViewTabsPreferences();
            }
        });

        viewPreset.valueProperty().addListener((obs, o, n) -> {
            if (n == null) return;
            state.setPresetKey(n.name());
            applyingViewPreset[0] = true;
            try {
                LocalDate today = LocalDate.now();
                DayOfWeek firstDay = settings.getFirstDayOfWeek();
                switch (n) {
                case DAY -> { viewFrom.setValue(today); viewTo.setValue(today); state.setFromDate(today); state.setToDate(today); }
                case WEEK -> {
                    LocalDate start = startOfWeek(today, firstDay);
                    LocalDate end = endOfWeek(today, firstDay);
                    viewFrom.setValue(start); viewTo.setValue(end); state.setFromDate(start); state.setToDate(end);
                }
                case MONTH -> {
                    LocalDate start = today.with(TemporalAdjusters.firstDayOfMonth());
                    LocalDate end = today.with(TemporalAdjusters.lastDayOfMonth());
                    viewFrom.setValue(start); viewTo.setValue(end); state.setFromDate(start); state.setToDate(end);
                }
                default -> { }
                }
                updateViewPeriodNavLabel(viewPeriodLabel, viewPeriodNavBox, viewPreset.getValue(), viewFrom.getValue(), viewTo.getValue());
                applyFilters();
                saveViewTabsPreferences();
            } finally {
                applyingViewPreset[0] = false;
            }
        });

        viewFrom.valueProperty().addListener((obs, o, n) -> updateViewPeriodNavLabel(viewPeriodLabel, viewPeriodNavBox, viewPreset.getValue(), viewFrom.getValue(), viewTo.getValue()));
        viewTo.valueProperty().addListener((obs, o, n) -> updateViewPeriodNavLabel(viewPeriodLabel, viewPeriodNavBox, viewPreset.getValue(), viewFrom.getValue(), viewTo.getValue()));

        updateViewPeriodNavLabel(viewPeriodLabel, viewPeriodNavBox, viewPreset.getValue(), viewFrom.getValue(), viewTo.getValue());

        Node tasksRow;
        if (state.getFixedTask() != null) {
            tasksRow = new HBox(10, new Label(i18n("filter.tasks.label")), new Label(state.getFixedTask().getName()));
        } else {
            tasksRow = new HBox(10, new Label(i18n("filter.tasks.label")), viewTaskFilter, viewClearTasks);
        }
        HBox datesRow = new HBox(10,
            new Label(i18n("filter.from.label")), viewFrom,
            new Label(i18n("filter.to.label")), viewTo,
            new Label(i18n("filter.preset.label")), viewPreset,
            viewPeriodNavBox,
            viewClearDates
        );
        VBox panel = new VBox(8, fixedFilterLabel, tasksRow, datesRow);
        panel.setPadding(new Insets(6, 0, 6, 0));
        return panel;
    }

    private String viewTabStateDescription(ViewTabState state) {
        if (state.getFixedCustomer() == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(i18n("filter.customer.label")).append(" ").append(state.getFixedCustomer().getName());
        if (state.getFixedProject() != null) {
            sb.append(" — ").append(i18n("filter.project.label")).append(" ").append(state.getFixedProject().getName());
            if (state.getFixedTask() != null) {
                sb.append(" — ").append(i18n("filter.tasks.label")).append(" ").append(state.getFixedTask().getName());
            }
        }
        return sb.toString();
    }

    private ObservableList<Task> tasksForViewState(ViewTabState state) {
        ObservableList<Task> out = FXCollections.observableArrayList();
        if (state.getFixedProject() != null) {
            out.addAll(state.getFixedProject().getTasks());
        } else if (state.getFixedCustomer() != null) {
            for (Project p : state.getFixedCustomer().getProjects()) {
                out.addAll(p.getTasks());
            }
        }
        return out;
    }

    private void openNewViewDialog() {
        if (customers.isEmpty()) {
            showInfo(i18n("view.new.no.customers"));
            return;
        }
        ComboBox<ViewLevel> levelCombo = new ComboBox<>();
        levelCombo.getItems().setAll(ViewLevel.values());
        levelCombo.setValue(ViewLevel.CUSTOMER);
        levelCombo.setPrefWidth(220);
        levelCombo.setButtonCell(viewLevelCell());
        levelCombo.setCellFactory(lv -> viewLevelCell());

        ComboBox<Customer> customerCombo = new ComboBox<>(FXCollections.observableArrayList(customers));
        customerCombo.setPromptText(i18n("filter.customer.placeholder"));
        customerCombo.setPrefWidth(220);
        customerCombo.setButtonCell(createCustomerFilterCell(i18n("filter.customer.placeholder")));
        customerCombo.setCellFactory(lv -> createCustomerFilterCell(i18n("filter.customer.placeholder")));

        ComboBox<Project> projectCombo = new ComboBox<>();
        projectCombo.setPrefWidth(220);
        projectCombo.setButtonCell(createProjectFilterCell(i18n("filter.project.placeholder")));
        projectCombo.setCellFactory(lv -> createProjectFilterCell(i18n("filter.project.placeholder")));

        ComboBox<Task> taskCombo = new ComboBox<>();
        taskCombo.setPrefWidth(220);
        taskCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Task item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? i18n("filter.tasks.label") : item.getName());
            }
        });

        levelCombo.valueProperty().addListener((obs, o, level) -> {
            Customer c = customerCombo.getValue();
            if (level == ViewLevel.CUSTOMER) {
                projectCombo.setItems(FXCollections.observableArrayList());
                projectCombo.setValue(null);
                taskCombo.setItems(FXCollections.observableArrayList());
                taskCombo.setValue(null);
            } else if (level == ViewLevel.CUSTOMER_PROJECT) {
                if (c != null) {
                    projectCombo.setItems(FXCollections.observableArrayList(c.getProjects()));
                    projectCombo.setValue(null);
                } else {
                    projectCombo.setItems(FXCollections.observableArrayList());
                }
                taskCombo.setItems(FXCollections.observableArrayList());
                taskCombo.setValue(null);
            } else {
                Project p = projectCombo.getValue();
                if (p != null) {
                    taskCombo.setItems(FXCollections.observableArrayList(p.getTasks()));
                    taskCombo.setValue(null);
                } else {
                    taskCombo.setItems(FXCollections.observableArrayList());
                }
            }
        });
        customerCombo.valueProperty().addListener((obs, o, c) -> {
            ViewLevel level = levelCombo.getValue();
            if (c == null) {
                projectCombo.setItems(FXCollections.observableArrayList());
                taskCombo.setItems(FXCollections.observableArrayList());
            } else if (level == ViewLevel.CUSTOMER_PROJECT || level == ViewLevel.CUSTOMER_PROJECT_TASK) {
                projectCombo.setItems(FXCollections.observableArrayList(c.getProjects()));
                projectCombo.setValue(null);
                taskCombo.setItems(FXCollections.observableArrayList());
            }
        });
        projectCombo.valueProperty().addListener((obs, o, p) -> {
            if (levelCombo.getValue() == ViewLevel.CUSTOMER_PROJECT_TASK && p != null) {
                taskCombo.setItems(FXCollections.observableArrayList(p.getTasks()));
                taskCombo.setValue(null);
            }
        });

        VBox form = new VBox(10,
            new Label(i18n("view.new.level")), levelCombo,
            new Label(i18n("filter.customer.label")), customerCombo,
            new Label(i18n("filter.project.label")), projectCombo,
            new Label(i18n("view.new.task")), taskCombo
        );
        form.setPadding(new Insets(20));

        Alert dialog = new Alert(AlertType.NONE);
        dialog.setTitle(i18n("view.new.title"));
        dialog.setHeaderText(i18n("view.new.header"));
        dialog.getDialogPane().setContent(form);
        ButtonType ok = new ButtonType(i18n("button.save"), ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        addEscapeToClose(dialog);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ok) {
            return;
        }
        Customer selectedCustomer = customerCombo.getValue();
        if (selectedCustomer == null) {
            showInfo(i18n("view.new.select.customer"));
            return;
        }
        ViewLevel level = levelCombo.getValue();
        Project selectedProject = level == ViewLevel.CUSTOMER ? null : projectCombo.getValue();
        if (level != ViewLevel.CUSTOMER && selectedProject == null) {
            showInfo(i18n("view.new.select.project"));
            return;
        }
        Task selectedTask = level == ViewLevel.CUSTOMER_PROJECT_TASK ? taskCombo.getValue() : null;
        if (level == ViewLevel.CUSTOMER_PROJECT_TASK && selectedTask == null) {
            showInfo(i18n("view.new.select.task"));
            return;
        }
        ViewTabState newState = new ViewTabState(level, selectedCustomer, selectedProject, selectedTask);
        viewTabStates.add(newState);
        TableView<Activity> viewTable = createViewActivityTable();
        ViewTabData tabData = new ViewTabData(newState, viewTable);
        Tab tab = new Tab(newState.getTabTitle());
        tab.setUserData(tabData);
        tab.setClosable(true);
        VBox viewContent = new VBox(10, createViewFilterPanel(newState), viewTable);
        VBox.setVgrow(viewTable, Priority.ALWAYS);
        tab.setContent(viewContent);
        viewTabPane.getTabs().add(tab);
        viewTabPane.getSelectionModel().select(tab);
        applyFilters();
        updateWriteTimesheetButtonForTab();
        saveViewTabsPreferences();
    }

    private ListCell<ViewLevel> viewLevelCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(ViewLevel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(i18n("view.level." + item.name()));
                }
            }
        };
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
        if (PresetRange.isRelativePreset(presetValue)) {
            applyRelativePresetFromRaw(presetValue, (from, to) -> {
                fromFilter.setValue(from);
                toFilter.setValue(to);
            });
        } else {
            fromFilter.setValue(fromValue != null && !fromValue.isBlank() ? LocalDate.parse(fromValue) : null);
            toFilter.setValue(toValue != null && !toValue.isBlank() ? LocalDate.parse(toValue) : null);
        }
        presetFilter.getSelectionModel().select(preset);
        updateMainPeriodNav();
        applyFilters();
        restoringPreferences = false;
    }

    private void saveViewTabsPreferences() {
        if (restoringViewTabs || viewTabPane == null) {
            return;
        }
        int count = viewTabStates.size();
        preferences.putInt("view.count", count);
        preferences.putInt("view.selectedIndex", viewTabPane.getSelectionModel().getSelectedIndex());
        for (int i = 0; i < count; i++) {
            ViewTabState state = viewTabStates.get(i);
            String prefix = "view." + i + ".";
            preferences.put(prefix + "level", state.getLevel().name());
            preferences.put(prefix + "customerId", state.getFixedCustomer() != null ? state.getFixedCustomer().getId() : "");
            preferences.put(prefix + "projectId", state.getFixedProject() != null ? state.getFixedProject().getId() : "");
            preferences.put(prefix + "taskId", state.getFixedTask() != null ? state.getFixedTask().getId() : "");
            preferences.put(prefix + "taskIds", String.join(",", state.getSelectedTaskIds()));
            preferences.put(prefix + "from", state.getFromDate() != null ? state.getFromDate().toString() : "");
            preferences.put(prefix + "to", state.getToDate() != null ? state.getToDate().toString() : "");
            preferences.put(prefix + "preset", state.getPresetKey() != null ? state.getPresetKey() : PresetRange.CUSTOM.name());
        }
        removeOrphanedViewPreferences(count);
    }

    /** Removes view.N.* keys where N >= count (leftover from when the user had more tabs and closed some). */
    private void removeOrphanedViewPreferences(int count) {
        try {
            for (String key : preferences.keys()) {
                if (key.startsWith("view.") && key.length() > "view.".length()) {
                    int dot = key.indexOf('.', "view.".length());
                    if (dot > 0) {
                        String numPart = key.substring("view.".length(), dot);
                        if (numPart.matches("\\d+")) {
                            int index = Integer.parseInt(numPart);
                            if (index >= count) {
                                preferences.remove(key);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) { }
    }

    private void restoreViewTabsPreferences() {
        if (viewTabPane == null) {
            return;
        }
        restoringViewTabs = true;
        // Keep main tab and calendar tab; remove view tabs so we don't duplicate when called from loadData()
        while (viewTabPane.getTabs().size() > 2) {
            viewTabPane.getTabs().remove(viewTabPane.getTabs().size() - 1);
        }
        viewTabStates.clear();
        int count = preferences.getInt("view.count", 0);
        for (int i = 0; i < count; i++) {
            String prefix = "view." + i + ".";
            String levelStr = preferences.get(prefix + "level", ViewLevel.CUSTOMER.name());
            String customerId = preferences.get(prefix + "customerId", "");
            String projectId = preferences.get(prefix + "projectId", "");
            String taskId = preferences.get(prefix + "taskId", "");
            String taskIdsStr = preferences.get(prefix + "taskIds", "");
            String fromStr = preferences.get(prefix + "from", "");
            String toStr = preferences.get(prefix + "to", "");
            String presetStr = preferences.get(prefix + "preset", PresetRange.CUSTOM.name());

            ViewLevel level;
            try {
                level = ViewLevel.valueOf(levelStr);
            } catch (Exception e) {
                level = ViewLevel.CUSTOMER;
            }
            Customer customer = findCustomerById(customerId);
            if (customer == null && !customerId.isBlank()) {
                customer = findCustomerByName(customerId);
            }
            if (customer == null) {
                continue;
            }
            Project project = null;
            if (level != ViewLevel.CUSTOMER && !projectId.isBlank()) {
                project = findProjectById(customer, projectId);
                if (project == null) {
                    project = findProjectByName(customer, projectId);
                }
            }
            if (level == ViewLevel.CUSTOMER_PROJECT && project == null) {
                continue;
            }
            Task task = null;
            if (level == ViewLevel.CUSTOMER_PROJECT_TASK && project != null && !taskId.isBlank()) {
                task = findTaskById(project, taskId);
                if (task == null) {
                    task = findTaskByName(project, taskId);
                }
            }
            if (level == ViewLevel.CUSTOMER_PROJECT_TASK && task == null) {
                continue;
            }

            ViewTabState state = new ViewTabState(level, customer, project, task);
            if (!taskIdsStr.isBlank()) {
                for (String id : taskIdsStr.split(",")) {
                    String tid = id.trim();
                    if (!tid.isEmpty()) {
                        state.getSelectedTaskIds().add(tid);
                    }
                }
            }
            if (PresetRange.isRelativePreset(presetStr)) {
                applyRelativePresetFromRaw(presetStr, (from, to) -> {
                    state.setFromDate(from);
                    state.setToDate(to);
                });
                state.setPresetKey(PresetRange.fromPreference(presetStr).name());
            } else {
                if (!fromStr.isBlank()) {
                    try {
                        state.setFromDate(LocalDate.parse(fromStr));
                    } catch (Exception ignored) { }
                }
                if (!toStr.isBlank()) {
                    try {
                        state.setToDate(LocalDate.parse(toStr));
                    } catch (Exception ignored) { }
                }
                state.setPresetKey(presetStr);
            }

            viewTabStates.add(state);
            TableView<Activity> viewTable = createViewActivityTable();
            ViewTabData tabData = new ViewTabData(state, viewTable);
            Tab tab = new Tab(state.getTabTitle());
            tab.setUserData(tabData);
            tab.setClosable(true);
            VBox viewContent = new VBox(10, createViewFilterPanel(state), viewTable);
            VBox.setVgrow(viewTable, Priority.ALWAYS);
            tab.setContent(viewContent);
            viewTabPane.getTabs().add(tab);
        }
        int selectedIndex = preferences.getInt("view.selectedIndex", 0);
        if (selectedIndex >= 0 && selectedIndex < viewTabPane.getTabs().size()) {
            viewTabPane.getSelectionModel().select(selectedIndex);
        }
        applyFilters();
        updateWriteTimesheetButtonForTab();
        restoringViewTabs = false;
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
        DayOfWeek firstDay = settings.getFirstDayOfWeek();
        switch (preset) {
            case DAY:
                fromFilter.setValue(today);
                toFilter.setValue(today);
                break;
            case WEEK: {
                LocalDate start = startOfWeek(today, firstDay);
                LocalDate end = endOfWeek(today, firstDay);
                fromFilter.setValue(start);
                toFilter.setValue(end);
                break;
            }
            case MONTH: {
                LocalDate start = today.with(TemporalAdjusters.firstDayOfMonth());
                LocalDate end = today.with(TemporalAdjusters.lastDayOfMonth());
                fromFilter.setValue(start);
                toFilter.setValue(end);
                break;
            }
            default:
                break;
        }
    }

    private void updateMainPeriodNav() {
        if (periodNavLabel == null || periodPrevButton == null || periodNextButton == null || periodNavBox == null) {
            return;
        }
        PresetRange preset = presetFilter != null ? presetFilter.getValue() : null;
        LocalDate from = fromFilter != null ? fromFilter.getValue() : null;
        LocalDate to = toFilter != null ? toFilter.getValue() : null;
        if (preset == PresetRange.DAY && from != null && to != null) {
            periodNavLabel.setText(formatDay(from));
            periodNavBox.setVisible(true);
            periodPrevButton.setDisable(false);
            periodNextButton.setDisable(false);
        } else if (preset == PresetRange.WEEK && from != null && to != null) {
            periodNavLabel.setText(formatWeekRange(from, to));
            periodNavBox.setVisible(true);
            periodPrevButton.setDisable(false);
            periodNextButton.setDisable(false);
        } else if (preset == PresetRange.MONTH && from != null && to != null) {
            periodNavLabel.setText(formatMonthRange(from, to));
            periodNavBox.setVisible(true);
            periodPrevButton.setDisable(false);
            periodNextButton.setDisable(false);
        } else {
            periodNavLabel.setText("");
            periodNavBox.setVisible(false);
        }
    }

    private void shiftMainPeriod(int delta) {
        PresetRange preset = presetFilter != null ? presetFilter.getValue() : null;
        LocalDate from = fromFilter.getValue();
        LocalDate to = toFilter.getValue();
        if (preset == null || from == null || to == null) return;
        if (preset == PresetRange.DAY) {
            fromFilter.setValue(from.plusDays(delta));
            toFilter.setValue(to.plusDays(delta));
        } else if (preset == PresetRange.WEEK) {
            fromFilter.setValue(from.plusWeeks(delta));
            toFilter.setValue(to.plusWeeks(delta));
        } else if (preset == PresetRange.MONTH) {
            fromFilter.setValue(from.plusMonths(delta));
            toFilter.setValue(to.plusMonths(delta));
        }
        applyFilters();
        saveFilterPreferences();
    }

    /** Applies dates for a relative preset (THIS_WEEK, LAST_WEEK, THIS_MONTH, LAST_MONTH) when restoring from preferences. */
    private void applyRelativePresetFromRaw(String rawPreset, java.util.function.BiConsumer<LocalDate, LocalDate> setFromTo) {
        if (rawPreset == null || setFromTo == null || !PresetRange.isRelativePreset(rawPreset)) {
            return;
        }
        LocalDate today = LocalDate.now();
        DayOfWeek firstDay = settings.getFirstDayOfWeek();
        String v = rawPreset.trim();
        if ("TODAY".equals(v) || "Today".equals(v)) {
            setFromTo.accept(today, today);
        } else if ("YESTERDAY".equals(v) || "Yesterday".equals(v)) {
            LocalDate y = today.minusDays(1);
            setFromTo.accept(y, y);
        } else if ("THIS_WEEK".equals(v) || "This Week".equals(v)) {
            setFromTo.accept(startOfWeek(today, firstDay), endOfWeek(today, firstDay));
        } else if ("LAST_WEEK".equals(v) || "Last Week".equals(v)) {
            LocalDate start = startOfWeek(today, firstDay).minusWeeks(1);
            setFromTo.accept(start, start.plusDays(6));
        } else if ("THIS_MONTH".equals(v) || "This Month".equals(v)) {
            setFromTo.accept(today.with(TemporalAdjusters.firstDayOfMonth()), today.with(TemporalAdjusters.lastDayOfMonth()));
        } else if ("LAST_MONTH".equals(v) || "Last Month".equals(v)) {
            LocalDate lastMonth = today.minusMonths(1);
            setFromTo.accept(lastMonth.with(TemporalAdjusters.firstDayOfMonth()), lastMonth.with(TemporalAdjusters.lastDayOfMonth()));
        }
    }

    private void applyFilters() {
        if (filteredActivities == null) {
            return;
        }
        int tabIndex = viewTabPane != null ? viewTabPane.getSelectionModel().getSelectedIndex() : 0;
        if (tabIndex == CALENDAR_TAB_INDEX) {
            return;
        }
        Customer customer;
        Project project;
        List<String> selectedTaskIds;
        LocalDate from;
        LocalDate to;

        if (tabIndex <= 0) {
            customer = customerFilter.getValue();
            project = projectFilter.getValue();
            selectedTaskIds = new ArrayList<>();
            for (Task t : taskFilter.getSelectionModel().getSelectedItems()) {
                selectedTaskIds.add(t.getId());
            }
            from = fromFilter.getValue();
            to = toFilter.getValue();
        } else {
            int stateIndex = tabIndex - 2;
            if (stateIndex < 0 || stateIndex >= viewTabStates.size()) {
                return;
            }
            ViewTabState state = viewTabStates.get(stateIndex);
            customer = state.getFixedCustomer();
            project = state.getFixedProject();
            if (state.getFixedTask() != null) {
                selectedTaskIds = List.of(state.getFixedTask().getId());
            } else {
                selectedTaskIds = new ArrayList<>(state.getSelectedTaskIds());
            }
            from = state.getFromDate();
            to = state.getToDate();
        }

        final Customer c = customer;
        final Project p = project;
        final List<String> taskIds = selectedTaskIds;
        final LocalDate fromDate = from;
        final LocalDate toDate = to;

        filteredActivities.setPredicate(activity -> {
            if (c != null && !c.getId().equals(activity.getCustomerId())) {
                return false;
            }
            if (p != null && !p.getId().equals(activity.getProjectId())) {
                return false;
            }
            if (!taskIds.isEmpty()) {
                if (!taskIds.contains(activity.getTaskId())) {
                    return false;
                }
            }
            if (fromDate != null || toDate != null) {
                LocalDateTime activityFrom = Activity.parseStoredDateTime(activity.getFrom());
                if (activityFrom == null) {
                    return false;
                }
                LocalDate activityDate = activityFrom.toLocalDate();
                if (fromDate != null && activityDate.isBefore(fromDate)) {
                    return false;
                }
                if (toDate != null && activityDate.isAfter(toDate)) {
                    return false;
                }
            }
            return true;
        });
        recomputeDailyTotals();
        refreshAllActivityTables();
    }

    private void consolidateFilteredActivities() {
        if (filteredActivities == null || filteredActivities.isEmpty()) {
            showInfo(i18n("filter.consolidate.none"));
            return;
        }
        List<Activity> ordered = new ArrayList<>(filteredActivities);
        ordered.sort(Comparator
            .comparing(this::activityFromOrMax)
            .thenComparing(this::activityToOrMax)
        );
        List<ConsolidationGroup> groups = buildConsolidationGroups(ordered);
        if (groups.isEmpty()) {
            showInfo(i18n("filter.consolidate.none"));
            return;
        }
        if (!confirmConsolidation(groups)) {
            return;
        }
        List<Activity> toRemove = new ArrayList<>();
        for (ConsolidationGroup group : groups) {
            Activity target = group.activities.get(0);
            mergeActivityRange(target, group.activities);
            for (int i = 1; i < group.activities.size(); i++) {
                toRemove.add(group.activities.get(i));
            }
        }
        activities.removeAll(toRemove);
        setLastActivityFromData();
        saveData();
        applyFilters();
    }

    private List<ConsolidationGroup> buildConsolidationGroups(List<Activity> ordered) {
        List<ConsolidationGroup> groups = new ArrayList<>();
        ConsolidationGroup current = null;
        for (Activity activity : ordered) {
            if (activity == null) {
                continue;
            }
            if (current == null || !current.matches(activity)) {
                current = new ConsolidationGroup(activity);
                groups.add(current);
            } else {
                current.activities.add(activity);
            }
        }
        groups.removeIf(group -> group.activities.size() < 2);
        return groups;
    }

    private boolean confirmConsolidation(List<ConsolidationGroup> groups) {
        int totalActivities = 0;
        for (ConsolidationGroup group : groups) {
            totalActivities += group.activities.size();
        }
        String header = i18n("filter.consolidate.confirm.header", groups.size(), totalActivities, groups.size());
        StringBuilder details = new StringBuilder();
        for (ConsolidationGroup group : groups) {
            Activity first = group.activities.get(0);
            Activity last = group.activities.get(group.activities.size() - 1);
            String customer = resolveCustomerName(group.customerId);
            String project = resolveProjectName(group.projectId);
            String task = resolveTaskName(group.taskId);
            details.append(customer)
                .append(" / ")
                .append(project)
                .append(" / ")
                .append(task)
                .append(": ")
                .append(safeDate(first.getFrom()))
                .append(" - ")
                .append(safeDate(last.getTo()))
                .append(" (")
                .append(group.activities.size())
                .append(")")
                .append(System.lineSeparator());
        }
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(i18n("filter.consolidate.confirm.title"));
        alert.setHeaderText(header);
        TextArea area = new TextArea(details.toString().trim());
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefRowCount(Math.min(groups.size() + 1, 12));
        alert.getDialogPane().setContent(area);
        addEscapeToClose(alert);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get().getButtonData() == ButtonData.OK_DONE;
    }

    private void mergeActivityRange(Activity target, List<Activity> group) {
        LocalDateTime earliestFrom = null;
        LocalDateTime latestTo = null;
        for (Activity activity : group) {
            LocalDateTime from = Activity.parseStoredDateTime(activity.getFrom());
            LocalDateTime to = Activity.parseStoredDateTime(activity.getTo());
            earliestFrom = minDateTime(earliestFrom, from);
            latestTo = maxDateTime(latestTo, to);
        }
        if (earliestFrom != null) {
            target.setFrom(Activity.formatDateTime(earliestFrom));
        }
        if (latestTo != null) {
            target.setTo(Activity.formatDateTime(latestTo));
        }
    }

    private LocalDateTime activityFromOrMax(Activity activity) {
        LocalDateTime value = Activity.parseStoredDateTime(activity.getFrom());
        return value != null ? value : LocalDateTime.MAX;
    }

    private LocalDateTime activityToOrMax(Activity activity) {
        LocalDateTime value = Activity.parseStoredDateTime(activity.getTo());
        return value != null ? value : LocalDateTime.MAX;
    }

    private String safeDate(String value) {
        return value != null && !value.isBlank() ? value.trim() : "?";
    }

    private LocalDateTime minDateTime(LocalDateTime left, LocalDateTime right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isBefore(right) ? left : right;
    }

    private LocalDateTime maxDateTime(LocalDateTime left, LocalDateTime right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isAfter(right) ? left : right;
    }

    private final class ConsolidationGroup {
        private final String customerId;
        private final String projectId;
        private final String taskId;
        private final List<Activity> activities = new ArrayList<>();

        private ConsolidationGroup(Activity activity) {
            this.customerId = activity.getCustomerId();
            this.projectId = activity.getProjectId();
            this.taskId = activity.getTaskId();
            this.activities.add(activity);
        }

        private boolean matches(Activity activity) {
            if (!customerId.equals(activity.getCustomerId())
                || !projectId.equals(activity.getProjectId())
                || !taskId.equals(activity.getTaskId())) {
                return false;
            }
            Activity last = activities.get(activities.size() - 1);
            return isDirectlyAdjacent(last, activity);
        }
    }

    private boolean isDirectlyAdjacent(Activity left, Activity right) {
        if (left == null || right == null) {
            return false;
        }
        LocalDateTime leftTo = Activity.parseStoredDateTime(left.getTo());
        LocalDateTime rightFrom = Activity.parseStoredDateTime(right.getFrom());
        if (leftTo == null || rightFrom == null) {
            return false;
        }
        return leftTo.equals(rightFrom);
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
        reminderService.start(settings, now -> Platform.runLater(() -> onReminderFired(now)));
    }

    /**
     * Called every full minute by the reminder timer (on JavaFX thread). Decides whether to show the reminder dialog.
     * Accepts {@code now} for testability. No reminder on startup (rule 1); only show on interval boundary within window (rule 2).
     */
    void onReminderFired(LocalDateTime now) {
        if (activityDialogOpen) {
            if (reminderDebug) {
                debugReminder("No reminder at %s: activity dialog is currently open.", now);
            }
            return;
        }
        boolean due = reminderService.isReminderDue(now, settings);
        if (!due) {
            if (reminderDebug) {
                boolean withinWindow = treimers.net.jtimesheet.service.ReminderWindowRules.isWithinWindow(now, settings);
                int interval = treimers.net.jtimesheet.model.AppSettings.normalizeReminderIntervalMinutes(
                        settings.getReminderIntervalMinutes());
                int minute = now.getMinute();
                boolean onBoundary = (minute % interval) == 0;
                debugReminder(
                        "No reminder at %s: isReminderDue=false (withinWindow=%s, intervalMinutes=%d, minute=%02d, onIntervalBoundary=%s).",
                        now,
                        withinWindow,
                        interval,
                        minute,
                        onBoundary);
            }
            return;
        }
        if (ReminderExceptionRules.shouldSuppressReminder(activities, customers, now)) {
            if (reminderDebug) {
                debugReminder("No reminder at %s: suppressed by ReminderExceptionRules (activities=%d).", now, activities.size());
            }
            return;
        }
        ReminderSuggestion s = reminderSuggestionLogic.compute(
                now,
                new ArrayList<>(activities),
                new ArrayList<>(customers),
                lastActivity,
                null,
                null,
                settings,
                true,
                programStartTime,
                reminderDebug ? this::debugSuggestion : null);
        if (s.isBlockedForReminder()) {
            if (reminderDebug) {
                debugReminder(
                        "No reminder at %s: suggestion is blockedForReminder (inside activity ending in the future).",
                        now);
            }
            return;
        }
        LocalDateTime[] range;
        if (lastShownGapRange != null) {
            range = lastShownGapRange;
        } else {
            range = s.getRange();
            if (s.getSuggestionType() == ReminderSuggestion.SuggestionType.GAP) {
                lastShownGapRange = new LocalDateTime[] { range[0], range[1] };
            } else {
                lastShownGapRange = null;
            }
        }
        if (reminderDebug) {
            debugReminder(
                    "Showing reminder dialog at %s: range=%s – %s, suggestionType=%s, lastShownGapRange=%s.",
                    now,
                    range[0],
                    range[1],
                    s.getSuggestionType(),
                    lastShownGapRange != null ? (lastShownGapRange[0] + " – " + lastShownGapRange[1]) : "null");
        }
        openAddOrPromptActivityDialogWithSuggestion(i18n("activity.add.title"), now, s, range);
    }

    private void debugReminder(String format, Object... args) {
        String line = REMINDER_DEBUG_PREFIX + String.format(format, args);
        reminderDebugLog.add(line);
        updateReminderDebugTextArea();
    }

    /** Logs suggestion debug when reminderDebug is enabled. Used by ReminderSuggestionLogic. */
    private void debugSuggestion(String msg) {
        if (reminderDebug) {
            reminderDebugLog.add(REMINDER_DEBUG_PREFIX + "Vorschlag: " + msg);
            updateReminderDebugTextArea();
        }
    }

    private void debugReminderInitialSettings() {
        int interval = treimers.net.jtimesheet.model.AppSettings.normalizeReminderIntervalMinutes(
                settings.getReminderIntervalMinutes());
        debugReminder(
                "Enabled. Interval=%d minutes, window=%s–%s, weekdays=%s.",
                interval,
                settings.getReminderStartTime(),
                settings.getReminderEndTime(),
                settings.getReminderWeekdays());
    }

    private void setReminderDebug(boolean enabled) {
        this.reminderDebug = enabled;
        if (reminderDebug) {
            debugReminderInitialSettings();
        } else {
            debugReminder("Disabled.");
        }
        if (reminderDebugDialog != null) {
            reminderDebugDialog.setHeaderText("Interner Reminder-Debug-Status (aktiv: " + reminderDebug + ")");
        }
    }

    private void toggleReminderDebugDialog() {
        if (reminderDebugDialog == null) {
            reminderDebugDialog = new Dialog<>();
            reminderDebugDialog.setTitle("Reminder-Debug-Log");
            reminderDebugDialog.setHeaderText("Interner Reminder-Debug-Status (aktiv: " + reminderDebug + ")");
            reminderDebugTextArea = new TextArea();
            reminderDebugTextArea.setEditable(false);
            reminderDebugTextArea.setWrapText(false);
            reminderDebugTextArea.setPrefColumnCount(80);
            reminderDebugTextArea.setPrefRowCount(20);
            reminderDebugDialog.getDialogPane().setContent(reminderDebugTextArea);
            reminderDebugDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            reminderDebugDialog.initOwner(primaryStage);
            reminderDebugDialog.initModality(Modality.NONE);
            reminderDebugDialog.setOnShown(e -> {
                var w = reminderDebugDialog.getDialogPane().getScene().getWindow();
                if (w instanceof Stage) {
                    ((Stage) w).setResizable(true);
                }
            });
            reminderDebugDialog.getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
                if (new KeyCodeCombination(KeyCode.F12).match(ev)) {
                    setReminderDebug(!reminderDebug);
                    ev.consume();
                } else if (new KeyCodeCombination(KeyCode.F12, KeyCombination.SHIFT_DOWN).match(ev)) {
                    reminderDebugDialog.hide();
                    ev.consume();
                }
            });
        }
        if (reminderDebugDialog.isShowing()) {
            reminderDebugDialog.hide();
        } else {
            updateReminderDebugTextArea();
            reminderDebugDialog.show();
        }
    }

    private void updateReminderDebugTextArea() {
        if (reminderDebugTextArea != null) {
            reminderDebugTextArea.setText(String.join("\n", reminderDebugLog));
            reminderDebugTextArea.positionCaret(reminderDebugTextArea.getText().length());
        }
    }

    /**
     * Opens the add activity dialog with the given reminder suggestion and range.
     * When user adds an activity, {@link #lastShownGapRange} is cleared (rule 9).
     */
    private void openAddOrPromptActivityDialogWithSuggestion(
            String title,
            LocalDateTime now,
            ReminderSuggestion suggestion,
            LocalDateTime[] range) {
        Customer defaultCustomer = suggestion.resolveCustomer(customers);
        Project defaultProject = suggestion.resolveProject(defaultCustomer);
        Task defaultTask = suggestion.resolveTask(defaultProject);
        ActivityInput reopenWith = null;
        while (true) {
            Optional<ActivityInput> input = showActivityDialog(
                    title,
                    null,
                    reopenWith != null ? reopenWith.getFrom() : range[0],
                    reopenWith != null ? reopenWith.getTo() : range[1],
                    reopenWith != null ? reopenWith.getCustomer() : defaultCustomer,
                    reopenWith != null ? reopenWith.getProject() : defaultProject,
                    reopenWith != null ? reopenWith.getTask() : defaultTask,
                    this::getLastProjectAndTaskForCustomer,
                    this::suggestedPromptRangeForSelection,
                    settings.getReminderIntervalMinutes(),
                    settings.getReminderIntervalMinutes() > 0 ? settings.getReminderEndTime() : null);
            if (!input.isPresent()) {
                return;
            }
            ActivityInput activityInput = input.get();
            Optional<ActivityInput> toApply = checkOverlapAndConfirm(activityInput, null);
            if (toApply.isPresent()) {
                lastShownGapRange = null;
                addOrMergeActivity(toApply.get());
                return;
            }
            reopenWith = activityInput;
        }
    }

    /**
     * Opens the add activity dialog (e.g. from menu). Computes suggestion via {@link ReminderSuggestionLogic};
     * when blocked, shows info message. When from reminder, use {@link #onReminderFired} instead.
     */
    private void openAddOrPromptActivityDialog(String title, LocalDateTime now, boolean fromReminder) {
        if (!fromReminder) {
            AddActivityDialogRules.BlockedReason blocked = AddActivityDialogRules.getBlockedReason(customers);
            if (blocked != AddActivityDialogRules.BlockedReason.NONE) {
                showInfo(i18n(blocked.getMessageKey()));
                return;
            }
        }
        ReminderSuggestion s = reminderSuggestionLogic.compute(
                now,
                new ArrayList<>(activities),
                new ArrayList<>(customers),
                lastActivity,
                null,
                null,
                settings,
                fromReminder,
                programStartTime,
                reminderDebug ? this::debugSuggestion : null);
        if (s.isBlockedForReminder()) {
            if (fromReminder) {
                return;
            }
            // Add Activity: show dialog with start=end=now (duration 0 allowed)
        }
        LocalDateTime[] range = s.getRange();
        openAddOrPromptActivityDialogWithSuggestion(title, now, s, range);
    }

    /** Callback for Add dialog: returns suggested [from, to] when user changes customer or project (rule 8). If no range, returns Programmstart–jetzt (gerundet). */
    private LocalDateTime[] suggestedPromptRangeForSelection(Customer customer, Project project) {
        LocalDateTime now = LocalDateTime.now();
        ReminderSuggestion s = reminderSuggestionLogic.compute(
                now,
                new ArrayList<>(activities),
                new ArrayList<>(customers),
                lastActivity,
                customer != null ? customer.getId() : null,
                project != null ? project.getId() : null,
                settings,
                false,
                programStartTime,
                reminderDebug ? this::debugSuggestion : null);
        if (s.getRange() != null) {
            return s.getRange();
        }
        return new LocalDateTime[] { now, now };
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
        if (customers.isEmpty()) {
            showInfo(i18n("timesheet.export.no.customer.or.project"));
            return;
        }
        Customer initialCustomer = null;
        Project initialProject = null;
        if (lastActivity != null) {
            initialCustomer = findCustomerById(lastActivity.getCustomerId());
            initialProject = initialCustomer != null ? findProjectById(initialCustomer, lastActivity.getProjectId()) : null;
        }
        if (initialCustomer == null) {
            initialCustomer = customers.get(0);
        }
        if (initialProject == null && initialCustomer != null && !initialCustomer.getProjects().isEmpty()) {
            initialProject = initialCustomer.getProjects().get(0);
        }
        if (initialCustomer == null || initialCustomer.getProjects().isEmpty()) {
            showInfo(i18n("timesheet.export.no.customer.or.project"));
            return;
        }
        if (initialProject == null) {
            initialProject = initialCustomer.getProjects().get(0);
        }
        TimesheetExportChoice choice = showTimesheetExportDialog(initialCustomer, initialProject);
        if (choice == null) {
            return;
        }
        Customer selectedCustomer = choice.customer;
        Project selectedProject = choice.project;
        LocalDate fromDate = choice.from;
        LocalDate toDate = choice.to;
        List<Activity> exportActivities = new ArrayList<>();
        for (Activity a : activities) {
            if (!selectedCustomer.getId().equals(a.getCustomerId())) {
                continue;
            }
            if (selectedProject != null && !selectedProject.getId().equals(a.getProjectId())) {
                continue;
            }
            if (fromDate != null || toDate != null) {
                LocalDateTime activityFrom = Activity.parseStoredDateTime(a.getFrom());
                if (activityFrom == null) continue;
                LocalDate ad = activityFrom.toLocalDate();
                if (fromDate != null && ad.isBefore(fromDate)) continue;
                if (toDate != null && ad.isAfter(toDate)) continue;
            }
            exportActivities.add(a);
        }
        if (exportActivities.isEmpty()) {
            showInfo(i18n("timesheet.none"));
            return;
        }
        if (!hasRequiredTimesheetConfig(selectedCustomer)) {
            showInfo(i18n("timesheet.customer.missing.properties", selectedCustomer.getName()));
            return;
        }
        Path templatePath = resolveTemplatePath(selectedCustomer.getTimesheetTemplatePath());
        if (templatePath == null) {
            showInfo(i18n("timesheet.template.notfound"));
            return;
        }
        File outputFile = chooseTimesheetOutputFile(selectedCustomer, templatePath);
        if (outputFile == null) {
            return;
        }
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

    /** User choices from the timesheet export dialog. */
    private static final class TimesheetExportChoice {
        final Customer customer;
        final Project project;
        final LocalDate from;
        final LocalDate to;

        TimesheetExportChoice(Customer customer, Project project, LocalDate from, LocalDate to) {
            this.customer = customer;
            this.project = project;
            this.from = from;
            this.to = to;
        }
    }

    /**
     * Shows the timesheet export dialog. Uses only local controls and does not read or modify
     * any view filters (customerFilter, projectFilter, fromFilter, toFilter, presetFilter or view tab states).
     */
    private TimesheetExportChoice showTimesheetExportDialog(Customer initialCustomer, Project initialProject) {
        List<Customer> customersWithProjects = new ArrayList<>();
        for (Customer c : customers) {
            if (c != null && !c.getProjects().isEmpty()) {
                customersWithProjects.add(c);
            }
        }
        ComboBox<Customer> customerCombo = new ComboBox<>(FXCollections.observableArrayList(customersWithProjects));
        customerCombo.setPrefWidth(220);
        customerCombo.setButtonCell(createCustomerFilterCell(i18n("filter.customer.placeholder")));
        customerCombo.setCellFactory(lv -> createCustomerFilterCell(i18n("filter.customer.placeholder")));
        customerCombo.setValue(initialCustomer);

        ComboBox<Project> projectCombo = new ComboBox<>();
        projectCombo.setPrefWidth(220);
        projectCombo.setButtonCell(createProjectFilterCell(i18n("filter.project.placeholder")));
        projectCombo.setCellFactory(lv -> createProjectFilterCell(i18n("filter.project.placeholder")));
        projectCombo.setItems(FXCollections.observableArrayList(initialCustomer.getProjects()));
        projectCombo.setValue(initialProject);
        customerCombo.valueProperty().addListener((obs, o, c) -> {
            if (c == null) {
                projectCombo.setItems(FXCollections.observableArrayList());
                projectCombo.setValue(null);
            } else {
                projectCombo.setItems(FXCollections.observableArrayList(c.getProjects()));
                projectCombo.setValue(c.getProjects().isEmpty() ? null : c.getProjects().get(0));
            }
        });

        LocalDate today = LocalDate.now();
        LocalDate refMonth = (today.getDayOfMonth() >= 1 && today.getDayOfMonth() <= 14)
            ? today.minusMonths(1) : today;
        LocalDate defaultFrom = refMonth.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate defaultTo = refMonth.with(TemporalAdjusters.lastDayOfMonth());

        ComboBox<PresetRange> presetCombo = new ComboBox<>();
        presetCombo.getItems().setAll(PresetRange.values());
        presetCombo.setPrefWidth(140);
        presetCombo.setButtonCell(createPresetCell());
        presetCombo.setCellFactory(lv -> createPresetCell());

        DatePicker fromPicker = new DatePicker(defaultFrom);
        DatePicker toPicker = new DatePicker(defaultTo);

        final boolean[] applyingPreset = { false };
        final boolean[] initializing = { true };
        presetCombo.valueProperty().addListener((obs, o, preset) -> {
            if (initializing[0]) return;
            if (preset == null || preset == PresetRange.CUSTOM) return;
            applyingPreset[0] = true;
            DayOfWeek firstDay = settings.getFirstDayOfWeek();
            LocalDate t = LocalDate.now();
            switch (preset) {
                case DAY -> {
                    fromPicker.setValue(t);
                    toPicker.setValue(t);
                }
                case WEEK -> {
                    fromPicker.setValue(startOfWeek(t, firstDay));
                    toPicker.setValue(endOfWeek(t, firstDay));
                }
                case MONTH -> {
                    fromPicker.setValue(t.with(TemporalAdjusters.firstDayOfMonth()));
                    toPicker.setValue(t.with(TemporalAdjusters.lastDayOfMonth()));
                }
                default -> { }
            }
            applyingPreset[0] = false;
        });
        fromPicker.valueProperty().addListener((obs, o, n) -> {
            if (!applyingPreset[0]) presetCombo.setValue(PresetRange.CUSTOM);
        });
        toPicker.valueProperty().addListener((obs, o, n) -> {
            if (!applyingPreset[0]) presetCombo.setValue(PresetRange.CUSTOM);
        });

        Label periodNavLabel = new Label();
        Button periodPrevBtn = new Button(i18n("filter.period.prev"));
        Button periodNextBtn = new Button(i18n("filter.period.next"));
        periodPrevBtn.setMinWidth(Region.USE_PREF_SIZE);
        periodNextBtn.setMinWidth(Region.USE_PREF_SIZE);
        HBox periodNavBox = new HBox(6, periodPrevBtn, periodNavLabel, periodNextBtn);
        periodNavBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(periodNavLabel, Priority.ALWAYS);
        Runnable updatePeriodNav = () -> updateViewPeriodNavLabel(periodNavLabel, periodNavBox, presetCombo.getValue(), fromPicker.getValue(), toPicker.getValue());
        periodPrevBtn.setOnAction(e -> {
            PresetRange p = presetCombo.getValue();
            LocalDate from = fromPicker.getValue();
            LocalDate to = toPicker.getValue();
            if (p == null || from == null || to == null) return;
            applyingPreset[0] = true;
            if (p == PresetRange.DAY) {
                fromPicker.setValue(from.plusDays(-1));
                toPicker.setValue(to.plusDays(-1));
            } else if (p == PresetRange.WEEK) {
                fromPicker.setValue(from.plusWeeks(-1));
                toPicker.setValue(to.plusWeeks(-1));
            } else if (p == PresetRange.MONTH) {
                fromPicker.setValue(from.plusMonths(-1));
                toPicker.setValue(to.plusMonths(-1));
            }
            applyingPreset[0] = false;
            updatePeriodNav.run();
        });
        periodNextBtn.setOnAction(e -> {
            PresetRange p = presetCombo.getValue();
            LocalDate from = fromPicker.getValue();
            LocalDate to = toPicker.getValue();
            if (p == null || from == null || to == null) return;
            applyingPreset[0] = true;
            if (p == PresetRange.DAY) {
                fromPicker.setValue(from.plusDays(1));
                toPicker.setValue(to.plusDays(1));
            } else if (p == PresetRange.WEEK) {
                fromPicker.setValue(from.plusWeeks(1));
                toPicker.setValue(to.plusWeeks(1));
            } else if (p == PresetRange.MONTH) {
                fromPicker.setValue(from.plusMonths(1));
                toPicker.setValue(to.plusMonths(1));
            }
            applyingPreset[0] = false;
            updatePeriodNav.run();
        });
        presetCombo.valueProperty().addListener((obs, o, n) -> updatePeriodNav.run());
        fromPicker.valueProperty().addListener((obs, o, n) -> updatePeriodNav.run());
        toPicker.valueProperty().addListener((obs, o, n) -> updatePeriodNav.run());

        presetCombo.setValue(PresetRange.MONTH);
        initializing[0] = false;
        updatePeriodNav.run();

        VBox form = new VBox(10,
            new Label(i18n("filter.customer.label")), customerCombo,
            new Label(i18n("filter.project.label")), projectCombo,
            new Label(i18n("filter.preset.label")), presetCombo,
            periodNavBox,
            new Label(i18n("filter.from.label")), fromPicker,
            new Label(i18n("filter.to.label")), toPicker
        );
        form.setPadding(new Insets(20));

        Alert dialog = new Alert(AlertType.NONE);
        dialog.setTitle(i18n("timesheet.export.dialog.title"));
        dialog.setHeaderText(i18n("timesheet.export.dialog.header"));
        dialog.getDialogPane().setContent(form);
        ButtonType ok = new ButtonType(i18n("button.save"), ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        addEscapeToClose(dialog);
        dialog.initOwner(primaryStage);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ok) {
            return null;
        }
        Customer c = customerCombo.getValue();
        Project p = projectCombo.getValue();
        if (c == null || p == null) {
            showInfo(p == null ? i18n("timesheet.export.customer.has.no.projects") : i18n("timesheet.customer.required"));
            return null;
        }
        return new TimesheetExportChoice(c, p, fromPicker.getValue(), toPicker.getValue());
    }

    private Properties buildTimesheetProperties(Customer customer) {
        Properties properties = new Properties();
        int timeGridMinutes = AppSettings.normalizeTimeGridMinutes(settings.getTimeGridMinutes());
        double rounding = timeGridMinutes > 0 ? 60.0 / timeGridMinutes : 4.0;
        properties.setProperty("rounding", String.valueOf(rounding));
        putIfNotBlank(properties, "target.sheetno", customer.getTimesheetSheetNo());
        putIfNotBlank(properties, "target.task.separator", customer.getTimesheetTaskSeparator());
        return properties;
    }

    private boolean hasRequiredTimesheetConfig(Customer customer) {
        return hasText(customer.getTimesheetSheetNo());
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
        String extension = resolveTemplateExtension(templatePath);
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter(
            i18n("timesheet.output.filter"),
            "*" + extension
        );
        chooser.getExtensionFilters().add(filter);
        chooser.setSelectedExtensionFilter(filter);
        if (templatePath != null) {
            File f = templatePath.toFile();
            File dir = f.isDirectory() ? f : f.getParentFile();
            if (dir != null && dir.exists()) {
                chooser.setInitialDirectory(dir);
            }
        }
        String baseName = baseNameForTimesheetOutput(customer, extension);
        String fileName = baseName != null ? baseName + extension : "timesheet" + extension;
        chooser.setInitialFileName(fileName);
        File file = chooser.showSaveDialog(primaryStage);
        if (file != null && !file.getName().toLowerCase().endsWith(extension.toLowerCase())) {
            String name = file.getName();
            if (name.toLowerCase().endsWith(".xls")) {
                name = name.substring(0, name.length() - 4);
            } else if (name.toLowerCase().endsWith(".xlsx")) {
                name = name.substring(0, name.length() - 5);
            }
            file = new File(file.getParent(), name + (name.isEmpty() ? "timesheet" : "") + extension);
        }
        return file;
    }

    /** Base name for timesheet output (no extension). Strips any .xls/.xlsx from suggestion so the current template extension is used. */
    private String baseNameForTimesheetOutput(Customer customer, String extension) {
        String raw = null;
        if (customer != null) {
            String suggestion = customer.getTimesheetFilenameSuggestion();
            if (suggestion != null && !suggestion.isBlank()) {
                raw = suggestion.trim();
            } else if (customer.getName() != null && !customer.getName().isBlank()) {
                raw = customer.getName().trim() + "-timesheet";
            }
        }
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        // Use only the file name part if the suggestion is a path
        if (raw.contains(File.separator) || raw.contains("/")) {
            raw = Paths.get(raw.replace('/', File.separatorChar)).getFileName().toString();
        }
        String lower = raw.toLowerCase();
        if (lower.endsWith(".xlsx")) {
            return raw.substring(0, raw.length() - 5);
        }
        if (lower.endsWith(".xls")) {
            return raw.substring(0, raw.length() - 4);
        }
        return raw;
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

    /** Returns the path if the file exists, null otherwise. No fallback to other extensions. */
    private Path resolveTemplatePath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        Path resolved = Paths.get(path.trim());
        return Files.exists(resolved) ? resolved : null;
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
        openManagementDialog(primaryStage);
    }

    /** Opens the management dialog. When called from the Activity dialog, pass the activity dialog's window as owner so Manage is modal to it. */
    private void openManagementDialog(javafx.stage.Window owner) {
        ManagementDialog dialog = new ManagementDialog(
            customers,
            this::saveData,
            this::loadData,
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
        dialog.show(owner != null ? owner : primaryStage);
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
            settings.setReminderWeekdays(values.getReminderWeekdays());
            settings.setFirstDayOfWeek(values.getFirstDayOfWeek());
            settings.setLanguage(values.getLanguage());
            settings.setDataDirectory(values.getDataDirectory());
            settingsService.save(settings);
            if (calendarViewNode instanceof CalendarView cv) {
                cv.setWeekFields(WeekFields.of(settings.getFirstDayOfWeek(), 1));
            }
            saveData();
            applyLanguage();
            startReminderScheduler();
        }
    }

    private void addActivity() {
        openAddOrPromptActivityDialog(i18n("activity.add.title"), LocalDateTime.now(), false);
    }

    /** Returns activities that overlap with [from, to), excluding {@code exclude}. Touching (end == start) is not overlap. */
    private List<Activity> findOverlappingActivities(LocalDateTime from, LocalDateTime to, Activity exclude) {
        if (from == null || to == null || !from.isBefore(to)) {
            return Collections.emptyList();
        }
        List<Activity> result = new ArrayList<>();
        for (Activity a : activities) {
            if (a == exclude) {
                continue;
            }
            LocalDateTime aFrom = Activity.parseStoredDateTime(a.getFrom());
            LocalDateTime aTo = Activity.parseStoredDateTime(a.getTo());
            if (aFrom == null || aTo == null) {
                continue;
            }
            if (from.isBefore(aTo) && aFrom.isBefore(to)) {
                result.add(a);
            }
        }
        result.sort(Comparator.comparing(a -> Activity.parseStoredDateTime(a.getFrom())));
        return result;
    }

    /**
     * If the proposed input overlaps with existing activities, shows a dialog once with overlap list and correction suggestions.
     * Suggestions are free slots (no overlap): one before and/or one after the conflict range. Dialog pops up only once.
     */
    private Optional<ActivityInput> checkOverlapAndConfirm(ActivityInput input, Activity excludeForEdit) {
        List<Activity> overlapping = findOverlappingActivities(input.getFrom(), input.getTo(), excludeForEdit);
        if (overlapping.isEmpty()) {
            return Optional.of(input);
        }
        return showOverlapDialog(input, overlapping, excludeForEdit);
    }

    /** Finds a free slot of the given duration that ends at or before {@code slotEndBy}, no overlap with any activity. */
    private LocalDateTime[] findFreeSlotBefore(LocalDateTime slotEndBy, long durationMinutes, Activity exclude) {
        LocalDateTime slotTo = slotEndBy;
        LocalDateTime slotFrom = slotTo.minusMinutes(durationMinutes);
        for (int i = 0; i < 200; i++) {
            List<Activity> conflict = findOverlappingActivities(slotFrom, slotTo, exclude);
            if (conflict.isEmpty()) {
                return new LocalDateTime[] { slotFrom, slotTo };
            }
            LocalDateTime earliestFrom = null;
            for (Activity a : conflict) {
                LocalDateTime aFrom = Activity.parseStoredDateTime(a.getFrom());
                if (aFrom != null && (earliestFrom == null || aFrom.isBefore(earliestFrom))) {
                    earliestFrom = aFrom;
                }
            }
            if (earliestFrom == null || !earliestFrom.isBefore(slotTo)) {
                return null;
            }
            slotTo = earliestFrom;
            slotFrom = slotTo.minusMinutes(durationMinutes);
        }
        return null;
    }

    /** Finds a free slot of the given duration that starts at or after {@code slotStartFrom}, no overlap with any activity. */
    private LocalDateTime[] findFreeSlotAfter(LocalDateTime slotStartFrom, long durationMinutes, Activity exclude) {
        LocalDateTime slotFrom = slotStartFrom;
        LocalDateTime slotTo = slotFrom.plusMinutes(durationMinutes);
        for (int i = 0; i < 200; i++) {
            List<Activity> conflict = findOverlappingActivities(slotFrom, slotTo, exclude);
            if (conflict.isEmpty()) {
                return new LocalDateTime[] { slotFrom, slotTo };
            }
            LocalDateTime latestTo = null;
            for (Activity a : conflict) {
                LocalDateTime aTo = Activity.parseStoredDateTime(a.getTo());
                if (aTo != null && (latestTo == null || aTo.isAfter(latestTo))) {
                    latestTo = aTo;
                }
            }
            if (latestTo == null || !slotFrom.isBefore(latestTo)) {
                return null;
            }
            slotFrom = latestTo;
            slotTo = slotFrom.plusMinutes(durationMinutes);
        }
        return null;
    }

    private Optional<ActivityInput> showOverlapDialog(ActivityInput input, List<Activity> overlapping, Activity excludeForEdit) {
        StringBuilder content = new StringBuilder();
        content.append(i18n("activity.overlap.message"));
        content.append("\n\n");
        DateTimeFormatter displayFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", currentLocale);
        for (Activity a : overlapping) {
            Customer c = findCustomerById(a.getCustomerId());
            Project p = c != null ? findProjectById(c, a.getProjectId()) : null;
            Task t = p != null ? findTaskById(p, a.getTaskId()) : null;
            String customerName = c != null ? c.getName() : a.getCustomerId();
            String projectName = p != null ? p.getName() : a.getProjectId();
            String taskName = t != null ? t.getName() : a.getTaskId();
            LocalDateTime aFrom = Activity.parseStoredDateTime(a.getFrom());
            LocalDateTime aTo = Activity.parseStoredDateTime(a.getTo());
            String fromStr = aFrom != null ? aFrom.format(displayFormat) : a.getFrom();
            String toStr = aTo != null ? aTo.format(displayFormat) : a.getTo();
            content.append("• ").append(customerName).append(", ").append(projectName).append(", ").append(taskName)
                .append(": ").append(fromStr).append(" – ").append(toStr).append("\n");
        }

        long durationMinutes = Duration.between(input.getFrom(), input.getTo()).toMinutes();
        LocalDateTime firstFrom = null;
        LocalDateTime lastTo = null;
        for (Activity a : overlapping) {
            LocalDateTime aFrom = Activity.parseStoredDateTime(a.getFrom());
            LocalDateTime aTo = Activity.parseStoredDateTime(a.getTo());
            if (aFrom != null && (firstFrom == null || aFrom.isBefore(firstFrom))) {
                firstFrom = aFrom;
            }
            if (aTo != null && (lastTo == null || aTo.isAfter(lastTo))) {
                lastTo = aTo;
            }
        }

        ButtonType applyAnyway = new ButtonType(i18n("activity.overlap.applyAnyway"), ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType(i18n("button.cancel"), ButtonData.CANCEL_CLOSE);

        List<ButtonType> buttons = new ArrayList<>();
        buttons.add(applyAnyway);
        List<ActivityInput> suggestions = new ArrayList<>();
        if (lastTo != null && durationMinutes > 0) {
            LocalDateTime[] slotAfter = findFreeSlotAfter(lastTo, durationMinutes, excludeForEdit);
            if (slotAfter != null) {
                String rangeStr = slotAfter[0].format(displayFormat) + " – " + slotAfter[1].format(displayFormat);
                suggestions.add(new ActivityInput(input.getCustomer(), input.getProject(), input.getTask(), slotAfter[0], slotAfter[1]));
                buttons.add(new ButtonType(i18n("activity.overlap.suggestion.after", rangeStr), ButtonData.APPLY));
            }
        }
        if (firstFrom != null && durationMinutes > 0) {
            LocalDateTime[] slotBefore = findFreeSlotBefore(firstFrom, durationMinutes, excludeForEdit);
            if (slotBefore != null) {
                String rangeStr = slotBefore[0].format(displayFormat) + " – " + slotBefore[1].format(displayFormat);
                suggestions.add(new ActivityInput(input.getCustomer(), input.getProject(), input.getTask(), slotBefore[0], slotBefore[1]));
                buttons.add(new ButtonType(i18n("activity.overlap.suggestion.before", rangeStr), ButtonData.APPLY));
            }
        }
        buttons.add(cancel);

        Alert alert = new Alert(AlertType.WARNING, content.toString(), buttons.toArray(new ButtonType[0]));
        alert.setTitle(i18n("activity.overlap.title"));
        alert.setHeaderText(null);
        addEscapeToClose(alert);
        Optional<ButtonType> choice = alert.showAndWait();
        if (!choice.isPresent() || choice.get() == cancel) {
            return Optional.empty();
        }
        if (choice.get() == applyAnyway) {
            return Optional.of(input);
        }
        int index = buttons.indexOf(choice.get());
        if (index >= 1 && index - 1 < suggestions.size()) {
            return Optional.of(suggestions.get(index - 1));
        }
        return Optional.of(input);
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
                refreshAllActivityTables();
                TableView<Activity> t = getCurrentActivityTable();
                if (t != null) {
                    t.getSelectionModel().select(lastActivity);
                }
            }
            syncAllCalendarsFromActivities();
        } else {
            activities.add(activity);
            lastActivity = activity;
            if (activityTable != null) {
                TableView<Activity> tab = getCurrentActivityTable();
                if (tab != null) {
                    tab.getSelectionModel().select(activity);
                }
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
        ActivityInput reopenWith = null;
        while (true) {
            Optional<ActivityInput> input = reopenWith != null
                ? showActivityDialog(
                    i18n("activity.edit.title"),
                    selected,
                    reopenWith.getFrom(),
                    reopenWith.getTo(),
                    reopenWith.getCustomer(),
                    reopenWith.getProject(),
                    reopenWith.getTask(),
                    null,
                    null,
                    0,
                    null
                )
                : showActivityDialog(i18n("activity.edit.title"), selected);
            if (!input.isPresent()) {
                return;
            }
            ActivityInput value = input.get();
            Optional<ActivityInput> toApply = checkOverlapAndConfirm(value, selected);
            if (toApply.isPresent()) {
                ActivityInput adjusted = toApply.get();
                selected.setCustomerId(adjusted.getCustomer().getId());
                selected.setProjectId(adjusted.getProject().getId());
                selected.setTaskId(adjusted.getTask().getId());
                selected.setFrom(formatDateTime(adjusted.getFrom()));
                selected.setTo(formatDateTime(adjusted.getTo()));
                lastActivity = selected;
                refreshAllActivityTables();
                saveData();
                applyFilters();
                syncAllCalendarsFromActivities();
                return;
            }
            reopenWith = value;
        }
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

    private void addPause() {
        Activity selected = getSelectedActivity();
        if (selected == null) {
            showInfo(i18n("activity.delete.select"));
            return;
        }
        LocalDateTime from = Activity.parseStoredDateTime(selected.getFrom());
        LocalDateTime to = Activity.parseStoredDateTime(selected.getTo());
        if (from == null || to == null || !from.isBefore(to)) {
            showInfo(i18n("activity.pause.validation.within"));
            return;
        }
        PauseDialogView pauseView = new PauseDialogView(messages, currentLocale);
        Optional<LocalDateTime[]> result = pauseView.show(from, to, settings.getTimeGridMinutes(), primaryStage);
        if (!result.isPresent()) {
            return;
        }
        LocalDateTime pauseStart = result.get()[0];
        LocalDateTime pauseEnd = result.get()[1];
        int index = activities.indexOf(selected);
        Activity firstPart = new Activity(
            selected.getCustomerId(),
            selected.getProjectId(),
            selected.getTaskId(),
            Activity.formatDateTime(from),
            Activity.formatDateTime(pauseStart)
        );
        Activity secondPart = new Activity(
            selected.getCustomerId(),
            selected.getProjectId(),
            selected.getTaskId(),
            Activity.formatDateTime(pauseEnd),
            Activity.formatDateTime(to)
        );
        activities.remove(selected);
        activities.add(index, firstPart);
        activities.add(index + 1, secondPart);
        if (selected == lastActivity) {
            lastActivity = secondPart;
        }
        saveData();
        applyFilters();
        TableView<Activity> currentTable = getCurrentActivityTable();
        if (currentTable != null) {
            currentTable.getSelectionModel().select(secondPart);
        }
        refreshAllActivityTables();
    }

    private void refreshAllActivityTables() {
        if (activityTable != null) {
            activityTable.refresh();
        }
        if (viewTabPane != null) {
            for (Tab tab : viewTabPane.getTabs()) {
                Object ud = tab.getUserData();
                if (ud instanceof ViewTabData) {
                    ((ViewTabData) ud).table.refresh();
                }
            }
        }
    }

    /** Table for the currently selected tab (main tab or a dynamic view tab); null for calendar tab. */
    private TableView<Activity> getCurrentActivityTable() {
        if (viewTabPane == null || activityTable == null) {
            return activityTable;
        }
        int idx = viewTabPane.getSelectionModel().getSelectedIndex();
        if (idx == 0) {
            return activityTable;
        }
        if (idx == CALENDAR_TAB_INDEX) {
            return null;
        }
        Tab tab = viewTabPane.getTabs().get(idx);
        Object ud = tab.getUserData();
        if (ud instanceof ViewTabData) {
            return ((ViewTabData) ud).table;
        }
        return activityTable;
    }

    private Activity getSelectedActivity() {
        TableView<Activity> table = getCurrentActivityTable();
        return table != null ? table.getSelectionModel().getSelectedItem() : null;
    }

    private Optional<ActivityInput> showActivityDialog(String title, Activity existing) {
        return showActivityDialog(title, existing, null, null, null, null, null, null, null, 0, null);
    }

    private Optional<ActivityInput> showActivityDialog(
        String title,
        Activity existing,
        LocalDateTime defaultFrom,
        LocalDateTime defaultTo,
        Customer defaultCustomer,
        Project defaultProject,
        Task defaultTask,
        Function<Customer, DefaultProjectAndTask> defaultSelectionForCustomer,
        java.util.function.BiFunction<Customer, Project, LocalDateTime[]> suggestedRangeForSelection,
        int endTimeRefreshIntervalMinutes,
        java.time.LocalTime maxEndTimeToday
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
            if (defaultFrom != null && defaultTo != null) {
                fromDateTime = defaultFrom;
                toDateTime = defaultTo;
            }
            if (defaultCustomer != null && defaultProject != null && defaultTask != null) {
                resolvedCustomer = defaultCustomer;
                resolvedProject = defaultProject;
                resolvedTask = defaultTask;
            }
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
            settings.getTimeGridMinutes(),
            defaultSelectionForCustomer,
            this::getLastTaskForProject,
            suggestedRangeForSelection,
            this::openManagementDialog,
            primaryStage,
            endTimeRefreshIntervalMinutes,
            maxEndTimeToday,
            () -> activityDialogOpen = true,
            () -> activityDialogOpen = false
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

    /** Chronologically last activity for the given customer (by latest "to" time). */
    private Activity findLastActivityForCustomer(String customerId) {
        if (customerId == null) {
            return null;
        }
        Activity last = null;
        LocalDateTime lastTo = null;
        for (Activity a : activities) {
            if (!customerId.equals(a.getCustomerId())) {
                continue;
            }
            LocalDateTime to = Activity.parseStoredDateTime(a.getTo());
            if (to == null) {
                continue;
            }
            if (lastTo == null || to.isAfter(lastTo)) {
                lastTo = to;
                last = a;
            }
        }
        return last;
    }

    /** Chronologically last activity for the given customer and project (by latest "to" time). */
    private Activity findLastActivityForProject(String customerId, String projectId) {
        if (customerId == null || projectId == null) {
            return null;
        }
        Activity last = null;
        LocalDateTime lastTo = null;
        for (Activity a : activities) {
            if (!customerId.equals(a.getCustomerId()) || !projectId.equals(a.getProjectId())) {
                continue;
            }
            LocalDateTime to = Activity.parseStoredDateTime(a.getTo());
            if (to == null) {
                continue;
            }
            if (lastTo == null || to.isAfter(lastTo)) {
                lastTo = to;
                last = a;
            }
        }
        return last;
    }

    /** Last task used for the given project (same customer context), or first task of the project. */
    private Task getLastTaskForProject(Customer customer, Project project) {
        if (customer == null || project == null) {
            return null;
        }
        Activity last = findLastActivityForProject(customer.getId(), project.getId());
        if (last != null) {
            Task t = findTaskById(project, last.getTaskId());
            if (t != null) {
                return t;
            }
        }
        return project.getTasks().isEmpty() ? null : project.getTasks().get(0);
    }

    /** Last project and task used for the given customer, or first project and first task. */
    private DefaultProjectAndTask getLastProjectAndTaskForCustomer(Customer customer) {
        if (customer == null) {
            return null;
        }
        Activity last = findLastActivityForCustomer(customer.getId());
        if (last != null) {
            Project p = findProjectById(customer, last.getProjectId());
            if (p != null) {
                Task t = findTaskById(p, last.getTaskId());
                if (t != null) {
                    return new DefaultProjectAndTask(p, t);
                }
                return new DefaultProjectAndTask(p, p.getTasks().isEmpty() ? null : p.getTasks().get(0));
            }
        }
        if (customer.getProjects().isEmpty()) {
            return new DefaultProjectAndTask(null, null);
        }
        Project firstProject = customer.getProjects().get(0);
        Task firstTask = firstProject.getTasks().isEmpty() ? null : firstProject.getTasks().get(0);
        return new DefaultProjectAndTask(firstProject, firstTask);
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
        refreshAllActivityTables();
        syncAllCalendarsFromActivities();
    }

    private void removeActivitiesForProject(String customerId, String projectId) {
        activities.removeIf(activity -> customerId.equals(activity.getCustomerId())
            && projectId.equals(activity.getProjectId()));
        refreshAllActivityTables();
        syncAllCalendarsFromActivities();
    }

    private void removeActivitiesForTask(String customerId, String projectId, String taskId) {
        activities.removeIf(activity -> customerId.equals(activity.getCustomerId())
            && projectId.equals(activity.getProjectId())
            && taskId.equals(activity.getTaskId()));
        refreshAllActivityTables();
        syncAllCalendarsFromActivities();
    }

    private void loadData() {
        Path dataPath = resolveDataPath();
        if (!Files.exists(dataPath)) {
            return;
        }
        try {
            StorageData data = storageService.load(dataPath);
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
                        customerData.timesheetSheetNo,
                        customerData.timesheetTaskSeparator
                    );
                    customer.setCalendarColorPalette(CalendarColorPalette.fromName(customerData.calendarColorPalette));
                    if (customerData.calendarColorIndex != null && customerData.calendarColorIndex >= 0 && customerData.calendarColorIndex <= 6) {
                        customer.setCalendarColorIndex(customerData.calendarColorIndex);
                    }
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
            restoreViewTabsPreferences();
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
            customerData.timesheetSheetNo = customer.getTimesheetSheetNo();
            customerData.timesheetTaskSeparator = customer.getTimesheetTaskSeparator();
            customerData.calendarColorPalette = customer.getCalendarColorPalette().name();
            customerData.calendarColorIndex = customer.getCalendarColorIndex();
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
            Path dataPath = resolveDataPath();
            Path parent = dataPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            storageService.save(dataPath, data);
            syncAllCalendarsFromActivities();
        } catch (IOException exception) {
            showInfo(i18n("data.save.error", exception.getMessage()));
        }
    }

    private Path resolveDataPath() {
        String directory = settings.getDataDirectory();
        if (directory == null || directory.isBlank()) {
            directory = AppSettings.DEFAULT_DATA_DIRECTORY;
        }
        return Paths.get(directory).resolve(DATA_FILENAME);
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
            settings.setReminderWeekdays(EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY));
        } else {
            settings.setLanguage(Language.fromCode(data.language));
            settings.setTimeGridMinutes(data.timeGridMinutes);
            settings.setReminderIntervalMinutes(data.reminderIntervalMinutes);
            LocalTime start = parseTimeValue(data.reminderStart, LocalTime.of(9, 0));
            LocalTime end = parseTimeValue(data.reminderEnd, LocalTime.of(17, 0));
            settings.setReminderWindow(start, end);
            Set<DayOfWeek> weekdays = parseWeekdaysValue(data.reminderWeekdays);
            if (weekdays != null) {
                settings.setReminderWeekdays(weekdays);
            }
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
        Locale.setDefault(currentLocale);
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
        addEscapeToClose(alert);
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
        helpDialog = null;
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
        if (viewMenu != null) {
            viewMenu.setText(i18n("menu.view"));
        }
        if (newViewMenuItem != null) {
            newViewMenuItem.setText(i18n("menu.view.new"));
        }
        if (addViewButton != null) {
            addViewButton.setText(i18n("menu.view.new"));
        }
        if (settingsMenu != null) {
            settingsMenu.setText(i18n("menu.settings"));
        }
        if (manageMenuItem != null) {
            manageMenuItem.setText(i18n("menu.manage.open"));
        }
        if (manageButton != null) {
            manageButton.setText(i18n("menu.manage.open"));
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
        if (exitMenuItem != null) {
            exitMenuItem.setText(i18n("menu.file.exit"));
        }
        if (addActivityMenuItem != null) {
            addActivityMenuItem.setText(i18n("menu.activity.add"));
        }
        if (addActivityToolbarButton != null) {
            addActivityToolbarButton.setText(i18n("menu.activity.add"));
        }
        if (writeTimesheetButton != null) {
            writeTimesheetButton.setText(i18n("menu.file.timesheet"));
        }
        if (editActivityMenuItem != null) {
            editActivityMenuItem.setText(i18n("menu.activity.edit"));
        }
        if (deleteActivityMenuItem != null) {
            deleteActivityMenuItem.setText(i18n("menu.activity.delete"));
        }
        if (consolidateActivityMenuItem != null) {
            consolidateActivityMenuItem.setText(i18n("menu.activity.consolidate"));
        }
        if (settingsMenuItem != null) {
            settingsMenuItem.setText(i18n("menu.settings.open"));
        }
        if (settingsButton != null) {
            settingsButton.setText(i18n("menu.settings.open"));
        }
        if (helpMenu != null) {
            helpMenu.setText(i18n("menu.help"));
        }
        if (helpMenuItem != null) {
            helpMenuItem.setText(i18n("menu.help"));
        }
        if (contextAddActivityItem != null) {
            contextAddActivityItem.setText(i18n("menu.activity.add"));
        }
        if (contextEditActivityItem != null) {
            contextEditActivityItem.setText(i18n("menu.activity.edit"));
        }
        if (contextAddPauseItem != null) {
            contextAddPauseItem.setText(i18n("menu.activity.addPause"));
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
        if (consolidateButton != null) {
            consolidateButton.setText(i18n("filter.consolidate"));
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
            case DAY:
                return i18n("preset.day");
            case WEEK:
                return i18n("preset.week");
            case MONTH:
                return i18n("preset.month");
            default:
                return "";
        }
    }

    private static LocalDate startOfWeek(LocalDate date, DayOfWeek firstDayOfWeek) {
        return date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek));
    }

    private static LocalDate endOfWeek(LocalDate date, DayOfWeek firstDayOfWeek) {
        return startOfWeek(date, firstDayOfWeek).plusDays(6);
    }

    private String formatWeekRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) return "";
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd.MM.yyyy", currentLocale);
        return from.format(f) + " – " + to.format(f);
    }

    private String formatMonthRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) return "";
        return from.format(DateTimeFormatter.ofPattern("MMMM yyyy", currentLocale));
    }

    private String formatDay(LocalDate date) {
        if (date == null) return "";
        return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy", currentLocale));
    }

    private void updateViewPeriodNavLabel(Label label, HBox navBox, PresetRange preset, LocalDate from, LocalDate to) {
        if (label == null) return;
        if (preset == PresetRange.DAY && from != null && to != null) {
            label.setText(formatDay(from));
            if (navBox != null) navBox.setVisible(true);
        } else if (preset == PresetRange.WEEK && from != null && to != null) {
            label.setText(formatWeekRange(from, to));
            if (navBox != null) navBox.setVisible(true);
        } else if (preset == PresetRange.MONTH && from != null && to != null) {
            label.setText(formatMonthRange(from, to));
            if (navBox != null) navBox.setVisible(true);
        } else {
            label.setText("");
            if (navBox != null) navBox.setVisible(false);
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

    private Set<DayOfWeek> parseWeekdaysValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            Set<DayOfWeek> result = EnumSet.noneOf(DayOfWeek.class);
            for (String part : value.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    result.add(DayOfWeek.valueOf(trimmed));
                }
            }
            return result.isEmpty() ? null : result;
        } catch (Exception exception) {
            return null;
        }
    }

    private enum PresetRange {
        CUSTOM,
        DAY,
        WEEK,
        MONTH;

        static PresetRange fromPreference(String value) {
            if (value == null || value.isBlank()) {
                return CUSTOM;
            }
            String v = value.trim();
            try {
                PresetRange r = PresetRange.valueOf(v);
                return r;
            } catch (IllegalArgumentException ignored) { }
            switch (v) {
                case "Custom":
                    return CUSTOM;
                case "Today":
                case "Yesterday":
                case "TODAY":
                case "YESTERDAY":
                    return DAY;
                case "This Week":
                case "Last Week":
                    return WEEK;
                case "This Month":
                case "Last Month":
                    return MONTH;
                default:
                    return CUSTOM;
            }
        }

        static boolean isRelativePreset(String rawValue) {
            if (rawValue == null) return false;
            String v = rawValue.trim();
            return "TODAY".equals(v) || "YESTERDAY".equals(v) || "Today".equals(v) || "Yesterday".equals(v)
                || "THIS_WEEK".equals(v) || "LAST_WEEK".equals(v)
                || "THIS_MONTH".equals(v) || "LAST_MONTH".equals(v)
                || "This Week".equals(v) || "Last Week".equals(v)
                || "This Month".equals(v) || "Last Month".equals(v);
        }
    }

    private boolean confirmDelete(String title, String content) {
        ButtonType delete = new ButtonType(i18n("button.delete"), ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType(i18n("button.cancel"), ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(AlertType.CONFIRMATION, content, delete, cancel);
        alert.setTitle(title);
        alert.setHeaderText(null);
        addEscapeToClose(alert);
        return alert.showAndWait().orElse(cancel) == delete;
    }

    private void showInfo(String message) {
        Alert alert = new Alert(AlertType.INFORMATION, message, ButtonType.OK);
        alert.setHeaderText(null);
        addEscapeToClose(alert);
        alert.showAndWait();
    }

    private void sortCustomers() {
        FXCollections.sort(customers, Comparator.comparing(customer -> sortKey(customer.getName())));
        for (Customer customer : customers) {
            sortProjects(customer);
        }
    }

    private void sortProjects(Customer customer) {
        Collections.sort(customer.getProjects(), Comparator.comparing(project -> sortKey(project.getName())));
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
