# File Operations

## Import CSV

**File → Import CSV…** – Imports activities from a CSV file. The file should have columns such as customer, project, task, start time, end time. Existing activities are not removed; new ones are added. Customer, project, and task must already exist in **Manage**.

## Export CSV

**File → Export CSV…** – Exports the **currently visible** activities (after applying filters and the active view) to a CSV file. You choose the file location.

## Write Timesheet

**File → Write Timesheet…** – Exports the visible activities into an **Excel** timesheet. You must:

1. Select a **customer** (via the main view filter or a view tab that has a fixed customer).
2. The customer must have **timesheet settings** and an **Excel template** configured in Manage → Properties.

The application fills the template with the activity data (dates, times, tasks) and lets you save the resulting file. Useful for monthly or weekly timesheets per customer.
