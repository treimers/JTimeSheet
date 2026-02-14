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

        ComboBox<Integer> timeGridChoice = new ComboBox<>(FXCollections.observableArrayList(1, 6, 15));
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

        ComboBox<Integer> reminderIntervalChoice = new ComboBox<>(FXCollections.observableArrayList(15, 30, 60));
        int currentInterval = settings.getReminderIntervalMinutes();
        if (!reminderIntervalChoice.getItems().contains(currentInterval)) {
            reminderIntervalChoice.getItems().add(currentInterval);
            FXCollections.sort(reminderIntervalChoice.getItems());
        }
        reminderIntervalChoice.getSelectionModel().select(Integer.valueOf(currentInterval));
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
        ComboBox<LocalTime> reminderEndChoice = new ComboBox<>(FXCollections.observableArrayList(timeOptions));
        LocalTime currentStart = settings.getReminderStartTime();
        LocalTime currentEnd = settings.getReminderEndTime();
        ensureTimeOption(reminderStartChoice, currentStart);
        ensureTimeOption(reminderEndChoice, currentEnd);
        reminderStartChoice.getSelectionModel().select(currentStart);
        reminderEndChoice.getSelectionModel().select(currentEnd);
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
            protected void updateItem(LocalTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(TIME_FORMAT.format(item));
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
            public String toString(LocalTime value) {
                return value == null ? "" : TIME_FORMAT.format(value);
            }

            @Override
            public LocalTime fromString(String string) {
                return currentEnd;
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
        grid.add(new Label(i18n("settings.language.label")), 0, 5);
        grid.add(languageChoice, 1, 5);
        grid.add(new Label(i18n("settings.dataFolder.label")), 0, 6);
        grid.add(dataDirectoryBox, 1, 6);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> button == saveButton ? button : null);
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
        if (selectedWeekdays.isEmpty()) {
            selectedWeekdays = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
        }
        return Optional.of(new SettingsResult(
            timeGridChoice.getValue(),
            reminderIntervalChoice.getValue(),
            reminderStartChoice.getValue(),
            reminderEndChoice.getValue(),
            selectedWeekdays,
            languageChoice.getValue(),
            dataDirectoryField.getText()
        ));
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
        private final Language language;
        private final String dataDirectory;

        public SettingsResult(
            int timeGridMinutes,
            int reminderIntervalMinutes,
            LocalTime reminderStart,
            LocalTime reminderEnd,
            Set<DayOfWeek> reminderWeekdays,
            Language language,
            String dataDirectory
        ) {
            this.timeGridMinutes = timeGridMinutes;
            this.reminderIntervalMinutes = reminderIntervalMinutes;
            this.reminderStart = reminderStart;
            this.reminderEnd = reminderEnd;
            this.reminderWeekdays = reminderWeekdays;
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

        public Language getLanguage() {
            return language;
        }

        public String getDataDirectory() {
            return dataDirectory;
        }
    }
}
