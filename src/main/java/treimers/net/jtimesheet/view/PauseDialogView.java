package treimers.net.jtimesheet.view;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Window;
import treimers.net.jtimesheet.model.AppSettings;

public class PauseDialogView {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ResourceBundle messages;
    private final Locale locale;

    public PauseDialogView(ResourceBundle messages, Locale locale) {
        this.messages = messages;
        this.locale = locale;
    }

    /**
     * Shows a dialog to choose pause start and end within the activity range.
     * @return Optional of [pauseStart, pauseEnd] if user confirmed, empty otherwise.
     */
    public Optional<LocalDateTime[]> show(
        LocalDateTime activityFrom,
        LocalDateTime activityTo,
        int timeGridMinutes,
        Window owner
    ) {
        if (activityFrom == null || activityTo == null || !activityFrom.isBefore(activityTo)) {
            return Optional.empty();
        }
        int step = AppSettings.normalizeTimeGridMinutes(timeGridMinutes);
        long durationMinutes = java.time.Duration.between(activityFrom, activityTo).toMinutes();
        long halfMinutes = (durationMinutes / 2) / step * step;
        LocalDateTime defaultPauseStart = activityFrom.plusMinutes(halfMinutes);
        defaultPauseStart = alignToGrid(defaultPauseStart, timeGridMinutes);
        LocalDateTime defaultPauseEnd = defaultPauseStart.plusMinutes(Math.max(step, 15));
        if (!defaultPauseEnd.isBefore(activityTo) || defaultPauseEnd.equals(activityTo)) {
            defaultPauseEnd = activityTo;
        } else {
            defaultPauseEnd = alignToGrid(defaultPauseEnd, timeGridMinutes);
            if (!defaultPauseEnd.isBefore(activityTo)) {
                defaultPauseEnd = activityTo;
            }
        }
        if (!defaultPauseStart.isBefore(defaultPauseEnd)) {
            defaultPauseEnd = defaultPauseStart.plusMinutes(step);
            if (defaultPauseEnd.isAfter(activityTo)) {
                defaultPauseEnd = activityTo;
            }
        }

        Dialog<LocalDateTime[]> dialog = new Dialog<>();
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setTitle(i18n("activity.pause.title"));
        ButtonType saveButton = new ButtonType(i18n("button.save"), javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType(i18n("button.cancel"), javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, cancelButton);

        Label activityRangeLabel = new Label(i18n("activity.pause.activity.range",
            FORMATTER.format(activityFrom),
            FORMATTER.format(activityTo)));
        activityRangeLabel.setWrapText(true);

        DatePicker fromDatePicker = new DatePicker(defaultPauseStart.toLocalDate());
        ComboBox<Integer> fromHourChoice = createHourChoiceBox();
        ComboBox<Integer> fromMinuteChoice = createMinuteChoiceBox(timeGridMinutes);
        fromHourChoice.getSelectionModel().select(Integer.valueOf(defaultPauseStart.getHour()));
        selectMinute(fromMinuteChoice, defaultPauseStart.getMinute());

        DatePicker toDatePicker = new DatePicker(defaultPauseEnd.toLocalDate());
        ComboBox<Integer> toHourChoice = createHourChoiceBox();
        ComboBox<Integer> toMinuteChoice = createMinuteChoiceBox(timeGridMinutes);
        toHourChoice.getSelectionModel().select(Integer.valueOf(defaultPauseEnd.getHour()));
        selectMinute(toMinuteChoice, defaultPauseEnd.getMinute());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(activityRangeLabel, 0, 0, 2, 1);
        grid.add(new Label(i18n("activity.pause.from")), 0, 1);
        grid.add(new HBox(10, fromDatePicker, fromHourChoice, new Label(":"), fromMinuteChoice), 1, 1);
        grid.add(new Label(i18n("activity.pause.to")), 0, 2);
        grid.add(new HBox(10, toDatePicker, toHourChoice, new Label(":"), toMinuteChoice), 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().lookupButton(saveButton).addEventFilter(ActionEvent.ACTION, event -> {
            LocalDateTime pauseStart = buildDateTime(
                fromDatePicker.getValue(),
                fromHourChoice.getSelectionModel().getSelectedItem(),
                fromMinuteChoice.getSelectionModel().getSelectedItem()
            );
            LocalDateTime pauseEnd = buildDateTime(
                toDatePicker.getValue(),
                toHourChoice.getSelectionModel().getSelectedItem(),
                toMinuteChoice.getSelectionModel().getSelectedItem()
            );
            if (pauseStart == null || pauseEnd == null) {
                showInfo(i18n("activity.validation.date", i18n("activity.pause.from")));
                event.consume();
                return;
            }
            if (!pauseStart.isBefore(pauseEnd)) {
                showInfo(i18n("activity.pause.validation.range"));
                event.consume();
                return;
            }
            if (pauseStart.isBefore(activityFrom) || pauseEnd.isAfter(activityTo)) {
                showInfo(i18n("activity.pause.validation.within"));
                event.consume();
                return;
            }
        });

        dialog.setResultConverter(button -> {
            if (button != saveButton) {
                return null;
            }
            LocalDateTime pauseStart = buildDateTime(
                fromDatePicker.getValue(),
                fromHourChoice.getSelectionModel().getSelectedItem(),
                fromMinuteChoice.getSelectionModel().getSelectedItem()
            );
            LocalDateTime pauseEnd = buildDateTime(
                toDatePicker.getValue(),
                toHourChoice.getSelectionModel().getSelectedItem(),
                toMinuteChoice.getSelectionModel().getSelectedItem()
            );
            if (pauseStart == null || pauseEnd == null || !pauseStart.isBefore(pauseEnd)
                || pauseStart.isBefore(activityFrom) || pauseEnd.isAfter(activityTo)) {
                return null;
            }
            return new LocalDateTime[] { pauseStart, pauseEnd };
        });

        dialog.setOnShown(e -> {
            dialog.getDialogPane().getScene().addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
                if (ev.getCode() == KeyCode.ESCAPE) {
                    dialog.setResult(null);
                    dialog.close();
                    ev.consume();
                }
            });
        });

        return dialog.showAndWait()
            .filter(r -> r != null && r.length == 2);
    }

    private LocalDateTime buildDateTime(LocalDate date, Integer hour, Integer minute) {
        if (date == null || hour == null || minute == null) {
            return null;
        }
        return LocalDateTime.of(date, java.time.LocalTime.of(hour, minute));
    }

    private static LocalDateTime alignToGrid(LocalDateTime value, int timeGridMinutes) {
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

    private void selectMinute(ComboBox<Integer> box, int minute) {
        if (!box.getItems().contains(minute)) {
            box.getItems().add(minute);
            FXCollections.sort(box.getItems());
        }
        box.getSelectionModel().select(Integer.valueOf(minute));
    }

    private void showInfo(String message) {
        Alert alert = new Alert(AlertType.INFORMATION, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setOnShown(e -> {
            alert.getDialogPane().getScene().addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
                if (ev.getCode() == KeyCode.ESCAPE) {
                    alert.close();
                    ev.consume();
                }
            });
        });
        alert.showAndWait();
    }

    private String i18n(String key, Object... args) {
        String value;
        try {
            value = messages.getString(key);
        } catch (MissingResourceException e) {
            value = key;
        }
        if (args == null || args.length == 0) {
            return value;
        }
        return String.format(locale, value, args);
    }
}
