package treimers.net.jtimesheet.service;

import java.util.List;

import treimers.net.jtimesheet.model.Customer;
import treimers.net.jtimesheet.model.Project;
import treimers.net.jtimesheet.model.Task;

/**
 * Rules for when the Add-Activity dialog must not be shown and a hint dialog must be shown instead.
 * <p>
 * See docs/Rules.md "Add-Activity-Ausnahmen".
 */
public final class AddActivityDialogRules {

    /** Reason why the Add-Activity dialog is blocked; use for hint message key. */
    public enum BlockedReason {
        /** Dialog can be shown. */
        NONE(null),
        /** No customers: show hint that customers must be added first. */
        NO_CUSTOMERS("activity.add.blocked.no.customers"),
        /** Customers exist but none has projects: show hint that projects are missing. */
        NO_PROJECTS("activity.add.blocked.no.projects"),
        /** Projects exist but none has tasks: show hint that tasks are missing. */
        NO_TASKS("activity.add.blocked.no.tasks");

        private final String messageKey;

        BlockedReason(String messageKey) {
            this.messageKey = messageKey;
        }

        /** Returns the i18n message key for the hint, or null for {@link #NONE}. */
        public String getMessageKey() {
            return messageKey;
        }
    }

    private AddActivityDialogRules() {}

    /**
     * Returns the reason why the Add-Activity dialog should not be shown (and a hint dialog instead).
     * If {@link BlockedReason#NONE}, the dialog can be shown.
     *
     * @param customers all customers (may be null or empty)
     * @return blocked reason
     */
    public static BlockedReason getBlockedReason(List<Customer> customers) {
        if (customers == null || customers.isEmpty()) {
            return BlockedReason.NO_CUSTOMERS;
        }
        boolean hasProject = false;
        boolean hasTask = false;
        for (Customer c : customers) {
            List<Project> projects = c.getProjects();
            if (projects != null && !projects.isEmpty()) {
                hasProject = true;
                for (Project p : projects) {
                    List<Task> tasks = p.getTasks();
                    if (tasks != null && !tasks.isEmpty()) {
                        hasTask = true;
                        break;
                    }
                }
                if (hasTask) {
                    break;
                }
            }
        }
        if (!hasProject) {
            return BlockedReason.NO_PROJECTS;
        }
        if (!hasTask) {
            return BlockedReason.NO_TASKS;
        }
        return BlockedReason.NONE;
    }
}
