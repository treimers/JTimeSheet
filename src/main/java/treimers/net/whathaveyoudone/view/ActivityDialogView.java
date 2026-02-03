package treimers.net.whathaveyoudone.view;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.geometry.Insets;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import treimers.net.whathaveyoudone.model.Activity;
import treimers.net.whathaveyoudone.model.AppSettings;
import treimers.net.whathaveyoudone.model.Customer;
import treimers.net.whathaveyoudone.model.Project;
import treimers.net.whathaveyoudone.model.Task;
import treimers.net.whathaveyoudone.ui.ActivityInput;

public class ActivityDialogView {
    private final ResourceBundle messages;
    private final Locale locale;

    public ActivityDialogView(ResourceBundle messages, Locale locale) {
        this.messages = messages;
        this.locale = locale;
    }

    public Optional<ActivityInput> show(
        String title,
        ObservableList<Customer> customers,
        LocalDateTime defaultFrom,
        LocalDateTime defaultTo,
        Customer defaultCustomer,
        Project defaultProject,
        Task defaultTask,
        int timeGridMinutes
    ) {
        Dialog<ActivityInput> dialog = new Dialog<>();
        dialog.setTitle(title);
        ButtonType saveButton = new ButtonType(i18n("button.save"), ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType(i18n("button.cancel"), ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, cancelButton);

        ChoiceBox<Customer> customerChoice = new ChoiceBox<>(customers);
        ChoiceBox<Project> projectChoice = new ChoiceBox<>();
        ChoiceBox<Task> taskChoice = new ChoiceBox<>();

        DatePicker fromDatePicker = new DatePicker();
        DatePicker toDatePicker = new DatePicker();
        ComboBox<Integer> fromHourChoice = createHourChoiceBox();
        ComboBox<Integer> fromMinuteChoice = createMinuteChoiceBox(timeGridMinutes);
        ComboBox<Integer> toHourChoice = createHourChoiceBox();
        ComboBox<Integer> toMinuteChoice = createMinuteChoiceBox(timeGridMinutes);

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

        LocalDateTime fromDateTime = defaultFrom;
        LocalDateTime toDateTime = defaultTo;
        if (fromDateTime == null || toDateTime == null) {
            LocalDateTime now = LocalDateTime.now();
            fromDateTime = now;
            toDateTime = now.plusHours(1);
        }
        fromDateTime = alignToGrid(fromDateTime, timeGridMinutes);
        toDateTime = alignToGrid(toDateTime, timeGridMinutes);

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
        selectMinute(fromMinuteChoice, fromDateTime.getMinute());

        toDatePicker.setValue(toDateTime.toLocalDate());
        toHourChoice.getSelectionModel().select(Integer.valueOf(toDateTime.getHour()));
        selectMinute(toMinuteChoice, toDateTime.getMinute());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label(i18n("activity.field.customer")), 0, 0);
        grid.add(customerChoice, 1, 0);
        grid.add(new Label(i18n("activity.field.project")), 0, 1);
        grid.add(projectChoice, 1, 1);
        grid.add(new Label(i18n("activity.field.task")), 0, 2);
        grid.add(taskChoice, 1, 2);
        grid.add(new Label(i18n("activity.field.from")), 0, 3);
        grid.add(createDateTimeRow(fromDatePicker, fromHourChoice, fromMinuteChoice), 1, 3);
        grid.add(new Label(i18n("activity.field.to")), 0, 4);
        grid.add(createDateTimeRow(toDatePicker, toHourChoice, toMinuteChoice), 1, 4);
        grid.add(new Label(i18n("activity.field.duration")), 0, 5);
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

        dialog.getDialogPane().lookupButton(saveButton).addEventFilter(ActionEvent.ACTION, event -> {
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
                showInfo(i18n("activity.validation.range"));
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
            showInfo(i18n("activity.validation.customer"));
            return null;
        }
        if (project == null) {
            showInfo(i18n("activity.validation.project"));
            return null;
        }
        if (task == null || task.getName() == null || task.getName().isBlank()) {
            showInfo(i18n("activity.validation.task"));
            return null;
        }
        LocalDateTime from = buildDateTime(TimeField.FROM, fromDate, fromHour, fromMinute);
        if (from == null) {
            return null;
        }
        LocalDateTime to = buildDateTime(TimeField.TO, toDate, toHour, toMinute);
        if (to == null) {
            return null;
        }
        return new ActivityInput(customer, project, task, from, to);
    }

    private LocalDateTime buildDateTime(TimeField label, LocalDate date, Integer hour, Integer minute) {
        if (date == null) {
            showInfo(i18n("activity.validation.date", timeFieldLabel(label)));
            return null;
        }
        if (hour == null || minute == null) {
            showInfo(i18n("activity.validation.time", timeFieldLabel(label)));
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

    private ComboBox<Integer> createMinuteChoiceBox(int timeGridMinutes) {
        List<Integer> minutes = new ArrayList<>();
        int step = AppSettings.normalizeTimeGridMinutes(timeGridMinutes);
        for (int i = 0; i < 60; i += step) {
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

    private LocalDateTime alignToGrid(LocalDateTime value, int timeGridMinutes) {
        if (value == null) {
            return null;
        }
        int step = AppSettings.normalizeTimeGridMinutes(timeGridMinutes);
        if (step <= 1) {
            return value.withSecond(0).withNano(0);
        }
        int minute = value.getMinute();
        int rounded = (minute / step) * step;
        return value.withMinute(rounded).withSecond(0).withNano(0);
    }

    private void selectMinute(ComboBox<Integer> box, int minute) {
        if (!box.getItems().contains(minute)) {
            box.getItems().add(minute);
            FXCollections.sort(box.getItems());
        }
        box.getSelectionModel().select(Integer.valueOf(minute));
    }

    private String timeFieldLabel(TimeField field) {
        if (field == TimeField.TO) {
            return i18n("activity.field.to.short");
        }
        return i18n("activity.field.from.short");
    }

    private void showInfo(String message) {
        Alert alert = new Alert(AlertType.INFORMATION, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
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

    private enum TimeField {
        FROM,
        TO
    }
}
