package treimers.net.jtimesheet.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import treimers.net.jtimesheet.model.Activity;
import treimers.net.jtimesheet.model.AppSettings;
import treimers.net.jtimesheet.model.Customer;
import treimers.net.jtimesheet.model.Project;
import treimers.net.jtimesheet.model.Task;

/**
 * Pure logic for when to show a reminder and what to suggest (time range and default customer/project/task).
 * Used by reminder dialog and Add Activity; supports customer switch in dialog (last activity of that customer or first project/task).
 */
public final class ReminderSuggestionLogic {

    /**
     * Computes the suggestion for the reminder or Add Activity dialog.
     *
     * @param now current time
     * @param activities all activities
     * @param customers all customers
     * @param lastActivity last activity (for default customer and carry-over)
     * @param contextCustomerId customer selected in dialog, or null to use default (last activity's or first)
     * @param contextProjectId project selected in dialog, or null to use default
     * @param settings app settings (time grid)
     * @param fromReminder true when called from reminder (no reminder if last end in future); false for Add Activity (suggest last gap)
     * @param programStartTime program start time (rounded to time grid for default range start); null = use now as start
     * @return suggestion (blocked or range + defaults)
     */
    public ReminderSuggestion compute(
            LocalDateTime now,
            List<Activity> activities,
            List<Customer> customers,
            Activity lastActivity,
            String contextCustomerId,
            String contextProjectId,
            AppSettings settings,
            boolean fromReminder,
            LocalDateTime programStartTime) {
        int timeGridMinutes = AppSettings.normalizeTimeGridMinutes(settings.getTimeGridMinutes());
        LocalDateTime nowOnGrid = alignToTimeGrid(now, timeGridMinutes);
        LocalDateTime defaultStart = defaultStartForSuggestion(nowOnGrid, programStartTime, timeGridMinutes);
        LocalDate today = now.toLocalDate();

        String customerId = contextCustomerId != null ? contextCustomerId : getDefaultCustomerId(customers, lastActivity);
        Customer customer = findCustomer(customers, customerId);
        if (customer == null) {
            // Rules.md Szenario 1: Keine vergangenen Aktivitäten → Start=Programmstart (gerundet), Ende=jetzt
            Customer first = customers != null && !customers.isEmpty() ? customers.get(0) : null;
            if (first == null) {
                return ReminderSuggestion.suggest(
                        defaultStart,
                        nowOnGrid,
                        null,
                        null,
                        null,
                        ReminderSuggestion.SuggestionType.DEFAULT_RANGE);
            }
            Project firstProject = first.getProjects().isEmpty() ? null : first.getProjects().get(0);
            Task firstTask = firstProject == null || firstProject.getTasks().isEmpty()
                    ? null
                    : firstProject.getTasks().get(0);
            return ReminderSuggestion.suggest(
                    defaultStart,
                    nowOnGrid,
                    first.getId(),
                    firstProject != null ? firstProject.getId() : null,
                    firstTask != null ? firstTask.getId() : null,
                    ReminderSuggestion.SuggestionType.DEFAULT_RANGE);
        }

        String projectId = contextProjectId;
        // Aktivitäten des aktuellen Kunden/Projekts für Defaults und "inside activity"-Erkennung.
        List<Activity> todaysForContext = getTodaysActivities(activities, today, customerId, projectId);
        Activity containingNow = findActivityContainingNow(todaysForContext, now);
        // Startzeit des Vorschlags: immer Ende der letzten Aktivität aller Kunden (Rules.md Szenarien 3–5, 4 (Task)).
        List<Activity> todaysAll = getTodaysActivities(activities, today, null, null);
        LocalDateTime lastEndBeforeNow = findLastEndBeforeNow(todaysAll, now);

        // Carry-over: when same customer/project as last activity, use its end as start for next suggestion
        if (lastActivity != null && customerId.equals(lastActivity.getCustomerId())
                && (projectId == null || projectId.equals(lastActivity.getProjectId()))) {
            LocalDateTime lastTo = Activity.parseStoredDateTime(lastActivity.getTo());
            if (lastTo != null && lastTo.toLocalDate().equals(today) && !lastTo.isAfter(nowOnGrid)
                    && (lastEndBeforeNow == null || lastTo.isAfter(lastEndBeforeNow))) {
                lastEndBeforeNow = lastTo;
            }
        }

        Activity last = projectId != null
                ? findLastActivityForProject(activities, customerId, projectId)
                : findLastActivityForCustomer(activities, customerId);
        boolean lastEndInFuture = false;
        if (last != null) {
            LocalDateTime lastTo = Activity.parseStoredDateTime(last.getTo());
            if (lastTo != null && lastTo.toLocalDate().equals(today) && lastTo.isAfter(nowOnGrid)) {
                lastEndInFuture = true;
            }
        }

        if (lastEndInFuture) {
            // Last activity end is in the future → Reminder: no dialog (suggestNowOnly); Add Activity: dialog with now/now or gap
            Activity forDefaults = containingNow != null ? containingNow : last;
            String defaultProjectId = forDefaults != null ? forDefaults.getProjectId() : getFirstProjectId(customer);
            String defaultTaskId = defaultProjectId != null && forDefaults != null
                    ? forDefaults.getTaskId()
                    : (defaultProjectId != null ? getFirstTaskId(customer, defaultProjectId) : null);
            if (containingNow != null) {
                // User is inside an activity ending in the future → Reminder: nothing; Add Activity: start=end=now
                return ReminderSuggestion.suggestNowOnly(nowOnGrid, customerId, defaultProjectId, defaultTaskId);
            }
            // Rule 7: Add Activity – suggest the gap (free slot) before now if there is one
            LocalDateTime slotFrom = lastEndBeforeNow != null ? lastEndBeforeNow : today.atStartOfDay();
            slotFrom = alignToTimeGrid(slotFrom, timeGridMinutes);
            if (slotFrom.isBefore(nowOnGrid)) {
                LocalDateTime[] range = capEndTimeToNow(slotFrom, nowOnGrid, nowOnGrid);
                String pid = last != null ? last.getProjectId() : (projectId != null ? projectId : getFirstProjectId(customer));
                String tid = last != null ? last.getTaskId() : (pid != null ? getFirstTaskId(customer, pid) : null);
                return ReminderSuggestion.suggest(range[0], range[1], customerId, pid, tid, ReminderSuggestion.SuggestionType.GAP);
            }
            // No gap (e.g. activity starts at day start) → Reminder: nothing; Add Activity: start=end=now
            return ReminderSuggestion.suggestNowOnly(nowOnGrid, customerId, defaultProjectId, defaultTaskId);
        }

        if (lastEndBeforeNow != null && lastEndBeforeNow.isAfter(nowOnGrid)) {
            lastEndBeforeNow = null;
        }

        // No last end in the future → use last end (lastEndBeforeNow) as start, or Programmstart bis jetzt (Rules.md Szenario 2)
        LocalDateTime fromAligned = lastEndBeforeNow != null
                ? alignToTimeGrid(lastEndBeforeNow, timeGridMinutes)
                : null;
        boolean gapNonEmpty = fromAligned != null && fromAligned.isBefore(nowOnGrid);
        LocalDateTime from = gapNonEmpty ? fromAligned : defaultStart;
        LocalDateTime to = nowOnGrid;
        LocalDateTime[] range = capEndTimeToNow(from, to, nowOnGrid);

        String defaultProjectId = last != null ? last.getProjectId() : getFirstProjectId(customer);
        String defaultTaskId = last != null ? last.getTaskId() : (defaultProjectId != null ? getFirstTaskId(customer, defaultProjectId) : null);

        ReminderSuggestion.SuggestionType type = gapNonEmpty
                ? ReminderSuggestion.SuggestionType.GAP
                : ReminderSuggestion.SuggestionType.DEFAULT_RANGE;

        return ReminderSuggestion.suggest(range[0], range[1], customerId, defaultProjectId, defaultTaskId, type);
    }

