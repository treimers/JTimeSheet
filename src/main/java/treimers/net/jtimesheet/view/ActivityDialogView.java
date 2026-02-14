package treimers.net.jtimesheet.view;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;
import javafx.scene.layout.HBox;
import treimers.net.jtimesheet.model.Activity;
import treimers.net.jtimesheet.model.AppSettings;
import treimers.net.jtimesheet.model.Customer;
import treimers.net.jtimesheet.model.Project;
import treimers.net.jtimesheet.model.Task;
import treimers.net.jtimesheet.ui.ActivityInput;
import treimers.net.jtimesheet.ui.DefaultProjectAndTask;

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
        int timeGridMinutes,
        Function<Customer, DefaultProjectAndTask> defaultSelectionForCustomer,
        Window owner
    ) {
        Dialog<ActivityInput> dialog = new Dialog<>();
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setTitle(title);
        ButtonType saveButton = new ButtonType(i18n("button.save"), ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType(i18n("button.cancel"), ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, cancelButton);

        ChoiceBox<Customer> customerChoice = new ChoiceBox<>(customers);
        ChoiceBox<Project> projectChoice = new ChoiceBox<>();
        ChoiceBox<Task> taskChoice = new ChoiceBox<>();

        DatePicker datePicker = new DatePicker();
        ComboBox<Integer> fromHourChoice = createHourChoiceBox();
        ComboBox<Integer> fromMinuteChoice = createMinuteChoiceBox(timeGridMinutes);
        ComboBox<Integer> toHourChoice = createHourChoiceBox();
        ComboBox<Integer> toMinuteChoice = createMinuteChoiceBox(timeGridMinutes);

        TextField durationTextField = new TextField();
        durationTextField.setPrefWidth(80);
        durationTextField.setPromptText("0:00");

        customerChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null) {
                projectChoice.setItems(FXCollections.observableArrayList());
                taskChoice.setItems(FXCollections.observableArrayList());
            } else {
                projectChoice.setItems(newValue.getProjects());
                projectChoice.getSelectionModel().clearSelection();
                taskChoice.setItems(FXCollections.observableArrayList());
                if (defaultSelectionForCustomer != null) {
                    DefaultProjectAndTask pt = defaultSelectionForCustomer.apply(newValue);
                    if (pt != null && pt.getProject() != null && newValue.getProjects().contains(pt.getProject())) {
                        projectChoice.getSelectionModel().select(pt.getProject());
                        if (pt.getTask() != null && pt.getProject().getTasks().contains(pt.getTask())) {
                            taskChoice.getSelectionModel().select(pt.getTask());
                        } else if (!pt.getProject().getTasks().isEmpty()) {
                            taskChoice.getSelectionModel().select(pt.getProject().getTasks().get(0));
                        }
                    } else if (!newValue.getProjects().isEmpty()) {
                        Project firstProject = newValue.getProjects().get(0);
                        projectChoice.getSelectionModel().select(firstProject);
                        if (!firstProject.getTasks().isEmpty()) {
                            taskChoice.getSelectionModel().select(firstProject.getTasks().get(0));
                        }
                    }
                }
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

        LocalDate date = fromDateTime.toLocalDate();
        datePicker.setValue(date);
        fromHourChoice.getSelectionModel().select(Integer.valueOf(fromDateTime.getHour()));
        selectMinute(fromMinuteChoice, fromDateTime.getMinute());
        toHourChoice.getSelectionModel().select(Integer.valueOf(toDateTime.getHour()));
        selectMinute(toMinuteChoice, toDateTime.getMinute());
        durationTextField.setText(formatDuration(fromDateTime, toDateTime));

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
        grid.add(new Label(i18n("activity.field.date")), 0, 3);
        grid.add(datePicker, 1, 3);
        grid.add(new Label(i18n("activity.field.from")), 0, 4);
        grid.add(createTimeRow(fromHourChoice, fromMinuteChoice), 1, 4);
        grid.add(new Label(i18n("activity.field.to")), 0, 5);
        grid.add(createTimeRow(toHourChoice, toMinuteChoice), 1, 5);
        grid.add(new Label(i18n("activity.field.duration")), 0, 6);
        grid.add(durationTextField, 1, 6);

        dialog.getDialogPane().setContent(grid);

        boolean[] updating = { false };
        Runnable updateToFromDuration = () -> {
            if (updating[0]) {
                return;
            }
            LocalDate selectedDate = datePicker.getValue();
            Long totalMinutes = parseDurationMinutes(durationTextField.getText());
            if (selectedDate == null || totalMinutes == null) {
                return;
            }
            totalMinutes = roundMinutesToGrid(totalMinutes, timeGridMinutes);
            LocalDateTime from = buildDateTimeOrNull(selectedDate, fromHourChoice.getValue(), fromMinuteChoice.getValue());
            if (from == null) {
                return;
            }
            LocalDateTime to = from.plusMinutes(totalMinutes);
            if (to.toLocalDate().isAfter(selectedDate)) {
                to = selectedDate.atTime(23, 59);
                to = alignToGrid(to, timeGridMinutes);
            }
            updating[0] = true;
            try {
                durationTextField.setText(formatDuration(from, to));
                setTimeSelection(toHourChoice, toMinuteChoice, to.getHour(), to.getMinute(), timeGridMinutes);
            } finally {
                updating[0] = false;
            }
        };

        Runnable updateDurationFromRange = () -> {
            if (updating[0]) {
                return;
            }
            LocalDateTime from = buildDateTimeOrNull(datePicker.getValue(), fromHourChoice.getValue(), fromMinuteChoice.getValue());
            LocalDateTime to = buildDateTimeOrNull(datePicker.getValue(), toHourChoice.getValue(), toMinuteChoice.getValue());
            if (from != null && to != null) {
                updating[0] = true;
                try {
                    durationTextField.setText(formatDuration(from, to));
                } finally {
                    updating[0] = false;
                }
            }
        };

        datePicker.valueProperty().addListener((obs, oldValue, newValue) -> updateDurationFromRange.run());
        fromHourChoice.valueProperty().addListener((obs, oldValue, newValue) -> updateToFromDuration.run());
        fromMinuteChoice.valueProperty().addListener((obs, oldValue, newValue) -> updateToFromDuration.run());
        durationTextField.setOnAction(e -> updateToFromDuration.run());
        durationTextField.focusedProperty().addListener((obs, wasFocused, nowFocused) -> {
            if (wasFocused && !nowFocused) {
                updateToFromDuration.run();
            }
        });
        toHourChoice.valueProperty().addListener((obs, oldValue, newValue) -> updateDurationFromRange.run());
        toMinuteChoice.valueProperty().addListener((obs, oldValue, newValue) -> updateDurationFromRange.run());

        dialog.getDialogPane().lookupButton(saveButton).addEventFilter(ActionEvent.ACTION, event -> {
            ActivityInput input = validateActivityInput(
                customerChoice.getSelectionModel().getSelectedItem(),
                projectChoice.getSelectionModel().getSelectedItem(),
                taskChoice.getSelectionModel().getSelectedItem(),
                datePicker.getValue(),
                fromHourChoice.getSelectionModel().getSelectedItem(),
                fromMinuteChoice.getSelectionModel().getSelectedItem(),
                datePicker.getValue(),
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
                datePicker.getValue(),
                fromHourChoice.getSelectionModel().getSelectedItem(),
                fromMinuteChoice.getSelectionModel().getSelectedItem(),
                datePicker.getValue(),
                toHourChoice.getSelectionModel().getSelectedItem(),
                toMinuteChoice.getSelectionModel().getSelectedItem()
            );
        });

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

    private HBox createTimeRow(ComboBox<Integer> hourChoice, ComboBox<Integer> minuteChoice) {
        Label colon = new Label(":");
        colon.setStyle("-fx-padding: 0 4 0 4;");
        return new HBox(6, hourChoice, colon, minuteChoice);
    }

    /** Rounds total minutes to the time grid (e.g. 91 with 15-min grid -> 90). */
    private long roundMinutesToGrid(long totalMinutes, int timeGridMinutes) {
        int step = AppSettings.normalizeTimeGridMinutes(timeGridMinutes);
        if (step <= 1) {
            return totalMinutes;
        }
        return (long) Math.round((double) totalMinutes / step) * step;
    }

    /** Parses duration string to total minutes. Accepts "h:mm", "hh:mm" (e.g. "1:30", "0:45") or plain minutes "90". */
    private Long parseDurationMinutes(String text) {
        if (text == null || (text = text.trim()).isEmpty()) {
            return null;
        }
        if (text.startsWith("-")) {
            return null;
        }
        if (text.contains(":")) {
            String[] parts = text.split(":", 2);
            if (parts.length != 2) {
                return null;
            }
            try {
                int hours = Integer.parseInt(parts[0].trim());
                int minutes = Integer.parseInt(parts[1].trim());
                if (hours < 0 || minutes < 0 || minutes >= 60) {
                    return null;
                }
                return (long) hours * 60 + minutes;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        try {
            int total = Integer.parseInt(text.trim());
            return total >= 0 ? (long) total : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void setTimeSelection(
        ComboBox<Integer> hourChoice,
        ComboBox<Integer> minuteChoice,
        int hour,
        int minute,
        int timeGridMinutes
    ) {
        if (hour >= 0 && hour < 24 && hourChoice.getItems().contains(hour)) {
            hourChoice.getSelectionModel().select(Integer.valueOf(hour));
        }
        selectMinute(minuteChoice, minute);
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
