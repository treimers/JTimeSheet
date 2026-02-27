package treimers.net.jtimesheet.service;

import java.time.LocalDateTime;
import java.util.Arrays;

import treimers.net.jtimesheet.model.Customer;
import treimers.net.jtimesheet.model.Project;
import treimers.net.jtimesheet.model.Task;

/**
 * Result of reminder/add-activity suggestion: suggested time range, default customer/project/task,
 * and whether the current time is "blocked" (inside an activity that ends in the future).
 * <p>
 * When {@link #isBlockedForReminder()} is true: Reminder shows no dialog; Add Activity shows a dialog
 * with start = end = now (duration 0). In all other cases a range is given (since last activity or last hour).
 */
public final class ReminderSuggestion {

    public enum SuggestionType {
        /** Default range (now - 1h, now); when re-showing after interval, end is extended to new now. */
        DEFAULT_RANGE,
        /** Gap from last activity end to now; when re-showing after interval, end is not extended. */
        GAP
    }

    private final LocalDateTime[] range;
    private final String customerId;
    private final String projectId;
    private final String taskId;
    private final boolean blockedForReminder;
    private final LocalDateTime blockedUntil;
    private final SuggestionType suggestionType;

    private ReminderSuggestion(
            LocalDateTime[] range,
            String customerId,
            String projectId,
            String taskId,
            boolean blockedForReminder,
            LocalDateTime blockedUntil,
            SuggestionType suggestionType) {
        this.range = range != null ? Arrays.copyOf(range, range.length) : null;
        this.customerId = customerId;
        this.projectId = projectId;
        this.taskId = taskId;
        this.blockedForReminder = blockedForReminder;
        this.blockedUntil = blockedUntil;
        this.suggestionType = suggestionType;
    }

    /**
     * Suggestion when current time is inside an activity ending in the future: Reminder does nothing,
     * Add Activity shows dialog with start = end = now (duration 0 allowed).
     */
    public static ReminderSuggestion suggestNowOnly(
            LocalDateTime now,
            String customerId,
            String projectId,
            String taskId) {
        LocalDateTime[] r = new LocalDateTime[] { now, now };
        return new ReminderSuggestion(r, customerId, projectId, taskId, true, null, null);
    }

    public static ReminderSuggestion suggest(
            LocalDateTime from,
            LocalDateTime to,
            String customerId,
            String projectId,
            String taskId,
            SuggestionType type) {
        return new ReminderSuggestion(
                new LocalDateTime[] { from, to },
                customerId,
                projectId,
                taskId,
                false,
                null,
                type);
    }

    public LocalDateTime[] getRange() {
        return range == null ? null : Arrays.copyOf(range, range.length);
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getTaskId() {
        return taskId;
    }

    /** True when current time is in an activity ending in the future: no Reminder, Add Activity with start=end=now. */
    public boolean isBlockedForReminder() {
        return blockedForReminder;
    }

    /** @deprecated Use {@link #isBlockedForReminder()}. */
    @Deprecated
    public boolean isBlocked() {
        return blockedForReminder;
    }

    public LocalDateTime getBlockedUntil() {
        return blockedUntil;
    }

    public SuggestionType getSuggestionType() {
        return suggestionType;
    }

    /** Resolve customer from list; returns null if customerId is null or not found. */
    public Customer resolveCustomer(java.util.List<Customer> customers) {
        if (customerId == null || customers == null) {
            return null;
        }
        for (Customer c : customers) {
            if (customerId.equals(c.getId())) {
                return c;
            }
        }
        return null;
    }

    /** Resolve project for the given customer; returns null if not found. */
    public Project resolveProject(Customer customer) {
        if (projectId == null || customer == null) {
            return null;
        }
        for (Project p : customer.getProjects()) {
            if (projectId.equals(p.getId())) {
                return p;
            }
        }
        return null;
    }

    /** Resolve task for the given project; returns null if not found. */
    public Task resolveTask(Project project) {
        if (taskId == null || project == null) {
            return null;
        }
        for (Task t : project.getTasks()) {
            if (taskId.equals(t.getId())) {
                return t;
            }
        }
        return null;
    }
}
