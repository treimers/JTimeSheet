package treimers.net.jtimesheet.view;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Map;
import java.util.EnumSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.util.StringConverter;
import treimers.net.jtimesheet.model.AppSettings;
import treimers.net.jtimesheet.model.Language;

public class SettingsDialogView {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    /** Display for reminder end time "until midnight"; stored as 00:00. */
    private static final String END_OF_DAY_LABEL = "24:00";

    /** Option for reminder end: time value plus display label (so 00:00 and 24:00 can both be offered). */
    private record TimeDisplayOption(LocalTime time, String display) {}

    private final ResourceBundle messages;
    private final Locale locale;

    public SettingsDialogView(ResourceBundle messages, Locale locale) {
        this.messages = messages;
        this.locale = locale;
    }

    public Optional<SettingsResult> show(AppSettings settings) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(i18n("settings.title"));
        ButtonType saveButton = new ButtonType(i18n("button.save"), ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType(i18n("button.cancel"), ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, cancelButton);

        List<Integer> gridOptions = TIME_GRID_AND_REMINDER_OPTIONS;
        ComboBox<Integer> timeGridChoice = new ComboBox<>(FXCollections.observableArrayList(gridOptions));
        int currentGrid = settings.getTimeGridMinutes();
        if (!timeGridChoice.getItems().contains(currentGrid)) {
            timeGridChoice.getItems().add(currentGrid);
            FXCollections.sort(timeGridChoice.getItems());
        }
        timeGridChoice.getSelectionModel().select(Integer.valueOf(currentGrid));
        timeGridChoice.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(i18n("minutes.format", item));
            }
        });
        timeGridChoice.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer value) {
                return value == null ? "" : i18n("minutes.format", value);
            }

            @Override
            public Integer fromString(String string) {
                return currentGrid;
            }
        });

        List<Integer> reminderIntervalOptions = reminderIntervalsForTimeGrid(currentGrid);
        ComboBox<Integer> reminderIntervalChoice = new ComboBox<>(FXCollections.observableArrayList(reminderIntervalOptions));
        int currentInterval = settings.getReminderIntervalMinutes();
        int intervalToSelect = (currentInterval >= currentGrid && reminderIntervalOptions.contains(currentInterval))
            ? currentInterval
            : reminderIntervalOptions.get(0);
        reminderIntervalChoice.getSelectionModel().select(Integer.valueOf(intervalToSelect));
        timeGridChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldGrid, newGrid) -> {
            if (newGrid == null) {
                return;
            }
            List<Integer> allowed = reminderIntervalsForTimeGrid(newGrid);
            reminderIntervalChoice.getItems().setAll(allowed);
            Integer selected = reminderIntervalChoice.getValue();
            if (selected == null || selected < newGrid || !reminderIntervalChoice.getItems().contains(selected)) {
                reminderIntervalChoice.getSelectionModel().select(allowed.isEmpty() ? null : allowed.get(0));
            }
        });
        reminderIntervalChoice.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(i18n("minutes.format", item));
            }
        });
        reminderIntervalChoice.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer value) {
                return value == null ? "" : i18n("minutes.format", value);
            }

            @Override
            public Integer fromString(String string) {
                return currentInterval;
            }
        });

        List<LocalTime> timeOptions = createTimeOptions();
        ComboBox<LocalTime> reminderStartChoice = new ComboBox<>(FXCollections.observableArrayList(timeOptions));
        List<TimeDisplayOption> endTimeOptions = createEndTimeOptions();
        ComboBox<TimeDisplayOption> reminderEndChoice = new ComboBox<>(FXCollections.observableArrayList(endTimeOptions));
        LocalTime currentStart = settings.getReminderStartTime();
        LocalTime currentEnd = settings.getReminderEndTime();
        ensureTimeOption(reminderStartChoice, currentStart);
        reminderStartChoice.getSelectionModel().select(currentStart);
        selectEndTimeOption(reminderEndChoice, currentEnd);
        reminderStartChoice.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(LocalTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(TIME_FORMAT.format(item));
            }
        });
        reminderEndChoice.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(TimeDisplayOption item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item.display());
            }
        });
        reminderStartChoice.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalTime value) {
                return value == null ? "" : TIME_FORMAT.format(value);
            }

            @Override
            public LocalTime fromString(String string) {
                return currentStart;
            }
        });
        reminderEndChoice.setConverter(new StringConverter<>() {
            @Override
            public String toString(TimeDisplayOption value) {
                return value == null ? "" : value.display();
            }

            @Override
            public TimeDisplayOption fromString(String string) {
                return reminderEndChoice.getValue();
            }
        });

        Set<DayOfWeek> currentWeekdays = settings.getReminderWeekdays();
        Map<DayOfWeek, CheckBox> weekdayCheckBoxes = new java.util.HashMap<>();
        HBox weekdaysBox = new HBox(8);
        for (DayOfWeek day : DayOfWeek.values()) {
            CheckBox cb = new CheckBox(day.getDisplayName(TextStyle.SHORT_STANDALONE, locale));
            cb.setSelected(currentWeekdays.contains(day));
            weekdayCheckBoxes.put(day, cb);
            weekdaysBox.getChildren().add(cb);
        }

        ComboBox<DayOfWeek> firstDayOfWeekChoice = new ComboBox<>(FXCollections.observableArrayList(DayOfWeek.values()));
        firstDayOfWeekChoice.getSelectionModel().select(settings.getFirstDayOfWeek());
        firstDayOfWeekChoice.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(DayOfWeek item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item.getDisplayName(TextStyle.FULL_STANDALONE, locale));
            }
        });
        firstDayOfWeekChoice.setConverter(new StringConverter<>() {
            @Override
            public String toString(DayOfWeek value) {
                return value == null ? "" : value.getDisplayName(TextStyle.FULL_STANDALONE, locale);
            }

            @Override
            public DayOfWeek fromString(String string) {
                return settings.getFirstDayOfWeek();
            }
        });

        ComboBox<Language> languageChoice = new ComboBox<>(FXCollections.observableArrayList(Language.ENGLISH, Language.GERMAN));
        languageChoice.getSelectionModel().select(settings.getLanguage());
        languageChoice.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Language item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(languageLabel(item));
            }
        });
        languageChoice.setConverter(new StringConverter<>() {
            @Override
            public String toString(Language value) {
                return value == null ? "" : languageLabel(value);
            }

            @Override
            public Language fromString(String string) {
                return settings.getLanguage();
            }
        });

        TextField dataDirectoryField = new TextField();
        dataDirectoryField.setText(settings.getDataDirectory());
        Button browseButton = new Button(i18n("settings.dataFolder.browse"));
        Button homeButton = new Button(i18n("settings.dataFolder.home"));
        browseButton.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle(i18n("settings.dataFolder.label"));
            File initialDirectory = new File(dataDirectoryField.getText());
            if (initialDirectory.exists() && initialDirectory.isDirectory()) {
                chooser.setInitialDirectory(initialDirectory);
            }
            File selected = chooser.showDialog(dialog.getDialogPane().getScene().getWindow());
            if (selected != null) {
                dataDirectoryField.setText(selected.getAbsolutePath());
            }
        });
        homeButton.setOnAction(event -> dataDirectoryField.setText(AppSettings.DEFAULT_DATA_DIRECTORY));
        HBox dataDirectoryBox = new HBox(8, dataDirectoryField, browseButton, homeButton);
        HBox.setHgrow(dataDirectoryField, Priority.ALWAYS);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 60, 10, 10));
        grid.add(new Label(i18n("settings.timeGrid.label")), 0, 0);
        grid.add(timeGridChoice, 1, 0);
        grid.add(new Label(i18n("settings.reminder.interval.label")), 0, 1);
        grid.add(reminderIntervalChoice, 1, 1);
        grid.add(new Label(i18n("settings.reminder.start.label")), 0, 2);
        grid.add(reminderStartChoice, 1, 2);
        grid.add(new Label(i18n("settings.reminder.end.label")), 0, 3);
        grid.add(reminderEndChoice, 1, 3);
        grid.add(new Label(i18n("settings.reminder.weekdays.label")), 0, 4);
        grid.add(weekdaysBox, 1, 4);
        grid.add(new Label(i18n("settings.firstDayOfWeek.label")), 0, 5);
        grid.add(firstDayOfWeekChoice, 1, 5);
        grid.add(new Label(i18n("settings.language.label")), 0, 6);
        grid.add(languageChoice, 1, 6);
        grid.add(new Label(i18n("settings.dataFolder.label")), 0, 7);
        grid.add(dataDirectoryBox, 1, 7);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> button == saveButton ? button : null);
        dialog.setOnShown(e -> {
            ((Button) dialog.getDialogPane().lookupButton(saveButton)).setDefaultButton(true);
            dialog.getDialogPane().getScene().addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
                if (ev.getCode() == KeyCode.ESCAPE) {
                    dialog.setResult(null);
                    dialog.close();
                    ev.consume();
                }
            });
        });
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return Optional.empty();
        }
        Set<DayOfWeek> selectedWeekdays = EnumSet.noneOf(DayOfWeek.class);
        for (Map.Entry<DayOfWeek, CheckBox> e : weekdayCheckBoxes.entrySet()) {
            if (e.getValue().isSelected()) {
                selectedWeekdays.add(e.getKey());
            }
        }
        TimeDisplayOption selectedEnd = reminderEndChoice.getValue();
        return Optional.of(new SettingsResult(
            timeGridChoice.getValue(),
            reminderIntervalChoice.getValue(),
            reminderStartChoice.getValue(),
            selectedEnd != null ? selectedEnd.time() : null,
            selectedWeekdays,
            firstDayOfWeekChoice.getValue(),
            languageChoice.getValue(),
            dataDirectoryField.getText()
        ));
    }

    /** Allowed values for Time Grid and Reminder interval (divisors of 60). */
    private static final List<Integer> TIME_GRID_AND_REMINDER_OPTIONS = List.of(1, 2, 3, 4, 5, 6, 10, 12, 15, 20, 30, 60);

    /** Reminder intervals that are not smaller than the time grid (same option set as grid). */
    private static List<Integer> reminderIntervalsForTimeGrid(int timeGridMinutes) {
        List<Integer> result = new ArrayList<>();
        for (Integer v : TIME_GRID_AND_REMINDER_OPTIONS) {
            if (v >= timeGridMinutes) {
                result.add(v);
            }
        }
        return result.isEmpty() ? List.of(timeGridMinutes) : result;
    }

    private List<LocalTime> createTimeOptions() {
        List<LocalTime> options = new ArrayList<>();
        int stepMinutes = 15;
        int steps = (24 * 60) / stepMinutes;
        for (int i = 0; i < steps; i++) {
            options.add(LocalTime.MIDNIGHT.plusMinutes((long) i * stepMinutes));
        }
        return options;
    }

    /** End time options: 00:00, 00:15, … 23:45 and additionally 24:00 (stored as 00:00). */
    private List<TimeDisplayOption> createEndTimeOptions() {
        List<TimeDisplayOption> options = new ArrayList<>();
        for (LocalTime t : createTimeOptions()) {
            options.add(new TimeDisplayOption(t, TIME_FORMAT.format(t)));
        }
        options.add(new TimeDisplayOption(LocalTime.MIDNIGHT, END_OF_DAY_LABEL));
        return options;
    }

    private void selectEndTimeOption(ComboBox<TimeDisplayOption> comboBox, LocalTime currentEnd) {
        if (currentEnd == null) {
            return;
        }
        if (currentEnd.equals(LocalTime.MIDNIGHT)) {
            for (TimeDisplayOption opt : comboBox.getItems()) {
                if (END_OF_DAY_LABEL.equals(opt.display())) {
                    comboBox.getSelectionModel().select(opt);
                    return;
                }
            }
        }
        for (TimeDisplayOption opt : comboBox.getItems()) {
            if (currentEnd.equals(opt.time()) && !END_OF_DAY_LABEL.equals(opt.display())) {
                comboBox.getSelectionModel().select(opt);
                return;
            }
        }
    }

    private void ensureTimeOption(ComboBox<LocalTime> comboBox, LocalTime value) {
        if (value == null) {
            return;
        }
        if (!comboBox.getItems().contains(value)) {
            comboBox.getItems().add(value);
            comboBox.getItems().sort(Comparator.naturalOrder());
        }
    }

    private String languageLabel(Language language) {
        if (language == null) {
            return "";
        }
        if (language == Language.GERMAN) {
            return i18n("language.german");
        }
        return i18n("language.english");
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

    public static class SettingsResult {
        private final int timeGridMinutes;
        private final int reminderIntervalMinutes;
        private final LocalTime reminderStart;
        private final LocalTime reminderEnd;
        private final Set<DayOfWeek> reminderWeekdays;
        private final DayOfWeek firstDayOfWeek;
        private final Language language;
        private final String dataDirectory;

        public SettingsResult(
            int timeGridMinutes,
            int reminderIntervalMinutes,
            LocalTime reminderStart,
            LocalTime reminderEnd,
            Set<DayOfWeek> reminderWeekdays,
            DayOfWeek firstDayOfWeek,
            Language language,
            String dataDirectory
        ) {
            this.timeGridMinutes = timeGridMinutes;
            this.reminderIntervalMinutes = reminderIntervalMinutes;
            this.reminderStart = reminderStart;
            this.reminderEnd = reminderEnd;
            this.reminderWeekdays = reminderWeekdays;
            this.firstDayOfWeek = firstDayOfWeek != null ? firstDayOfWeek : DayOfWeek.MONDAY;
            this.language = language;
            this.dataDirectory = dataDirectory;
        }

        public int getTimeGridMinutes() {
            return timeGridMinutes;
        }

        public int getReminderIntervalMinutes() {
            return reminderIntervalMinutes;
        }

        public LocalTime getReminderStart() {
            return reminderStart;
        }

        public LocalTime getReminderEnd() {
            return reminderEnd;
        }

        public Set<DayOfWeek> getReminderWeekdays() {
            return reminderWeekdays;
        }

        public DayOfWeek getFirstDayOfWeek() {
            return firstDayOfWeek;
        }

        public Language getLanguage() {
            return language;
        }

        public String getDataDirectory() {
            return dataDirectory;
        }
    }
}
