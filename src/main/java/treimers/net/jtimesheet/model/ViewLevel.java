package treimers.net.jtimesheet.model;

/**
 * Defines how much of the hierarchy is fixed in a view tab.
 * In a view tab only subcategories (tasks) and time range can be filtered.
 */
public enum ViewLevel {
    /** Only customer is fixed; project and task can vary within that customer. */
    CUSTOMER,
    /** Customer and project are fixed; only task selection and time can be filtered. */
    CUSTOMER_PROJECT,
    /** Customer, project and task are fixed; only time range can be filtered. */
    CUSTOMER_PROJECT_TASK
}
