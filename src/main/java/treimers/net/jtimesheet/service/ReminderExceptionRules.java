package treimers.net.jtimesheet.service;

import java.time.LocalDateTime;
import java.util.List;

import treimers.net.jtimesheet.model.Activity;
import treimers.net.jtimesheet.model.Customer;

/**
 * Rules for when the reminder dialog must not be shown (exceptions).
 * <p>
 * See docs/Rules.md "Reminder-Ausnahmen".
 */
public final class ReminderExceptionRules {

    private ReminderExceptionRules() {}

    /**
     * Returns true if the reminder should be suppressed: there is an activity whose end is now or in the future,
     * or there are no customers (no customer/project/task data).
     *
     * @param activities all activities
     * @param customers  all customers (may be null or empty)
     * @param now        current time
     * @return true to suppress the reminder dialog
     */
    public static boolean shouldSuppressReminder(
            List<Activity> activities,
            List<Customer> customers,
            LocalDateTime now) {
        if (hasActivityEndingNowOrInFuture(activities, now)) {
            return true;
        }
        return hasNoCustomerProjectTask(customers);
    }

    /**
     * True if there is at least one activity whose end time is now or in the future (rule 2).
     */
    public static boolean hasActivityEndingNowOrInFuture(List<Activity> activities, LocalDateTime now) {
        if (activities == null) {
            return false;
        }
        for (Activity a : activities) {
            LocalDateTime to = Activity.parseStoredDateTime(a.getTo());
            if (to != null && !to.isBefore(now)) {
                return true;
            }
        }
        return false;
    }

    /**
     * True if there are no customers, or no customer has any project, or no project has any task (rule 3).
     */
    public static boolean hasNoCustomerProjectTask(List<Customer> customers) {
        if (customers == null || customers.isEmpty()) {
            return true;
        }
        boolean hasProject = false;
        boolean hasTask = false;
        for (Customer c : customers) {
            if (c.getProjects() != null && !c.getProjects().isEmpty()) {
                hasProject = true;
                for (var p : c.getProjects()) {
                    if (p.getTasks() != null && !p.getTasks().isEmpty()) {
                        hasTask = true;
                        break;
                    }
                }
                if (hasTask) {
                    break;
                }
            }
        }
        return !hasProject || !hasTask;
    }
}
