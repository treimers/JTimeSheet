package treimers.net.whathaveyoudone.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Activity {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final StringProperty customerName;
    private final StringProperty projectName;
    private final StringProperty taskName;
    private final StringProperty from;
    private final StringProperty to;
    private final StringProperty duration;

    public Activity(String customerName, String projectName, String taskName, String from, String to) {
        this.customerName = new SimpleStringProperty(customerName);
        this.projectName = new SimpleStringProperty(projectName);
        this.taskName = new SimpleStringProperty(taskName);
        this.from = new SimpleStringProperty(from);
        this.to = new SimpleStringProperty(to);
        this.duration = new SimpleStringProperty();
        updateDuration();
    }

    public String getCustomerName() {
        return customerName.get();
    }

    public void setCustomerName(String value) {
        customerName.set(value);
    }

    public String getProjectName() {
        return projectName.get();
    }

    public void setProjectName(String value) {
        projectName.set(value);
    }

    public String getTaskName() {
        return taskName.get();
    }

    public void setTaskName(String value) {
        taskName.set(value);
    }

    public String getFrom() {
        return from.get();
    }

    public void setFrom(String value) {
        from.set(value);
        updateDuration();
    }

    public String getTo() {
        return to.get();
    }

    public void setTo(String value) {
        to.set(value);
        updateDuration();
    }

    public StringProperty customerNameProperty() {
        return customerName;
    }

    public StringProperty projectNameProperty() {
        return projectName;
    }

    public StringProperty taskNameProperty() {
        return taskName;
    }

    public StringProperty fromProperty() {
        return from;
    }

    public StringProperty toProperty() {
        return to;
    }

    public StringProperty durationProperty() {
        return duration;
    }

    public static String formatDateTime(LocalDateTime value) {
        return FORMATTER.format(value);
    }

    public static LocalDateTime parseStoredDateTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), FORMATTER);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    public static String formatDuration(LocalDateTime from, LocalDateTime to) {
        Duration duration = Duration.between(from, to);
        long totalMinutes = duration.toMinutes();
        boolean negative = totalMinutes < 0;
        long minutes = Math.abs(totalMinutes);
        long hours = minutes / 60;
        long remainder = minutes % 60;
        String value = String.format("%02d:%02d", hours, remainder);
        return negative ? "-" + value : value;
    }

    private void updateDuration() {
        LocalDateTime fromDate = parseStoredDateTime(from.get());
        LocalDateTime toDate = parseStoredDateTime(to.get());
        if (fromDate == null || toDate == null) {
            duration.set("--:--");
            return;
        }
        duration.set(formatDuration(fromDate, toDate));
    }
}