    /**
     * Start time for the default suggestion range when there is no gap: program start (rounded to grid) or now if no program start given.
     * Result is never after nowOnGrid.
     */
    private static LocalDateTime defaultStartForSuggestion(
            LocalDateTime nowOnGrid, LocalDateTime programStartTime, int timeGridMinutes) {
        if (programStartTime == null) {
            return nowOnGrid;
        }
        LocalDateTime start = alignToTimeGrid(programStartTime, timeGridMinutes);
        return start.isAfter(nowOnGrid) ? nowOnGrid : start;
    }

    /** True if current time is within reminder window (weekday and time between start–end). Delegates to {@link ReminderWindowRules}. */
    public boolean isNowWithinReminderWindow(LocalDateTime now, AppSettings settings) {
        return ReminderWindowRules.isWithinWindow(now, settings);
    }

    private static String getDefaultCustomerId(List<Customer> customers, Activity lastActivity) {
        if (lastActivity != null && customers != null) {
            for (Customer c : customers) {
                if (c.getId().equals(lastActivity.getCustomerId())) {
                    return c.getId();
                }
            }
        }
        return (customers != null && !customers.isEmpty()) ? customers.get(0).getId() : null;
    }

    private static Customer findCustomer(List<Customer> customers, String id) {
        if (id == null || customers == null) {
            return null;
        }
        for (Customer c : customers) {
            if (id.equals(c.getId())) {
                return c;
            }
        }
        return null;
    }

