# File Operations

## Import CSV

**File → Import CSV…** – Imports activities from a CSV file. The file should have columns such as customer, project, task, start time, end time. Existing activities are not removed; new ones are added. Customer, project, and task must already exist in **Manage**.

## Export CSV

**File → Export CSV…** – Exports the **currently visible** activities (after applying filters and the active view) to a CSV file. You choose the file location.

## Write Timesheet

**File → Write Timesheet…** – Exports the visible activities into an **Excel** timesheet. You must:

1. Select a **customer** (via the main view filter or a view tab that has a fixed customer).
2. The customer must have **timesheet settings** in Manage → Properties: **sheet number**, **Excel template** (path to an existing .xls or .xlsx file), and optionally **timesheet file name** and **task separator**.

The application fills the template with the activity data (dates, times, pauses, tasks) and lets you save the resulting file. **Format:** The output file extension (.xls or .xlsx) always matches the template; the suggested file name uses the same extension.

**Template layout:** The layout (where month, start, end, pause, and tasks go) is discovered from placeholders in the template: **$month**, **$start**, **$end**, **$pause**, **$task**. These markers must appear in the first data row of the Excel template. If they are missing, an error is shown – there is no per-customer row/column configuration. Empty pauses are written as **0:00**. Formulas in the template are evaluated before saving.

![Excel template with placeholders](../timesheet-template-mockup.png)

**Errors:** If no template is set or the template file does not exist, an error message is shown (no dialog to pick a file). The template path must be set correctly in Manage and point to an existing file.
