package treimers.net.whathaveyoudone.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Activity {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final StringProperty customerId;
    private final StringProperty projectId;
    private final StringProperty taskId;
    private final StringProperty from;
    private final StringProperty to;
    private final StringProperty duration;

    public Activity(String customerId, String projectId, String taskId, String from, String to) {
        this.customerId = new SimpleStringProperty(customerId);
        this.projectId = new SimpleStringProperty(projectId);
        this.taskId = new SimpleStringProperty(taskId);
        this.from = new SimpleStringProperty(from);
        this.to = new SimpleStringProperty(to);
        this.duration = new SimpleStringProperty();
        updateDuration();
    }

    public String getCustomerId() {
        return customerId.get();
    }

    public void setCustomerId(String value) {
        customerId.set(value);
    }

    public String getProjectId() {
        return projectId.get();
    }

    public void setProjectId(String value) {
        projectId.set(value);
    }

    public String getTaskId() {
        return taskId.get();
    }

    public void setTaskId(String value) {
        taskId.set(value);
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

    public StringProperty customerIdProperty() {
        return customerId;
    }

    public StringProperty projectIdProperty() {
        return projectId;
    }

    public StringProperty taskIdProperty() {
        return taskId;
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