    private static String getFirstProjectId(Customer customer) {
        if (customer == null || customer.getProjects().isEmpty()) {
            return null;
        }
        return customer.getProjects().get(0).getId();
    }

    private static String getFirstTaskId(Customer customer, String projectId) {
        if (customer == null || projectId == null) {
            return null;
        }
        for (Project p : customer.getProjects()) {
            if (projectId.equals(p.getId())) {
                return p.getTasks().isEmpty() ? null : p.getTasks().get(0).getId();
            }
        }
        return null;
    }

    private static List<Activity> getTodaysActivities(
            List<Activity> activities,
            LocalDate today,
            String customerId,
            String projectId) {
        List<Activity> result = new ArrayList<>();
        if (activities == null) {
            return result;
        }
        for (Activity a : activities) {
            LocalDateTime from = Activity.parseStoredDateTime(a.getFrom());
            if (from == null || !from.toLocalDate().equals(today)) {
                continue;
            }
            if (customerId != null && !customerId.equals(a.getCustomerId())) {
                continue;
            }
            if (projectId != null && !projectId.equals(a.getProjectId())) {
                continue;
            }
            result.add(a);
        }
        return result;
    }

    private static Activity findActivityContainingNow(List<Activity> todays, LocalDateTime now) {
        for (Activity a : todays) {
            LocalDateTime from = Activity.parseStoredDateTime(a.getFrom());
            LocalDateTime to = Activity.parseStoredDateTime(a.getTo());
            if (from == null || to == null) {
                continue;
            }
            if (!from.isAfter(now) && to.isAfter(now)) {
                return a;
            }
        }
        return null;
    }

    private static LocalDateTime findLastEndBeforeNow(List<Activity> todays, LocalDateTime now) {
        LocalDateTime last = null;
        for (Activity a : todays) {
            LocalDateTime to = Activity.parseStoredDateTime(a.getTo());
            if (to == null || to.isAfter(now)) {
                continue;
            }
            if (last == null || to.isAfter(last)) {
                last = to;
            }
        }
        return last;
    }

    private static Activity findLastActivityForCustomer(List<Activity> activities, String customerId) {
        if (customerId == null || activities == null) {
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

    private static Activity findLastActivityForProject(
            List<Activity> activities,
            String customerId,
            String projectId) {
        if (customerId == null || projectId == null || activities == null) {
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

    private static LocalDateTime[] capEndTimeToNow(LocalDateTime from, LocalDateTime to, LocalDateTime nowOnGrid) {
        if (!to.isAfter(nowOnGrid)) {
            return new LocalDateTime[] { from, to };
        }
        LocalDateTime cappedTo = nowOnGrid;
        LocalDateTime cappedFrom = from;
        if (!cappedFrom.isBefore(cappedTo)) {
            cappedFrom = cappedTo.minusHours(1);
            if (cappedFrom.toLocalDate().isBefore(cappedTo.toLocalDate())) {
                cappedFrom = cappedTo.toLocalDate().atStartOfDay();
            }
        }
        return new LocalDateTime[] { cappedFrom, cappedTo };
    }

    private static LocalDateTime alignToTimeGrid(LocalDateTime value, int timeGridMinutes) {
        int step = AppSettings.normalizeTimeGridMinutes(timeGridMinutes);
        if (step <= 1) {
            return value.withSecond(0).withNano(0);
        }
        int minute = value.getMinute();
        int rounded = (minute / step) * step;
        return value.withMinute(rounded).withSecond(0).withNano(0);
    }
}
